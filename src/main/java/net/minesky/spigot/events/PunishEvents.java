package net.minesky.spigot.events;

import litebans.api.*;
import litebans.api.Events;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.minesky.MineSkyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.UUID;

public class PunishEvents {

    public static void registerEvents() {
        litebans.api.Events.get().register(new Events.Listener() {

            // verificando punições especificas do rpg
            @Override
            public void entryAdded(Entry entry) {
                OfflinePlayer punishedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.getUuid()));
                String nickname = punishedPlayer == null ? "Desconhecido [Bugado]" : punishedPlayer.getName() + " ["+entry.getUuid()+"]";

                createEmbed(entry.getType().toUpperCase(),
                        nickname, entry.getExecutorName(), entry.getReason(), entry.getDurationString(),
                        entry.getServerOrigin()+", afetado: "+entry.getServerScope(), entry.isSilent());

            }
        });
    }

    public static void createEmbed(String punishType, String playerInfo, String staffer, String reason, String duration,
                                  String server, boolean silent) {
        JDA jda = MineSkyDiscord.jdapubl;

        boolean un = punishType.startsWith("UN");

        GuildChannel c = jda.getGuildChannelById("801963253219065882");
        if(c == null) return;

        EmbedBuilder emb = new EmbedBuilder();
        emb.setTitle(":hammer:  Nova punição "+(un ? "REMOVIDA! :x:" : "APLICADA! :white_check_mark:"));
        emb.setDescription("Uma nova punição foi aplicada em um jogador.");
        emb.addField("Punição", punishType, false);
        emb.addField("Staffer", staffer, false);
        emb.addField("Jogador punido", playerInfo, false);
        emb.addField("Duração", duration, false);
        emb.addField("Motivo", reason, false);
        emb.addField("Servidor", server, false);
        if(silent)
            emb.addField(":mute: **Silencioso**", "Essa punição foi marcada com ``-s``", false);

        emb.setColor( (un ? new Color(204, 209, 188)
                : new Color(209, 31, 31)) );
        // cor verde/cinza caso a puniçao seja uma remoçao | vermelho pra puniçao normal

        emb.setFooter("Nenhuma prova é anexada aqui automáticamente, o Staffer deve responder a essa mensagem com a prova anexada, podendo ser vídeos ou imagens.");

        TextChannel ce = (TextChannel) c;

        ce.sendMessageEmbeds(emb.build()).complete();
    }


}
