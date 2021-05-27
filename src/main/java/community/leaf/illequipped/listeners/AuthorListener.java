package community.leaf.illequipped.listeners;

import community.leaf.illequipped.IllEquippedPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthorListener implements Listener
{
    private final Map<UUID, PermissionAttachment> activeAttachmentsByUuid = new HashMap<>();
    
    private final IllEquippedPlugin plugin;
    
    public AuthorListener(IllEquippedPlugin plugin) { this.plugin = plugin; }
    
    private void removeExistingAttachment(UUID uuid)
    {
        @NullOr PermissionAttachment attachment = activeAttachmentsByUuid.remove(uuid);
        if (attachment != null) { attachment.remove(); }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAuthorJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!plugin.authors().contains(uuid)) { return; }
        
        removeExistingAttachment(uuid);
        
        PermissionAttachment attachment = player.addAttachment(plugin);
        activeAttachmentsByUuid.put(uuid, attachment);
        attachment.setPermission(plugin.permissions().management().node(), true);
    }
    
    @EventHandler
    public void onAuthorQuit(PlayerQuitEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.authors().contains(uuid)) { removeExistingAttachment(uuid); }
    }
}
