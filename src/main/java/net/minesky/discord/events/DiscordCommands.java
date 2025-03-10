package net.minesky.discord.events;

import com.mongodb.client.MongoCursor;
import kong.unirest.*;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.minesky.MineSkyDiscord;
import net.minesky.addons.charactercreation.assets.AssetCategory;
import net.minesky.addons.charactercreation.assets.AssetManager;
import net.minesky.addons.charactercreation.assets.CategoryManager;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.core.databridge.MineSkyDB;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.minesky.spigot.commands.Vincular;
import net.minesky.utils.EventManagement;
import net.minesky.utils.PIXUtils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordCommands extends ListenerAdapter {

    public static List<String> getAllCategoriesList() {
        return CategoryManager.getAllCategoriesList();
    }

    public static List<String> alreadyUsingWikiCmd = new ArrayList<>();

    static void unirestGetWikiResponse(User user, String userId, String label, InteractionHook hook) {
        if(alreadyUsingWikiCmd.contains(userId)) {
            hook.sendMessage("⌛ "+user.getAsMention()+", aguarde a sua pesquisa atual acabar!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        alreadyUsingWikiCmd.add(userId);

        CompletableFuture<HttpResponse<JsonNode>> req = Unirest.get("https://api.gitbook.com/v1/spaces/MEcG9gqUcmTgMafsBqBG/search/ask")
                .header("accept", "application/json")
                .queryString("query", label)
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> httpResponse) {
                        JsonNode node = httpResponse.getBody();
                        if(node.getObject().isEmpty()) {
                            hook.sendMessage(":x: Nada encontrado. Pergunte coisas relacionadas ao servidor e que estejam documentadas na Wiki.").complete();
                            alreadyUsingWikiCmd.remove(userId);
                            return;
                        }

                        JSONObject answer = node.getObject().getJSONObject("answer");
                        WebhookMessageCreateAction<Message> response = hook.sendMessage(":grey_question: "+label+"\n \n:mag_right: " + answer.getString("text")+"\n ");

                        JSONArray followupQuestions = answer.getJSONArray("followupQuestions");
                        int perguntaatual = 1;
                        for(String s : (List<String>)followupQuestions.toList()) {
                            response.addActionRow(
                                    Button.primary("wiki-pergunta"+perguntaatual, s)
                            );
                            perguntaatual++;
                        }

                        response.complete();
                        alreadyUsingWikiCmd.remove(userId);
                    }
                    @Override
                    public void failed(UnirestException exception) {
                        hook.sendMessage(":x: Um erro ocorreu, provavelmente nada foi encontrado.")
                                .complete();
                        alreadyUsingWikiCmd.remove(userId);
                    }
                    @Override
                    public void cancelled() {
                        hook.sendMessage(":x: Um erro ocorreu, provavelmente nada foi encontrado.")
                                .complete();
                        alreadyUsingWikiCmd.remove(userId);
                    }
                });

        new BukkitRunnable() {
            @Override
            public void run() {
                if(alreadyUsingWikiCmd.contains(userId)
                && !req.isDone()) {
                    /*alreadyUsingWikiCmd.remove(userId);
                    hook.sendMessage(":x: Um erro ocorreu, provavelmente nada foi encontrado.")
                            .complete();*/
                    req.cancel(true);
                }
            }
        }.runTaskLater(MineSkyDiscord.getInstance(), 680);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getButton().getId().contains("wiki-pergunta")) {
            String label = event.getButton().getLabel();
            InteractionHook hook = event.getHook();

            event.deferReply().queue();

            unirestGetWikiResponse(event.getUser(), event.getUser().getId(), label, hook);
        }

        if (event.getButton().getId().contains("comofuncionapix")) {
            event.deferReply().queue();

            event.getHook().sendMessage("Para efetuar o pagamento, você só precisa escanear o código QR acima com o aplicativo de seu banco. Lembre-se de verificar se o valor corresponde ao valor da compra que você está fazendo! Após efetuar a compra, envie o comprovante neste mesmo canal.")
                    .queue();
        }

        if (event.getButton().getId().contains("codigocopiacola")) {
            event.deferReply().queue();

            event.getHook().sendMessage(
                    PIXUtils.pixCodes.getOrDefault(event.getChannelId(), "Nenhum código Pix Copia e Cola encontrado, caso ache que isso é um erro, contate a staff.")
                    )
                    .queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {

        String cmd = event.getName().toLowerCase();
        Member member = event.getInteraction().getMember();
        Role equipe = member.getGuild().getRoleById("817588867234136075");

        if(!member.getRoles().contains(equipe)) return;

        if(cmd.equals("asset") && event.getFocusedOption().getName().equals("categoria")) {

            event.replyChoiceStrings(getAllCategoriesList())
                    .queue();

        }

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName().toLowerCase();
        InteractionHook hook = event.getHook();
        final Member member = event.getInteraction().getMember();

        switch (cmd) {
            case "ligar": {
                event.deferReply(true).queue();

                if (!EventManagement.hasPermission(member)) {
                    event.reply("❌ Você não tem permissão para usar este comando!").setEphemeral(true).queue();
                    return;
                }

                EventManagement.toggleServerState(event, "start", "🔵 Ligando o servidor...", "✅ Servidor ligado com sucesso!");
                break;
            }
            case "desligar": {
                event.deferReply(true).queue();

                if (!EventManagement.hasPermission(member)) {
                    event.reply("❌ Você não tem permissão para usar este comando!").setEphemeral(true).queue();
                    return;
                }

                EventManagement.toggleServerState(event, "stop", "🔴 Desligando o servidor...", "✅ Servidor desligado com sucesso!");
                break;
            }
            case "versão":
            case "ip": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("\uD83D\uDD17  **IP do Servidor**");
                eb.setDescription("\uD83D\uDDA5️ Apenas **Java Edition**\n> IP: ``jogar.minesky.com.br``\n> Versão: ``1.19.4``\n \n ");
                eb.addField("Dúvidas de como entrar?", "Confira nossa seção na wiki [clicando aqui](https://wiki.minesky.com.br/guias-rapidos/como-entrar)", false);
                eb.setThumbnail("https://minesky.com.br/img/logo.png");
                eb.setColor(new Color(0, 98, 255));
                eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");
                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }

            case "site": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("``📰``  **Site:**");
                eb.setDescription("Confira o site do servidor aqui:\n> https://minesky.com.br");
                eb.setThumbnail("https://minesky.com.br/img/logo.png");
                eb.setColor(new Color(0, 98, 255));
                eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");
                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }

            case "convite": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("<:minesky:902615387798126622> **Discord**");
                eb.setDescription("> https://minesky.com.br/discord");
                eb.setThumbnail("https://minesky.com.br/img/logo.png");
                eb.setColor(new Color(0, 98, 255));
                eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");
                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }

            case "loja": {
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("\uD83D\uDECD **Loja**");
                eb.setDescription("> https://loja.minesky.com.br/");
                eb.setThumbnail("https://cdn.craftingstore.net/rPPmDHlLQ1/376ae17f432d9518701b627e018673e5/mxpswwkbglaqq4tagl49.png");
                eb.setColor(new Color(0, 98, 255));
                eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");
                hook.sendMessageEmbeds(eb.build()).queue();
                break;
            }

            case "vincular": {
                event.deferReply(true).queue();

                Document query = new Document("discord.id", member.getId());

                MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator();

                // Iterar pelos resultados
                while (cursor.hasNext()) {
                    Document document = cursor.next();

                    hook.sendMessage("Seu discord já está vinculado a uma conta! Utilize /desvincular caso queira desvincular sua conta, o comando funciona tanto aqui no Discord como dentro do Minecraft.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if (Vincular.vinc.containsValue(member.getId())) {

                    String codigoatual = "Desconhecido";

                    for (String s : Vincular.vinc.keySet()) {
                        if (Vincular.vinc.get(s).equals(member.getId())) {
                            codigoatual = s;
                        }
                    }

                    hook.sendMessage("Você já esta vinculando sua conta! Seu código de vinculação é ``" + codigoatual + "``! Volte ate o servidor e digite ``/vincular " + codigoatual + "``")
                            .setEphemeral(true)
                            .queue();

                    return;

                }

                String cd = "" + (int) (Math.random() * ((9999 - 1000) + 1));

                Vincular.vinc.put(cd, member.getId());

                hook.sendMessage("Seu código de vinculação é ``" + cd + "``!\n \nEntre no servidor e digite ``/vincular " + cd + "``")
                        .setEphemeral(true)
                        .queue();

                break;
            }

            case "lançou": {
                event.deferReply().queue();

                hook.sendMessage("Não, o servidor ainda não lançou! ``DICA: ative as notificações de ping em seu discord para saber quando for lançar``").queue();

                break;
            }

            /*case "wiki": {
                event.deferReply().queue();

                String pergunta = event.getOption("pergunta").getAsString();

                unirestGetWikiResponse(event.getUser(), event.getUser().getId(), pergunta, hook);

                break;
            }*/

            case "criarpix": {
                event.deferReply(true).queue(); // Deferindo a resposta para aguardar a geração do QR Code

                if(!member.hasPermission(Permission.ADMINISTRATOR)) {
                    hook.sendMessage("Você não tem permissão para executar este comando.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                String valor = event.getOption("valor").getAsString();
                String codigoPix = PIXUtils.getCodigoPix(valor);

                PIXUtils.pixCodes.put(event.getChannelId(), codigoPix);

                PIXUtils.gerarQRCodeAsync(codigoPix, 300).thenAccept(arquivo -> {
                    if (arquivo == null) {
                        event.getHook().sendMessage("Erro ao gerar o QR Code.").queue();
                        return;
                    }

                    FileUpload fileUpload = FileUpload.fromData(arquivo);

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("Pix Gerado!")
                            .setDescription("Escaneie o QR Code para efetuar o pagamento.")
                            .setImage("attachment://qrcode_pix.png") // Referencia o arquivo anexado
                            .setColor(new Color(49,187,172)); // Verde

                    hook.sendMessage("Pix gerado com sucesso.")
                            .setEphemeral(true)
                            .complete();

                    event.getChannel().sendMessageEmbeds(embed.build())
                            .addFiles(fileUpload)
                            .addActionRow(
                                    Button.primary("comofuncionapix", "Como efetuar o pagamento?"),
                                    Button.success("codigocopiacola", "Pix Copia e Cola")
                            )
                            .queue();
                });

                break;
            }

            case "asset": {

                Role equipe = member.getGuild().getRoleById("817588867234136075");
                if(!member.getRoles().contains(equipe)) return;

                event.deferReply().queue();

                String id = event.getOption("id").getAsString();
                String nome = event.getOption("nome").getAsString();
                String category = event.getOption("categoria").getAsString();
                Message.Attachment asset = event.getOption("asset").getAsAttachment();
                Message.Attachment icon = event.getOption("icone").getAsAttachment();

                if (!getAllCategoriesList().contains(category)) {
                    hook.sendMessage("Essa categoria não existe!")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if(!asset.getFileExtension().toLowerCase().contains("png")
                || !icon.getFileExtension().toLowerCase().contains("png")) {
                    hook.sendMessage("Os arquivos precisam ser PNG!")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                hook.sendMessage("Baixando arquivos...")
                        .setEphemeral(true)
                        .queue();

                AssetCategory assetCategory = CategoryManager.getCategoryByIdOrName(category);

                asset.getProxy().downloadToFile(new File(assetCategory.file(), id+".png")).whenComplete((a,b) -> {
                    hook.editOriginal("Primeiro arquivo enviado").queue();
                    icon.getProxy().downloadToFile(new File(assetCategory.file(), "ICON_"+id+".png"))
                            .whenComplete((c, d) -> {
                                hook.editOriginal("Segundo arquivo enviado! Asset criado e registrado dentro do servidor.")
                                        .queue();
                                AssetManager.completelyRegisterAsset(id, nome, id, assetCategory);
                            });
                });

                break;
            }

            case "nickname": {
                event.deferReply().queue();

                Role r = event.getGuild().getRoleById("880921139457708042");

                if (!member.getRoles().contains(r)) {
                    EmbedBuilder eb = new EmbedBuilder();

                    eb.setTitle("❌️ Apenas jogadores com conta vinculada podem trocar o nickname no Discord! Saiba como vincular em nossa Wiki: https://wiki.minesky.com.br/vincular");

                    eb.setColor(new Color(212, 23, 23));
                    eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");

                    hook.sendMessageEmbeds(eb.build())
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                Document query = new Document("discord.id", member.getId());

                MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator();

                String msg = event.getOption("nickname").getAsString();
                String nick = "";

                if(!cursor.hasNext()) {
                    hook.sendMessage("Seu discord não esta vinculado. Vincule seu discord para poder alterar seu nickname.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                // Iterar pelos resultados
                while (cursor.hasNext()) {
                    Document document = cursor.next();

                    nick = document.getString("latest-nickname");

                    return;
                }

                String nicktochange = msg + " [" + nick + "]";
                if (nicktochange.length() > 32) {
                    try {
                        EmbedBuilder eb = new EmbedBuilder();

                        eb.setTitle("❌️ O Seu nickname escolhido é muito grande! Escolha um nickname menor.");

                        eb.setColor(new Color(212, 23, 23));
                        eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");

                        hook.sendMessageEmbeds(eb.build())
                                .setEphemeral(true)
                                .queue();

                        return;

                    } catch (Exception ignored) {}
                }

                try {
                    member.modifyNickname(nicktochange).queue();
                } catch(Exception ignored) {}

                try {
                    EmbedBuilder eb = new EmbedBuilder();

                    eb.setTitle("✔️ Seu Nickname no Discord foi alterado para ``" + msg + " [" + nick + "]`` ");
                    eb.setColor(new Color(30, 194, 27));
                    eb.setFooter("MineSky Network ©️ 2025", "https://i.imgur.com/FZsSpLK.png");

                    hook.sendMessageEmbeds(eb.build())
                            .queue();

                } catch (Exception ignored) {}

                break;
            }

            case "desvincular": {

                event.deferReply(true).queue();

                Document query = new Document("discord.id", member.getId());

                MongoCursor<Document> cursor = MineSkyDB.getPlayersCollection().find(query).iterator();

                String nick = "";

                if(!cursor.hasNext()) {
                    hook.sendMessage("Seu discord não esta vinculado. Vincule seu discord para poder alterar seu nickname.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                // Iterar pelos resultados
                while (cursor.hasNext()) {
                    Document document = cursor.next();

                    Document discordDocument = document.get("discord", Document.class);
                    String status = discordDocument.getString("status");

                    if(status == null || status.isEmpty() || status.equals("nv"))
                        return;

                    nick = document.getString("latest-nickname");
                    final String finalNick = nick;

                    UpdatedData d = new UpdatedData();
                    d.add("discord.status", "nv");
                    d.add("discord.id", "");

                    PlayerDatabase.setPlayerData(document.getString("uuid"), d, new SetOneCallback() {
                        @Override
                        public void onSetDone() {
                            Player p = Bukkit.getPlayer(finalNick);
                            if(p != null)
                                p.sendMessage("§cSua conta do Minecraft foi desvinculada com a sua conta do Discord com sucesso.");

                            Guild g = MineSkyDiscord.jdapubl.getGuildById("672661692395814933");
                            Role r = g.getRoleById("880921139457708042");

                            if(r != null) {
                                try {
                                    member.modifyNickname(member.getUser().getName()).queue();
                                    g.removeRoleFromMember(member, r).queue();
                                } catch(Exception ignored) {}
                            }

                            hook.sendMessage("Sua conta foi desvinculada com sucesso da conta de Minecraft: "+finalNick)
                                    .setEphemeral(true)
                                    .queue();
                        }

                        @Override
                        public void onSetError(ErrorType errorType) {

                        }
                    });

                    break;
                }
            }
        }
    }

}
