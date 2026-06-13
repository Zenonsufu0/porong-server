package kr.zenon.rpg.life.engine;

import java.util.concurrent.ThreadLocalRandom;

public final class ThreadLocalRandomProvider implements RandomProvider {
    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public int nextInt(int boundExclusive) {
        if (boundExclusive <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(boundExclusive);
    }
}
