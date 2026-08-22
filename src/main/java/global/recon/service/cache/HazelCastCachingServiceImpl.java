package global.recon.service.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class HazelCastCachingServiceImpl implements HazelCastCachingService<Object> {

    private final HazelcastInstance hazelcastInstance;

    public HazelCastCachingServiceImpl(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void put(String cacheName, String key, Object value) {
        getMap(cacheName).put(key, value);
    }

    @Override
    public void putIfAbsent(String cacheName, String key, Object value) {
        getMap(cacheName).putIfAbsent(key, value);
    }

    @Override
    public Object get(String cacheName, String key) {
        return getMap(cacheName).get(key);
    }

    @Override
    public Map<String, Object> getAllValues(String cacheName) {
        IMap<String, Object> map = getMap(cacheName);
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.copyOf(map);
    }

    @Override
    public boolean evict(String cacheName, String key) {
        return getMap(cacheName).evict(key);
    }

    @Override
    public IMap<String, Object> getMap(String cacheName) {
        return hazelcastInstance.getMap(cacheName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getValueFromCachedMap(String cacheName, String cacheKey, K mapKey) {
        Object cached = get(cacheName, cacheKey);
        if (!(cached instanceof Map<?, ?> nested)) {
            return null;
        }
        return (V) nested.get(mapKey);
    }
}
