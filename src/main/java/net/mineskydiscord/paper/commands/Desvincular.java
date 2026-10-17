package net.mineskydiscord.paper.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.FindOneCallback;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.utils.Utils;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Desvincular implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, @NotNull String @NotNull [] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Apenas jogadores podem executar este comando.");
            return true;
        }

        String cachedStatus = MineSkyDiscord.getInstance().getCache().getCachedStatus(p.getUniqueId());

        if (cachedStatus == null) {
            sendError(p, "Seus dados de vínculo ainda estão sendo carregados. Por favor, aguarde alguns segundos.");
            return true;
        }

        if (cachedStatus.isEmpty() || cachedStatus.equals("nv")) {
            sendError(p, "Você não tem sua conta vinculada, para vincular utilize: /vincular");
            return true;
        }

        PlayerDatabase.getPlayerDataAsync(p.getUniqueId().toString(), new FindOneCallback() {
            @Override
            public void onQueryDone(Document document) {
                if (document == null) {
                    sendError(p, "Não foi possível carregar os seus dados.");
                    return;
                }

                Document discordDocument = document.get("discord", Document.class);
                if (discordDocument == null) {
                    sendError(p, "Você não tem sua conta vinculada, para vincular utilize: /vincular");
                    return;
                }

                String vinculacao = discordDocument.getString("status");
                if (vinculacao != null && !vinculacao.isEmpty() && !vinculacao.equals("nv")) {
                    String id = discordDocument.getString("id");

                    if (id != null && !id.isEmpty()) {
                        Guild g = MineSkyDiscord.getInstance().jda.getGuildById("672661692395814933");

                        if (g != null) {
                            Role r = g.getRoleById("880921139457708042");

                            g.retrieveMemberById(id).queue((member) -> {
                                if (member != null) {
                                    try {
                                        member.modifyNickname(member.getUser().getName()).queue();

                                        if (r != null) {
                                            g.removeRoleFromMember(member, r).queue();
                                        }

                                        MineSkyDiscord.getInstance().getVipRolesListener().removeAllVipRoles(g, member);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }, (throwable) -> {
                            });
                        }

                        UpdatedData d = new UpdatedData();
                        d.add("discord.status", "nv");
                        d.add("discord.id", "");

                        PlayerDatabase.setPlayerData(p.getUniqueId().toString(), d, new SetOneCallback() {
                            @Override
                            public void onSetDone() {
                                MineSkyDiscord.getInstance().getCache().updateCachedStatus(p.getUniqueId(), "nv");
                                p.sendMessage(Utils.c("&aSua conta foi desvinculada com sucesso! Caso queira vincular novamente, utilize o comando /vincular"));
                            }

                            @Override
                            public void onSetError(ErrorType errorType) {
                                sendError(p, "Um erro ocorreu ao salvar os novos dados: " + errorType);
                            }
                        });
                    }
                } else {
                    sendError(p, "Você não tem sua conta vinculada, para vincular utilize: /vincular");
                }
            }

            @Override
            public void onQueryError(ErrorType errorType) {
                sendError(p, "Um erro ocorreu ao buscar seus dados: " + errorType);
            }
        });

        return true;
    }

    private void sendError(Player p, String message) {
        p.sendMessage(Utils.c("&c" + message));
    }
}