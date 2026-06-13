package kr.zenon.rpg.listener;

import kr.zenon.rpg.boss.engine.BossRunService;
import kr.zenon.rpg.boss.party.PartyManager;
import kr.zenon.rpg.boss.room.BossDamageTracker;
import kr.zenon.rpg.boss.room.BossRoomManager;
import kr.zenon.rpg.field.FieldTeleportService;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import kr.zenon.rpg.gui.BossAbandonGui;
import kr.zenon.rpg.gui.GuiTitles;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 보스룸에서 영지/필드 이동 시 "보스 포기" 확인 (DL-129 추가#20).
 * 예 → 파티 탈퇴 + 보스룸 퇴장 + 이동 / 아니요 → 계속 전투.
 */
public final class BossAbandonListener implements Listener {

    private final BossRoomManager      bossRoomManager;
    private final PartyManager         partyManager;
    private final FieldTeleportService fieldTeleportService;
    private final BossRunService       bossRunService;
    private final BossDamageTracker    damageTracker;
    /** 이동 대기 목적지: "home" / "visit:<owner>" / "field:<id>". */
    private final Map<UUID, String> pending = new ConcurrentHashMap<>();

    public BossAbandonListener(BossRoomManager bossRoomManager, PartyManager partyManager,
                               FieldTeleportService fieldTeleportService, BossRunService bossRunService,
                               BossDamageTracker damageTracker) {
        this.bossRoomManager      = bossRoomManager;
        this.partyManager         = partyManager;
        this.fieldTeleportService = fieldTeleportService;
        this.bossRunService       = bossRunService;
        this.damageTracker        = damageTracker;
    }

    /** 보스룸이면 포기 확인 GUI를 띄우고 true(이동 보류). 아니면 false(호출자가 정상 이동). */
    public boolean promptIfInRoom(Player player, String destCode) {
        if (!bossRoomManager.isInBossRoom(player.getUniqueId())) return false;
        pending.put(player.getUniqueId(), destCode);
        BossAbandonGui.open(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiTitles.BOSS_ABANDON.equals(event.getView().title())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();
        int slot = event.getRawSlot();

        if (slot == BossAbandonGui.SLOT_NO) {
            pending.remove(uid);
            player.closeInventory();
            player.sendMessage("§7[보스] 전투를 계속합니다.");
            return;
        }
        if (slot == BossAbandonGui.SLOT_YES) {
            String dest = pending.remove(uid);
            // 보스 런 이탈 처리. 솔로/전원 이탈(empty)이면 보스 despawn은 exitRoom→releaseSlot이 단일 처리(DL-129 추가#22).
            // 일부만 남으면 보스 HP를 남은 인원수 비율로 즉시 재조정 — 입힌 피해 보존(max·cur 동시 축소).
            BossRunService.LeaveResult lr = bossRunService.leaveRun(uid.toString());
            if (lr != null && !lr.empty() && lr.oldActiveCount() > 0 && damageTracker != null) {
                java.util.UUID mobUuid = damageTracker.mobForRun(lr.runId());
                if (mobUuid != null && Bukkit.getEntity(mobUuid) instanceof LivingEntity boss) {
                    double ratio = BossRoomListener.partyHpMultiplier(lr.newActiveCount())
                            / BossRoomListener.partyHpMultiplier(lr.oldActiveCount());
                    AttributeInstance hpAttr = boss.getAttribute(Attribute.MAX_HEALTH);
                    if (hpAttr != null && ratio > 0 && ratio < 1.0) {
                        double newMax = hpAttr.getBaseValue() * ratio;
                        double newCur = Math.min(newMax, boss.getHealth() * ratio);
                        hpAttr.setBaseValue(newMax);
                        boss.setHealth(Math.max(1.0, newCur));
                    }
                }
            }
            // 파티 떠남 — 리더면 남은 멤버에게 위임(해체 안 함), 멤버면 탈퇴 (DL-129 추가#25)
            PartyManager.PartyLeaveOutcome outcome = partyManager.leaveOrDelegate(uid);
            if (outcome.type() == PartyManager.LeaveResultType.LEADER_DELEGATED && outcome.newLeaderId() != null) {
                Player newLeader = org.bukkit.Bukkit.getPlayer(outcome.newLeaderId());
                if (newLeader != null) {
                    newLeader.sendMessage("§6[파티] 파티장이 보스룸을 떠나 §f당신§6이 새 파티장이 되었습니다.");
                }
            }
            bossRoomManager.exitRoom(uid);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.closeInventory();
            player.sendMessage("§c[보스] 도전을 포기하고 이동합니다.");
            if (dest == null) return;
            if (dest.equals("home")) {
                player.performCommand("is home");
            } else if (dest.startsWith("visit:")) {
                player.performCommand("is visit " + dest.substring(6));
            } else if (dest.startsWith("field:")) {
                fieldTeleportService.teleportToField(player, dest.substring(6));
            }
        }
    }
}
