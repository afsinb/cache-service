package com.afsinb.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class CacheService {

    // Dynamic anomaly toggles for live demos
    private volatile boolean evictionEnabled = false;
    private volatile int generationBurstMultiplier = 1;
    private volatile boolean warningStormEnabled = false;

    // INTENTIONAL BUG #1: cache grows unbounded when evictionEnabled=false
    private Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return evictionEnabled && size() > 2000;
        }
    };
    private long hits = 0;
    private long misses = 0;

    @Scheduled(fixedRate = 2000)
    public void generateCacheEntries() {
        try {
            int entryCount = 100 * Math.max(1, generationBurstMultiplier);
            for (int i = 0; i < entryCount; i++) {
                String key = "cache_" + UUID.randomUUID().toString();
                cache.put(key, new CacheEntry(key, "value_" + i, System.currentTimeMillis()));
            }

            log.info("Cache entries added. Total: {}, Size: {}MB, evictionEnabled={}, burstMultiplier={}",
                    cache.size(), getMemoryUsage(), evictionEnabled, generationBurstMultiplier);

            if (warningStormEnabled || cache.size() % 500 == 0) {
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

    public void setEvictionEnabled(boolean enabled) {
        this.evictionEnabled = enabled;
        log.warn("Anomaly toggle changed: evictionEnabled={}", enabled);
    }

    public void setGenerationBurstMultiplier(int multiplier) {
        this.generationBurstMultiplier = Math.max(1, multiplier);
        log.warn("Anomaly toggle changed: generationBurstMultiplier={}", this.generationBurstMultiplier);
    }

    public void setWarningStormEnabled(boolean enabled) {
        this.warningStormEnabled = enabled;
        log.warn("Anomaly toggle changed: warningStormEnabled={}", enabled);
    }

    public Map<String, Object> anomalyState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("eviction_enabled", evictionEnabled);
        state.put("generation_burst_multiplier", generationBurstMultiplier);
        state.put("warning_storm_enabled", warningStormEnabled);
        state.put("cache_size", cache.size());
        return state;
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
