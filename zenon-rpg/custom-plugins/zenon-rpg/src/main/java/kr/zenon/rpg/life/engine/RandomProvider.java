package kr.zenon.rpg.life.engine;

public interface RandomProvider {
    double nextDouble();

    int nextInt(int boundExclusive);
}
