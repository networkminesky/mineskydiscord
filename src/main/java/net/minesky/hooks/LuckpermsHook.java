package net.minesky.hooks;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.group.GroupCreateEvent;
import net.luckperms.api.event.log.LogNotifyEvent;
import net.luckperms.api.event.log.LogPublishEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import net.minesky.MineSkyDiscord;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.awt.*;
import java.util.function.Consumer;

public class LuckpermsHook {

    public static final String LOG_CHANNEL = MineSkyDiscord.config.getString("luckperms.log-channel", "---");

    public static LuckPerms luckpermsAPI;

    public static void setup() {

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckpermsAPI = provider.getProvider();
        }

        EventBus eventBus = luckpermsAPI.getEventBus();

        eventBus.subscribe(MineSkyDiscord.getInstance(), LogNotifyEvent.class, a -> {

            TextChannel channel = (TextChannel) MineSkyDiscord.jdapubl.getGuildChannelById(LOG_CHANNEL);
            if(channel == null) return;

            Action entry = a.getEntry();

            EmbedBuilder emb = new EmbedBuilder();
            emb.setTitle("Novo registro do Luckperms");
            emb.setDescription("Uma nova modificação foi detectada nas permissões do servidor.");

            emb.addField("\uD83D\uDC64 Origem (source)", entry.getSource().getName()+" - ``("+entry.getSource().getUniqueId()+")``", true);

            emb.addField("\uD83C\uDFAF Alvo (target)", entry.getTarget().getType()+" - "+entry.getTarget().getName(), true);

            emb.addField("\uD83D\uDD70", entry.getTimestamp().toString(), true);

            emb.addField("\uD83D\uDCDD Modificação", entry.getDescription(), false);

            emb.setColor(new Color(118,178,2));

            channel.sendMessageEmbeds(emb.build()).complete();

        });

    }




}
