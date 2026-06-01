package com.poro.rpg.growth.engine;

public interface RandomProvider {
    double nextDouble();

    int nextInt(int boundExclusive);
}
