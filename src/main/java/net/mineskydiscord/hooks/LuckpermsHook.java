package net.mineskydiscord.hooks;

import java.awt.Color;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.mineskydiscord.MineSkyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckpermsHook {

    public static long logChannelId;
    public static LuckPerms luckpermsAPI;

    public static void setup() {
        logChannelId = MineSkyDiscord.config.getLong("luckperms.log-channel", 1348099159743397948L);

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            MineSkyDiscord.l.warning("[LUCKPERMS] Não foi possível encontrar a API do LuckPerms. Hook desativado!");
            return;
        }

        luckpermsAPI = provider.getProvider();

        EventBus eventBus = luckpermsAPI.getEventBus();
        eventBus.subscribe(MineSkyDiscord.getInstance(), LogBroadcastEvent.class, (event) -> {
            if (MineSkyDiscord.getInstance().jda == null) return;

            TextChannel channel = MineSkyDiscord.getInstance().jda.getTextChannelById(logChannelId);
            if (channel != null) {
                Action entry = event.getEntry();

                EmbedBuilder emb = new EmbedBuilder()
                        .setTitle("🍀 Novo registro do Luckperms")
                        .setDescription("Uma nova modificação foi detectada nas permissões do servidor.")
                        .addField("👤 Origem (source)", entry.getSource().getName() + " - ``(" + entry.getSource().getUniqueId() + ")``", false)
                        .addField("🎯 Alvo (target)", entry.getTarget().getType() + " - " + entry.getTarget().getName(), false)
                        .addField("🕰️ Horário (servidor)", "<t:" + Instant.now().getEpochSecond() + ":f>", false)
                        .addField("📝 Modificação", entry.getDescription(), false)
                        .setColor(new Color(118, 178, 2));

                channel.sendMessageEmbeds(emb.build()).queue();
            }
        });
    }
}