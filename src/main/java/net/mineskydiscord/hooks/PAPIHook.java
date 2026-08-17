package net.mineskydiscord.hooks;

import com.google.common.base.CharMatcher;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatColor;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.discord.events.DiscordVoice;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PAPIHook extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "mineskydiscord";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bruno C.";
    }

    @Override
    public @NotNull String getVersion() {
        return MineSkyDiscord.getInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        String discordColor = ChatColor.of("#7289DA").toString();

        switch (identifier.toLowerCase()) {
            case "tag": {
                String tag = MineSkyDiscord.getInstance().getCache().getCachedTag(uuid);

                if (tag != null && !tag.isEmpty()) {
                    return "&f༂ " + discordColor + tag;
                } else {
                    return "§7...";
                }
            }

            case "voice": {
                if (DiscordVoice.invoice.containsKey(uuid.toString())) {
                    String tag = DiscordVoice.invoice.get(uuid.toString());

                    String tocheck = StringUtils.strip(tag);
                    tocheck = tocheck.replace(" ", "");
                    tocheck = tocheck.replace("¹", " 1");
                    tocheck = tocheck.replace("²", " 2");
                    tocheck = tocheck.replace("³", " 3");

                    String asciiOnly = CharMatcher.ascii().retainFrom(tocheck);
                    return "\uD83C\uDFA7 " + discordColor + asciiOnly;
                }

                String tag = MineSkyDiscord.getInstance().getCache().getCachedTag(uuid);
                if (tag != null && !tag.isEmpty()) {
                    return "&f༂ " + discordColor + tag;
                } else {
                    return "";
                }
            }

            default:
                return "";
        }
    }
}