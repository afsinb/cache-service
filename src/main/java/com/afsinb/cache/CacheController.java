package com.afsinb.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/{key}")
    public String get(@PathVariable String key) {
        return cacheService.get(key);
    }

    @PostMapping("/{key}")
    public void put(@PathVariable String key, @RequestBody String value) {
        cacheService.put(key, value);
    }

    @DeleteMapping
    public void clear() {
        cacheService.clearCache();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "cache-service",
            "cache_size", cacheService.getCacheSize(),
            "memory_mb", cacheService.getMemoryUsage(),
            "hit_ratio", String.format("%.2f", cacheService.getHitRatio())
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
            "size", cacheService.getCacheSize(),
            "memory", cacheService.getMemoryUsage(),
            "hit_ratio", cacheService.getHitRatio(),
            "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/admin/anomalies")
    public Map<String, Object> anomalyState() {
        return cacheService.anomalyState();
    }

    @PostMapping("/admin/anomalies")
    public Map<String, Object> configureAnomalies(
            @RequestParam(required = false) Boolean evictionEnabled,
            @RequestParam(required = false) Integer generationBurstMultiplier,
            @RequestParam(required = false) Boolean warningStormEnabled,
            @RequestParam(required = false, defaultValue = "false") boolean clearCache
    ) {
        if (evictionEnabled != null) {
            cacheService.setEvictionEnabled(evictionEnabled);
        }
        if (generationBurstMultiplier != null) {
            cacheService.setGenerationBurstMultiplier(generationBurstMultiplier);
        }
        if (warningStormEnabled != null) {
            cacheService.setWarningStormEnabled(warningStormEnabled);
        }
        if (clearCache) {
            cacheService.clearCache();
        }
        return cacheService.anomalyState();
    }
}
