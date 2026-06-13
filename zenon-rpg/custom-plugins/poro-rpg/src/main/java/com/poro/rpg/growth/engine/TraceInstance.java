package com.poro.rpg.growth.engine;

import java.util.List;

/**
 * 장비의 흔적 개별 인스턴스 (DL-129 추가#38).
 *
 * <p>기존 스택형 흔적(equip_trace_* item_id → 수량)을 대체하는 인스턴스 모델.
 * 드랍 시점에 등급 + 세부스탯이 롤되어 박히고, 전승 시 그 등급·세부스탯이 장비로 이전된다.
 * 결정1(범용)에 따라 슬롯 필드는 없다 — 어떤 장비에든 전승 가능하며, 세부스탯은
 * 슬롯-무관 통합풀(P1b)에서 롤한다.</p>
 *
 * <p>{@link PlayerEquipmentItem}의 grade + substatLines 모델을 흔적용으로 미러링한 불변 레코드.
 * customItems 스택(다른 재료)과 공존하며, {@code equip_trace_*} 스택만 인스턴스로 이관된다.</p>
 */
public record TraceInstance(
        String instanceId,
        ItemGrade grade,
        List<PotentialLine> substats
) {
    public TraceInstance {
        instanceId = instanceId == null ? "" : instanceId.trim();
        grade = grade != null ? grade : ItemGrade.COMMON;
        substats = substats != null ? List.copyOf(substats) : List.of();
    }
}
