package com.porong.gun.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 전 코어 목록·영역 인덱스의 단일 출처 (M-Core — 월드 SavedData NBT, impl_plan §2).
 *
 * 영역 = 코어 중심 ±{@link #RADIUS}(64×64 고정). 설치 겹침 판정·자물쇠·자가복구의 기준점.
 * 1단계는 좌표·소유자·레벨만. 보안칸·손상 블록 맵은 후속.
 */
public class CoreManager extends SavedData {

    public static final String NAME = "porongun_cores";
    public static final int RADIUS = 32; // 64×64 = ±32

    public record CoreData(@Nullable UUID owner, int level) {}

    private final Map<BlockPos, CoreData> cores = new HashMap<>();

    public CoreManager() {}

    public static CoreManager get(ServerLevel level) {
        // 1.20.1: computeIfAbsent(deserializer, factory, name) — SavedData.Factory는 1.20.2+
        return level.getDataStorage().computeIfAbsent(CoreManager::load, CoreManager::new, NAME);
    }

    /** 새 코어(64×64)가 기존 코어 영역과 겹치면 그 좌표 반환(없으면 null). 맞닿음(거리=64)은 허용. */
    @Nullable
    public BlockPos findOverlap(BlockPos pos) {
        for (BlockPos p : cores.keySet()) {
            if (Math.abs(p.getX() - pos.getX()) < RADIUS * 2
                    && Math.abs(p.getZ() - pos.getZ()) < RADIUS * 2) {
                return p;
            }
        }
        return null;
    }

    public void addCore(BlockPos pos, @Nullable UUID owner) {
        cores.put(pos.immutable(), new CoreData(owner, 1));
        setDirty();
    }

    public void removeCore(BlockPos pos) {
        if (cores.remove(pos) != null) {
            setDirty();
        }
    }

    public boolean has(BlockPos pos) {
        return cores.containsKey(pos);
    }

    public int count() {
        return cores.size();
    }

    public static CoreManager load(CompoundTag tag) {
        CoreManager m = new CoreManager();
        ListTag list = tag.getList("cores", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            BlockPos pos = new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
            UUID owner = c.hasUUID("owner") ? c.getUUID("owner") : null;
            m.cores.put(pos, new CoreData(owner, c.getInt("level")));
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        cores.forEach((pos, data) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("x", pos.getX());
            c.putInt("y", pos.getY());
            c.putInt("z", pos.getZ());
            if (data.owner() != null) {
                c.putUUID("owner", data.owner());
            }
            c.putInt("level", data.level());
            list.add(c);
        });
        tag.put("cores", list);
        return tag;
    }
}
