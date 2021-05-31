/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaughtLog
{
    private static final String LOG_ENTRY_FORMAT = "[%s] %s: %s (%s) @ %s equips\n";
    
    private static final Pattern LOG_ENTRY_PATTERN = Pattern.compile(
        "\\[(?<timestamp>[^]]+)]\\s*(?<status>\\w+):\\s*(?<name>\\w+)\\s*\\((?<uuid>[^]]+)\\)\\s*@\\s(?<equips>\\d+)\\s*equips"
    );
    
    public record Entry(LocalDateTime timestamp, Status status, String name, UUID uuid, int equips) {}
    
    private final AtomicReference<@NullOr List<Entry>> cachedEntries = new AtomicReference<>();
    private final Deque<String> pendingLines = new ConcurrentLinkedDeque<>();
    private final Object lock = new Object();
    
    private final IllEquippedPlugin plugin;
    private final Path logFilePath;
    
    public CaughtLog(IllEquippedPlugin plugin)
    {
        this.plugin = plugin;
        this.logFilePath = plugin.directory().resolve("caught-players.log");
    }
    
    public void log(Status status, Player player)
    {
        if (status == Status.CANCELLED && !plugin.config().getOrDefault(Config.LOG_IF_CANCELLED)) { return; }
        
        EquipData.PlayerCounter counter = plugin.equips().counter(player);
        
        pendingLines.add(String.format(
            LOG_ENTRY_FORMAT, LocalDateTime.now(), status, counter.name(), counter.uuid(), counter.totalEquips()
        ));
        
        plugin.async().run(() ->
        {
            synchronized (lock)
            {
                List<String> lines = new ArrayList<>();
                while (!pendingLines.isEmpty()) { lines.add(pendingLines.pop()); }
                String appended = String.join("", lines);
                
                try
                {
                    if (!Files.isDirectory(plugin.directory())) { Files.createDirectories(plugin.directory()); }
                    Files.writeString(logFilePath, appended, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                    cachedEntries.set(null);
                }
                catch (IOException e)
                {
                    plugin.getLogger().log(Level.WARNING, "Unable to append to " + logFilePath, e);
                }
            }
        });
    }
    
    public void readLogEntries(Consumer<List<Entry>> reader)
    {
        @NullOr List<Entry> entries = cachedEntries.get();
        if (entries != null)
        {
            reader.accept(entries);
            return;
        }
        
        plugin.async().run(() ->
        {
            synchronized (lock)
            {
                try
                {
                    if (!Files.isRegularFile(logFilePath))
                    {
                        plugin.sync().run(() -> reader.accept(List.of()));
                        return;
                    }
                    
                    List<Entry> updatedEntries = new ArrayList<>();
                    
                    for (String line : Files.readAllLines(logFilePath))
                    {
                        Matcher matcher = LOG_ENTRY_PATTERN.matcher(line);
                        if (!matcher.matches()) { continue; }
                        
                        try
                        {
                            LocalDateTime timestamp = LocalDateTime.parse(matcher.group("timestamp"));
                            Status status = Status.valueOf(matcher.group("status"));
                            String name = matcher.group("name");
                            UUID uuid = UUID.fromString(matcher.group("uuid"));
                            int equips = Integer.parseInt(matcher.group("equips"));
                            
                            updatedEntries.add(new Entry(timestamp, status, name, uuid, equips));
                        }
                        catch (RuntimeException ignored) {}
                    }
                    
                    List<Entry> unmodifiable = Collections.unmodifiableList(updatedEntries);
                    
                    cachedEntries.set(unmodifiable);
                    plugin.sync().run(() -> reader.accept(unmodifiable));
                }
                catch (IOException e)
                {
                    plugin.getLogger().log(Level.WARNING, "Unable to read entries of " + logFilePath, e);
                }
            }
        });
    }
}
