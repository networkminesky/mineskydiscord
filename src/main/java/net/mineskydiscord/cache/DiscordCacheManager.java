package net.mineskydiscord.cache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minesky.api.database.PlayerDatabase;
import net.minesky.core.databridge.callbacks.FindOneCallback;
import net.minesky.core.databridge.callbacks.ErrorType;
import org.bson.Document;

public class DiscordCacheManager {

    private final ConcurrentHashMap<UUID, String> statusCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> tagCache = new ConcurrentHashMap<>(); // Novo cache para a Tag

    public void loadPlayerProfile(UUID uuid) {
        PlayerDatabase.getPlayerDataAsync(uuid.toString(), new FindOneCallback() {
            @Override
            public void onQueryDone(Document document) {
                if (document == null) {
                    statusCache.put(uuid, "");
                    tagCache.put(uuid, "");
                    return;
                }

                Document discordDocument = document.get("discord", Document.class);
                if (discordDocument != null) {
                    String status = discordDocument.getString("status");
                    String tag = discordDocument.getString("tag");

                    statusCache.put(uuid, status != null ? status : "");
                    tagCache.put(uuid, tag != null ? tag : "");
                } else {
                    statusCache.put(uuid, "");
                    tagCache.put(uuid, "");
                }
            }

            @Override
            public void onQueryError(ErrorType errorType) {
                statusCache.put(uuid, "");
                tagCache.put(uuid, "");
            }
        });
    }

    public String getCachedStatus(UUID uuid) {
        return statusCache.getOrDefault(uuid, "");
    }

    public String getCachedTag(UUID uuid) {
        return tagCache.getOrDefault(uuid, "");
    }

    public void updateCachedStatus(UUID uuid, String status) {
        statusCache.put(uuid, status != null ? status : "");
    }

    public void updateCachedTag(UUID uuid, String tag) {
        tagCache.put(uuid, tag != null ? tag : "");
    }

    public void invalidate(UUID uuid) {
        statusCache.remove(uuid);
        tagCache.remove(uuid);
    }
}