package kr.zenon.moncore.shop;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.zenon.moncore.ZenonMonCore;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

    /**
     * 한 TM 항목.
     * - {@code displayName}: 정렬용 문자열(서버 locale, 보통 영어).
     * - {@code displayText}: GUI 렌더용 translatable Text(클라 ko_kr 자동 한글).
     * - {@code koName}: 한글 부분검색용(Cobblemon ko_kr.json에서 로드, 없으면 빈 문자열).
     */
    public record Entry(String itemId, String moveName, String displayName, Text displayText,
                        String koName, String type, double power) {}

    private static List<Entry> all;                 // 전체(이름순)
    private static Map<String, List<Entry>> byType; // 타입 → 항목들

    /** 표준 18타입 순서(메뉴 그리드용). */
    public static final String[] TYPES = {
            "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"};

    private static synchronized void ensureBuilt() {
        if (all != null) return;
        Map<String, String> koNames = loadKoMoveNames();
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
            Text dispText;
            try { Text t = tpl.getDisplayName(); dispText = t; disp = t.getString(); }
            catch (Throwable t) { disp = move; dispText = Text.literal(move); }
            String ko = koNames.getOrDefault(move, "");
            list.add(new Entry(id.toString(), move, disp, dispText, ko, type, tpl.getPower()));
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

    /** 이름/기술ID 부분일치 검색(영문 키·영문명·한글명 모두, 대소문자 무시). */
    public static List<Entry> search(String query) {
        ensureBuilt();
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Entry> r = new ArrayList<>();
        for (Entry e : all) {
            if (e.moveName().contains(q)
                    || e.displayName().toLowerCase(Locale.ROOT).contains(q)
                    || (!e.koName().isEmpty() && e.koName().contains(q))) {
                r.add(e);
            }
        }
        return r;
    }

    /**
     * Cobblemon ko_kr.json에서 기술 한글명 맵({@code move → 한글})을 로드.
     * 서버 locale이 영어라 {@code getDisplayName().getString()}는 영어 → 한글 검색용으로 별도 로드.
     * 키 {@code cobblemon.move.<move>}만 사용(`.desc`/`.category.*` 등 하위 키 제외). 실패 시 빈 맵.
     */
    private static Map<String, String> loadKoMoveNames() {
        Map<String, String> m = new HashMap<>();
        String prefix = "cobblemon.move.";
        try (InputStream in = Moves.class.getClassLoader()
                .getResourceAsStream("assets/cobblemon/lang/ko_kr.json")) {
            if (in == null) {
                ZenonMonCore.LOGGER.warn("[TmCatalog] cobblemon ko_kr.json 없음 — 한글 검색 비활성");
                return m;
            }
            JsonObject obj = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                String k = e.getKey();
                if (!k.startsWith(prefix)) continue;
                String tail = k.substring(prefix.length());
                if (tail.indexOf('.') >= 0) continue; // .desc / .category.* 제외
                m.put(tail, e.getValue().getAsString());
            }
        } catch (Throwable t) {
            ZenonMonCore.LOGGER.warn("[TmCatalog] ko_kr.json 로드 실패 — 한글 검색 비활성: {}", t.toString());
        }
        return m;
    }
}
