package net.mineskydiscord.discord.events;

import com.mongodb.client.MongoCursor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.mineskydiscord.MineSkyDiscord;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.core.databridge.MineSkyDB;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.mineskydiscord.paper.commands.Vincular;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName().toLowerCase();
        final InteractionHook hook = event.getHook();
        final Member member = event.getInteraction().getMember();

        if (member == null) return;

        switch(cmd) {
            case "versao":
            case "ip": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("\ud83d\udd17 **IP do Servidor**")
                        .addField(":desktop: Java Edition (Computadores)", "> **IP:** minesky.com.br\n> **Versão:** 26.1.2", false)
                        .addField(":mobile_phone: Bedrock (Celular & Console)", "> **IP:** minesky.com.br\n> **Porta:** 19132", false)
                        .addField("Dúvidas de como entrar?", "Confira nossa seção na wiki [clicando aqui](https://wiki.minesky.com.br/tutorial/guias-rapidos/como-entrar-no-servidor)", false)
                        .setThumbnail("https://minesky.com.br/logo-min.png")
                        .setColor(new Color(0, 98, 255))
                        .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "anunciar": {
                event.deferReply(true).queue();

                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.getHook().sendMessage("❌ Você não tem permissão para usar este comando.").queue();
                    return;
                }

                final String broadcast = event.getOption("broadcast").getAsString();
                Guild guild = event.getGuild();

                if (guild == null) {
                    event.getHook().sendMessage("❌ Este comando só pode ser usado em servidores.").queue();
                    return;
                }

                event.getHook().sendMessage("⏳ Carregando membros e iniciando o envio dos anúncios...").queue();

                guild.loadMembers().onSuccess(members -> {
                    List<Member> targetMembers = members.stream()
                            .filter(m -> !m.getUser().isBot())
                            .toList();

                    int total = targetMembers.size();
                    java.util.concurrent.atomic.AtomicInteger enviados = new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicInteger falhas = new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(0);

                    Bukkit.getAsyncScheduler().runAtFixedRate(MineSkyDiscord.getInstance(), (task) -> {
                        int currentIndex = index.getAndIncrement();

                        if (currentIndex >= total) {
                            task.cancel();
                            return;
                        }

                        Member target = targetMembers.get(currentIndex);

                        MessageEmbed embed = new EmbedBuilder()
                                .setTitle("Olá, " + target.getEffectiveName())
                                .setDescription(broadcast.replace("\\n", "\n"))
                                //.setThumbnail("https://minesky.com.br/logo-min.png")
                                .setImage("https://minesky.com.br/images/bg2.png")
                                .setColor(new Color(0, 98, 255))
                                .setFooter("MineSky SMP ・ minesky.com.br", "https://minesky.com.br/logo-min.png")
                                .build();

                        target.getUser().openPrivateChannel().queue(
                                privateChannel -> {
                                    privateChannel.sendMessageEmbeds(embed).queue(
                                            success -> enviados.incrementAndGet(),
                                            error -> falhas.incrementAndGet()
                                    );
                                },
                                error -> falhas.incrementAndGet()
                        );

                    }, 1, (long)(1.5 * 1000), java.util.concurrent.TimeUnit.MILLISECONDS);

                }).onError(error -> {
                    event.getHook().sendMessage("❌ Ocorreu um erro ao carregar os membros: " + error.getMessage()).queue();
                });

                break;
            }

            case "site": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("``\ud83d\udcf0``  **Site:**")
                        .setDescription("Confira o site do servidor aqui:\n> https://minesky.com.br")
                        .setThumbnail("https://minesky.com.br/logo-min.png")
                        .setColor(new Color(0, 98, 255))
                        .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "convite": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("<:minesky:902615387798126622> **Discord**")
                        .setDescription("> https://minesky.com.br/discord")
                        .setThumbnail("https://minesky.com.br/logo-min.png")
                        .setColor(new Color(0, 98, 255))
                        .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "loja": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("\ud83d\udecd **Loja**")
                        .setDescription("> https://loja.minesky.com.br/")
                        .setThumbnail("https://cdn.craftingstore.net/rPPmDHlLQ1/376ae17f432d9518701b627e018673e5/mxpswwkbglaqq4tagl49.png")
                        .setColor(new Color(0, 98, 255))
                        .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "vincular": {
                event.deferReply(true).queue();
                Document query = new Document("discord.id", member.getId());

                try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                    if (cursor.hasNext()) {
                        hook.sendMessage("Seu discord já está vinculado a uma conta! Utilize /desvincular caso queira desvincular sua conta, o comando funciona tanto aqui no Discord como dentro do Minecraft.").setEphemeral(true).queue();
                        return;
                    }
                }

                String existingCode = Vincular.vinc.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(member.getId()))
                        .map(java.util.Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (existingCode != null) {
                    hook.sendMessage("Você já está vinculando sua conta! Seu código de vinculação é ``" + existingCode + "``! Volte até o servidor e digite ``/vincular " + existingCode + "``").setEphemeral(true).queue();
                    return;
                }

                String generatedCode = String.valueOf((int) (Math.random() * 9000.0D) + 1000);
                Vincular.vinc.put(generatedCode, member.getId());
                hook.sendMessage("Seu código de vinculação é ``" + generatedCode + "``!\n \nEntre no servidor e digite ``/vincular " + generatedCode + "``").setEphemeral(true).queue();
                break;
            }
            case "nickname": {
                event.deferReply().queue();
                Role r = event.getGuild().getRoleById("880921139457708042");

                if (r == null || !member.getRoles().contains(r)) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("❌️ Apenas jogadores com conta vinculada podem trocar o nickname no Discord! Saiba como vincular em nossa Wiki: https://wiki.minesky.com.br/vincular")
                            .setColor(new Color(212, 23, 23))
                            .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                    hook.sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
                    return;
                }

                Document query = new Document("discord.id", member.getId());

                try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                    if (!cursor.hasNext()) {
                        hook.sendMessage("Seu discord não está vinculado. Vincule seu discord para poder alterar seu nickname.").setEphemeral(true).queue();
                        return;
                    }

                    Document document = cursor.next();
                    String mcNick = document.getString("latest-nickname");
                    String requestedNick = event.getOption("nickname").getAsString();
                    String nickToChange = requestedNick + " [" + mcNick + "]";

                    if (nickToChange.length() > 32) {
                        EmbedBuilder eb = new EmbedBuilder()
                                .setTitle("❌️ O Seu nickname escolhido é muito grande! Escolha um nickname menor.")
                                .setColor(new Color(212, 23, 23))
                                .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                        hook.sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
                        return;
                    }

                    member.modifyNickname(nickToChange).queue((success) -> {
                        EmbedBuilder ebSuccess = new EmbedBuilder()
                                .setTitle("✔️ Seu Nickname no Discord foi alterado para ``" + requestedNick + " [" + mcNick + "]`` ")
                                .setColor(new Color(30, 194, 27))
                                .setFooter("MineSky Server ©️ 2026", "https://i.imgur.com/FZsSpLK.png");

                        hook.sendMessageEmbeds(ebSuccess.build()).queue();
                    }, (error) -> {
                        hook.sendMessage("❌ Não foi possível alterar seu apelido no Discord (Verifique se o bot possui cargo superior ao seu).").setEphemeral(true).queue();
                    });
                }
                break;
            }
            case "desvincular": {
                event.deferReply(true).queue();
                Document query = new Document("discord.id", member.getId());

                try (MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator()) {
                    if (!cursor.hasNext()) {
                        hook.sendMessage("Seu discord não está vinculado.").setEphemeral(true).queue();
                        return;
                    }

                    Document document = cursor.next();
                    Document discordDocument = document.get("discord", Document.class);

                    if (discordDocument == null) {
                        hook.sendMessage("Seu discord não está vinculado.").setEphemeral(true).queue();
                        return;
                    }

                    String status = discordDocument.getString("status");
                    if (status == null || status.isEmpty() || status.equals("nv")) {
                        hook.sendMessage("Sua conta já está desvinculada!").setEphemeral(true).queue();
                        return;
                    }

                    String mcNick = document.getString("latest-nickname");
                    UpdatedData d = new UpdatedData();
                    d.add("discord.status", "nv");
                    d.add("discord.id", "");

                    PlayerDatabase.setPlayerData(document.getString("uuid"), d, new SetOneCallback() {
                        @Override
                        public void onSetDone() {
                            Player p = Bukkit.getPlayer(mcNick);
                            if (p != null) {
                                p.sendMessage("§cSua conta do Minecraft foi desvinculada com a sua conta do Discord com sucesso.");
                                MineSkyDiscord.getInstance().getCache().updateCachedStatus(p.getUniqueId(), "nv");
                            }

                            Guild g = MineSkyDiscord.getInstance().jda.getGuildById("672661692395814933");
                            if (g != null) {
                                Role r = g.getRoleById("880921139457708042");
                                if (r != null) {
                                    try {
                                        member.modifyNickname(member.getUser().getName()).queue();
                                        g.removeRoleFromMember(member, r).queue();
                                    } catch (Exception ignored) {
                                    }
                                }
                                MineSkyDiscord.getInstance().getVipRolesListener().removeAllVipRoles(g, member);
                            }

                            hook.sendMessage("Sua conta foi desvinculada com sucesso da conta de Minecraft: " + mcNick).setEphemeral(true).queue();
                        }

                        @Override
                        public void onSetError(ErrorType errorType) {
                            hook.sendMessage("Um erro ocorreu ao desvincular no banco de dados: " + errorType).setEphemeral(true).queue();
                        }
                    });
                }
                break;
            }
        }
    }
}