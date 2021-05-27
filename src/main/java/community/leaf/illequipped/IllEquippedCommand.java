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
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.List;
import java.util.Set;

public class IllEquippedCommand implements CommandExecutor, TabExecutor
{
    private static final Set<String> USAGE = Set.of("?", "help");
    
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
        handleUsage(sender);
        return true;
    }
    
    @Override
    public @NullOr List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return null;
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
                .create();
        
        sender.spigot().sendMessage(usage);
    }
}
