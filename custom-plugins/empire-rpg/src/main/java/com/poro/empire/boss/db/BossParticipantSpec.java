package com.poro.empire.boss.db;

/**
 * 보스 입장 시 참여자 장비 스펙 — boss_session_player 실측 기록용 (INBOX-004 #4 / DL-081).
 * 1차 시즌은 방무(defense ignore) 메커니즘이 없어(CANON 방깎 제외) defense_ignore는 항상 0으로 기록한다.
 *
 * @param weaponEnhance 무기 슬롯 강화도
 * @param avgEnhance    5슬롯 평균 강화도
 * @param il            평균 아이템 레벨 (강화 1당 IL 5 → avgEnhance × 5)
 */
public record BossParticipantSpec(int weaponEnhance, double avgEnhance, double il) {
    public static final BossParticipantSpec ZERO = new BossParticipantSpec(0, 0.0, 0.0);
}
