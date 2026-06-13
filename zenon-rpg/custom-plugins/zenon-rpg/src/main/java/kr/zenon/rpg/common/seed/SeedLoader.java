package kr.zenon.rpg.common.seed;

import kr.zenon.rpg.common.result.Result;

import java.util.List;

public interface SeedLoader<T> {
    String name();

    Result<List<T>> load();
}
