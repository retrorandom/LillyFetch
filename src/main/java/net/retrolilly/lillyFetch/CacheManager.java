package net.retrolilly.lillyFetch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheManager {

    private static class CachedResult {
        final List<String> output;
        final long timestamp;

        CachedResult(List<String> output, long timestamp) {
            this.output = output;
            this.timestamp = timestamp;
        }

        boolean isExpired(long cacheDuration) {
            return System.currentTimeMillis() - timestamp > (cacheDuration * 1000);
        }
    }

    private final Map<String, CachedResult> cache = new HashMap<>();

    public void cache(String key, List<String> output) {
        cache.put(key, new CachedResult(output, System.currentTimeMillis()));
    }

    public List<String> get(String key, long cacheDuration) {
        CachedResult result = cache.get(key);
        if (result != null && !result.isExpired(cacheDuration)) {
            return result.output;
        }
        return null;
    }

    public void clear() {
        cache.clear();
    }

    public void clearExpired(long cacheDuration) {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheDuration));
    }
}
