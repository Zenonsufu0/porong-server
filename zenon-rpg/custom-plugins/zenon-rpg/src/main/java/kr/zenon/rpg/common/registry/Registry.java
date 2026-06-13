package kr.zenon.rpg.common.registry;

import java.util.Map;
import java.util.Optional;

public interface Registry<K, V> {
    void register(K key, V value);

    Optional<V> find(K key);

    Map<K, V> snapshot();
}
