package com.poro.empire.admin.config;

/**
 * 몹 스탯 런타임 오버라이드 1행 (INBOX-010 축 A).
 *
 * <p>{@code mobKey} = MythicMobs mobId. 각 스탯은 nullable(Double) — null이면 "오버라이드 없음
 * (MythicMobs YAML 원본 유지)". MVP는 atk(평타=`Damage:` 속성)만 시드, hp/def는 운영자 명령으로
 * 필요 시 설정. def는 저장만 하고 적용은 2단계(PlayerDefenseListener 연동).</p>
 */
public record MobStatOverride(String mobKey, Double maxHp, Double def, Double atk) {

    public static MobStatOverride empty(String mobKey) {
        return new MobStatOverride(mobKey, null, null, null);
    }

    public MobStatOverride withMaxHp(Double v) { return new MobStatOverride(mobKey, v, def, atk); }
    public MobStatOverride withDef(Double v)   { return new MobStatOverride(mobKey, maxHp, v, atk); }
    public MobStatOverride withAtk(Double v)   { return new MobStatOverride(mobKey, maxHp, def, v); }

    public boolean isEmpty() { return maxHp == null && def == null && atk == null; }
}
