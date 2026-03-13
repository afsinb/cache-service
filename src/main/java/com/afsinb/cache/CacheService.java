package com.afsinb.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class CacheService {

    // INTENTIONAL BUG #1: Cache with no eviction policy
    private Map<String, CacheEntry> cache = new HashMap<>();
    private long hits = 0;
    private long misses = 0;

    @Scheduled(fixedRate = 2000)
    public void generateCacheEntries() {
        try {
            // INTENTIONAL BUG #2: Adding entries without cleanup
            for (int i = 0; i < 100; i++) {
                String key = "cache_" + UUID.randomUUID().toString();
                cache.put(key, new CacheEntry(key, "value_" + i, System.currentTimeMillis()));
            }

            log.info("Cache entries added. Total: {}, Size: {}MB", cache.size(), getMemoryUsage());

            // INTENTIONAL BUG #3: Periodic memory warning
            if (cache.size() % 500 == 0) {
                log.warn("Cache size exceeding threshold: {} entries", cache.size());
            }

        } catch (Exception e) {
            log.error("Cache operation failed", e);
        }
    }

    public String get(String key) {
        if (cache.containsKey(key)) {
            hits++;
            CacheEntry entry = cache.get(key);
            log.debug("Cache HIT for key: {}", key);
            return entry.getValue();
        } else {
            misses++;
            log.debug("Cache MISS for key: {}", key);
            return null;
        }
    }

    public void put(String key, String value) {
        cache.put(key, new CacheEntry(key, value, System.currentTimeMillis()));
        log.debug("Cache PUT: {} = {}", key, value);
    }

    public long getCacheSize() {
        return cache.size();
    }

    public long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    public double getHitRatio() {
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }

    public void clearCache() {
        cache.clear();
        log.info("Cache cleared");
    }

    static class CacheEntry {
        private String key;
        private String value;
        private long timestamp;

        CacheEntry(String key, String value, long timestamp) {
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getValue() {
            return value;
        }
    }
}
