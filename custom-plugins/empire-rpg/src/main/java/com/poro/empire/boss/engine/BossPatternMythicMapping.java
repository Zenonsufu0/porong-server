package com.poro.empire.boss.engine;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CSV pattern_id ↔ Mythic skill name 매핑 (DL-077 잔여 3/3).
 * <p>
 * 1차 시즌은 메타데이터 보관용. 향후 BossPatternScheduler.select 결과를
 * Mythic BukkitAPIHelper.castSkill로 발동시키는 hook 연결 시 사용.
 */
public final class BossPatternMythicMapping {

    /** pattern_id (lowercase) → mythic skill name. */
    private final Map<String, String> mapping = new HashMap<>();

    public Optional<String> mythicSkillFor(String patternId) {
        if (patternId == null) return Optional.empty();
        return Optional.ofNullable(mapping.get(patternId.toLowerCase()));
    }

    public Map<String, String> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
    }

    public int size() {
        return mapping.size();
    }

    /** plugin 리소스에서 CSV 로드. 헤더: pattern_id,mythic_skill,notes. */
    public void loadFromResource(Plugin plugin, String resourcePath) {
        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                plugin.getLogger().warning("[BossPatternMythicMapping] 리소스 없음: " + resourcePath);
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String header = r.readLine();
                if (header == null) return;
                String line;
                int loaded = 0;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length < 2) continue;
                    String patternId = parts[0].trim().toLowerCase();
                    String mythic    = parts[1].trim();
                    if (patternId.isEmpty() || mythic.isEmpty()) continue;
                    mapping.put(patternId, mythic);
                    loaded++;
                }
                plugin.getLogger().info("[BossPatternMythicMapping] " + loaded + "개 매핑 로드");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BossPatternMythicMapping] 로드 실패: " + e.getMessage());
        }
    }
}
