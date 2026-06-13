package kr.zenon.rpg.growth.engine;

import java.util.concurrent.ThreadLocalRandom;

public final class ThreadLocalRandomProvider implements RandomProvider {
    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public int nextInt(int boundExclusive) {
        return ThreadLocalRandom.current().nextInt(boundExclusive);
    }
}
