package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.mineskydiscord.MineSkyDiscord;
import net.minesky.core.databridge.MineSkyDB;
import net.mineskydiscord.discord.registering.CommandRegistering;
import net.mineskydiscord.utils.SimpleCommand;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordMisc extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent e) {
        MineSkyDiscord.getInstance().jda = e.getJDA();

        MineSkyDiscord.l.info("[DISCORD] Módulo inicializado com sucesso!");
        MineSkyDiscord.l.info("[DISCORD] Response number: " + e.getResponseNumber());

        Guild g = e.getJDA().getGuildById("672661692395814933");
        if (g != null) {
            CommandListUpdateAction updateAction = g.updateCommands();

            for (SimpleCommand c : CommandRegistering.DISCORD_COMMANDS) {
                updateAction.addCommands(Commands.slash(c.getName(), c.getDescription()));
                MineSkyDiscord.l.info("[DISCORD] Registrando comando: " + c.getName());
            }

            updateAction.addCommands(
                    Commands.slash("asset", "Adiciona um asset")
                            .addOption(OptionType.STRING, "id", "ID. Sem maiúsculo e sem espaços, com traços", true)
                            .addOption(OptionType.STRING, "nome", "Nome visível do asset", true)
                            .addOption(OptionType.STRING, "categoria", "Categoria do asset", true, true)
                            .addOption(OptionType.ATTACHMENT, "asset", "Imagem do asset", true)
                            .addOption(OptionType.ATTACHMENT, "icone", "Imagem do ícone do asset", true)
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),

                    Commands.slash("nickname", "Altera seu apelido no discord")
                            .addOption(OptionType.STRING, "nickname", "Insira seu novo Nickname", true)
            );

            updateAction.queue();
        }
    }

    @Override
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent e) {
        Member m = e.getMember();
        Guild g = e.getGuild();

        if (g.getId().equals("672661692395814933")) {
            Document query = new Document("discord.id", m.getId());

            try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                while (cursor.hasNext()) {
                    Document document = cursor.next();
                    Document discordDocument = document.get("discord", Document.class);

                    if (discordDocument == null) continue;

                    String status = discordDocument.getString("status");
                    String uuidStr = document.getString("uuid");

                    if (m.isBoosting()) {
                        if (status == null || status.isEmpty() || status.equals("nv")) {
                            continue;
                        }

                        Bukkit.getGlobalRegionScheduler().execute(MineSkyDiscord.getInstance(), () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuidStr + " parent add booster");
                        });

                        try {
                            Player p = Bukkit.getPlayer(UUID.fromString(uuidStr));
                            if (p != null) {
                                p.sendMessage("§aObrigado por Boostar nosso Discord! Sua tag foi recebida e irá permanecer até você parar de boostar nosso Discord.");
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    } else {
                        Bukkit.getGlobalRegionScheduler().execute(MineSkyDiscord.getInstance(), () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + uuidStr + " parent remove booster");
                        });
                    }
                }
            }
        }
    }
}