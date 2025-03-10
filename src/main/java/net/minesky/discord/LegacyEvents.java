package net.minesky.discord;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minesky.MineSkyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class LegacyEvents extends ListenerAdapter {

    public static HashMap<String, String> invoice = new HashMap<>();

    void runBukkitCommand(String s) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
            }
        }.runTask(MineSkyDiscord.getInstance());
    }
}
