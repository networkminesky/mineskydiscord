package net.minesky.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minesky.MineSkyDiscord;
import net.minesky.core.databridge.MineSkyDB;
import org.bson.Document;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class DiscordVoice extends ListenerAdapter {

    public static HashMap<String, String> invoice = new HashMap<>();

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {

        Member member = e.getMember();

        new BukkitRunnable() {
            @Override
            public void run() {
                Document query = new Document("discord.id", member.getId());

                MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator();

                if(!cursor.hasNext())
                    return;

                while (cursor.hasNext()) {
                    Document document = cursor.next();

                    String uuid = document.getString("uuid");

                    if(e.getChannelJoined() == null) {
                        invoice.remove(uuid);
                        return;
                    }

                    invoice.put(uuid, e.getChannelJoined().getName());

                    return;
                }
            }
        }.runTaskAsynchronously(MineSkyDiscord.getInstance());


    }
}
