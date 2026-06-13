package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.InMemoryRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

abstract class AbstractMasterRegistry<T> {
    private final InMemoryRegistry<String, T> delegate = new InMemoryRegistry<>();
    private final Function<T, String> keyExtractor;

    protected AbstractMasterRegistry(Function<T, String> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    public void register(T value) {
        delegate.register(keyExtractor.apply(value), value);
    }

    public Optional<T> find(String id) {
        return delegate.find(id);
    }

    public Map<String, T> all() {
        return delegate.snapshot();
    }

    public boolean contains(String id) {
        return find(id).isPresent();
    }

    public int size() {
        return all().size();
    }
}
