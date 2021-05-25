/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class EquipListener implements Listener
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
    
    public EquipListener(IllEquippedPlugin plugin) { this.plugin = plugin; }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        plugin.equips().remove(event.getPlayer());
    }
    
    private void evaluate(Cancellable event, Player player)
    {
        EquipData.PlayerCounter counter = plugin.equips().counter(player);
        
        counter.currentTick().increment();
        int total = counter.totalEquips();
        
        if (total > plugin.config().getOrDefault(Config.CANCEL_THRESHOLD))
        {
            if (!counter.hasAny(Status.CANCELLED) && plugin.config().getOrDefault(Config.LOG_IF_CANCELLED))
            {
                plugin.getLogger().info(player.getName() + " equipped too fast.");
            }
            
            event.setCancelled(true);
            counter.currentTick().designate(Status.CANCELLED);
        }
        
        if (total > plugin.config().getOrDefault(Config.PUNISH_THRESHOLD))
        {
            if (!counter.hasAny(Status.PUNISHED) && plugin.config().getOrDefault(Config.NOTIFY_STAFF_IF_PUNISHED))
            {
                plugin.getLogger().info("Punishing " +  player.getName() + " for exploiting equips.");
    
                BaseComponent[] message =
                    plugin.prefixed()
                        .event(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder().append("Equipped armor " + total + " times.").create()
                        ))
                        .append("Detected ")
                        .color(ChatColor.RED).append(player.getName())
                        .color(ChatColor.WHITE).append(" attempting to ")
                        .underlined(true).append("crash")
                        .underlined(false).append(" players")
                    .create();
                
                plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(IllEquippedPlugin.NOTIFICATION_PERMISSION))
                    .forEach(p -> p.spigot().sendMessage(message));
            }
            
            ConsoleCommandSender console = plugin.getServer().getConsoleSender();
            
            for (String command : plugin.config().getOrDefault(Config.PUNISHMENT_COMMANDS))
            {
                plugin.getServer().dispatchCommand(console, command.replace("%player%", player.getName()));
            }
            
            counter.currentTick().designate(Status.PUNISHED);
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
    public void onArmorEquip(InventoryMoveItemEvent event)
    {
        //event.get
    }
}
