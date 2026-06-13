package kr.zenon.moncore.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** 등록된 홈 위치 1칸 (이름 + 차원 + 좌표 + 시선). PlayerProgress.homes 에 저장. */
public final class Home {
    public String name;            // null=기본 이름("N번 홈") 사용
    public final String dimension; // 예: "minecraft:overworld"
    public final double x, y, z;
    public final float yaw, pitch;

    public Home(String name, String dimension, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.dimension = dimension;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
    }

    /** 플레이어의 현재 위치로 홈 생성(이름은 유지/지정). */
    public static Home ofCurrent(ServerPlayerEntity player, String name) {
        ServerWorld world = player.getServerWorld();
        return new Home(name, world.getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
    }

    /** 표시 이름(없으면 기본 "N번 홈"). slot은 0-based. */
    public String displayName(int slot) {
        return (name == null || name.isBlank()) ? (slot + 1) + "번 홈" : name;
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        if (name != null && !name.isBlank()) nbt.putString("name", name);
        nbt.putString("dim", dimension);
        nbt.putDouble("x", x);
        nbt.putDouble("y", y);
        nbt.putDouble("z", z);
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("pitch", pitch);
        return nbt;
    }

    public static Home readNbt(NbtCompound nbt) {
        return new Home(nbt.contains("name") ? nbt.getString("name") : null,
                nbt.getString("dim"),
                nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"),
                nbt.getFloat("yaw"), nbt.getFloat("pitch"));
    }
}
