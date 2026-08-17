package net.mineskydiscord.paper.listeners;

import net.mineskydiscord.MineSkyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerEvents implements Listener {
    private final MineSkyDiscord plugin;

    public PlayerEvents(MineSkyDiscord plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Bukkit.getAsyncScheduler().runNow(MineSkyDiscord.getInstance(), (task) -> {
            MineSkyDiscord.getInstance().getCache().loadPlayerProfile(uuid);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCache().invalidate(event.getPlayer().getUniqueId());
    }
}
