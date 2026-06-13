package kr.zenon.rpg.pvp;

public enum PvpMatchType {
    FREE,      // 자유대전 — 현재 장비, 점수 없음
    RANKED,    // 정규대전 — IL 60 동일화, 점수 +15/-10
    FRIENDLY   // 친선대전 — 현재 장비, 점수 없음
}
