package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minesky.core.databridge.MineSkyDB;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.cache.DiscordCacheManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeUTF("Discord");
            String serializedComponent = GsonComponentSerializer.gson().serialize(p);
            dos.writeUTF(serializedComponent);

            carrier.sendPluginMessage(MineSkyDiscord.getInstance(), "minesky:proxy", baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
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