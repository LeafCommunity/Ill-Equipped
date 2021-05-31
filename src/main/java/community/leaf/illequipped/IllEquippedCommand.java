/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.Aggregated;
import com.rezzedup.util.constants.types.TypeCapture;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IllEquippedCommand implements CommandExecutor, TabExecutor
{
    private static final Set<String> USAGE = Set.of("?", "help", "usage");
    
    private static final Set<String> RELOAD = Set.of("reload");
    
    private static final Set<String> LOGS = Set.of("log", "logs");
    
    private static final Set<String> TEST_PUNISHMENT = Set.of("test-punishment");
    
    @Aggregated.Result
    private static final List<String> ARGUMENTS =
        Aggregates.list(
            IllEquippedCommand.class,
            TypeCapture.type(String.class),
            Aggregates.matching().collections(true)
        );
    
    private final IllEquippedPlugin plugin;
    
    public IllEquippedCommand(IllEquippedPlugin plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        String choice = (args.length > 0) ? args[0].toLowerCase(Locale.ROOT) : "?";
        
        if (USAGE.contains(choice)) { handleUsage(sender); }
        else if (RELOAD.contains(choice)) { handleReload(sender); }
        else if (LOGS.contains(choice)) { handleLogsPage(sender, label, args); }
        else if (TEST_PUNISHMENT.contains(choice)) { handleTestPunishment(sender, label, args); }
        else
        {
            sender.spigot().sendMessage(new ComponentBuilder()
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " help"))
                .event(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder()
                        .append("Click for usage.")
                            .italic(true).color(ChatColor.BLUE)
                        .create()
                ))
                .append("Uhoh! ")
                    .bold(true).italic(true).color(ChatColor.RED)
                .append("Unknown argument: ")
                    .bold(false).italic(false).color(ChatColor.WHITE)
                .append(args[0])
                    .underlined(true).color(ChatColor.GRAY)
                .append(String.join(" ", args).replaceFirst("[^\\s]+", ""))
                    .underlined(false).italic(true).color(ChatColor.DARK_GRAY)
                .append("Click ")
                    .italic(false).color(ChatColor.BLUE)
                .append("here")
                    .underlined(true)
                .append(" for usage.")
                    .underlined(false)
                .create()
            );
        }
        
        return true;
    }
    
    @Override
    public @NullOr List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        List<String> suggestions = new ArrayList<>();
        String last = (args.length > 0) ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        
        if (args.length <= 1) { suggestions.addAll(ARGUMENTS); }
        
        if (!last.isEmpty()) { suggestions.removeIf(suggestion -> !suggestion.contains(last)); }
        suggestions.sort(String.CASE_INSENSITIVE_ORDER);
        return suggestions;
    }
    
    private void handleUsage(CommandSender sender)
    {
        ComponentBuilder authors = new ComponentBuilder().append("Plugin Authors:");
        
        for (IllEquippedPlugin.Author author : plugin.authors().all())
        {
            authors.reset().append("\n")
                .append("- ").color(ChatColor.YELLOW)
                .append(author.name()).color(ChatColor.GOLD);
        }
        
        BaseComponent[] usage =
            PrefixedMessages.info()
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, authors.create()))
                .append("v")
                    .color(ChatColor.GOLD)
                .append(plugin.getDescription().getVersion())
                    .italic(true)
                .append(" Usage:")
                    .color(ChatColor.GRAY).italic(false)
                .append("\n")
                .create();
        
        sender.spigot().sendMessage(usage);
    }
    
    private void handleReload(CommandSender sender)
    {
        plugin.config().reload();
        
        @NullOr Exception invalid = plugin.config().getInvalidReason();
        if (invalid != null)
        {
            sender.spigot().sendMessage(PrefixedMessages.warning()
                .append("An error occurred while reloading the config (check console): ")
                .append(invalid.getMessage())
                    .color(ChatColor.RED)
                .create()
            );
        }
        else
        {
            sender.spigot().sendMessage(PrefixedMessages.info()
                .append("Reloaded the config.")
                .create());
        }
    }
    
    private void handleTestPunishment(CommandSender sender, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage("Only players may execute this command.");
            return;
        }
        
        if (args.length >= 2 && "--confirm".equals(args[1]))
        {
            EquipData.TickCounter tick = plugin.equips().counter(player).currentTick();
            int threshold = plugin.config().getOrDefault(Config.PUNISH_THRESHOLD);
            
            while (tick.equips() <= threshold) { tick.increment(); }
            
            plugin.getServer().getPluginManager().callEvent(
                new PlayerSwapHandItemsEvent(player, new ItemStack(Material.SHIELD), new ItemStack(Material.AIR))
            );
            return;
        }
        
        player.spigot().sendMessage(PrefixedMessages.info()
            .append("Warning! ")
                .bold(true).color(ChatColor.RED)
            .append("By testing the punishment, all configured commands will be run against you. ")
                .reset()
            .append("If you accept this risk, ")
                .underlined(false)
            .append("click here")
                .italic(true)
                .underlined(true)
                .event(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder().append("WARNING: ")
                            .bold(true).color(ChatColor.RED)
                        .append("You will receive all configured punishments.")
                            .bold(false)
                        .append("\n")
                        .append("(For testing purposes)")
                            .italic(true).color(ChatColor.GRAY)
                        .create()
                ))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label +" test-punishment --confirm"))
            .append(".")
                .reset()
            .create()
        );
    }
    
    private static final int PAGE_SIZE = 6;
    
    private void handleLogsPage(CommandSender sender, String label, String[] args)
    {
        int pageArg = 0;
        
        if (args.length >= 2)
        {
            try { pageArg = Integer.parseInt(args[1]); }
            catch (NumberFormatException e)
            {
                sender.spigot().sendMessage(new ComponentBuilder()
                    .append("Uhoh! ")
                        .bold(true).italic(true).color(ChatColor.RED)
                    .append("Invalid page number: ")
                        .bold(false).italic(false).color(ChatColor.WHITE)
                    .append(args[2])
                        .underlined(true).color(ChatColor.GRAY)
                    .create()
                );
                return;
            }
        }
        
        int selectedIndexedPage = pageArg - 1;
        
        plugin.caughtLog().readLogEntries(entries ->
        {
            int totalEntries = entries.size();
            
            int totalIndexedPages = totalEntries / PAGE_SIZE;
            int currentIndexedPage =
                (totalIndexedPages >= selectedIndexedPage && selectedIndexedPage >= 0)
                    ? selectedIndexedPage
                    : totalIndexedPages;
            
            int totalPages = totalIndexedPages + ((totalEntries % PAGE_SIZE != 0) ? 1 : 0);
            int currentPage = currentIndexedPage + 1;
            
            boolean hasNextPage = currentPage < totalPages;
            boolean hasPrevPage = currentPage > 1;
            
            ComponentBuilder view = PrefixedMessages.info()
                .append("Log Page " + currentPage + "/" + totalPages)
                .append(" :: ")
                    .color(ChatColor.GRAY)
                .append("←");
            
            if (hasPrevPage)
            {
                int prevPage = currentPage - 1;
                
                view.color(ChatColor.GOLD)
                    .event(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder().append("View previous page: " + prevPage).create()
                    ))
                    .event(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/" + label + " logs " + prevPage
                    ));
            }
            else
            {
                view.color(ChatColor.DARK_GRAY)
                    .event(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder().append("No previous pages!").create()
                    ));
            }
            
            view.append(" ")
                .append("→");
            
            if (hasNextPage)
            {
                int nextPage = currentPage + 1;
                
                view.color(ChatColor.GOLD)
                    .event(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder().append("View next page: " + nextPage).create()
                    ))
                    .event(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/" + label + " logs " + nextPage
                    ));
            }
            else
            {
                view.color(ChatColor.DARK_GRAY)
                    .event(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder().append("No more pages!").create()
                    ));
            }
            
            List<CaughtLog.Entry> pageEntries =
                (entries.isEmpty())
                    ? entries
                    : entries.subList(
                        PAGE_SIZE * currentIndexedPage,
                        (currentIndexedPage == totalIndexedPages)
                            ? entries.size()
                            : PAGE_SIZE * currentPage
                    );
            
            if (pageEntries.isEmpty())
            {
                view.append("\n")
                        .reset()
                    .append("No log entries yet.")
                        .color(ChatColor.DARK_GRAY);
            }
            else
            {
                for (CaughtLog.Entry entry : pageEntries)
                {
                    ChatColor main = (entry.status() == Status.PUNISHED) ? ChatColor.RED : ChatColor.GRAY;
                    ChatColor accent = (entry.status() == Status.PUNISHED) ? ChatColor.DARK_RED : ChatColor.AQUA;
                    
                    String timestamp = entry.timestamp().toString()
                        .replace('T', '@')
                        .replaceAll("\\.\\d+$", "");
                    
                    view.append("\n")
                            .reset()
                        .append("[" + timestamp + "]")
                            .color(ChatColor.DARK_GRAY)
                        .append(" ")
                            .color(main)
                        .append(entry.status().name())
                        .append(" ")
                        .append(entry.name())
                            .underlined(true)
                            .event(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder()
                                    .append("UUID")
                                        .underlined(true)
                                    .append(": ")
                                        .underlined(false)
                                    .append(entry.uuid().toString())
                                    .append("\n")
                                    .append("Click to copy UUID.")
                                        .italic(true).color(ChatColor.GRAY)
                                    .create()
                            ))
                            .event(new ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                entry.uuid().toString()
                            ))
                        .append(" equips: ")
                            .underlined(false)
                            .event((HoverEvent) null)
                            .event((ClickEvent) null)
                        .append("" + entry.equips())
                            .color(accent);
                }
            }
            
            sender.spigot().sendMessage(view.create());
        });
    }
}
