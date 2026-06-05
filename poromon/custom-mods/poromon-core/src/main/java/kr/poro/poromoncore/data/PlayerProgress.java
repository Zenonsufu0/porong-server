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
        return p;
    }
}
