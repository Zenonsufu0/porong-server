package kr.zenon.moncore.shop;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.zenon.moncore.data.PlayerProgress;
import kr.zenon.moncore.data.ZenonMonState;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * 마개조 해제 상태 관리 (결정 033-a). 해금석 = 그 포켓몬 영구 해제(소모).
 * 해제 상태는 PlayerProgress.makeoverPokemon(UUID 집합)에 영속. off-learnset 각인 허용 여부.
 */
public final class MakeoverService {
    private MakeoverService() {}

    /** 플레이어 파티에서 UUID로 포켓몬 조회(없으면 null). */
    public static Pokemon findPartyPokemon(ServerPlayerEntity player, UUID id) {
        if (id == null) return null;
        for (Pokemon p : Cobblemon.INSTANCE.getStorage().getParty(player)) {
            if (p != null && p.getUuid().equals(id)) return p;
        }
        return null;
    }

    public static boolean isMakeover(ServerPlayerEntity player, Pokemon pk) {
        if (pk == null) return false;
        PlayerProgress pr = ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid());
        return pr.makeoverPokemon.contains(pk.getUuid().toString());
    }

    /** 기술 해제(이미 해제면 false). */
    public static boolean unlock(ServerPlayerEntity player, Pokemon pk) {
        ZenonMonState st = ZenonMonState.get(player.getServer());
        PlayerProgress pr = st.getOrCreate(player.getUuid());
        boolean added = pr.makeoverPokemon.add(pk.getUuid().toString());
        if (added) st.markDirty();
        return added;
    }

    /** 특성 마개조 해제 여부 (결정 034). */
    public static boolean isAbilityMakeover(ServerPlayerEntity player, Pokemon pk) {
        if (pk == null) return false;
        PlayerProgress pr = ZenonMonState.get(player.getServer()).getOrCreate(player.getUuid());
        return pr.abilityMakeoverPokemon.contains(pk.getUuid().toString());
    }

    /** 특성 해제(이미 해제면 false). */
    public static boolean unlockAbility(ServerPlayerEntity player, Pokemon pk) {
        ZenonMonState st = ZenonMonState.get(player.getServer());
        PlayerProgress pr = st.getOrCreate(player.getUuid());
        boolean added = pr.abilityMakeoverPokemon.add(pk.getUuid().toString());
        if (added) st.markDirty();
        return added;
    }
}
