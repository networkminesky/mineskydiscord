package net.minesky.hooks; // Certifique-se de que o pacote bate com o da sua estrutura

import java.awt.Color;
import java.util.UUID;
import litebans.api.Entry;
import litebans.api.Events;
import litebans.api.Events.Listener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.mineskydiscord.MineSkyDiscord; // Certifique-se de usar o pacote correto da sua Main
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class LitebansHook {

    public static long logChannelId;

    public static void registerEvents() {
        logChannelId = MineSkyDiscord.config.getLong("litebans.log-channel", 801963253219065882L);

        Events.get().register(new Listener() {
            @Override
            public void entryAdded(Entry entry) {
                String uuidStr = entry.getUuid();
                OfflinePlayer punishedPlayer = null;

                if (uuidStr != null) {
                    try {
                        punishedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String nickname;
                if (punishedPlayer != null && punishedPlayer.getName() != null) {
                    nickname = punishedPlayer.getName() + " [" + uuidStr + "]";
                } else {
                    nickname = "IP / Desconhecido" + (uuidStr != null ? " [" + uuidStr + "]" : "");
                }

                LitebansHook.createEmbed(
                        entry.getType().toUpperCase(),
                        nickname,
                        entry.getExecutorName(),
                        entry.getReason(),
                        entry.getDurationString(),
                        entry.getServerOrigin() + ", afetado: " + entry.getServerScope(),
                        entry.isSilent()
                );
            }
        });
    }

    public static void createEmbed(String punishType, String playerInfo, String staffer, String reason, String duration, String server, boolean silent) {
        JDA jda = MineSkyDiscord.getInstance().jda;
        if (jda == null) return;

        boolean un = punishType.startsWith("UN");

        TextChannel channel = jda.getTextChannelById(logChannelId);

        if (channel != null) {
            EmbedBuilder emb = new EmbedBuilder()
                    .setTitle("🔨 Nova punição " + (un ? "REMOVIDA! ❌" : "APLICADA! ✅"))
                    .setDescription("Uma nova punição foi aplicada em um jogador.")
                    .addField("Punição", punishType, false)
                    .addField("Staffer", staffer, false)
                    .addField("Jogador punido", playerInfo, false)
                    .addField("Duração", duration, false)
                    .addField("Motivo", reason, false)
                    .addField("Servidor", server, false);

            if (silent) {
                emb.addField("🔇 **Silencioso**", "Essa punição foi marcada com ``-s``", false);
            }

            emb.setColor(un ? new Color(204, 209, 188) : new Color(209, 31, 31));
            emb.setFooter("Nenhuma prova é anexada aqui automaticamente, o Staffer deve responder a essa mensagem com a prova anexada, podendo ser vídeos ou imagens.");

            channel.sendMessageEmbeds(emb.build()).queue();
        }
    }
}