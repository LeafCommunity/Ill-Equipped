/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class EquipData
{
    private final Map<UUID, PlayerCounter> playerCountersByUuid = new HashMap<>();
    
    private long currentTick = 0;
    
    private final IllEquippedPlugin plugin;
    
    public EquipData(IllEquippedPlugin plugin) { this.plugin = plugin; }
    
    void tick()
    {
        currentTick += 1;
        if (playerCountersByUuid.isEmpty()) { return; }
        
        long expired = currentTick - plugin.config().getOrDefault(Config.RATE);
        Iterator<PlayerCounter> equips = playerCountersByUuid.values().iterator();
        
        while (equips.hasNext())
        {
            PlayerCounter counter = equips.next();
            counter.removeEquipsBeforeTick(expired);
            if (counter.isEmpty()) { equips.remove(); }
        }
    }
    
    public PlayerCounter counter(Player player)
    {
        return playerCountersByUuid.computeIfAbsent(player.getUniqueId(), PlayerCounter::new);
    }
    
    public void remove(Player player)
    {
        playerCountersByUuid.remove(player.getUniqueId());
    }
    
    public class PlayerCounter
    {
        private final NavigableMap<Long, TickCounter> equipCountersByTick = new TreeMap<>();
        
        private final UUID uuid;
        
        public PlayerCounter(UUID uuid) { this.uuid = uuid; }
    
        public UUID uuid() { return uuid; }
    
        public OfflinePlayer toOfflinePlayer() { return plugin.getServer().getOfflinePlayer(uuid); }
    
        public String name()
        {
            @NullOr String name = toOfflinePlayer().getName();
            return (name != null) ? name : "Player#" + uuid;
        }
    
        public void removeEquipsBeforeTick(long tick)
        {
            equipCountersByTick.headMap(tick).clear();
        }
    
        public TickCounter currentTick()
        {
            return equipCountersByTick.computeIfAbsent(currentTick, TickCounter::new);
        }
    
        public int totalEquips()
        {
            return equipCountersByTick.values().stream().mapToInt(tick -> tick.equips).sum();
        }
    
        public boolean hasAny(Status status)
        {
            return equipCountersByTick.values().stream().anyMatch(tick -> tick.has(status));
        }
    
        boolean isEmpty() { return equipCountersByTick.isEmpty(); }
    }
    
    public static class TickCounter
    {
        private @NullOr Set<Status> statuses = null;
        private int equips = 0;
        
        private final long tick;
        
        public TickCounter(long currentTick) { this.tick = currentTick; }
        
        public long tick() { return tick; }
        
        public int equips() { return equips; }
        
        public void increment() { equips += 1; }
        
        public boolean has(Status status)
        {
            return statuses != null && statuses.contains(status);
        }
        
        public void designate(Status status)
        {
            if (statuses == null) { statuses = EnumSet.of(status); }
            else { statuses.add(status); }
        }
    }
}
