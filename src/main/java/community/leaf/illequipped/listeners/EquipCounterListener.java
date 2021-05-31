/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped.listeners;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import community.leaf.illequipped.Config;
import community.leaf.illequipped.EquipData;
import community.leaf.illequipped.IllEquippedPlugin;
import community.leaf.illequipped.PrefixedMessages;
import community.leaf.illequipped.Status;
import community.leaf.illequipped.events.EquipExploitPunishEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class EquipCounterListener implements Listener
{
    private static final Set<Material> ARMOR;
    
    static
    {
        List<String> valid = List.of("SHIELD", "BOOTS", "LEGGINGS", "CHESTPLATE", "HELMET");
        Set<Material> matches = EnumSet.noneOf(Material.class);
        
        for (Material material : Material.values())
        {
            if (material.name().contains("LEGACY")) { continue; }
            
            for (String match : valid)
            {
                if (material.name().contains(match))
                {
                    matches.add(material);
                    break;
                }
            }
        }
        
        ARMOR = Collections.unmodifiableSet(matches);
    }
    
    private static boolean isArmor(@NullOr ItemStack stack)
    {
        return stack != null && ARMOR.contains(stack.getType());
    }
    
    private final IllEquippedPlugin plugin;
    
    public EquipCounterListener(IllEquippedPlugin plugin) { this.plugin = plugin; }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        plugin.equips().remove(event.getPlayer());
    }
    
    @EventHandler
    public void onPunish(EquipExploitPunishEvent event)
    {
        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        
        for (String command : plugin.config().getOrDefault(Config.PUNISHMENT_COMMANDS))
        {
            plugin.getServer().dispatchCommand(console, command.replace("%player%", event.getPlayer().getName()));
        }
    }
    
    private void evaluate(Cancellable event, Player player)
    {
        EquipData.PlayerCounter counter = plugin.equips().counter(player);
        
        counter.currentTick().increment();
        int total = counter.totalEquips();
        
        if (total > plugin.config().getOrDefault(Config.CANCEL_THRESHOLD))
        {
            if (!counter.hasAny(Status.CANCELLED))
            {
                plugin.getLogger().info(player.getName() + " equipped too fast.");
                
                if (plugin.config().getOrDefault(Config.LOG_IF_CANCELLED))
                {
                    plugin.caughtLog().log(Status.CANCELLED, player);
                }
            }
            
            event.setCancelled(true);
            counter.currentTick().designate(Status.CANCELLED);
        }
        
        if (total > plugin.config().getOrDefault(Config.PUNISH_THRESHOLD))
        {
            if (!counter.hasAny(Status.PUNISHED))
            {
                plugin.caughtLog().log(Status.PUNISHED, player);
                
                if (plugin.config().getOrDefault(Config.NOTIFY_STAFF_IF_PUNISHED))
                {
                    plugin.getLogger().info("Punishing " +  player.getName() + " for exploiting equips.");
                    
                    BaseComponent[] message =
                        PrefixedMessages.warning()
                            .event(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder().append("Equipped armor " + total + " times.").create()
                            ))
                            .append("Detected ")
                            .append(player.getName())
                                .color(ChatColor.RED)
                            .append(" attempting to ")
                                .color(ChatColor.WHITE)
                            .append("crash")
                                .underlined(true)
                            .append(" players")
                                .underlined(false)
                        .create();
                    
                    plugin.permissions().notifications().onlinePlayersWithPermission()
                        .forEach(p -> p.spigot().sendMessage(message));
                    
                    plugin.getServer().getConsoleSender().spigot().sendMessage(message);
                }
            }
            
            counter.currentTick().designate(Status.PUNISHED);
            plugin.getServer().getPluginManager().callEvent(new EquipExploitPunishEvent(player, counter));
        }
    }
    
    @EventHandler
    public void onShieldEquip(PlayerSwapHandItemsEvent event)
    {
        if (isArmor(event.getMainHandItem()) || isArmor(event.getOffHandItem()))
        {
            evaluate(event, event.getPlayer());
        }
    }
    
    @EventHandler
    public void onArmorEquip(ArmorEquipEvent event)
    {
        evaluate(event, event.getPlayer());
    }
}
