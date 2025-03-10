package net.minesky.discord.events;

import com.mongodb.client.MongoCursor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.minesky.MineSkyDiscord;
import net.minesky.core.databridge.MineSkyDB;
import net.minesky.discord.registering.CommandRegistering;
import net.minesky.utils.SimpleCommand;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordMisc extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent e) {
        MineSkyDiscord.jdapubl = e.getJDA();
        MineSkyDiscord.l.info("§3[DISCORD] Módulo inicializado com sucesso, 'JDAPUBL' atualizado!");
        MineSkyDiscord.l.info("§3[DISCORD] Response number: " + e.getResponseNumber());

        Guild g = e.getJDA().getGuildById("672661692395814933");
        if (g != null) {
            for (SimpleCommand c : CommandRegistering.DISCORD_COMMANDS) {
                g.upsertCommand(c.getName(), c.getDescription())
                        .queue();
                MineSkyDiscord.l.info("§3[DISCORD] Registrando comando: "+c.getName());
            }

            g.upsertCommand(Commands.slash("asset", "Adiciona um asset")
                    .addOption(OptionType.STRING, "id", "ID. Sem maiúsculo e sem espaços, com traços", true)
                    .addOption(OptionType.STRING, "nome", "Nome visível do asset", true)
                    .addOption(OptionType.STRING, "categoria", "Categoria do asset", true, true)
                    .addOption(OptionType.ATTACHMENT, "asset", "Imagem do asset", true)
                    .addOption(OptionType.ATTACHMENT, "icone", "Imagem do ícone do asset", true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
            ).queue();

            g.upsertCommand(Commands.slash("criarpix", "Criar um QR Code pix")
                    .addOption(OptionType.STRING, "valor", "Valor do pagamento", true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            ).queue();

            /*g.upsertCommand(Commands.slash("wiki", "Faz uma pesquisa na Wiki e responde a sua pergunta")
                    .addOption(OptionType.STRING, "pergunta", "Faça a sua pergunta sobre algo relacionado ao servidor", true)
            ).queue();*/

            g.upsertCommand(Commands.slash("nickname", "Altera seu apelido no discord")
                    .addOption(OptionType.STRING, "nickname", "Insira seu novo Nickname", true))
                            .queue();

            g.updateCommands().queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

        Message m = null;
        String msg = null;
        String[] args;

        try {
            m = e.getMessage();
            msg = e.getMessage().getContentRaw();
            args = msg.toLowerCase().split(" ");
        } catch (Exception ex) {
            return;
        }

        if (m.getType() != MessageType.DEFAULT) return;

        if (e.getAuthor().isBot())
            return;

    }

    @Override
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent e) {

        Member m = e.getMember();
        Guild g = e.getGuild();
        if (!g.getId().equals("672661692395814933")) return;

        Document query = new Document("discord.id", m.getId());

        MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator();
        if(!cursor.hasNext())
            return;

        while (cursor.hasNext()) {
            Document document = cursor.next();

            Document discordDocument = document.get("discord", Document.class);
            String status = discordDocument.getString("status");

            String uuid = document.getString("uuid");

            if(!m.isBoosting())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user "+uuid+" parent remove booster");
            else {
                if(status == null || status.isEmpty() || status.equals("nv"))
                    return;

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user "+uuid+" parent add booster");

                Player p = Bukkit.getPlayer(uuid);
                if(p != null) {
                    p.sendMessage("§aObrigado por Boostar nosso Discord! Sua tag foi recebida e irá permanecer até você parar de boostar nosso Discord.");
                }

            }
        }
    }

}