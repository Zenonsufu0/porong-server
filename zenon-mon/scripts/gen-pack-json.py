#!/usr/bin/env python3
"""
gen-pack-json.py ─ 클라 모드 실파일 → pack.json 생성 (결정 046 / installer_design.md §3)

`client_mod_tiers.md` 분류를 prefix→티어 테이블로 코드화한다. `modpack/client/mods`
실제 jar 파일명을 prefix 매칭해 required / optional(T1·T2) / libraries 로 나누고,
서버전용(S)은 클라 번들에서 제외한다. 매칭 안 되는 파일이 하나라도 있으면 에러로
중단(86개 전수 분류 보장 — 모드팩 업데이트로 새 jar가 들어오면 테이블 갱신 강제).

prefix 매칭은 longest-prefix 우선(예: InvMove- vs InvMoveCompats-, sodium-fabric- vs
sodium-extra-).

사용:
    python3 scripts/gen-pack-json.py                # modpack/pack.json 생성
    python3 scripts/gen-pack-json.py --out -        # stdout 출력
    python3 scripts/gen-pack-json.py --check        # 분류만 검증(파일 미생성)

출력 pack.json 은 빌드 입력(번들 정의 + 버전 핀). jar 아닌 JSON 이라 Git 추적 가치 있음.
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # zenon-mon/
MODS_DIR = os.path.join(ROOT, "modpack", "client", "mods")
OVERRIDES_DIR = os.path.join(ROOT, "modpack", "overrides")
DEFAULT_OUT = os.path.join(ROOT, "modpack", "pack.json")

MC = {"version": "1.21.1", "loader": "fabric", "loaderVersion": "0.19.3", "java": 21}

# 티어:
#   required : T0 코어(강제·서버 정합·토글 불가)
#   t1       : 권장 편의(optional, 기본 ON)
#   t2       : 선택 취향(optional, 기본 OFF)
#   lib      : 라이브러리(자동 의존, 0.1은 항상 포함, UI 비노출)
#   server   : 서버전용(클라 번들 제외)
#
# (prefix, tier, group, name_ko, desc)  ─ 출처: client_mod_tiers.md (§1~§5)
TABLE = [
    # ── T0 코어 필수 (14) ─────────────────────────────────────────────
    ("fabric-api-",                  "required", "core", "Fabric API", ""),
    ("architectury-",                "required", "core", "Architectury", ""),
    ("owo-lib-",                     "required", "core", "oωo Lib", ""),
    ("accessories-fabric-",          "required", "core", "Accessories", ""),
    ("Cobblemon-fabric-",            "required", "core", "Cobblemon", "포켓몬 엔진(코어)"),
    ("mega_showdown-",               "required", "core", "Mega Showdown", "메가/배틀 기믹"),
    ("SimpleTMs-",                   "required", "core", "SimpleTMs", "기술머신(TM/TR)"),
    ("zenon-mon-core-",                "required", "core", "ZenonMonCore", "서버 규칙 엔진 + 커스텀 텍스처"),
    ("LegendaryMonuments-",          "required", "core", "Legendary Monuments", "레지스트리 정합(비활성)"),
    ("chipped-fabric-",              "required", "core", "Chipped", "LM 의존"),
    ("CobbleFurnies-",               "required", "core", "CobbleFurnies", "LM 의존"),
    ("TerraBlender-",                "required", "core", "TerraBlender", "LM 의존(바이옴)"),
    ("athena-fabric-",               "required", "core", "Athena", "LM 체인 lib"),
    ("resourcefullib-",              "required", "core", "Resourceful Lib", "LM 체인 lib"),

    # ── T1 성능 (2-1) ─────────────────────────────────────────────────
    ("sodium-fabric-",               "t1", "performance", "Sodium", "렌더 최적화(핵심)"),
    ("sodium-extra-",                "t1", "performance", "Sodium Extra", "Sodium 추가 옵션"),
    ("reeses-sodium-options-",       "t1", "performance", "Reese's Sodium Options", "Sodium 옵션 UI"),
    ("sodiumoptionsapi-",            "t1", "performance", "Sodium Options API", ""),
    ("sodiumextras-",                "t1", "performance", "Sodium Extras", ""),
    ("sodium-shadowy-path-blocks-",  "t1", "performance", "Shadowy Path Blocks", "흙길 렌더 보정"),
    ("sodiumleafculling-",           "t1", "performance", "Sodium Leaf Culling", "나뭇잎 컬링"),
    ("entityculling-",               "t1", "performance", "Entity Culling", "시야 밖 엔티티 컬링"),
    ("ferritecore-",                 "t1", "performance", "FerriteCore", "메모리 절감"),
    ("lithium-fabric-",              "t1", "performance", "Lithium", "틱/AI 최적화"),
    ("krypton-",                     "t1", "performance", "Krypton", "네트워크 최적화"),

    # ── T1 전설 모델 보충 (2-1b, 강권장) ──────────────────────────────
    ("complete-cobblemon-collection-", "t1", "models", "전설 모델 보충", "미구현 전설/환상 모델(없으면 인형으로 보임)"),

    # ── T1 정보/뷰어 (2-2) ────────────────────────────────────────────
    ("emi-",                         "t1", "viewer", "EMI", "레시피/아이템 뷰어(메인)"),
    ("emi_enchanting-",              "t1", "viewer", "EMI Enchanting", ""),
    ("emi_ores-",                    "t1", "viewer", "EMI Ores", ""),
    ("EMIProfessions-",              "t1", "viewer", "EMI Professions", ""),
    ("jei-",                         "t1", "viewer", "JEI", "레시피 뷰어(EMI와 중복 — 택1 검토)"),
    ("jeed-",                        "t1", "viewer", "JEED", "효과(포션) 정보"),
    ("AdvancedLootInfo-",            "t1", "viewer", "Advanced Loot Info", "루트테이블 정보"),
    ("appleskin-",                   "t1", "viewer", "AppleSkin", "포만도 표시"),
    ("enchdesc-",                    "t1", "viewer", "Enchantment Descriptions", "인챈트 설명"),
    ("shulkerboxtooltip-",           "t1", "viewer", "Shulker Box Tooltip", "셔커상자 미리보기"),
    ("tooltipfix-",                  "t1", "viewer", "ToolTip Fix", "긴 툴팁 보정"),
    ("BetterPingDisplay-",           "t1", "viewer", "Better Ping Display", "탭 핑 표시"),
    ("stendhal-",                    "t1", "viewer", "Stendhal", "채팅/표지판 서식"),
    ("BetterAdvancements-",          "t1", "viewer", "Better Advancements", "발전과제 화면 개선"),
    ("tipsmod-",                     "t1", "viewer", "Tips", "로딩 팁"),
    # xaero 미니맵/월드맵 = 제외(결정 047): ARR 번들 불가 + TP 시스템으로 길찾기 보완.
    #   .local/removed-mods/ 로 이동. 재도입 시 여기 복원 + client/mods 복귀.

    # ── T1 조작/인벤 QoL (2-3) ────────────────────────────────────────
    ("craftingtweaks-",              "t1", "qol", "Crafting Tweaks", "제작 편의"),
    ("InvMove-",                     "t1", "qol", "InvMove", "인벤 열고 이동"),
    ("InvMoveCompats-",              "t1", "qol", "InvMove Compats", ""),
    ("notenoughanimations-",         "t1", "qol", "Not Enough Animations", "1인칭 손 동작"),
    ("dynamiccrosshair-",            "t1", "qol", "Dynamic Crosshair", "상황별 십자선"),
    ("swingthrough-",                "t1", "qol", "Swing Through Grass", "풀 통과 공격"),
    ("BetterThirdPerson-",           "t1", "qol", "Better Third Person", "3인칭 카메라"),
    ("enhanced_attack_indicator-",   "t1", "qol", "Enhanced Attack Indicator", "공격 인디케이터"),

    # ── T1 UI/메뉴 (2-4) ──────────────────────────────────────────────
    ("modmenu-",                     "t1", "ui", "Mod Menu", "모드 설정 메뉴"),
    ("fancymenu_fabric_",            "t1", "ui", "FancyMenu", "타이틀/메뉴 커스터마이즈"),
    ("language-reload-",             "t1", "ui", "Language Reload", "언어 로딩 가속"),

    # ── T2 선택 취향 (기본 OFF) ───────────────────────────────────────
    ("iris-fabric-",                 "t2", "shader", "Iris Shaders", "셰이더(무거움)"),
    ("entity_texture_features_",     "t2", "models", "ETF", "커스텀 엔티티 텍스처"),
    ("entity_model_features_",       "t2", "models", "EMF", "커스텀 엔티티 모델"),
    ("lambdynamiclights-",           "t2", "visual", "LambDynamicLights", "동적 조명"),
    ("particlerain-",                "t2", "visual", "Particle Rain", "날씨 파티클"),
    ("particular-",                  "t2", "visual", "Particular", "환경 파티클"),
    ("visuality-",                   "t2", "visual", "Visuality", "추가 파티클"),
    ("fallingleaves-",               "t2", "visual", "Falling Leaves", "낙엽 파티클"),
    ("make_bubbles_pop-",            "t2", "visual", "Make Bubbles Pop", "물거품 효과"),
    ("wakes-",                       "t2", "visual", "Wakes", "물 항적 효과"),
    ("AmbientEnvironment-",          "t2", "sound", "Ambient Environment", "환경 사운드"),
    ("PresenceFootsteps-",           "t2", "sound", "Presence Footsteps", "발소리"),
    ("citresewn-",                   "t2", "models", "CIT Resewn", "커스텀 아이템 텍스처"),
    ("CraftPresence-",               "t2", "misc", "CraftPresence", "디스코드 Rich Presence"),
    ("BHMenu-",                      "t2", "misc", "BHMenu", "메인 메뉴 변경"),
    ("cherishedworlds-",             "t2", "misc", "Cherished Worlds", "싱글 즐겨찾기(멀티 무용)"),
    ("monsters-in-the-closet-",      "t2", "sound", "Monsters in the Closet", "분위기 사운드"),
    ("yosbr-",                       "t2", "misc", "YOSBR", "옵션 기본값 유지"),

    # ── L 라이브러리 (자동 의존, 항상 포함) ───────────────────────────
    ("cloth-config-",                "lib", "library", "Cloth Config", ""),
    ("yet_another_config_lib_v3-",   "lib", "library", "YACL", ""),
    ("balm-fabric-",                 "lib", "library", "Balm", ""),
    ("konkrete_fabric_",             "lib", "library", "Konkrete", ""),
    ("melody_fabric_",               "lib", "library", "Melody", ""),
    ("bookshelf-fabric-",            "lib", "library", "Bookshelf", ""),
    ("UniLib-",                      "lib", "library", "UniLib", ""),
    ("Almanac-",                     "lib", "library", "Almanac", ""),
    ("prickle-fabric-",              "lib", "library", "Prickle", ""),

    # ── (S) 서버전용 (클라 번들 제외) ─────────────────────────────────
    ("OpenLoader-",                  "server", "server", "OpenLoader", "서버 datapack 로더"),
    ("letmedespawn-",                "server", "server", "Let Me Despawn", "디스폰 제어(서버)"),
    ("netherportalfix-",             "server", "server", "NetherPortalFix", "포털 링크(서버)"),
    ("bwncr-",                       "server", "server", "BWNCR", "보스 브로드캐스트 억제(서버)"),
    ("Clumps-",                      "server", "server", "Clumps", "XP 오브 병합(서버)"),
]

# longest-prefix 우선
_TABLE_SORTED = sorted(TABLE, key=lambda e: len(e[0]), reverse=True)


def classify(filename):
    for prefix, tier, group, name, desc in _TABLE_SORTED:
        if filename.startswith(prefix):
            return {"file": filename, "tier": tier, "group": group, "name": name, "desc": desc}
    return None


def main():
    args = sys.argv[1:]
    out_path = DEFAULT_OUT
    check_only = False
    if "--check" in args:
        check_only = True
    if "--out" in args:
        out_path = args[args.index("--out") + 1]

    if not os.path.isdir(MODS_DIR):
        print(f"ERROR: mods 디렉터리 없음: {MODS_DIR}", file=sys.stderr)
        return 1

    jars = sorted(f for f in os.listdir(MODS_DIR) if f.endswith(".jar"))
    classified, unmatched = [], []
    for j in jars:
        c = classify(j)
        (classified if c else unmatched).append(c or j)

    if unmatched:
        print("ERROR: 분류 안 된 jar (TABLE 갱신 필요):", file=sys.stderr)
        for u in unmatched:
            print(f"  - {u}", file=sys.stderr)
        return 2

    buckets = {"required": [], "t1": [], "t2": [], "lib": [], "server": []}
    for c in classified:
        buckets[c["tier"]].append(c)

    # overrides = modpack/overrides 하위 디렉터리 전부 (mods 빌드 산출 제외).
    # 클라에 그대로 복사할 config/showdown/xaero/fancymenu_data/shaderpacks 등.
    # icon.png·instance.png 같은 CF 메타 파일은 디렉터리가 아니라 자동 제외.
    overrides = []
    if os.path.isdir(OVERRIDES_DIR):
        overrides = sorted(
            d for d in os.listdir(OVERRIDES_DIR)
            if os.path.isdir(os.path.join(OVERRIDES_DIR, d)) and d != "mods"
        )

    def entry(c, with_toggle):
        e = {"file": c["file"], "name": c["name"]}
        if c["desc"]:
            e["desc"] = c["desc"]
        if with_toggle:
            e["group"] = c["group"]
            e["default"] = c["tier"] == "t1"
        return e

    pack = {
        "pack": {"id": "zenonmon", "name": "Zenon Mon", "version": "0.1.0"},
        "minecraft": MC,
        "install": {"instanceName": "ZenonMon", "isolated": True},
        "server": {"name": "Zenon Mon", "address": "TODO_HOST:25566"},
        "required": [entry(c, False) for c in buckets["required"]],
        "optional": [entry(c, True) for c in (buckets["t1"] + buckets["t2"])],
        "libraries": [{"file": c["file"], "name": c["name"]} for c in buckets["lib"]],
        "overrides": overrides,
        "_excluded_server_only": [c["file"] for c in buckets["server"]],
    }

    n = {k: len(v) for k, v in buckets.items()}
    total = sum(n.values())
    client_total = n["required"] + n["t1"] + n["t2"] + n["lib"]
    EXPECTED_TOTAL = 84  # 현 모드팩 jar 수(회귀 체크). 모드 추가/제거 시 갱신. (86 − xaero 2, 결정 047)
    print(f"분류 결과: T0={n['required']} / T1={n['t1']} / T2={n['t2']} / "
          f"L={n['lib']} / S(제외)={n['server']}  (전수 {total}={EXPECTED_TOTAL} "
          f"{'OK' if total == EXPECTED_TOTAL else 'MISMATCH'})", file=sys.stderr)
    print(f"클라 번들 = required+optional+libraries = {client_total}개 "
          f"(서버전용 {n['server']}개 제외)", file=sys.stderr)

    js = json.dumps(pack, ensure_ascii=False, indent=2)
    if check_only:
        print("--check: 분류 검증만 (파일 미생성)", file=sys.stderr)
    elif out_path == "-":
        print(js)
    else:
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(js + "\n")
        print(f"생성: {out_path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
