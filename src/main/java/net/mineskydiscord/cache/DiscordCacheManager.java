package net.mineskydiscord.cache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.core.databridge.callbacks.FindOneCallback;
import net.minesky.core.databridge.callbacks.ErrorType;
import org.bson.Document;

public class DiscordCacheManager {

    private final ConcurrentHashMap<UUID, String> statusCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> tagCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> nicknameCache = new ConcurrentHashMap<>(); // Cache por UUID
    private final ConcurrentHashMap<String, String> discordNicknameCache = new ConcurrentHashMap<>(); // Cache por Discord ID
    private final ConcurrentHashMap<UUID, String> uuidToDiscordId = new ConcurrentHashMap<>();

    public void loadPlayerProfile(UUID uuid) {
        PlayerDatabase.getPlayerDataAsync(uuid.toString(), new FindOneCallback() {
            @Override
            public void onQueryDone(Document document) {
                if (document == null) {
                    statusCache.put(uuid, "");
                    tagCache.put(uuid, "");
                    nicknameCache.put(uuid, "");
                    return;
                }

                String latestNickname = document.getString("latest-nickname");
                String nick = latestNickname != null ? latestNickname : "";
                nicknameCache.put(uuid, nick);

                Document discordDocument = document.get("discord", Document.class);
                if (discordDocument != null) {
                    String status = discordDocument.getString("status");
                    String tag = discordDocument.getString("tag");
                    Object discordIdObj = discordDocument.get("id");

                    statusCache.put(uuid, status != null ? status : "");
                    tagCache.put(uuid, tag != null ? tag : "");

                    if (discordIdObj != null) {
                        String discordId = String.valueOf(discordIdObj);
                        uuidToDiscordId.put(uuid, discordId);
                        if (!nick.isEmpty()) {
                            discordNicknameCache.put(discordId, nick);
                        }
                    }
                } else {
                    statusCache.put(uuid, "");
                    tagCache.put(uuid, "");
                }
            }

            @Override
            public void onQueryError(ErrorType errorType) {
                statusCache.put(uuid, "");
                tagCache.put(uuid, "");
                nicknameCache.put(uuid, "");
            }
        });
    }

    public String getCachedStatus(UUID uuid) {
        return statusCache.getOrDefault(uuid, "");
    }

    public String getCachedTag(UUID uuid) {
        return tagCache.getOrDefault(uuid, "");
    }

    public String getCachedNickname(UUID uuid) {
        return nicknameCache.getOrDefault(uuid, "");
    }

    public String getCachedNicknameByDiscordId(String discordId) {
        return discordNicknameCache.get(discordId);
    }

    public void updateCachedStatus(UUID uuid, String status) {
        statusCache.put(uuid, status != null ? status : "");
    }

    public void updateCachedTag(UUID uuid, String tag) {
        tagCache.put(uuid, tag != null ? tag : "");
    }

    public void updateCachedNickname(UUID uuid, String nickname) {
        String nick = nickname != null ? nickname : "";
        nicknameCache.put(uuid, nick);

        String discordId = uuidToDiscordId.get(uuid);
        if (discordId != null) {
            discordNicknameCache.put(discordId, nick);
        }
    }

    public void updateCachedNicknameByDiscordId(String discordId, String nickname) {
        if (discordId != null && nickname != null) {
            discordNicknameCache.put(discordId, nickname);
        }
    }

    public void invalidate(UUID uuid) {
        statusCache.remove(uuid);
        tagCache.remove(uuid);
        nicknameCache.remove(uuid);
        String discordId = uuidToDiscordId.remove(uuid);
        if (discordId != null) {
            discordNicknameCache.remove(discordId);
        }
    }

    public void invalidateByDiscordId(String discordId) {
        discordNicknameCache.remove(discordId);
    }
}