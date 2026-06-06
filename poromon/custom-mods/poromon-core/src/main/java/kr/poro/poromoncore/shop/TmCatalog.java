package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SimpleTMs 전체 TM 카탈로그(마개조 기술머신 상점용). 결정 033.
 * simpletms:tm_<기술> 아이템을 레지스트리에서 수집 → Cobblemon Moves 레지스트리로 타입/위력 조회.
 * 타입 분류 + 검색 + 위력 등급가의 데이터 원천. 최초 1회 빌드 후 캐시.
 */
public final class TmCatalog {
    private TmCatalog() {}

    /** 한 TM 항목. */
    public record Entry(String itemId, String moveName, String displayName, String type, double power) {}

    private static List<Entry> all;                 // 전체(이름순)
    private static Map<String, List<Entry>> byType; // 타입 → 항목들

    /** 표준 18타입 순서(메뉴 그리드용). */
    public static final String[] TYPES = {
            "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"};

    private static synchronized void ensureBuilt() {
        if (all != null) return;
        List<Entry> list = new ArrayList<>();
        for (Identifier id : Registries.ITEM.getIds()) {
            if (!id.getNamespace().equals("simpletms")) continue;
            String path = id.getPath();
            if (!path.startsWith("tm_") || path.equals("tm_blank")) continue;
            String move = path.substring(3);
            MoveTemplate tpl = Moves.getByName(move);
            if (tpl == null) continue;
            String type = tpl.getElementalType().getName().toLowerCase(Locale.ROOT);
            String disp;
            try { disp = tpl.getDisplayName().getString(); } catch (Throwable t) { disp = move; }
            list.add(new Entry(id.toString(), move, disp, type, tpl.getPower()));
        }
        list.sort(Comparator.comparing(Entry::displayName));
        Map<String, List<Entry>> map = new LinkedHashMap<>();
        for (String t : TYPES) map.put(t, new ArrayList<>());
        for (Entry e : list) map.computeIfAbsent(e.type(), k -> new ArrayList<>()).add(e);
        all = list;
        byType = map;
    }

    public static List<Entry> all() { ensureBuilt(); return all; }

    public static List<Entry> ofType(String type) {
        ensureBuilt();
        return byType.getOrDefault(type, List.of());
    }

    /** 이름/기술ID 부분일치 검색(대소문자 무시). */
    public static List<Entry> search(String query) {
        ensureBuilt();
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Entry> r = new ArrayList<>();
        for (Entry e : all) {
            if (e.moveName().contains(q) || e.displayName().toLowerCase(Locale.ROOT).contains(q)) r.add(e);
        }
        return r;
    }
}
