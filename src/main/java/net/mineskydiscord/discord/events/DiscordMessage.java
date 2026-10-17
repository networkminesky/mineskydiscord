package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minesky.api.messaging.Messaging;
import net.minesky.core.databridge.MineSkyDB;
import net.minesky.core.messaging.MessageType;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.cache.DiscordCacheManager;
import org.bson.Document;

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
                attachments.append(" [").append(att.getFileName()).append("]");
            }
            content = content.isEmpty() ? attachments.toString().trim() : content + " " + attachments;
        }

        if (content.trim().isEmpty()) return;

        sendDiscordToStaff(authorName, content);
    }

    public static void sendDiscordToStaff(String authorName, String msg) {
        Component discordTag = Component.text("[Discord] ", NamedTextColor.BLUE)
                .hoverEvent(HoverEvent.showText(Component.text("Mensagem enviada via Discord", NamedTextColor.GRAY)));

        Component p = Component.text("[s] ", NamedTextColor.DARK_RED)
                .append(discordTag)
                .append(Component.text(authorName, NamedTextColor.RED))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(msg, TextColor.fromHexString("#ffa8a8")));

        String serializedComponent = GsonComponentSerializer.gson().serialize(p);

        Document payload = new Document("component", serializedComponent)
                .append("author", authorName);

        Messaging.publish("minesky:network", "discord-staff", MessageType.DOCUMENT, payload);
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
}