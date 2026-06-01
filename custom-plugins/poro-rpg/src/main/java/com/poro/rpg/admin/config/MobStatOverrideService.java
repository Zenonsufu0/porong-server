package com.poro.rpg.admin.config;

import com.poro.rpg.common.logging.DomainLogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 몹 스탯 런타임 오버라이드 서비스 (INBOX-010 축 A MVP).
 *
 * <p>부팅 시 DB에서 캐시 로드 + DL-116 정본값 시드(없는 mobKey만). 스폰 시 어트리뷰트 적용
 * (MAX_HEALTH·ATTACK_DAMAGE). 운영자 명령({@code /poro-mobstat})이 set/reset 호출 → DB·감사로그·캐시 동기.</p>
 *
 * <p><b>적용 범위:</b> 평타(=`Damage:` 속성)·HP만 어트리뷰트로 즉시 적용. def는 저장만(2단계 — PlayerDefenseListener 연동).
 * 보스 스킬 패턴 데미지는 MythicMobs YAML 상수라 본 레이어 밖(기획안 §4.1-C, C-2).</p>
 */
public final class MobStatOverrideService {

    public static final String SEED_AUTHOR = "seed:DL-116";

    /**
     * DL-116 정본값 시드 — mobId → 평타(ATK) (`docs/06_fields_bosses/mob_attack_stats_v1.md`).
     * 일반몹 2.5% / 정예 6% / 필드보스 기본공격 8%. (HP/DEF는 미시드 = MythicMobs YAML 유지.)
     */
    private static final Map<String, Double> ATK_SEED = buildAtkSeed();

    private final MobStatOverrideRepository repository;
    private final ConfigChangeLogRepository changeLog;
    private final DomainLogger logger;
    private final Map<String, MobStatOverride> cache = new LinkedHashMap<>();

    public MobStatOverrideService(MobStatOverrideRepository repository,
                                  ConfigChangeLogRepository changeLog,
                                  DomainLogger logger) {
        this.repository = Objects.requireNonNull(repository);
        this.changeLog  = Objects.requireNonNull(changeLog);
        this.logger     = Objects.requireNonNull(logger);
    }

    /** 부팅: DB 캐시 로드 + DL-116 시드(없는 키만 삽입). */
    public void loadAndSeed() {
        cache.clear();
        cache.putAll(repository.findAll());
        int seeded = 0;
        for (Map.Entry<String, Double> e : ATK_SEED.entrySet()) {
            if (cache.containsKey(e.getKey())) continue;
            MobStatOverride row = new MobStatOverride(e.getKey(), null, null, e.getValue());
            if (repository.insertIfAbsent(row, SEED_AUTHOR)) {
                cache.put(e.getKey(), row);
                seeded++;
            }
        }
        logger.info("mob_stat_override 로드 — 캐시 " + cache.size() + "건 (DL-116 신규 시드 " + seeded + "건).");
    }

    public MobStatOverride get(String mobKey) {
        return cache.getOrDefault(mobKey, MobStatOverride.empty(mobKey));
    }

    public Map<String, MobStatOverride> all() {
        return java.util.Collections.unmodifiableMap(cache);
    }

    /** 스폰 직후 어트리뷰트 적용. MythicMobs 후속 적용과의 race를 피해 1틱 지연. */
    public void applyOnSpawn(Plugin plugin, Entity entity, String mobId) {
        MobStatOverride o = cache.get(mobId);
        if (o == null || (o.maxHp() == null && o.atk() == null)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!(entity instanceof LivingEntity le) || le.isDead() || !le.isValid()) return;
            if (o.maxHp() != null) {
                AttributeInstance hp = le.getAttribute(Attribute.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(o.maxHp());
                    le.setHealth(Math.min(o.maxHp(), hp.getValue()));
                }
            }
            if (o.atk() != null) {
                AttributeInstance atk = le.getAttribute(Attribute.ATTACK_DAMAGE);
                if (atk != null) atk.setBaseValue(o.atk());
            }
        }, 1L);
    }

    /** 운영자 set. field ∈ {hp, def, atk}. value null = 해당 스탯 오버라이드 해제. */
    public void set(String mobKey, String field, Double value, String by) {
        MobStatOverride cur = get(mobKey);
        Double oldV;
        MobStatOverride next;
        switch (field) {
            case "hp"  -> { oldV = cur.maxHp(); next = cur.withMaxHp(value); }
            case "def" -> { oldV = cur.def();   next = cur.withDef(value); }
            case "atk" -> { oldV = cur.atk();   next = cur.withAtk(value); }
            default -> throw new IllegalArgumentException("알 수 없는 필드: " + field);
        }
        if (next.isEmpty()) {
            repository.delete(mobKey);
            cache.remove(mobKey);
        } else {
            repository.save(next, by);
            cache.put(mobKey, next);
        }
        changeLog.append("mob", mobKey, field, str(oldV), str(value), by);
    }

    /** DL-116 시드값으로 복원(없으면 완전 제거). */
    public void reset(String mobKey, String by) {
        MobStatOverride old = get(mobKey);
        repository.delete(mobKey);
        cache.remove(mobKey);
        Double seedAtk = ATK_SEED.get(mobKey);
        if (seedAtk != null) {
            MobStatOverride row = new MobStatOverride(mobKey, null, null, seedAtk);
            repository.save(row, SEED_AUTHOR);
            cache.put(mobKey, row);
        }
        changeLog.append("mob", mobKey, "reset", summarize(old), seedAtk == null ? null : "atk=" + seedAtk, by);
    }

    private static String str(Double v) { return v == null ? null : trim(v); }

    private static String summarize(MobStatOverride o) {
        if (o.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        if (o.maxHp() != null) sb.append("hp=").append(trim(o.maxHp())).append(' ');
        if (o.def()   != null) sb.append("def=").append(trim(o.def())).append(' ');
        if (o.atk()   != null) sb.append("atk=").append(trim(o.atk()));
        return sb.toString().trim();
    }

    private static String trim(double v) {
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static Map<String, Double> buildAtkSeed() {
        Map<String, Double> m = new LinkedHashMap<>();
        // F1 수도 외곽 평원 — 일반 4 / 정예 9 / 보스 기본공격 12
        m.put("Plains_Soldier", 4.0);
        m.put("Plains_Wildling", 4.0);
        m.put("Plains_StalkerElite", 9.0);
        m.put("Plains_Predator", 12.0);
        // F2 폐광 지대 — 일반 5 / 정예 11 / 보스 14
        m.put("Mine_Crawler", 5.0);
        m.put("Mine_Husk", 5.0);
        m.put("Mine_Golem_Fragment", 5.0);
        m.put("Mine_WatcherElite", 11.0);
        m.put("Mine_Golem", 14.0);
        // F3 오염된 수로 — 일반 5 / 정예 12 / 보스 16
        m.put("Waterway_Drowned", 5.0);
        m.put("Waterway_Guardian", 5.0);
        m.put("Waterway_LurkerElite", 12.0);
        m.put("Waterway_Lord", 16.0);
        // F4 무너진 초소 — 일반 5 / 정예 13 / 보스 17
        m.put("Outpost_Pillager", 5.0);
        m.put("Outpost_Vindicator", 5.0);
        m.put("Outpost_CaptainElite", 13.0);
        m.put("Outpost_Knight", 17.0);
        // F5 고대 성벽 잔해 — 일반 6 / 정예 14 / 보스 18
        m.put("Wall_Enderman", 6.0);
        m.put("Wall_Shade", 6.0);
        m.put("Wall_SentinelElite", 14.0);
        m.put("Rift_Watcher", 18.0);
        return m;
    }
}
