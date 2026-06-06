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

    // 홈 (결정 029): 해금된 슬롯 수(기본 1) + 슬롯별 등록 위치(null=미등록)
    public static final int HOME_MAX = 5;
    public int homesUnlocked = 1;
    public final Home[] homes = new Home[HOME_MAX];

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

        nbt.putInt("homesUnlocked", homesUnlocked);
        NbtCompound homesNbt = new NbtCompound();
        for (int i = 0; i < HOME_MAX; i++) {
            if (homes[i] != null) homesNbt.put(Integer.toString(i), homes[i].writeNbt());
        }
        nbt.put("homes", homesNbt);
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
        return p;
    }
}
