package com.poro.empire.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 관리자 글로벌 토글 서비스 (Phase 2 Step 2).
 * <p>이벤트성 플래그를 in-memory로 관리. 재시작 시 초기화 (운영 정책: 이벤트 종료 시 재시작 권장).
 */
public final class AdminTogglesService {

    public enum Toggle {
        BOSS_SPAWN_PAUSE("보스 스폰 일시정지", false),
        ENHANCE_BOOST   ("강화 확률 2배",       false),
        EXP_BOOST       ("EXP 2배",            false),
        DROP_BOOST      ("필드 드랍 2배",       false),
        PVP_QUEUE_PAUSE ("PvP 큐 일시정지",     false);

        public final String displayName;
        public final boolean defaultValue;
        Toggle(String displayName, boolean defaultValue) {
            this.displayName  = displayName;
            this.defaultValue = defaultValue;
        }
    }

    private final Map<Toggle, Boolean> state = new LinkedHashMap<>();

    public AdminTogglesService() {
        for (Toggle t : Toggle.values()) state.put(t, t.defaultValue);
    }

    public boolean isOn(Toggle t)       { return Boolean.TRUE.equals(state.get(t)); }
    public void    setOn(Toggle t)      { state.put(t, true); }
    public void    setOff(Toggle t)     { state.put(t, false); }
    public boolean toggle(Toggle t)     { boolean next = !isOn(t); state.put(t, next); return next; }
    public Map<Toggle, Boolean> all()   { return Map.copyOf(state); }
}
