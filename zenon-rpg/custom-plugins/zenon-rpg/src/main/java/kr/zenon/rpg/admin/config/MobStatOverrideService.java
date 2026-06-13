package kr.zenon.rpg.admin.config;

import kr.zenon.rpg.common.logging.DomainLogger;
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
 * (MAX_HEALTH·ATTACK_DAMAGE). 운영자 명령({@code /rpg-mobstat})이 set/reset 호출 → DB·감사로그·캐시 동기.</p>
 *
 * <p><b>적용 범위:</b> 평타(=`Damage:` 속성)·HP만 어트리뷰트로 즉시 적용. def는 저장만(2단계 — PlayerDefenseListener 연동).
 * 보스 스킬 패턴 데미지는 MythicMobs YAML 상수라 본 레이어 밖(기획안 §4.1-C, C-2).</p>
 */
public final class MobStatOverrideService {

    public static final String SEED_AUTHOR = "seed:DL-128#13";

    /**
     * DL-116 정본값 시드 — mobId → 평타(ATK) (`docs/06_fields_bosses/mob_attack_stats_v1.md`).
     * 일반몹 2.5% / 정예 6% / 필드보스 기본공격 8%. (HP/DEF는 미시드 = MythicMobs YAML 유지.)
     */
    private static final Map<String, Double> ATK_SEED = buildAtkSeed();

    /**
     * 보스 DEF 시드 (DL-128#14 — 방어력무시 잠재 의미화). 스폰 시 엔티티 PDC에 기록되어
     * 플레이어→보스 피해에 200/(200+유효DEF) 경감. 설계값 = season_boss_stats_v1.
     */
    private static final Map<String, Double> DEF_SEED = buildDefSeed();

    /**
     * 보스 HP 시드 (DL-129 추가#16 — A 제안 곡선). 권장강화 비례로 시즌1→시즌6 상승(역전 해소, balance_review_dl128 §2).
     * 솔로/소규모 파티 기준. DEF 경감(×(DEF+200)/200)이 추가로 실효 HP를 늘림. <b>운영 검토 후 조정 대상</b>.
     */
    private static final Map<String, Double> HP_SEED = buildHpSeed();

    private static Map<String, Double> buildHpSeed() {
        Map<String, Double> m = new LinkedHashMap<>();
        // 시즌보스 6종 — DEF 경감 역산(raw×(DEF+200)/200=실효). raw 낮춰 솔로 친화(타락기사 실효~4.5만, balance_review 30~35k 부합).
        m.put("fallen_knight",   30_000.0); // 시즌1 (6~10강)   실효 ~45k
        m.put("corrupted_lord",  45_000.0); // 시즌2 (10~13강)  실효 ~79k
        m.put("stone_colossus",  70_000.0); // 시즌3 (13~16강)  실효 ~133k
        m.put("storm_sorcerer", 100_000.0); // 시즌4 (16~18강)  실효 ~205k
        m.put("abyss_guardian", 130_000.0); // 시즌5 (18~20강)  실효 ~286k
        m.put("void_herald",    180_000.0); // 시즌6 (20~22강)  실효 ~419k
        // 최종보스 3종 (22강+)
        m.put("rift_king",      420_000.0); // 실효 ~1.0M
        m.put("corrupted_dyad", 300_000.0); // 이중체 — 각 체력 낮게
        m.put("spirit_watcher", 420_000.0);
        return m;
    }

    /** 보스정보 GUI 등 표시용 — 시드된 HP/ATK/DEF (없으면 null). */
    public static Double seededHp(String mobId)  { return HP_SEED.get(mobId); }
    public static Double seededAtk(String mobId) { return ATK_SEED.get(mobId); }
    public static Double seededDef(String mobId) { return DEF_SEED.get(mobId); }

