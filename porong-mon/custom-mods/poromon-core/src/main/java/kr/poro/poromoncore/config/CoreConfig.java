package kr.poro.poromoncore.config;

/**
 * core.json 매핑 POJO (config_structure.md §3, 0.1 부분집합).
 * 밸런스/규칙 값 하드코딩 금지 원칙(CLAUDE.md) → 메뉴 아이템·허브 좌표·로깅 토글은 여기로.
 * Gson 기본 직렬화. 필드 기본값 = 파일 부재 시 생성되는 기본 설정.
 */
public class CoreConfig {
    public int configVersion = 1;
    public MenuItem menuItem = new MenuItem();
    public Hub hub = new Hub();
    public Home home = new Home();
    public Wild wild = new Wild();
    public FieldEvent fieldEvent = new FieldEvent();
    public Nether nether = new Nether();
    public End end = new End();
    public Logging logging = new Logging();

    /** 9번 슬롯 리그 패스 아이템 정책 (menu_design.md §2). */
    public static class MenuItem {
        public boolean enabled = true;
        public String itemId = "minecraft:clock";   // 베이스 아이템(임시 — 전용 모델 전)
        public String displayName = "리그 패스";       // 표시명(한국어)
        public int hotbarSlot = 8;                   // 0-based → 핫바 9번째 칸
        public boolean giveOnFirstJoin = true;
        public boolean restoreOnJoin = true;
        public boolean restoreOnRespawn = true;
        public boolean preventDrop = true;
        public boolean lockSlot = true;              // 9번 칸 고정(이동/드롭 잠금)
    }

    /** 허브 텔레포트 목적지 (menu_design.md §3 슬롯 19, commands.md /poromon hub). */
    public static class Hub {
        public String world = "minecraft:overworld";
        public Spawn spawn = new Spawn();
        public boolean teleportCommandEnabled = true;
    }

    public static class Spawn {
        public double x = 0.5;
        public double y = 64.0;
        public double z = 0.5;
        public float yaw = 0.0f;
        public float pitch = 0.0f;
    }

    /** 홈 등록/텔레포트 (결정 029, menu_design.md §3 슬롯20). */
    public static class Home {
        public boolean enabled = true;
        public int maxSlots = 5;
        public int freeSlots = 1;              // 기본 개방 슬롯 수
        public long[] unlockCosts = {10000L, 30000L, 70000L, 150000L}; // 2~5번째 해금 비용
        public int warmupSeconds = 3;          // 채널링(이동 전 대기)
        public int cooldownSeconds = 30;       // 이동 후 재사용 대기
        public boolean cancelOnMove = true;    // 채널링 중 이동 시 취소
        public boolean cancelOnDamage = true;  // 채널링 중 피격 시 취소
    }

    /** 야생 랜덤 이동 (결정 030: 허브 무중심 → 월드보더 안 완전 랜덤). */
    public static class Wild {
        public boolean enabled = true;
        public int warmupSeconds = 3;     // 채널링(이동 전 대기)
        public int cooldownSeconds = 30;
        public boolean cancelOnMove = true;
        public boolean cancelOnDamage = true;
        public int maxAttempts = 24;      // 안전 착지 탐색 시도 횟수
        public int minSurfaceY = 45;      // 이보다 낮은 지표(구덩이/공동)는 회피
        public int edgeMargin = 16;       // 월드보더 가장자리 여유
    }

    /** 하급·중급 전설 필드 이벤트 (결정 019 → 038: 좌표 공개·30분). */
    public static class FieldEvent {
        public boolean enabled = true;
        public int intervalMinutes = 120;   // 주기(2시간마다 1회)
        public int durationMinutes = 30;    // 출현 후 디스폰까지
        public String poolId = "field_event_legendary_pool"; // 하급70/중급30
        public int level = 60;              // 출현 레벨
        public int edgeMargin = 200;        // 월드보더 안쪽 여유
        public int minSurfaceY = 50;        // 이보다 낮은 지표 회피
        public int maxAttempts = 40;        // 안전 위치 탐색 횟수
    }

    /** 네더 차원 정책 (결정 039, IB-005): 경계 + 고정 허브 포탈 + 허브 보호. */
    public static class Nether {
        public boolean enabled = true;
        public int borderDiameter = 5000;   // 네더 월드보더(÷8 비적용)
        public double borderCenterX = 0.0;
        public double borderCenterZ = 0.0;

        // 고정 허브 포탈: 오버월드→네더 = 허브 도착 / 네더→오버월드 = 플레이어별 복귀 좌표
        public boolean hubRedirect = true;
        public double hubX = 0.5;            // ⚠️ 허브 건설 후 실좌표로 교체
        public double hubY = 64.0;
        public double hubZ = 0.5;
        public float hubYaw = 0.0f;

        // 허브 보호: 중심 기준 반경(체비셰프). 10 → 21×21 (포탈·블레이즈 스포너 파괴 방지)
        public boolean protectHub = true;
        public int protectRadius = 10;
        public boolean opBypassProtect = true; // op(권한2)는 보호 무시(건설/유지보수)
    }

    /** 엔드 차원 정책 (결정 039): 드래곤 제거 + 입장 시 무작위 외곽 섬 착지(엔드시티 탐험). */
    public static class End {
        public boolean enabled = true;
        public boolean removeDragon = true;   // 엔더 드래곤 스폰 시 제거(드래곤전 없음)
        public boolean randomLanding = true;  // 입장마다 무작위 외곽 섬으로(함선 고갈 완화)
        public int minRadius = 1100;          // 중앙 섬 바깥(외곽 섬 시작)
        public int maxRadius = 6000;          // 무작위 밴드 상한
        public int maxAttempts = 12;          // 안전 섬 탐색 시도(청크 생성 비용 ↔ 성공률)
        // 복귀는 /poromon hub(오버월드 허브 TP) 사용 — 별도 엔드 귀환 포탈 불필요.
    }

    /** 감사 로깅 토글 (0.1 일부만 사용). */
    public static class Logging {
        public boolean auditEnabled = true;
        public boolean logTicketUse = true;
        public boolean logRoomAssign = true;
        public boolean logRewardGrant = true;
        public boolean logAdminCommand = true;
    }
}
