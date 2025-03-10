package net.minesky.spigot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.minesky.MineSkyDiscord;
import net.minesky.api.MineSkyPlayer;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.UpdatedData;
import net.minesky.core.databridge.callbacks.ErrorType;
import net.minesky.core.databridge.callbacks.FindOneCallback;
import net.minesky.core.databridge.callbacks.SetOneCallback;
import net.minesky.utils.Utils;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Desvincular implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

        if(!(s instanceof Player)) {
            return true;
        }

        Player p = (Player)s;
        MineSkyPlayer msp = new MineSkyPlayer(p);

        PlayerDatabase.getPlayerDataAsync(p.getUniqueId().toString(), new FindOneCallback() {
            @Override
            public void onQueryError(ErrorType errorType) {
                msp.sendErrorMessage("Um erro ocorreu: "+errorType);
            }

            @Override
            public void onQueryDone(Document document) {
                Document discordDocument = document.get("discord", Document.class);

                String vinculacao = discordDocument.getString("status");

                if(vinculacao == null || vinculacao.isEmpty() || vinculacao.equals("nv")) {
                    msp.sendErrorMessage("Você não tem sua conta vinculada, para vincular utilize: /vincular");
                    return;
                }

                String id = discordDocument.getString("id");
                if(id == null || id.isEmpty())
                    return;

                Guild g = MineSkyDiscord.jdapubl.getGuildById("672661692395814933");
                Role r = g.getRoleById("880921139457708042");
                g.retrieveMemberById(id).queue(m -> {
                    if(m != null && r != null) {
                        try {
                            m.modifyNickname(m.getUser().getName()).queue();
                            g.removeRoleFromMember(m, r).queue();
                        } catch(Exception ignored) {}
                    }
                });

                UpdatedData d = new UpdatedData();
                d.add("discord.status", "nv");
                d.add("discord.id", "");

                PlayerDatabase.setPlayerData(p.getUniqueId().toString(), d, new SetOneCallback() {
                    @Override
                    public void onSetDone() {
                        p.sendMessage(Utils.c("&aSua conta foi desvinculada com sucesso! Caso queira vincular novamente, utilize o comando /vincular"));
                    }

                    @Override
                    public void onSetError(ErrorType errorType) {
                        msp.sendErrorMessage("Um erro ocorreu ao setar: "+errorType);
                    }
                });
            }
        });

        return false;

    }
}
