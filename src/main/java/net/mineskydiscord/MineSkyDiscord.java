package net.mineskydiscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.mineskydiscord.cache.DiscordCacheManager;
import net.mineskydiscord.discord.events.DiscordCommands;
import net.mineskydiscord.discord.events.DiscordMisc;
import net.mineskydiscord.discord.events.DiscordVoice;
import net.mineskydiscord.discord.registering.CommandRegistering;
import net.mineskydiscord.hooks.LuckpermsHook;
import net.mineskydiscord.hooks.PAPIHook;
import net.mineskydiscord.hooks.VipDiscordRolesListener;
import net.mineskydiscord.paper.commands.Desvincular;
import net.mineskydiscord.paper.commands.Vincular;
import net.mineskydiscord.paper.listeners.PlayerEvents;
import net.mineskydiscord.utils.SimpleCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class MineSkyDiscord extends JavaPlugin {

    public JDA jda;
    public static Logger l;
    public static FileConfiguration config;

    private DiscordCacheManager cache;
    private VipDiscordRolesListener vipRolesListener;

    @Override
    public void onEnable() {
        l = this.getLogger();
        l.info("    __  ___    _                  _____    __               ____     _                                      __");
        l.info("   /  |/  /   (_)   ____   ___   / ___/   / /__   __  __   / __ \\   (_)   _____  _____  ____    _____  ____/ /");
        l.info("  / /|_/ /   / /   / __ \\ / _ \\  \\__ \\   / //_/  / / / /  / / / /  / /   / ___/ / ___/ / __ \\  / ___/ / __  / ");
        l.info(" / /  / /   / /   / / / //  __/ ___/ /  / ,<    / /_/ /  / /_/ /  / /   (__  ) / /__  / /_/ / / /    / /_/ /  ");
        l.info("/_/  /_/   /_/   /_/ /_/ \\___/ /____/  /_/|_|   \\__, /  /_____/  /_/   /____/  \\___/  \\____/ /_/     \\__,_/  ");
        l.info("                                               /____/");
        l.info(" ");
        this.saveDefaultConfig();
        config = this.getConfig();

        this.cache = new DiscordCacheManager();

        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("ip", "Visualizar IP do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("versao", "Visualizar a versão atual do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("site", "Visualizar o site oficial do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("convite", "Obter o link de convite do discord do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("loja", "Obter o link da loja do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("desvincular", "Desvincular sua conta do Minecraft"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("vincular", "Vincular sua conta com a do Minecraft"));

        this.getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);

        try {
            l.info("[DISCORD] Criando instância do bot no JDA!");
            this.build();
        } catch (LoginException e) {
            e.fillInStackTrace();
        }

        this.getCommand("vincular").setExecutor(new Vincular(this));
        this.getCommand("desvincular").setExecutor(new Desvincular());
        if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            l.info("[FOLIA] Registrando placeholders do serviço: PlaceholderAPI");
            (new PAPIHook()).register();
        }

        if (this.getServer().getPluginManager().isPluginEnabled("LiteBans")) {
            l.info("[FOLIA] Registrando eventos do LiteBans");
            net.minesky.hooks.LitebansHook.registerEvents();
        }

        if (this.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            l.info("[FOLIA] Registrando eventos do Luckperms");
            LuckpermsHook.setup();
            vipRolesListener = new VipDiscordRolesListener(this);
            vipRolesListener.register();
        }

        this.jda.getPresence().setStatus(OnlineStatus.valueOf(config.getString("bot-status")));
        l.info("[FOLIA] Executando runnables do plugin");

        List<Supplier<Activity>> activities = Arrays.asList(
                () -> Activity.competing("https://minesky.com.br/"),
                () -> Activity.listening("https://loja.minesky.com.br/"),
                () -> Activity.playing("https://wiki.minesky.com.br/"),
                () -> Activity.playing("Jogando junto com " + Bukkit.getOnlinePlayers().size() + " outros jogadores!")
        );

        AtomicInteger index = new AtomicInteger(0);

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
            int currentIdx = index.getAndUpdate(val -> (val + 1) % activities.size());

            Activity currentActivity = activities.get(currentIdx).get();
            this.jda.getPresence().setActivity(currentActivity);
        }, 20L, config.getLong("activity-change-interval") * 20L);
    }

    private void build() throws LoginException {
        this.jda = JDABuilder.createDefault(config.getString("token"))
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_MODERATION,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setBulkDeleteSplittingEnabled(false)
                .addEventListeners(new DiscordMisc(), new DiscordCommands(), new DiscordVoice())
                .setEnableShutdownHook(true)
                .setStatus(OnlineStatus.ONLINE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
    }

    @Override
    public void onDisable() {
        if (this.jda != null) {
            this.jda.shutdown();
        }

        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public DiscordCacheManager getCache() {
        return this.cache;
    }

    public VipDiscordRolesListener getVipRolesListener() {
        return this.vipRolesListener;
    }

    public static MineSkyDiscord getInstance() {
        return MineSkyDiscord.getPlugin(MineSkyDiscord.class);
    }
}