    private static Map<String, Double> buildDefSeed() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("fallen_knight",  100.0);
        m.put("corrupted_lord", 150.0);
        m.put("stone_colossus", 180.0);
        m.put("storm_sorcerer", 210.0);
        m.put("abyss_guardian", 240.0);
        m.put("void_herald",    265.0);
        m.put("rift_king",      280.0);
        m.put("corrupted_dyad", 270.0);
        m.put("spirit_watcher", 270.0);
        m.put("fallen_knight_phantom", 40.0);
        return m;
    }

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

    /**
     * 부팅: DB 캐시 로드 + ATK 시드 적용.
     * 밸런스 단계(DL-128#13)에서는 코드 시드가 ATK 정본 — 기존 행의 ATK가 시드와 다르면 시드값으로 upsert(재적용).
     * 기존 HP/DEF 오버라이드는 보존(withAtk). 운영자 /rpg-mobstat ATK 변경은 재부팅 시 시드로 복원됨(밸런스 확정 후 정책 전환).
     */
    public void loadAndSeed() {
        cache.clear();
        cache.putAll(repository.findAll());
        int seeded = 0;
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(ATK_SEED.keySet());
        keys.addAll(DEF_SEED.keySet());
        keys.addAll(HP_SEED.keySet());
        for (String key : keys) {
            MobStatOverride existing = cache.get(key);
            Double atk = ATK_SEED.containsKey(key) ? ATK_SEED.get(key) : (existing != null ? existing.atk() : null);
            Double def = DEF_SEED.containsKey(key) ? DEF_SEED.get(key) : (existing != null ? existing.def() : null);
            Double hp  = HP_SEED.containsKey(key)  ? HP_SEED.get(key)  : (existing != null ? existing.maxHp() : null);
            if (existing != null
                    && java.util.Objects.equals(atk, existing.atk())
                    && java.util.Objects.equals(def, existing.def())
                    && java.util.Objects.equals(hp, existing.maxHp())) continue; // 변화 없음
            MobStatOverride row = new MobStatOverride(key, hp, def, atk);
            repository.save(row, SEED_AUTHOR);
            cache.put(key, row);
            seeded++;
        }
        logger.info("mob_stat_override 로드 — 캐시 " + cache.size() + "건 (ATK/DEF 시드 적용/갱신 " + seeded + "건).");
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
        if (o == null || (o.maxHp() == null && o.atk() == null && o.def() == null)) return;
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
            // DEF는 어트리뷰트가 없으므로 PDC에 기록 — 플레이어→대상 피해(BaseWeaponSkill)에서 200/(200+DEF) 경감에 사용.
            if (o.def() != null) {
                le.getPersistentDataContainer().set(
                        kr.zenon.rpg.combat.SkillContext.MOB_DEF_KEY,
                        org.bukkit.persistence.PersistentDataType.DOUBLE, o.def());
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
        // DL-128#13 재조정: 플레이어 HP 상향(200~556)에 맞춰 필드별 ATK 가파르게 스케일(상위 필드 = 저강화 게이팅).
        // F1 수도 외곽 평원 — 일반 8 / 정예 18 / 보스 24
        m.put("Plains_Soldier", 8.0);
        m.put("Plains_Wildling", 8.0);
        m.put("Plains_StalkerElite", 18.0);
        m.put("Plains_Predator", 24.0);
        // F2 폐광 지대 — 일반 13 / 정예 28 / 보스 36
        m.put("Mine_Crawler", 13.0);
        m.put("Mine_Husk", 13.0);
        m.put("Mine_Golem_Fragment", 13.0);
        m.put("Mine_WatcherElite", 28.0);
        m.put("Mine_Golem", 36.0);
        // F3 오염된 수로 — 일반 19 / 정예 40 / 보스 52
        m.put("Waterway_Drowned", 19.0);
        m.put("Waterway_Guardian", 19.0);
        m.put("Waterway_LurkerElite", 40.0);
        m.put("Waterway_Lord", 52.0);
        // F4 무너진 초소 — 일반 26 / 정예 56 / 보스 70
        m.put("Outpost_Pillager", 26.0);
        m.put("Outpost_Vindicator", 26.0);
        m.put("Outpost_CaptainElite", 56.0);
        m.put("Outpost_Knight", 70.0);
        // F5 고대 성벽 잔해 — 일반 34 / 정예 72 / 보스 90
        m.put("Wall_Enderman", 34.0);
        m.put("Wall_Shade", 34.0);
        m.put("Wall_SentinelElite", 72.0);
        m.put("Rift_Watcher", 90.0);
        return m;
    }
}
