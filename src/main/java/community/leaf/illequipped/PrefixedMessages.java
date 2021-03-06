/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class PrefixedMessages
{
    private PrefixedMessages() {}
    
    public static ComponentBuilder colors(ChatColor main, ChatColor accent)
    {
        return new ComponentBuilder()
            .append("Ill")
                .color(main).bold(true)
            .append("-")
                .color(accent).bold(false)
            .append("Equipped")
                .color(main)
            .append(":")
                .color(accent)
            .append(" ")
                .reset();
    }
    
    public static ComponentBuilder warning()
    {
        return colors(ChatColor.RED, ChatColor.DARK_RED);
    }
    
    public static ComponentBuilder info()
    {
        return colors(ChatColor.WHITE, ChatColor.GRAY);
    }
}
