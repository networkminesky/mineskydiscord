package net.minesky.spigot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.minesky.MineSkyDiscord;
import net.minesky.api.MineSkyPlayer;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.api.database.ValueType;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.FindValueCallback;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.minesky.utils.Utils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class Vincular implements CommandExecutor {

    // codigo , discord-id
    public static HashMap<String, String> vinc = new HashMap<>();

    /*public static String getStatus(Player p) {
        return MSDiscord.data.getString(p.getUniqueId()+".discord.status");
    }*/

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

        if (!(s instanceof Player)) {
            return true;
        }

        Player p = (Player) s;
        MineSkyPlayer msp = new MineSkyPlayer(p);

        PlayerDatabase.getPlayerSpecificDataAsync(p.getUniqueId().toString(), ValueType.STRING, "discord.status", new FindValueCallback() {
            @Override
            public void onQueryDone(Document document, Object o, boolean b) {
                String valor = (String)o;

                if(valor != null && valor.equals("v")) {

                    msp.sendErrorMessage("Sua conta já esta vinculada! Caso queira desvincular, utilize /desvincular aqui ou no Discord.");

                    return;
                }

                if (args.length == 0) {
                    msp.sendErrorMessage("Sua conta não está vinculada! Para vincular, entre em nosso Discord: https://minesky.com.br/discord e utilize o comando /vincular em algum canal.");
                    return;
                }

                if(vinc.containsKey(args[0])) {
                    String dcid = vinc.get(args[0]).trim();

                    MineSkyDiscord.jdapubl.retrieveUserById(dcid).queue(a -> {

                        UpdatedData updateData = new UpdatedData();
                        updateData.add("discord.status", "v");
                        updateData.add("discord.tag", a.getName());
                        updateData.add("discord.id", dcid);

                        PlayerDatabase.setPlayerData(p.getUniqueId().toString(), updateData, new SetOneCallback() {
                            @Override
                            public void onSetDone() {
                                p.sendMessage(Utils.c("&a✔ Sua conta foi vinculada com sucesso!"));

                                MineSkyDiscord.jdapubl.retrieveUserById(dcid).complete().openPrivateChannel().complete().sendMessage("Sua conta de discord foi vinculada com o jogador: ``" + p.getDisplayName() + "``!").complete();

                                try {
                                    Guild g = MineSkyDiscord.jdapubl.getGuildById("672661692395814933");
                                    Role r = g.getRoleById("880921139457708042");
                                    Member m = g.retrieveMemberById(dcid).complete();

                                    if (m.isBoosting()) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " parent add booster");
                                        p.sendMessage(Utils.c("\n&aObrigado por boostar nosso Discord! Sua Tag foi recebida e permanecerá conforme seu boost continua!\n "));
                                    }

                                    g.addRoleToMember(m, r).queue();

                                    String nicktochange = p.getName() + " [" + p.getName() + "]";
                                    if (nicktochange.length() > 32) {
                                        m.modifyNickname("[" + p.getName() + "]").queue();
                                    } else {
                                        m.modifyNickname(nicktochange).queue();
                                    }
                                    vinc.remove(args[0]);

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    MineSkyDiscord.l.info(Utils.c("&cNão foi possível setar o cargo 'Vinculado' para o usuário " + p.getName()));
                                }
                            }

                            @Override
                            public void onSetError(ErrorType errorType) {
                                msp.sendErrorMessage("Um erro ocorreu ao setar: "+errorType);

                            }
                        });

                    });
                } else {

                    msp.sendErrorMessage("Este código não existe ou é inválido! Verifique corretamente o código que o Bot lhe enviou e coloque logo apos o comando '/vinculado'");

                }

            }

            @Override
            public void onQueryError(ErrorType errorType) {

                msp.sendErrorMessage("Um erro ocorreu: "+errorType);

            }
        });

        return false;
    }
}
