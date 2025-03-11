package net.minesky.spigot.events;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minesky.MineSkyDiscord;
import net.minesky.hooks.LitebansHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class MessassingEvents implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            String formattedData;
            try {
                formattedData = msgin.readUTF();
            } catch (IOException e) {
                //e.printStackTrace();
                return;
            }
            if (subchannel.equals("redstonemeter")) {
                MineSkyDiscord.l.info("Recebido notficação do MineSkyTerrenos reportando um jogador acima do limite de redstone!");

                String[] data = in.readUTF().split(" ");
                UUID u = UUID.fromString(data[0].trim());
                String claimID = data[1];
                String counter = data[2];
                OfflinePlayer ps = Bukkit.getOfflinePlayer(u);

                Guild g = MineSkyDiscord.jdapubl.getGuildById("672661692395814933");
                TextChannel tx = g.getTextChannelById("1025861491267735624");

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Abuso excessivo de Redstone em Terrenos");
                eb.setColor(new Color(184, 0, 0));
                eb.setDescription("``" + new Date() + "``");
                eb.addField("Jogador", "Nick: " + ps.getName() + "\nUUID: " + u, false);
                eb.addField("Informações", "Terreno: " + claimID + "\nComando: ``/terreno visitar id " + claimID + "``", false);
                eb.addField("Ticks no terreno: " + counter, "O sistema de Redstone nos terrenos baseia-se em ticks.\nEste terreno excedeu o limite de 1000 ticks e teve a redstone desativada por 3 horas.\n \nUm membro da staff precisará averiguar, e se for um erro, re-ativar a redstone do terreno.", false);
                eb.setFooter("Realities Studios ©️ 2022", "https://i.imgur.com/FZsSpLK.png");

                tx.sendMessageEmbeds(eb.build())
                        .queue();
            }

        }

        if(channel.equals("minesky:proxy")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            String tudo = in.readUTF();

            if(subchannel.equals("litebansevent")) {
                //         byteData.writeUTF(punishType+"|"+playerInfo+"|"+staffer+"|"+reason+"|"+duration+"|"+server+"|"+silent);
                final String[] args = tudo.split("\\|");
                final String punishType = args[0];
                final String playerInfo = args[1];
                final String staffer = args[2];
                final String reason = args[3];
                final String duration = args[4];
                final String server = args[5];
                final String silent = args[6];

                MineSkyDiscord.l.info("[Debug] Recebido punição do Velocity! Enviando embeds...");

                LitebansHook.createEmbed(punishType, playerInfo, staffer, reason, duration, server, Boolean.parseBoolean(silent));
            }
        }
    }

}
