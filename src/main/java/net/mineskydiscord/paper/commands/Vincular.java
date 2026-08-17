package net.mineskydiscord.paper.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPermsProvider;
import net.minesky.api.MineSkyPlayer;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.mineskydiscord.MineSkyDiscord;
import net.mineskydiscord.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

public class Vincular implements CommandExecutor {
    public static ConcurrentHashMap<String, String> vinc = new ConcurrentHashMap<>();

    private final MineSkyDiscord plugin;

    public Vincular(MineSkyDiscord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, @NotNull String @NotNull [] args) {
        if (!(s instanceof Player)) {
            return true;
        }

        final Player p = (Player) s;
        final MineSkyPlayer msp = new MineSkyPlayer(p);

        String valor = plugin.getCache().getCachedStatus(p.getUniqueId());

        if (valor == null) {
            msp.sendErrorMessage("Seus dados ainda estão sendo carregados. Por favor, aguarde alguns segundos e tente novamente.");
            return true;
        }

        if (valor.equals("v")) {
            msp.sendErrorMessage("Sua conta já esta vinculada! Caso queira desvincular, utilize /desvincular aqui ou no Discord.");
            return true;
        }

        if (args.length == 0) {
            msp.sendErrorMessage("Sua conta não está vinculada! Para vincular, entre em nosso Discord: https://minesky.com.br/discord e utilize o comando /vincular em algum canal.");
            return true;
        }

        String codigo = args[0];
        if (!Vincular.vinc.containsKey(codigo)) {
            msp.sendErrorMessage("Este código não existe ou é inválido! Verifique corretamente o código que o Bot lhe enviou e coloque logo apos o comando '/vinculado'");
            return true;
        }

        String dcid = Vincular.vinc.get(codigo).trim();

        plugin.jda.retrieveUserById(dcid).queue((discordUser) -> {
            UpdatedData updateData = new UpdatedData();
            updateData.add("discord.status", "v");
            updateData.add("discord.tag", discordUser.getName());
            updateData.add("discord.id", dcid);

            PlayerDatabase.setPlayerData(p.getUniqueId().toString(), updateData, new SetOneCallback() {
                @Override
                public void onSetDone() {
                    plugin.getCache().updateCachedStatus(p.getUniqueId(), "v");

                    p.sendMessage(Utils.c("&a✔ Sua conta foi vinculada com sucesso!"));

                    discordUser.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessage("Sua conta de discord foi vinculada com o jogador: ``" + p.getDisplayName() + "``!"))
                            .queue();

                    try {
                        Guild g = plugin.jda.getGuildById("672661692395814933");
                        if (g != null) {
                            Role r = g.getRoleById("880921139457708042");

                            g.retrieveMemberById(dcid).queue(member -> {
                                if (member.isBoosting()) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " parent add booster");
                                    });
                                    p.sendMessage(Utils.c("\n&aObrigado por boostar nosso Discord! Sua Tag foi recebida e permanecerá conforme seu boost continua!\n "));
                                }

                                if (r != null) {
                                    g.addRoleToMember(member, r).queue();
                                }

                                LuckPermsProvider.get().getUserManager().loadUser(p.getUniqueId()).thenAccept(lpUser -> {
                                    if (lpUser != null) {
                                        plugin.getVipRolesListener().updateMemberVipRoles(lpUser, g, member);
                                    }
                                });

                                String nicktochange = discordUser.getName() + " [" + p.getName() + "]";
                                if (nicktochange.length() > 32) {
                                    member.modifyNickname("[" + p.getName() + "]").queue();
                                } else {
                                    member.modifyNickname(nicktochange).queue();
                                }
                            });
                        }

                        Vincular.vinc.remove(codigo);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MineSkyDiscord.l.info(Utils.c("&cNão foi possível setar o cargo 'Vinculado' para o usuário " + p.getName()));
                    }
                }

                @Override
                public void onSetError(ErrorType errorType) {
                    msp.sendErrorMessage("Um erro ocorreu ao setar: " + String.valueOf(errorType));
                }
            });
        });

        return true;
    }
}
