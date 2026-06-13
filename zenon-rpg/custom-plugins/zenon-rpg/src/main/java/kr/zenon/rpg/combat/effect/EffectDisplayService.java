package kr.zenon.rpg.combat.effect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 2D 스킬/보스 이펙트를 ItemDisplay(빌보드)로 띄우는 서비스 (skill_effect_2d_integration_v1).
 *
 * 리소스팩: carrier 아이템 {@code firework_star} + {@code custom_model_data}(문자열 케이스) → poro:effect/* 평면 모델.
 * ModelEngine 없이 바닐라만으로 동작. 짧은 수명 + 스케일-팝 + 풀밝기.
 */
public final class EffectDisplayService {

    /** 디스플레이 전용 운반 아이템(플레이어에게 지급하지 않음). paper=평범한 item/generated 렌더라 커스텀 모델 안정.
     *  (firework_star는 폭발색 특수 렌더라 부적합 — paper 400101은 미사용 슬롯.) */
    private static final Material CARRIER = Material.PAPER;

    private final JavaPlugin plugin;

    public EffectDisplayService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private ItemStack carrier(int cmd) {
        ItemStack item = new ItemStack(CARRIER);
        ItemMeta meta = item.getItemMeta();
        CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        comp.setStrings(List.of(String.valueOf(cmd)));   // select property "custom_model_data" strings[0] 매칭
        meta.setCustomModelDataComponent(comp);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 플레이어 전방에 평면 이펙트를 띄운다. 빌보드 VERTICAL(시전자/관전자 정면), 등장 스케일-팝, {@code lifeTicks} 후 제거.
     * (PoC: 베기/직선 검증용. 방향 정렬 DIRECTIONAL은 후속.)
     */
    public ItemDisplay spawnFront(int cmd, Player player, double distance, float scale, int lifeTicks) {
        Location eye = player.getEyeLocation();
        Location loc = eye.clone().add(eye.getDirection().clone().normalize().multiply(distance));
        loc.setYaw(0f);
        loc.setPitch(0f);

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.VERTICAL);
            e.setBrightness(new Display.Brightness(15, 15));   // 풀밝기(글로우)
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            float s0 = scale * 0.3f;
            e.setTransformation(transform(s0));                // 시작은 작게
        });

        // 다음 틱: 풀 스케일로 보간(팝)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(3);
            display.setTransformation(transform(scale));
        }, 1L);

        // 수명 후 제거 (누수 방지)
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));

        return display;
    }

    /**
     * 검기/투사체형 이펙트 — 플레이어 전방에서 시선 방향으로 {@code travel}블록 활공(보간 텔레포트)하며 표시.
     * 빌보드 VERTICAL(정면), 등장 스케일-팝, 풀밝기, {@code lifeTicks} 후 제거.
     */
    public ItemDisplay spawnSlash(int cmd, Player player, double travel, float scale, int lifeTicks) {
        return spawnSlash(cmd, player, travel, scale, lifeTicks, 0f, 0f);
    }

    public ItemDisplay spawnSlash(int cmd, Player player, double travel, float scale, int lifeTicks, float rollDeg) {
        return spawnSlash(cmd, player, travel, scale, lifeTicks, rollDeg, 0f);
    }

    /**
     * {@code rollDeg}: 시선축(Z) 롤 — 가로 선/빔을 90°면 세로로 세움.
     * {@code pitchDeg}: X축 기울임 — 빌보드(정면 가시성) 유지하며 위쪽을 앞으로 기울여 "발사감"을 준다.
     */
    public ItemDisplay spawnSlash(int cmd, Player player, double travel, float scale, int lifeTicks,
                                  float rollDeg, float pitchDeg) {
        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().clone().normalize();
        Location start = player.getEyeLocation().add(dir.clone().multiply(1.2));
        start.setYaw(0f);
        start.setPitch(0f);
        Location end = start.clone().add(dir.clone().multiply(travel));

        int glide = Math.max(1, lifeTicks - 1);
        ItemDisplay display = player.getWorld().spawn(start, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.VERTICAL);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            e.setViewRange(1.5f);
            e.setTeleportDuration(glide);              // 위치 보간(활공)
            e.setTransformation(transform(scale * 0.5f, rollDeg, pitchDeg));
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(glide);
            display.setTransformation(transform(scale, rollDeg, pitchDeg));   // 스케일-팝
            display.teleport(end);                                  // 전방 활공
        }, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));
        return display;
    }

    /**
     * 발사체형 이펙트 — 빌보드 OFF(FIXED), 스프라이트를 시선 방향으로 정렬해 "앞으로 발사"되게 한다.
     * (모델 X축 → 전방, 법선 → 위. 빔/볼트가 진행 방향으로 뻗는다.) 전방 활공 + 스케일-팝.
     */
    public ItemDisplay spawnProjectile(int cmd, Player player, double travel, float scale, int lifeTicks) {
        org.bukkit.util.Vector dvec = player.getEyeLocation().getDirection().clone().normalize();
        Location start = player.getEyeLocation().add(dvec.clone().multiply(1.5));
        start.setYaw(0f);
        start.setPitch(0f);
        Location end = start.clone().add(dvec.clone().multiply(travel));

        Vector3f fwd = new Vector3f((float) dvec.getX(), (float) dvec.getY(), (float) dvec.getZ()).normalize();
        Vector3f up0 = Math.abs(fwd.y()) > 0.98f ? new Vector3f(0f, 0f, 1f) : new Vector3f(0f, 1f, 0f);
        Vector3f nrm = new Vector3f(up0).sub(new Vector3f(fwd).mul(up0.dot(fwd))).normalize(); // 전방에 수직인 위
        Vector3f side = new Vector3f(nrm).cross(fwd).normalize();
        Quaternionf q = new Matrix3f().setColumn(0, fwd).setColumn(1, side).setColumn(2, nrm)
                .getNormalizedRotation(new Quaternionf());

        int glide = Math.max(1, lifeTicks - 1);
        ItemDisplay display = player.getWorld().spawn(start, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            e.setViewRange(1.5f);
            e.setTeleportDuration(glide);
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale * 0.5f, scale * 0.5f, scale * 0.5f),
                    new Quaternionf()));
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(glide);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            display.teleport(end);
        }, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));
        return display;
    }

    /**
     * 바닥에 누운(수평) 정적 빔 — 플레이어로부터 {@code range}블록까지 정확히 뻗고 그 거리에서 끝난다.
     * 법선=월드 위, 긴 축=수평 시선. 위에서 보면 앞으로 뻗는 빔. 중앙 정렬 모델 전제.
     * {@code width}: 빔 폭(블록, 등장 시 얇게→full 폭 플래시). {@code heightOffset}: 발 기준 높이.
     */
    public ItemDisplay spawnGround(int cmd, Player player, double range, double width, int lifeTicks, double heightOffset) {
        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().clone();
        dir.setY(0);
        if (dir.lengthSquared() < 1e-4) dir = new org.bukkit.util.Vector(1, 0, 0);
        dir.normalize();
        // 중앙 정렬 모델 → 중심을 사거리 절반 앞에 두면 빔이 [플레이어, range]를 정확히 span.
        Location center = player.getLocation().clone().add(0, heightOffset, 0).add(dir.clone().multiply(range / 2.0));
        center.setYaw(0f);
        center.setPitch(0f);

        Vector3f fwd = new Vector3f((float) dir.getX(), 0f, (float) dir.getZ()).normalize();
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f side = new Vector3f(up).cross(fwd).normalize();
        Quaternionf q = new Matrix3f().setColumn(0, fwd).setColumn(1, side).setColumn(2, up)
                .getNormalizedRotation(new Quaternionf());

        float rangeF = (float) range;
        float widthF = (float) width;
        ItemDisplay display = player.getWorld().spawn(center, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            e.setViewRange(1.5f);
            // X(forward)=사거리, Y(side)=폭(시작 얇게), Z=1
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(rangeF, widthF * 0.35f, 1f), new Quaternionf()));
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(rangeF, widthF, 1f), new Quaternionf()));   // 폭 플래시
        }, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));
        return display;
    }

    /**
     * 바닥 평면 비행 — 수평으로 깔린 스프라이트(법선=위)가 시선 전방으로 {@code range}블록까지 날아간다.
     * 늘리지 않고 균일 스케일로 활공. {@code heightOffset}: 발 기준 높이.
     */
    public ItemDisplay spawnGroundTravel(int cmd, Player player, double range, float scale, int lifeTicks, double heightOffset) {
        return spawnGroundTravel(cmd, player, range, scale, lifeTicks, heightOffset, false);
    }

    /**
     * {@code followPitch}: true면 시선 상하(pitch)까지 따라 3D 조준 방향으로 날아간다(석궁 등 공중 저격).
     * false면 수평 바닥 평면 비행. 스프라이트 법선은 "전방에 수직인 위"라 수평일 땐 바닥과 동일.
     */
    public ItemDisplay spawnGroundTravel(int cmd, Player player, double range, float scale, int lifeTicks,
                                         double heightOffset, boolean followPitch) {
        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().clone();
        if (!followPitch) dir.setY(0);
        if (dir.lengthSquared() < 1e-4) dir = new org.bukkit.util.Vector(1, 0, 0);
        dir.normalize();
        // followPitch 여부와 무관하게 발 높이 기준 — 수평 조준 시 평면이 눈 아래라 잘 보임(창처럼).
        Location base = player.getLocation().clone().add(0, heightOffset, 0);
        Location start = base.clone().add(dir.clone().multiply(1.0));
        start.setYaw(0f);
        start.setPitch(0f);
        Location end = base.clone().add(dir.clone().multiply(range));
        end.setYaw(0f);     // start와 동일 회전 — 활공 중 yaw 보간(회전) 방지
        end.setPitch(0f);

        Vector3f fwd = new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()).normalize();
        Vector3f up0 = Math.abs(fwd.y()) > 0.98f ? new Vector3f(0f, 0f, 1f) : new Vector3f(0f, 1f, 0f);
        Vector3f nrm = new Vector3f(up0).sub(new Vector3f(fwd).mul(up0.dot(fwd))).normalize(); // 전방에 수직인 위
        Vector3f side = new Vector3f(nrm).cross(fwd).normalize();
        Quaternionf q = new Matrix3f().setColumn(0, fwd).setColumn(1, side).setColumn(2, nrm)
                .getNormalizedRotation(new Quaternionf());

        int glide = Math.max(1, lifeTicks - 2);
        ItemDisplay display = player.getWorld().spawn(start, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            e.setViewRange(1.5f);
            e.setTeleportDuration(glide);
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale * 0.6f, scale * 0.6f, scale * 0.6f), new Quaternionf()));
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(glide);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale, scale, scale), new Quaternionf()));
            display.teleport(end);
        }, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));
        return display;
    }

    /**
     * 바닥 평면 장판/표식 — 위치 {@code loc}에 수평으로 깔리는 원형/마크 스프라이트(법선=위), 균일 스케일 + 스케일-팝.
     * 도끼 착탄 균열·스태프 광역 원·낫 처형 표식·공용 임팩트 등.
     */
    public ItemDisplay spawnDecal(int cmd, Location loc, float scale, int lifeTicks) {
        Location at = loc.clone();
        at.setYaw(0f);
        at.setPitch(0f);

        Vector3f fwd = new Vector3f(1f, 0f, 0f);
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f side = new Vector3f(up).cross(fwd).normalize();
        Quaternionf q = new Matrix3f().setColumn(0, fwd).setColumn(1, side).setColumn(2, up)
                .getNormalizedRotation(new Quaternionf());

        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class, e -> {
            e.setItemStack(carrier(cmd));
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0f);
            e.setShadowStrength(0f);
            e.setPersistent(false);
            e.setViewRange(1.5f);
            e.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale * 0.4f, scale * 0.4f, scale * 0.4f), new Quaternionf()));
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(3);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), q,
                    new Vector3f(scale, scale, scale), new Quaternionf()));
        }, 1L);

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, Math.max(1, lifeTicks));
        return display;
    }

    private static Transformation transform(float scale) {
        return transform(scale, 0f, 0f);
    }

    private static Transformation transform(float scale, float rollDeg) {
        return transform(scale, rollDeg, 0f);
    }

    private static Transformation transform(float scale, float rollDeg, float pitchDeg) {
        Quaternionf q = new Quaternionf()
                .rotateZ((float) Math.toRadians(rollDeg))     // 시선축 롤(세로 세움)
                .rotateX((float) Math.toRadians(pitchDeg));   // 전방 기울임(발사감)
        return new Transformation(
                new Vector3f(0f, 0f, 0f), q,
                new Vector3f(scale, scale, scale),
                new Quaternionf());
    }
}
