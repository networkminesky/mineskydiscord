package net.minesky;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minesky.discord.events.DiscordCommands;
import net.minesky.discord.events.DiscordMisc;
import net.minesky.discord.events.DiscordVoice;
import net.minesky.discord.registering.CommandRegistering;
import net.minesky.hooks.LitebansHook;
import net.minesky.hooks.LuckpermsHook;
import net.minesky.spigot.commands.Desvincular;
import net.minesky.spigot.commands.Vincular;
import net.minesky.spigot.events.MessassingEvents;
import net.minesky.spigot.events.SpigotPlayerEvents;
import net.minesky.hooks.PAPIHook;
import net.minesky.utils.SimpleCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MineSkyDiscord extends JavaPlugin {

    public static YamlConfiguration cache;

    private JDA jda = null;
    public static JDA jdapubl = null;
    public static MineSkyDiscord instance;
    public static Logger l = null;

    public static FileConfiguration config;

    public static MineSkyDiscord getInstance() {
        return instance;
    }

    private void build() throws LoginException {
        ArrayList<GatewayIntent> intents = new ArrayList<>();
        intents.add(GatewayIntent.GUILD_MEMBERS);
        intents.add(GatewayIntent.GUILD_MESSAGES);
        intents.add(GatewayIntent.GUILD_VOICE_STATES);
        intents.add(GatewayIntent.DIRECT_MESSAGES);
        intents.add(GatewayIntent.MESSAGE_CONTENT);
        jda = JDABuilder.createDefault(config.getString("token"))
                .setBulkDeleteSplittingEnabled(false)
                .addEventListeners(new DiscordMisc(), new DiscordCommands(), new DiscordVoice())
                .setEnableShutdownHook(true)
                .setStatus(OnlineStatus.ONLINE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(intents)
                .build();

        jdapubl = jda;
    }

    @Override
    public void onDisable() {
        if (jda != null) {
                jda.getEventManager().getRegisteredListeners().forEach(listener -> jda.getEventManager().unregister(listener));
                jda.shutdownNow();
        }
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        config = this.getConfig();

        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("ip", "Visualizar IP do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("versão", "Visualizar a versão atual do servidor"));

        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("site", "Visualizar o site oficial do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("convite", "Obter o link de convite do discord do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("loja", "Obter o link da loja do servidor"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("desvincular", "Desvincular sua conta do Minecraft"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("vincular", "Vincular sua conta com a do Minecraft"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("lançou", "O MineSky lançou?"));

        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("ligar", "Ligar servidor de eventos"));
        CommandRegistering.DISCORD_COMMANDS.add(new SimpleCommand("desligar", "Desligar servidor de eventos"));

        l = this.getLogger();
        instance = this;

        l.info("---------------------------");
        l.info("\n  ____  _                       _ \n" +
                " |  _ \\(_)___  ___ ___  _ __ __| |\n" +
                " | | | | / __|/ __/ _ \\| '__/ _` |\n" +
                " | |_| | \\__ \\ (_| (_) | | | (_| |\n" +
                " |____/|_|___/\\___\\___/|_|  \\__,_|\n" +
                "                                  ");
        l.info("---------------------------");
        l.info("Plugin criado por Drawn e feito exclusivamente para a MineSky Network!");

        l.info("§6[SPIGOT] Iniciando módulo spigot!");

        File f = new File(this.getDataFolder(), "cache.yml");
        cache = YamlConfiguration.loadConfiguration(f);
        if(!f.exists()) {
            try {
                l.info("Arquivo cache.yml criado");
                cache.save(f);
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }

        try {
            l.info("§3[DISCORD] Criando instância do bot no JDA!");
            build();
        } catch (LoginException e) {
            e.fillInStackTrace();
        }

        this.getCommand("vincular").setExecutor(new Vincular());
        this.getCommand("desvincular").setExecutor(new Desvincular());

        if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            l.info("§6[SPIGOT] Registrando placeholders do serviço: PlaceholderAPI");
            new PAPIHook().register();
        }

        if(getServer().getPluginManager().isPluginEnabled("LiteBans")) {
            l.info("§6[SPIGOT] Registrando eventos do LiteBans");
            LitebansHook.registerEvents();
        }

        if(getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            l.info("§6[SPIGOT] Registrando eventos do Luckperms");
            LuckpermsHook.setup();
        }

        getServer().getPluginManager().registerEvents(new SpigotPlayerEvents(), this);

        jda.getPresence().setStatus(OnlineStatus.valueOf(config.getString("bot-status")));

        l.info("§6[SPIGOT] Executando runnables do plugin");
        new BukkitRunnable() {
            int n = 1;
            @Override
            public void run() {

                if(n == 1) {
                    jda.getPresence().setActivity(Activity.competing("https://minesky.com.br/"));
                }
                if(n == 2) {
                    jda.getPresence().setActivity(Activity.listening("https://loja.minesky.com.br/"));
                }
                if(n == 3) {
                    jda.getPresence().setActivity(Activity.playing("https://wiki.minesky.com.br/"));
                }
                if(n == 4) {
                    jda.getPresence().setActivity(Activity.playing("Taking the hobbits to isengard"));
                }

                if(n >= 5) {
                    n = 1;
                }

                /*if(n == 1) {
                    String total = PlaceholderAPI.setPlaceholders(null, "%bungee_total%");
                    if(total.equals("%bungee_total%")) total = "??";
                    jda.getPresence().setActivity(Activity.playing(total + " onlines e "+config.getKeys(false).size() + " registrados!"));
                }
                if(n == 2) {
                    jda.getPresence().setActivity(Activity.playing("Criado com \uD83D\uDC99 pela Realities Studios™"));
                }
                if(n == 3) {
                    jda.getPresence().setActivity(Activity.watching("Vincule sua conta aqui!"));
                }
                if(n == 4) {
                    n = 1;
                    return;
                }*/

                n++;
            }
        }.runTaskTimer(this, 20, (config.getLong("activity-change-interval")*20));

        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new MessassingEvents());
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "minesky:proxy", new MessassingEvents());
    }
}
