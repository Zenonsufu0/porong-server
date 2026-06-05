package kr.poro.poromoncore.mixin;

import com.cobblemon.mod.common.battles.dispatch.WaitDispatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 배틀 속도 가속 — Cobblemon 배틀의 메시지/액션 간 대기시간(WaitDispatch)을 단축.
 * WaitDispatch(float duration) 의 duration 을 SCALE 배로 줄여 readyTime 을 앞당긴다.
 * (사용자 요청: 배틀이 너무 느림 → 기본 모드 딜레이를 mixin으로 단축.)
 */
@Mixin(WaitDispatch.class)
public class WaitDispatchMixin {

    /** 0.12 = 대기시간 12%(약 8배 빠름). 너무 낮추면 애니/메시지 겹침 가능. */
    private static final float SCALE = 0.12f;

    // 생성자 HEAD(super() 호출 전) 주입 → 핸들러는 static 이어야 함
    @ModifyVariable(method = "<init>(F)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float poromon$scaleWait(float duration) {
        return duration * SCALE;
    }
}
