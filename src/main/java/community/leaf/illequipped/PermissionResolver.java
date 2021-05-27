/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.List;
import java.util.stream.Collectors;

public class PermissionResolver
{
    private final Node notify = new Node("notify");
    private final Node manage = new Node("manage");
    
    private final IllEquippedPlugin plugin;
    
    public PermissionResolver(IllEquippedPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    public Node notifications() { return notify; }
    
    public Node management() { return manage; }
    
    public class Node
    {
        private static final String PREFIX = "ill-equipped";
        
        private final String permissionNode;
        
        Node(String permission)
        {
            this.permissionNode = String.join(".", PREFIX, permission);
        }
        
        public String node() { return permissionNode; }
        
        public boolean allows(Permissible permissible)
        {
            return (permissible.hasPermission(permissionNode))
                || (permissible instanceof Player && plugin.authors().contains(((Player) permissible).getUniqueId()));
        }
        
        public List<? extends Player> onlinePlayersWithPermission()
        {
            return plugin.getServer().getOnlinePlayers().stream().filter(this::allows).collect(Collectors.toList());
        }
    }
}
