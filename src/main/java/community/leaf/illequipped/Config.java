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
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;

import java.util.List;

public class Config extends YamlDataFile
{
    public static final YamlValue<String> VERSION =
        YamlValue.ofString("ill-equipped.config.version").maybe();
    
    public static final DefaultYamlValue<Integer> CANCEL_THRESHOLD =
        YamlValue.ofInteger("ill-equipped.actions.cancel-event.threshold").defaults(15);
    
    public static final DefaultYamlValue<Boolean> LOG_IF_CANCELLED =
        YamlValue.ofBoolean("ill-equipped.actions.cancel-event.log").defaults(true);
    
    public static final DefaultYamlValue<Integer> PUNISH_THRESHOLD =
        YamlValue.ofInteger("ill-equipped.actions.punish.threshold").defaults(50);
    
    public static final DefaultYamlValue<Boolean> NOTIFY_STAFF_IF_PUNISHED =
        YamlValue.ofBoolean("ill-equipped.actions.punish.notify-staff").defaults(true);
    
    public static final DefaultYamlValue<List<String>> PUNISHMENT_COMMANDS =
        YamlValue.ofStringList("ill-equipped.actions.punish.punishment")
            .defaults(List.of("ban %player% Attempted to crash clients"));
    
    public static final DefaultYamlValue<Long> RATE =
        YamlValue.ofLong("ill-equipped.advanced.data-expiration-ticks").defaults(20L);
    
    @Aggregated.Result
    private static final List<YamlValue<?>> VALUES =
        Aggregates.list(Config.class, YamlValue.type(), Aggregates.matching().all());
    
    Config(IllEquippedPlugin plugin)
    {
        super(plugin.directory(), "config.yml");
        
        reloadsWith(() ->
        {
            if (isInvalid()) { return; }
            
            String configVersion = get(VERSION).orElse("");
            String pluginVersion = plugin.getDescription().getVersion();
            
            boolean isOutdated = !configVersion.equals(pluginVersion);
            
            if (isOutdated) { set(VERSION, pluginVersion); }
        
            headerFromResource("config.header.txt");
            defaultValues(VALUES);
            
            if (isOutdated) { migrateValues(VALUES, data()); }
            if (isUpdated()) { backupThenSave(plugin.backups(), "v" + configVersion); }
        });
    }
}
