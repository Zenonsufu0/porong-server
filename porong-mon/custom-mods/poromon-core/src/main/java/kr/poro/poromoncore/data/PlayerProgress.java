package kr.poro.poromoncore.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

import java.util.TreeSet;

/**
 * 플레이어별 진행 상태 (database_schema.md §3-1, 0.1 부분집합).
 * 포켓몬 파티/PC는 저장하지 않는다(Cobblemon 소관) — 진행 플래그/수치만.
 */
public class PlayerProgress {
    public static final int SCHEMA_VERSION = 1;

    public long firstJoinEpoch = 0L;
    public boolean leaguePassGiven = false;
    public long balance = 0L;                 // 골드 단일 화폐 (economy_design.md)

    // 배틀타워 (league_season_design §3, battle_tower_design)
    public int battleTowerHighestClearedFloor = 0;
    public final TreeSet<Integer> battleTowerRewardedFloors = new TreeSet<>();

    // 정규리그 (league_season_design §4): 점수제 래더. rankedInit=false면 미참가(점수 표시 안 함)
    public boolean rankedInit = false;
    public int rankedScore = 0;
    public int rankedWins = 0;
    public int rankedLosses = 0;

    // 홈 (결정 029): 해금된 슬롯 수(기본 1) + 슬롯별 등록 위치(null=미등록)
    public static final int HOME_MAX = 5;
    public int homesUnlocked = 1;
    public final Home[] homes = new Home[HOME_MAX];

    // 관장 배지 (gym_badge_design): 획득한 배지(=관장) id 집합
    public final java.util.Set<String> badges = new java.util.HashSet<>();

    // 전설 제단 해금 (결정 031): 해금한 등급(tier) 집합 — 해당 등급 조우권 사용 선행조건
    public final java.util.Set<String> altarsUnlocked = new java.util.HashSet<>();

    // 마개조 해제 포켓몬 (결정 033-a): 정수·기술머신 사용한 포켓몬 UUID — off-learnset 각인 허용
    public final java.util.Set<String> makeoverPokemon = new java.util.HashSet<>();
    // 특성 마개조 해제 포켓몬 (결정 034): 정수·특성 사용한 포켓몬 UUID — 임의 특성 강제 부여 허용
    public final java.util.Set<String> abilityMakeoverPokemon = new java.util.HashSet<>();

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("schemaVersion", SCHEMA_VERSION);
        nbt.putLong("firstJoinEpoch", firstJoinEpoch);
        nbt.putBoolean("leaguePassGiven", leaguePassGiven);
        nbt.putLong("balance", balance);

        NbtCompound tower = new NbtCompound();
        tower.putInt("highestClearedFloor", battleTowerHighestClearedFloor);
        NbtList rewarded = new NbtList();
        for (int floor : battleTowerRewardedFloors) {
            rewarded.add(NbtInt.of(floor));
        }
        tower.put("rewardedFloors", rewarded);
        nbt.put("battleTower", tower);

        NbtCompound ranked = new NbtCompound();
        ranked.putBoolean("init", rankedInit);
        ranked.putInt("score", rankedScore);
        ranked.putInt("wins", rankedWins);
        ranked.putInt("losses", rankedLosses);
        nbt.put("rankedLeague", ranked);

        nbt.putInt("homesUnlocked", homesUnlocked);
        NbtCompound homesNbt = new NbtCompound();
        for (int i = 0; i < HOME_MAX; i++) {
            if (homes[i] != null) homesNbt.put(Integer.toString(i), homes[i].writeNbt());
        }
        nbt.put("homes", homesNbt);

        NbtList badgesNbt = new NbtList();
        for (String b : badges) badgesNbt.add(net.minecraft.nbt.NbtString.of(b));
        nbt.put("badges", badgesNbt);

        NbtList altarsNbt = new NbtList();
        for (String a : altarsUnlocked) altarsNbt.add(net.minecraft.nbt.NbtString.of(a));
        nbt.put("altarsUnlocked", altarsNbt);

        NbtList moNbt = new NbtList();
        for (String u : makeoverPokemon) moNbt.add(net.minecraft.nbt.NbtString.of(u));
        nbt.put("makeoverPokemon", moNbt);

        NbtList aboNbt = new NbtList();
        for (String u : abilityMakeoverPokemon) aboNbt.add(net.minecraft.nbt.NbtString.of(u));
        nbt.put("abilityMakeoverPokemon", aboNbt);
        return nbt;
    }

    public static PlayerProgress readNbt(NbtCompound nbt) {
        PlayerProgress p = new PlayerProgress();
        p.firstJoinEpoch = nbt.getLong("firstJoinEpoch");
        p.leaguePassGiven = nbt.getBoolean("leaguePassGiven");
        p.balance = nbt.getLong("balance");

        if (nbt.contains("battleTower", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tower = nbt.getCompound("battleTower");
            p.battleTowerHighestClearedFloor = tower.getInt("highestClearedFloor");
            NbtList rewarded = tower.getList("rewardedFloors", NbtElement.INT_TYPE);
            for (int i = 0; i < rewarded.size(); i++) {
                p.battleTowerRewardedFloors.add(rewarded.getInt(i));
            }
        }

        if (nbt.contains("rankedLeague", NbtElement.COMPOUND_TYPE)) {
            NbtCompound ranked = nbt.getCompound("rankedLeague");
            p.rankedInit = ranked.getBoolean("init");
            p.rankedScore = ranked.getInt("score");
            p.rankedWins = ranked.getInt("wins");
            p.rankedLosses = ranked.getInt("losses");
        }

        if (nbt.contains("homesUnlocked", NbtElement.INT_TYPE)) {
            p.homesUnlocked = Math.max(1, Math.min(HOME_MAX, nbt.getInt("homesUnlocked")));
        }
        if (nbt.contains("homes", NbtElement.COMPOUND_TYPE)) {
            NbtCompound homesNbt = nbt.getCompound("homes");
            for (int i = 0; i < HOME_MAX; i++) {
                String key = Integer.toString(i);
                if (homesNbt.contains(key, NbtElement.COMPOUND_TYPE)) {
                    p.homes[i] = Home.readNbt(homesNbt.getCompound(key));
                }
            }
        }

        if (nbt.contains("badges", NbtElement.LIST_TYPE)) {
            NbtList badgesNbt = nbt.getList("badges", NbtElement.STRING_TYPE);
            for (int i = 0; i < badgesNbt.size(); i++) p.badges.add(badgesNbt.getString(i));
        }
        if (nbt.contains("altarsUnlocked", NbtElement.LIST_TYPE)) {
            NbtList altarsNbt = nbt.getList("altarsUnlocked", NbtElement.STRING_TYPE);
            for (int i = 0; i < altarsNbt.size(); i++) p.altarsUnlocked.add(altarsNbt.getString(i));
        }
        if (nbt.contains("makeoverPokemon", NbtElement.LIST_TYPE)) {
            NbtList moNbt = nbt.getList("makeoverPokemon", NbtElement.STRING_TYPE);
            for (int i = 0; i < moNbt.size(); i++) p.makeoverPokemon.add(moNbt.getString(i));
        }
        if (nbt.contains("abilityMakeoverPokemon", NbtElement.LIST_TYPE)) {
            NbtList aboNbt = nbt.getList("abilityMakeoverPokemon", NbtElement.STRING_TYPE);
            for (int i = 0; i < aboNbt.size(); i++) p.abilityMakeoverPokemon.add(aboNbt.getString(i));
        }
        return p;
    }
}
