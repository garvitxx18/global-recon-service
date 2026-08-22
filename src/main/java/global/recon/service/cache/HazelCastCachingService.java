package global.recon.service.cache;

import com.hazelcast.map.IMap;

import java.util.Map;
import java.util.Optional;

public interface HazelCastCachingService<T> {

    void put(String cacheName, String key, T value);

    void putIfAbsent(String cacheName, String key, T value);

    T get(String cacheName, String key);

    default Optional<T> find(String cacheName, String key) {
        return Optional.ofNullable(get(cacheName, key));
    }

    Map<String, T> getAllValues(String cacheName);

    boolean evict(String cacheName, String key);

    IMap<String, T> getMap(String cacheName);

    <K, V> V getValueFromCachedMap(String cacheName, String cacheKey, K mapKey);
}
