package net.mineskydiscord.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import net.mineskydiscord.MineSkyDiscord;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.api.database.ValueType;
import net.minesky.core.databridge.callbacks.FindValueCallback;
import net.minesky.core.databridge.callbacks.ErrorType;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VipDiscordRolesListener {

    private final JavaPlugin plugin;

    private String guildId;
    private String roleIronId;
    private String roleGoldId;
    private String roleDiamondId;
    private String roleNetheriteId;
    private String roleRubiId;

    public VipDiscordRolesListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().warning("LuckPerms não encontrado! O sistema de cargos VIP no Discord não irá funcionar.");
            return;
        }

        this.guildId = MineSkyDiscord.config.getString("vip-roles.guild-id", "672661692395814933");
        this.roleIronId = MineSkyDiscord.config.getString("vip-roles.role-iron", "705951794907447366");
        this.roleGoldId = MineSkyDiscord.config.getString("vip-roles.role-gold", "705952419233660998");
        this.roleDiamondId = MineSkyDiscord.config.getString("vip-roles.role-diamond", "736353716256505877");
        this.roleNetheriteId = MineSkyDiscord.config.getString("vip-roles.role-netherite", "816824166740262923");
        this.roleRubiId = MineSkyDiscord.config.getString("vip-roles.role-rubi", "1069740271010599074");

        LuckPerms luckPerms = LuckPermsProvider.get();
        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
        eventBus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
    }

    private void onNodeAdd(NodeAddEvent event) {
        if (!event.isUser()) {
            return;
        }

        if (isVipRelatedNode(event.getNode())) {
            net.luckperms.api.model.user.User lpUser = (net.luckperms.api.model.user.User) event.getTarget();
            plugin.getLogger().info("[VIP-Sync] Adição de cargo detectada para: " + lpUser.getUsername() + " (Nó: " + event.getNode().getKey() + ")");

            Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> {
                syncDiscordRoles(lpUser.getUniqueId());
            }, 1L, TimeUnit.SECONDS);
        }
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        if (!event.isUser()) {
            return;
        }

        if (isVipRelatedNode(event.getNode())) {
            net.luckperms.api.model.user.User lpUser = (net.luckperms.api.model.user.User) event.getTarget();
            plugin.getLogger().info("[VIP-Sync] Remoção de cargo detectada para: " + lpUser.getUsername() + " (Nó: " + event.getNode().getKey() + ")");

            Bukkit.getAsyncScheduler().runDelayed(plugin, (task) -> {
                syncDiscordRoles(lpUser.getUniqueId());
            }, 1L, TimeUnit.SECONDS);
        }
    }

    private boolean isVipRelatedNode(net.luckperms.api.node.Node node) {
        String key = node.getKey().toLowerCase();

        if (key.equals("minesky.vip.ferro") ||
                key.equals("minesky.vip.ouro") ||
                key.equals("minesky.vip.diamante") ||
                key.equals("minesky.vip.netherite") ||
                key.equals("minesky.vip.rubi")) {
            return true;
        }

        if (key.startsWith("group.")) {
            String groupName = key.substring("group.".length());
            return groupName.equals("ferro") ||
                    groupName.equals("ouro") ||
                    groupName.equals("diamante") ||
                    groupName.equals("netherite") ||
                    groupName.equals("rubi") ||
                    groupName.startsWith("vip");
        }

        return false;
    }

    public void syncDiscordRoles(UUID uuid) {
        plugin.getLogger().info("[VIP-Sync] Iniciando sincronização assíncrona para o UUID: " + uuid);

        PlayerDatabase.getPlayerSpecificDataAsync(uuid.toString(), ValueType.STRING, "discord.id", new FindValueCallback() {
            @Override
            public void onQueryDone(Document document, Object value, boolean exists) {
                String discordId = (String) value;
                if (discordId == null || discordId.isEmpty()) {
                    plugin.getLogger().info("[VIP-Sync] O jogador com UUID " + uuid + " não possui conta do Discord vinculada.");
                    return;
                }

                plugin.getLogger().info("[VIP-Sync] Discord ID encontrado para o jogador: " + discordId);

                JDA jda = MineSkyDiscord.getInstance().jda;
                if (jda == null) {
                    plugin.getLogger().warning("[VIP-Sync] Instância do JDA é nula! Sincronização cancelada.");
                    return;
                }

                Guild guild = jda.getGuildById(guildId);
                if (guild == null) {
                    plugin.getLogger().warning("[VIP-Sync] Servidor do Discord (Guild ID: " + guildId + ") não encontrado!");
                    return;
                }

                LuckPerms luckPerms = LuckPermsProvider.get();

                luckPerms.getUserManager().loadUser(uuid).thenAccept(lpUser -> {
                    if (lpUser == null) {
                        plugin.getLogger().warning("[VIP-Sync] Não foi possível carregar o perfil do LuckPerms para " + uuid);
                        return;
                    }

                    guild.retrieveMemberById(discordId).queue(member -> {
                        plugin.getLogger().info("[VIP-Sync] Membro carregado no Discord: " + member.getEffectiveName() + " (" + lpUser.getUsername() + ")");
                        updateMemberVipRoles(lpUser, guild, member);
                    }, throwable -> {
                        plugin.getLogger().warning("[VIP-Sync] Não foi possível carregar o membro " + discordId + " no servidor de Discord.");
                    });
                });
            }

            @Override
            public void onQueryError(ErrorType errorType) {
                plugin.getLogger().severe("[VIP-Sync] Erro de banco de dados ao buscar dados para " + uuid + ": " + errorType);
            }
        });
    }

    public void updateMemberVipRoles(net.luckperms.api.model.user.User lpUser, Guild guild, Member member) {
        boolean hasRubi = hasDirectVip(lpUser, "rubi");
        boolean hasNetherite = !hasRubi && hasDirectVip(lpUser, "netherite");
        boolean hasDiamond = !hasRubi && !hasNetherite && hasDirectVip(lpUser, "diamante");
        boolean hasGold = !hasRubi && !hasNetherite && !hasDiamond && hasDirectVip(lpUser, "ouro");
        boolean hasIron = !hasRubi && !hasNetherite && !hasDiamond && !hasGold && hasDirectVip(lpUser, "ferro");

        plugin.getLogger().info("[VIP-Sync] Detecção de VIPs para " + member.getEffectiveName() + ":");
        plugin.getLogger().info(" - Rubi: " + hasRubi);
        plugin.getLogger().info(" - Netherite: " + hasNetherite);
        plugin.getLogger().info(" - Diamante: " + hasDiamond);
        plugin.getLogger().info(" - Ouro: " + hasGold);
        plugin.getLogger().info(" - Ferro: " + hasIron);

        updateRoleState(guild, member, roleRubiId, hasRubi);
        updateRoleState(guild, member, roleNetheriteId, hasNetherite);
        updateRoleState(guild, member, roleDiamondId, hasDiamond);
        updateRoleState(guild, member, roleGoldId, hasGold);
        updateRoleState(guild, member, roleIronId, hasIron);
    }

    private boolean hasDirectVip(net.luckperms.api.model.user.User lpUser, String groupName) {
        return lpUser.getNodes().stream().anyMatch(node -> {
            String key = node.getKey().toLowerCase();

            if (key.startsWith("group.")) {
                String name = key.substring("group.".length());
                return name.equals(groupName) ||
                        name.equals("vip-" + groupName) ||
                        name.equals("vip_" + groupName) ||
                        name.equals("vip" + groupName);
            }

            return key.equals("minesky.vip." + groupName);
        });
    }

    private void updateRoleState(Guild guild, Member member, String roleId, boolean shouldHave) {
        if (roleId == null || roleId.isEmpty() || roleId.startsWith("ID_DO_CARGO")) {
            return;
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            plugin.getLogger().warning("[VIP-Sync] Cargo com ID " + roleId + " não foi encontrado no Discord!");
            return;
        }

        boolean currentlyHas = member.getRoles().contains(role);

        if (shouldHave && !currentlyHas) {
            guild.addRoleToMember(member, role).queue(
                    success -> plugin.getLogger().info("[VIP-Sync] Adicionado cargo " + role.getName() + " para " + member.getEffectiveName()),
                    error -> plugin.getLogger().severe("[VIP-Sync] Erro ao adicionar cargo " + role.getName() + " para " + member.getEffectiveName() + ": " + error.getMessage())
            );
        } else if (!shouldHave && currentlyHas) {
            guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info("[VIP-Sync] Removido cargo " + role.getName() + " de " + member.getEffectiveName()),
                    error -> plugin.getLogger().severe("[VIP-Sync] Erro ao remover cargo " + role.getName() + " de " + member.getEffectiveName() + ": " + error.getMessage())
            );
        }
    }

    public void removeAllVipRoles(Guild guild, Member member) {
        removeSingleRole(guild, member, roleRubiId);
        removeSingleRole(guild, member, roleNetheriteId);
        removeSingleRole(guild, member, roleDiamondId);
        removeSingleRole(guild, member, roleGoldId);
        removeSingleRole(guild, member, roleIronId);
    }

    private void removeSingleRole(Guild guild, Member member, String roleId) {
        if (roleId == null || roleId.isEmpty() || roleId.startsWith("ID_DO_CARGO")) {
            return;
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) return;

        if (member.getRoles().contains(role)) {
            guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info("[VIP-Sync] Removido cargo VIP " + role.getName() + " de " + member.getEffectiveName() + " devido à desvinculação."),
                    error -> plugin.getLogger().severe("[VIP-Sync] Erro ao remover cargo VIP de desvínculo: " + error.getMessage())
            );
        }
    }
}