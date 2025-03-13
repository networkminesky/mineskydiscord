package net.minesky.utils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.minesky.MineSkyDiscord;
import okhttp3.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

public final class EventManagement extends JavaPlugin {
    private static final String PEBBLE_API_KEY = MineSkyDiscord.config.getString("event-management.pebble-api-key");
    private static final String SERVER_ID = MineSkyDiscord.config.getString("event-management.pebble-server-id");
    private static final String PEBBLE_API_URL = "https://panel.pebblehost.com/api/client/servers/" + SERVER_ID + "/power";

    private static final List<String> ALLOWED_ROLES = MineSkyDiscord.config.getStringList("event-management.allowed-roles");

    public static boolean hasPermission(Member member) {
        return member != null &&
                (member.getRoles().stream().anyMatch(role ->
                        ALLOWED_ROLES.contains(role.getId()))
                        || member.hasPermission(Permission.BAN_MEMBERS));
    }

    public static void toggleServerState(SlashCommandInteractionEvent event, String sinal, String mensagemInicial, String mensagemSucesso) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create("{\"signal\": \"" + sinal + "\"}", MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(PEBBLE_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + PEBBLE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        event.reply(mensagemInicial).queue();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                event.getHook().sendMessage("❌ Erro ao alterar o estado do servidor: " + e.getMessage()).queue();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    event.getHook().sendMessage(mensagemSucesso).queue();
                } else {
                    event.getHook().sendMessage("❌ Falha ao alterar o estado do servidor! Código: " + response.code()).queue();
                }
            }
        });
    }
}
