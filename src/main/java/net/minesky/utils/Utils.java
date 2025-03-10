package net.minesky.utils;

import net.minesky.MineSkyDiscord;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;

public class Utils {

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void saveCache() {
        File f = new File(MineSkyDiscord.getInstance().getDataFolder(), "cache.yml");
        try {
            MineSkyDiscord.cache.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MineSkyDiscord.cache = YamlConfiguration.loadConfiguration(f);
    }

    public static String stripAccents(final String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
