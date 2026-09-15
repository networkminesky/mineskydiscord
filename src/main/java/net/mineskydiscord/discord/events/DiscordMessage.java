package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minesky.core.databridge.MineSkyDB;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.cache.DiscordCacheManager;
import org.bson.Document;
import org.bukkit.Bukkit;

public class DiscordMessage extends ListenerAdapter {
    private static final String STAFF_CHANNEL_ID = "1549229512754077737";

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (!e.isFromGuild() || !e.getChannel().getId().equals(STAFF_CHANNEL_ID)) return;

        if (e.getAuthor().isBot() || e.isWebhookMessage()) return;

        String authorName = getAuthorName(e);
        String content = e.getMessage().getContentDisplay();

        if (!e.getMessage().getAttachments().isEmpty()) {
            StringBuilder attachments = new StringBuilder();
            for (var att : e.getMessage().getAttachments()) {
                attachments.append(" §8[").append(att.getFileName()).append("§8]");
            }
            content = content.isEmpty() ? attachments.toString().trim() : content + " " + attachments;
        }

        if (content.trim().isEmpty()) return;

        sendDiscordToStaff(authorName, content);
    }

    public static void sendDiscordToStaff(String authorName, String msg) {
        Component discordTag = Component.text("§9[Discord] ")
                .hoverEvent(HoverEvent.showText(Component.text("§7Mensagem enviada via Discord")));

        Component p = Component.text("§4[s] ")
                .append(discordTag)
                .append(Component.text("§c" + authorName))
                .append(Component.text("§8: "))
                .append(Component.text(msg, TextColor.fromHexString("#ffa8a8")));

        Bukkit.getConsoleSender().sendMessage(translateColorsToANSI(LegacyComponentSerializer.legacySection().serialize(p)));

        Bukkit.getOnlinePlayers().stream()
                .filter(bs -> bs.hasPermission("mineskycore.staffchat"))
                .forEach(bs -> bs.sendMessage(p));
    }

    private String getAuthorName(MessageReceivedEvent e) {
        String discordId = e.getAuthor().getId();
        String fallbackName = (e.getMember() != null) ? e.getMember().getEffectiveName() : e.getAuthor().getName();

        DiscordCacheManager discordCacheManager = MineSkyDiscord.getInstance().getCache();
        String mcNick = discordCacheManager.getCachedNicknameByDiscordId(discordId);

        if (mcNick == null) {
            Document query = new Document("discord.id", discordId);

            try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                if (cursor.hasNext()) {
                    Document document = cursor.next();
                    mcNick = document.getString("latest-nickname");

                    if (mcNick != null) {
                        discordCacheManager.updateCachedNicknameByDiscordId(discordId, mcNick);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return (mcNick != null && !mcNick.isEmpty()) ? mcNick : fallbackName;
    }

    public static String translateColorsToANSI(String message) {
        return message
                .replace("§0", "\u001B[30m")
                .replace("§1", "\u001B[34m")
                .replace("§2", "\u001B[32m")
                .replace("§3", "\u001B[36m")
                .replace("§4", "\u001B[31m")
                .replace("§5", "\u001B[35m")
                .replace("§6", "\u001B[33m")
                .replace("§7", "\u001B[37m")
                .replace("§8", "\u001B[90m")
                .replace("§9", "\u001B[94m")
                .replace("§a", "\u001B[92m")
                .replace("§b", "\u001B[96m")
                .replace("§c", "\u001B[91m")
                .replace("§d", "\u001B[95m")
                .replace("§e", "\u001B[93m")
                .replace("§f", "\u001B[97m")
                .replace("§r", "\u001B[0m")
                .replace("§n", "\u001B[4m")
                .replace("§l", "\u001B[1m")
                .replace("§o", "\u001B[3m");
    }
}