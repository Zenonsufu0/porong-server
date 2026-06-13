package kr.zenon.rpg.combat;

/**
 * 스킬 데미지 적용 중 재진입 가드.
 *
 * <p>스킬의 {@code target.damage()} 호출은 다시 {@link org.bukkit.event.entity.EntityDamageByEntityEvent}를
 * 발생시킨다. 평타 리스너(SkillInputListener)가 그 이벤트를 일반 좌클릭으로 오인해 {@code setDamage()}로
 * 스킬 데미지를 덮어쓰는 문제(섬광베기 163 → 평타 37로 덮임)를 막는다.</p>
 *
 * <p>스킬 데미지 적용을 {@link #run(Runnable)}로 감싸면, 그 안에서 발생하는 데미지 이벤트 동안
 * {@link #isApplying()}가 true가 되어 평타 처리를 건너뛴다. ThreadLocal이라 동일 스레드(메인) 재진입만 가드.</p>
 */
public final class SkillDamageGuard {

    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SkillDamageGuard() {}

    public static boolean isApplying() {
        return APPLYING.get();
    }

    public static void run(Runnable action) {
        APPLYING.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            APPLYING.set(Boolean.FALSE);
        }
    }
}
