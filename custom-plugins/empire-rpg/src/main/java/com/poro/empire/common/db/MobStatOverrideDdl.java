package com.poro.empire.common.db;

/**
 * 몹 스탯 런타임 오버라이드 테이블 (INBOX-010 축 A MVP).
 *
 * <p>운영자가 플러그인 재배포 없이 몹별 HP/DEF/평타 ATK를 핫에딧하기 위한 저장소.
 * {@code mob_key} = MythicMobs mobId (예: {@code Plains_Soldier}, {@code Mine_Golem}).
 * 각 값은 nullable — NULL이면 "오버라이드 없음(MythicMobs YAML 원본 유지)"을 의미한다.</p>
 *
 * <p>초기 시드 = DL-116 정본값(`docs/06_fields_bosses/mob_attack_stats_v1.md`). atk만 시드,
 * hp/def는 NULL(MVP는 HP·DEF 미시드). 보스 스킬 패턴 데미지는 MythicMobs YAML 상수라 본 레이어
 * 적용 대상 아님(평타=`Damage:` 속성만 어트리뷰트로 적용 가능 — 기획안 §4.1-C).</p>
 */
public final class MobStatOverrideDdl {
    private MobStatOverrideDdl() {}

    public static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS mob_stat_override (
            mob_key       TEXT    NOT NULL,
            max_hp        REAL,
            def           REAL,
            atk           REAL,
            updated_by    TEXT,
            updated_at    INTEGER NOT NULL,
            PRIMARY KEY (mob_key)
        )
        """;
}
