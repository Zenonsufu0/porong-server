package com.poro.rpg.boss.db;

/**
 * 참여자 UUID → 장비 스펙 해석기 (INBOX-004 #4 / DL-081).
 * 보스 엔진이 growth 내부에 직접 의존하지 않도록 인터페이스로 분리 — 구현(IL 계산)은 플러그인 와이어링이 제공.
 * 상태가 없으면(오프라인 등) {@link BossParticipantSpec#ZERO}를 반환한다.
 */
@FunctionalInterface
public interface BossParticipantSpecResolver {
    BossParticipantSpec resolve(String uuid);

    /** 기본값 — 항상 ZERO (해석기 미주입 시). */
    BossParticipantSpecResolver ZERO = uuid -> BossParticipantSpec.ZERO;
}
