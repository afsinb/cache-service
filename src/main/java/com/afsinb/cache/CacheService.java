package com.afsinb.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class CacheService {

    private volatile boolean evictionEnabled = true;
    private volatile int generationBurstMultiplier = 1;
    private volatile boolean warningStormEnabled = false;
    private volatile boolean raceModeEnabled = true;
    private volatile boolean autoChaosEnabled = true;

    private final Random random = new Random();
    private long anomalyErrors = 0;

    private final Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(1024, 0.75f, true) {
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
            int effectiveBurst = Math.max(1, generationBurstMultiplier);
            if (autoChaosEnabled && random.nextInt(100) < 20) {
                effectiveBurst = Math.max(effectiveBurst, 30);
                evictionEnabled = false;
                log.warn("Natural anomaly: burst write with eviction disabled");
            }

            int entryCount = 100 * effectiveBurst;
            for (int i = 0; i < entryCount; i++) {
                String key = "cache_" + UUID.randomUUID();
                cache.put(key, new CacheEntry(key, "value_" + i, System.currentTimeMillis()));
            }

            if (warningStormEnabled && cache.size() % 500 == 0) {
                log.warn("Cache size exceeding threshold: {} entries", cache.size());
            }

            if (raceModeEnabled && cache.size() > 1500 && random.nextInt(100) < 25) {
                runRaceProbe();
            }

            if (autoChaosEnabled && random.nextInt(100) < 8) {
                throw new CacheCorruptionWindowException("Cache index drift detected");
            }

            log.info("Cache entries added. Total={}, memory={}MB, errors={}",
                    cache.size(), getMemoryUsage(), anomalyErrors);

        } catch (Exception e) {
            anomalyErrors++;
            log.error("Cache operation failed", e);
        }
    }

    private void runRaceProbe() {
        try {
            for (String key : cache.keySet()) {
                if (key.hashCode() % 97 == 0) {
                    cache.remove(key);
                }
            }
        } catch (ConcurrentModificationException e) {
            anomalyErrors++;
            log.error("Race condition anomaly detected in cache iteration", e);
        }
    }

    public String get(String key) {
        if (cache.containsKey(key)) {
            hits++;
            CacheEntry entry = cache.get(key);
            log.debug("Cache HIT for key: {}", key);
            return entry.getValue();
        }
        misses++;
        log.debug("Cache MISS for key: {}", key);
        return null;
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

    public long getErrorCount() {
        return anomalyErrors;
    }

    public void clearCache() {
        cache.clear();
        anomalyErrors = 0;
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

    public void setRaceModeEnabled(boolean enabled) {
        this.raceModeEnabled = enabled;
        log.warn("Anomaly toggle changed: raceModeEnabled={}", enabled);
    }

    public void setAutoChaosEnabled(boolean enabled) {
        this.autoChaosEnabled = enabled;
        log.warn("Anomaly toggle changed: autoChaosEnabled={}", enabled);
    }

    public Map<String, Object> anomalyState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("eviction_enabled", evictionEnabled);
        state.put("generation_burst_multiplier", generationBurstMultiplier);
        state.put("warning_storm_enabled", warningStormEnabled);
        state.put("race_mode_enabled", raceModeEnabled);
        state.put("auto_chaos_enabled", autoChaosEnabled);
        state.put("cache_size", cache.size());
        state.put("error_count", anomalyErrors);
        return state;
    }

    static class CacheEntry {
        private final String key;
        private final String value;
        private final long timestamp;

        CacheEntry(String key, String value, long timestamp) {
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getValue() {
            return value;
        }
    }

    static class CacheCorruptionWindowException extends RuntimeException {
        CacheCorruptionWindowException(String message) {
            super(message);
        }
    }
}
