/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import com.codingforcookies.armorequip.ArmorListener;
import community.leaf.illequipped.listeners.AuthorListener;
import community.leaf.illequipped.listeners.EquipCounterListener;
import community.leaf.tasks.bukkit.BukkitTaskSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class IllEquippedPlugin extends JavaPlugin implements BukkitTaskSource
{
    private @NullOr Path directory;
    private @NullOr Path backups;
    private @NullOr Config config;
    private @NullOr Authors authors;
    private @NullOr EquipData equips;
    private @NullOr PermissionResolver permissions;
    
    @Override
    public void onEnable()
    {
        this.directory = getDataFolder().toPath();
        this.backups = directory.resolve("backups");
        this.config = new Config(this);
        this.authors = new Authors().load();
        this.equips = new EquipData(this);
        this.permissions = new PermissionResolver(this);
        
        PluginManager plugins = getServer().getPluginManager();
        
        plugins.registerEvents(new ArmorListener(List.of()), this);
        plugins.registerEvents(new AuthorListener(this), this);
        plugins.registerEvents(new EquipCounterListener(this), this);
        
        sync().every(1).ticks().forever().run(equips::tick);
    }
    
    private static <T> T initialized(@NullOr T thing, String name)
    {
        if (thing != null) { return thing; }
        throw new IllegalStateException(name + " is not initialized yet.");
    }
    
    @Override
    public Plugin plugin()
    {
        return this;
    }
    
    public Path directory()
    {
        return initialized(directory, "directory");
    }
    
    public Path backups()
    {
        return initialized(backups, "backups");
    }
    
    public Config config()
    {
        return initialized(config, "config");
    }
    
    public Authors authors()
    {
        return initialized(authors, "authors");
    }
    
    public EquipData equips()
    {
        return initialized(equips, "equips");
    }
    
    public PermissionResolver permissions()
    {
        return initialized(permissions, "permissions");
    }
    
    public static class Author
    {
        private final String name;
        private final UUID uuid;
        
        private Author(String name, UUID uuid)
        {
            this.name = name;
            this.uuid = uuid;
        }
    
        public String name() { return name; }
    
        public UUID uuid() { return uuid; }
    }
    
    public class Authors
    {
        private final Map<UUID, Author> authorsByUuid = new LinkedHashMap<>();
        
        private Authors load()
        {
            authorsByUuid.clear();
            
            @NullOr InputStream input = getResource("authors.yml");
            if (input == null) { return this; }
            
            try
            (
                InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader);
            )
            {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.loadFromString(reader.lines().collect(Collectors.joining("\n")));
                
                @NullOr ConfigurationSection section = yaml.getConfigurationSection("authors");
                if (section != null)
                {
                    for (String name : section.getKeys(false))
                    {
                        UUID uuid = UUID.fromString(section.getString(name, ""));
                        authorsByUuid.put(uuid, new Author(name, uuid));
                    }
                }
            }
            catch (IOException | InvalidConfigurationException | IllegalStateException e)
            {
                getLogger().log(Level.WARNING, "Unable to load authors", e);
            }
            
            return this;
        }
        
        public Collection<Author> all()
        {
            return Collections.unmodifiableCollection(authorsByUuid.values());
        }
        
        public boolean contains(UUID uuid)
        {
            return authorsByUuid.containsKey(uuid);
        }
    }
}
