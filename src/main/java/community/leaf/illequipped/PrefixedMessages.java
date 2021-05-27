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
    
    public static ComponentBuilder warning()
    {
        return new ComponentBuilder()
            .color(ChatColor.RED).append("Ill")
            .color(ChatColor.DARK_RED).append("-")
            .color(ChatColor.RED).bold(true).append("Equipped")
            .color(ChatColor.RED).bold(false).append(": ")
            .reset();
    }
}
