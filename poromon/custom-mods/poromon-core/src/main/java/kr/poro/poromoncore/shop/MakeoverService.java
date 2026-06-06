package kr.poro.poromoncore.shop;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poro.poromoncore.data.PlayerProgress;
import kr.poro.poromoncore.data.PoroMonState;
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
        PlayerProgress pr = PoroMonState.get(player.getServer()).getOrCreate(player.getUuid());
        return pr.makeoverPokemon.contains(pk.getUuid().toString());
    }

    /** 해제(이미 해제면 false). */
    public static boolean unlock(ServerPlayerEntity player, Pokemon pk) {
        PoroMonState st = PoroMonState.get(player.getServer());
        PlayerProgress pr = st.getOrCreate(player.getUuid());
        boolean added = pr.makeoverPokemon.add(pk.getUuid().toString());
        if (added) st.markDirty();
        return added;
    }
}
