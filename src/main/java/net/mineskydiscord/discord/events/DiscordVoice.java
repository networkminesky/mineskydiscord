package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.mineskydiscord.MineSkyDiscord;
import net.minesky.core.databridge.MineSkyDB;
import org.bson.Document;
import org.bukkit.Bukkit;

public class DiscordVoice extends ListenerAdapter {
    public static final ConcurrentHashMap<String, String> invoice = new ConcurrentHashMap<>();

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
        Member member = e.getMember();

        Bukkit.getAsyncScheduler().runNow(MineSkyDiscord.getInstance(), (task) -> {
            Document query = new Document("discord.id", member.getId());

            try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                if (cursor.hasNext()) {
                    Document document = cursor.next();
                    String uuid = document.getString("uuid");

                    if (uuid != null) {
                        if (e.getChannelJoined() == null) {
                            invoice.remove(uuid);
                        } else {
                            invoice.put(uuid, e.getChannelJoined().getName());
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}