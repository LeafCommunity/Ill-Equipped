/*
 * Copyright © 2021, RezzedUp <https://github.com/LeafCommunity/Ill-Equipped>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.leaf.illequipped;

import community.leaf.tasks.bukkit.BukkitTaskSource;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.nio.file.Path;

public class IllEquippedPlugin extends JavaPlugin implements BukkitTaskSource
{
    public static final String NOTIFICATION_PERMISSION = "ill-equipped.notify";
    
    public static final String MANAGE_PERMISSION = "ill-equipped.manage";
    
    private @NullOr Path directory;
    private @NullOr Path backups;
    private @NullOr Config config;
    private @NullOr EquipData equips;
    
    @Override
    public void onEnable()
    {
        this.directory = getDataFolder().toPath();
        this.backups = directory.resolve("backups");
        this.config = new Config(this);
        this.equips = new EquipData(this);
        
        getServer().getPluginManager().registerEvents(new EquipListener(this), this);
        
        sync().every(1).ticks().forever().run(equips::tick);
    }
    
    private <T> T initialized(@NullOr T thing, String name)
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
    
    public EquipData equips()
    {
        return initialized(equips, "equips");
    }
    
    public ComponentBuilder prefixed()
    {
        return new ComponentBuilder()
            .color(ChatColor.RED).append("Ill")
            .color(ChatColor.DARK_RED).append("-")
            .color(ChatColor.RED).bold(true).append("Equipped")
            .color(ChatColor.RED).bold(false).append(": ")
            .reset();
    }
}
