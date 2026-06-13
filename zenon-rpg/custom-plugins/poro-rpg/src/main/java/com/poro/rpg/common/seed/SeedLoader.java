package com.poro.rpg.common.seed;

import com.poro.rpg.common.result.Result;

import java.util.List;

public interface SeedLoader<T> {
    String name();

    Result<List<T>> load();
}
