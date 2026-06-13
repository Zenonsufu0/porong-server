package kr.zenon.rpg.market;

import com.google.gson.Gson;
import kr.zenon.rpg.growth.engine.ItemGrade;
import kr.zenon.rpg.growth.engine.PotentialGrade;
import kr.zenon.rpg.growth.engine.PotentialLine;
import kr.zenon.rpg.growth.engine.TraceInstance;

import java.util.List;
import java.util.Locale;

/**
 * 흔적 인스턴스 ↔ JSON 직렬화 (DL-129 추가#38, P5).
 * 경매 listing/배달의 {@code item_payload} 컬럼에 인스턴스 등급+세부스탯을 보관·복원한다.
 */
public final class TraceInstanceCodec {
    private TraceInstanceCodec() {}

    private static final Gson GSON = new Gson();

    private record LineDto(int lineNo, String grade, String optionCode, double value) {}
    private record TraceDto(String instanceId, String grade, List<LineDto> substats) {}

    public static String toJson(TraceInstance trace) {
        if (trace == null) return null;
        List<LineDto> lines = trace.substats().stream()
                .map(l -> new LineDto(l.lineNo(), l.grade().name(), l.optionCode(), l.value()))
                .toList();
        return GSON.toJson(new TraceDto(trace.instanceId(), trace.grade().name(), lines));
    }

    /** JSON → TraceInstance. 파싱 실패 시 null. */
    public static TraceInstance fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            TraceDto dto = GSON.fromJson(json, TraceDto.class);
            if (dto == null) return null;
            List<PotentialLine> subs = dto.substats() == null ? List.of() : dto.substats().stream()
                    .map(d -> new PotentialLine(d.lineNo(), parse(PotentialGrade.class, d.grade(), PotentialGrade.COMMON), d.optionCode(), d.value()))
                    .toList();
            return new TraceInstance(dto.instanceId(), parse(ItemGrade.class, dto.grade(), ItemGrade.COMMON), subs);
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> cls, String name, E fallback) {
        if (name == null) return fallback;
        try {
            return Enum.valueOf(cls, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
