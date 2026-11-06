package com.nexskin.velocity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public SkinCache(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public SkinFetcher.SkinData get(String username) {
        Entry entry = cache.get(username.toLowerCase());
        if (entry == null) return null;
        if (System.currentTimeMillis() / 1000 - entry.timestamp > ttlSeconds) {
            cache.remove(username.toLowerCase());
            return null;
        }
        return entry.skin;
    }

    public void put(String username, SkinFetcher.SkinData skin) {
        cache.put(username.toLowerCase(), new Entry(skin, System.currentTimeMillis() / 1000));
    }

    public void remove(String username) {
        cache.remove(username.toLowerCase());
    }

    public int size() {
        return cache.size();
    }

    private static class Entry {
        final SkinFetcher.SkinData skin;
        final long timestamp;

        Entry(SkinFetcher.SkinData skin, long timestamp) {
            this.skin = skin;
            this.timestamp = timestamp;
        }
    }
}