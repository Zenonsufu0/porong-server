package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.poro.poromoncore.PoroMonCore;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cobblemon 특성 카탈로그(포로공학 정수·특성 = 완전 마개조용). 결정 034.
 * Cobblemon ko_kr.json의 {@code cobblemon.ability.<name>} 키로 특성 목록을 구성하고,
 * {@link Abilities#get(String)}로 실제 템플릿이 존재하는 것만 채택. 한글명은 표시·검색용.
 * 최초 1회 빌드 후 캐시.
 */
public final class AbilityCatalog {
    private AbilityCatalog() {}

    /**
     * 한 특성 항목.
     * - {@code name}: 영문 키(forced 부여 시 {@code Abilities.get(name)}).
     * - {@code translationKey}: GUI 렌더용 번역키({@code Text.translatable} → 클라 ko_kr 한글).
     * - {@code koName}: 한글 표시·검색용(ko_kr.json, 없으면 영문 키).
     */
    public record Entry(String name, String translationKey, String koName) {}

    private static List<Entry> all;

    private static synchronized void ensureBuilt() {
        if (all != null) return;
        List<Entry> list = new ArrayList<>();
        String prefix = "cobblemon.ability.";
        // ko_kr.json에서 (영문키 → 한글) 수집(.desc 등 하위 키 제외)
        Map<String, String> ko = new TreeMap<>();
        try (InputStream in = Abilities.class.getClassLoader()
                .getResourceAsStream("assets/cobblemon/lang/ko_kr.json")) {
            if (in != null) {
                JsonObject obj = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                    String k = e.getKey();
                    if (!k.startsWith(prefix)) continue;
                    String tail = k.substring(prefix.length());
                    if (tail.indexOf('.') >= 0) continue; // .desc / 기타 제외
                    ko.put(tail, e.getValue().getAsString());
                }
            } else {
                PoroMonCore.LOGGER.warn("[AbilityCatalog] cobblemon ko_kr.json 없음 — 영문 키로 진행");
            }
        } catch (Throwable t) {
            PoroMonCore.LOGGER.warn("[AbilityCatalog] ko_kr.json 로드 실패: {}", t.toString());
        }

        for (Map.Entry<String, String> e : ko.entrySet()) {
            String name = e.getKey();
            AbilityTemplate tpl = Abilities.get(name);
            if (tpl == null) continue; // 모드팩에 없는 특성 제외
            String key;
            try { key = tpl.getDisplayName(); } catch (Throwable t) { key = prefix + name; }
            list.add(new Entry(name, key, e.getValue()));
        }
        // ko에 없지만 레지스트리엔 있는 특성 누락 대비는 생략(표시 한글이 핵심 동선).
        list.sort(Comparator.comparing(Entry::koName));
        all = list;
    }

    public static List<Entry> all() { ensureBuilt(); return all; }

    /** 한글명·영문 키 부분일치 검색(대소문자 무시). */
    public static List<Entry> search(String query) {
        ensureBuilt();
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Entry> r = new ArrayList<>();
        for (Entry e : all) {
            if (e.name().contains(q) || e.koName().toLowerCase(Locale.ROOT).contains(q)) r.add(e);
        }
        return r;
    }
}
