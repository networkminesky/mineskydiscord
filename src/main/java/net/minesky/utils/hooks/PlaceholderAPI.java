package net.minesky.utils.hooks;

import com.google.common.base.CharMatcher;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatColor;
import net.minesky.MineSkyDiscord;
import net.minesky.discord.LegacyEvents;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPI extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "minesky";
    }

    @Override
    public String getIdentifier() {
        return "mineskydiscord";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        /*if(params.equalsIgnoreCase("vinculado")) {
            try {
                return MSDiscord.data.getString(player.getUniqueId()+".discord.status").equalsIgnoreCase("v") ? "&a✔ Vinculado" : "&c❌ Não vinculado";
            } catch(Exception ex) {
                return "&c❌ Não vinculado";
            }
        }*/

        if(params.equalsIgnoreCase("tag")) {
            try {

                if(MineSkyDiscord.cache.contains(player.getUniqueId().toString())) {
                    String tag = MineSkyDiscord.cache.getString(player.getUniqueId()+".tag");
                    if(tag == null || tag.isEmpty()) return "§7...";

                    return "&f\uF821 "+ChatColor.of("#7289DA")+ MineSkyDiscord.cache.getString(player.getUniqueId()+".tag");
                }
                return "";

            } catch (Exception ex) {
                return "&cErro";
            }
        }

        if(params.equalsIgnoreCase("voice")) {

            if(LegacyEvents.invoice.containsKey(player.getUniqueId().toString())) {

                String nome = LegacyEvents.invoice.get(player.getUniqueId().toString());

                String tocheck = StringUtils.strip(nome);

                tocheck = tocheck.replace(" ","");

                tocheck = tocheck.replace("¹", " 1");
                tocheck = tocheck.replace("²", " 2");
                tocheck = tocheck.replace("³", " 3");

                String nov = CharMatcher.ascii().retainFrom(tocheck);

                return "\uD80C\uDCFD "+ChatColor.of("#7289DA")+nov;

            }

            try {

                if(MineSkyDiscord.cache.contains(player.getUniqueId().toString())) {
                    String tag = MineSkyDiscord.cache.getString(player.getUniqueId()+".tag");
                    if(tag == null || tag.isEmpty()) return "§7...";

                    return "&f\uF821 "+ChatColor.of("#7289DA")+ MineSkyDiscord.cache.getString(player.getUniqueId()+".tag");
                }
                return "";
            } catch (Exception ex) {
                return "";
            }
        }

        return null;
    }

}
