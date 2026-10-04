package com.presenca;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Sistema de sanidade por jogador (salvo no jogador, funciona no servidor). */
public final class Sanity {
    public static final AttachmentType<Float> DATA =
            AttachmentRegistry.createPersistent(Presenca.id("sanity"), Codec.FLOAT);

    public static float get(PlayerEntity p) { return p.getAttachedOrElse(DATA, 100f); }
    public static void set(PlayerEntity p, float v) { p.setAttached(DATA, MathHelper.clamp(v, 0f, 100f)); }
    public static void add(PlayerEntity p, float d) { set(p, get(p) + d); }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0 || !Config.get().enabled) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p.isSpectator() || p.isCreative()) continue;
                tick(p);
            }
        });
    }

    private static void tick(ServerPlayerEntity p) {
        Config c = Config.get();
        ServerWorld w = p.getServerWorld();
        BlockPos pos = p.getBlockPos();
        int light = w.getLightLevel(pos);
        boolean dark = light < 4;
        boolean together = !w.getPlayers(o -> o != p && !o.isSpectator() && o.squaredDistanceTo(p) < 20 * 20).isEmpty();
        boolean stalkerNear = !w.getEntitiesByType(ModEntities.STALKER, p.getBoundingBox().expand(24), e -> true).isEmpty();

        double d = 0;
        if (dark) d -= c.darkLoss;
        if (!together) d -= c.aloneLoss;
        if (stalkerNear) d -= c.stalkerLoss;
        if (!dark && together && !stalkerNear) d += c.recover;
        add(p, (float) d);

        float s = get(p);
        var r = p.getRandom();

        if (s < 60 && r.nextFloat() < 0.15f) fakeFootstep(p);
        if (s < 40 && r.nextFloat() < 0.10f)
            soundTo(p, SoundEvents.AMBIENT_CAVE.value(), p.getX(), p.getY(), p.getZ(), 1f, 0.8f + r.nextFloat() * 0.4f);
        if (s < 30 && r.nextFloat() < 0.05f)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0, false, false, false));
        if (s < 20) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, false));
            if (r.nextFloat() < 0.08f) p.sendMessage(Text.literal("§8Você não está sozinho..."), true);
        }
        if (s < 30 && !stalkerNear && r.nextDouble() < c.spawnChance
                && w.getEntitiesByType(ModEntities.STALKER, p.getBoundingBox().expand(64), e -> true).isEmpty()) {
            spawnStalker(p, 14, 22, false);
        }
    }

    // ---------- helpers de som / spawn ----------

    /** Toca um som que SÓ esse jogador ouve. */
    public static void soundTo(ServerPlayerEntity p, SoundEvent s, double x, double y, double z, float vol, float pitch) {
        p.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(s), SoundCategory.AMBIENT, x, y, z, vol, pitch, p.getRandom().nextLong()));
    }

    public static void fakeFootstep(ServerPlayerEntity p) {
        Vec3d look = flatLook(p);
        Vec3d pos = p.getPos().add(look.multiply(-4));
        soundTo(p, SoundEvents.BLOCK_GRAVEL_STEP, pos.x, pos.y, pos.z, 1f, 0.8f);
    }

    private static Vec3d flatLook(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f).multiply(1, 0, 1);
        return look.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : look.normalize();
    }

    public static BlockPos findSpot(ServerWorld w, double x, double y, double z) {
        BlockPos base = BlockPos.ofFloored(x, y, z);
        if (!w.isChunkLoaded(base)) return null;
        for (int dy = 0; dy <= 6; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = base.up(dy * sign);
                if (w.getBlockState(p).isAir() && w.getBlockState(p.up()).isAir()
                        && w.getBlockState(p.down()).isSolidBlock(w, p.down())) return p;
            }
        }
        return null;
    }

    public static StalkerEntity spawnStalker(ServerPlayerEntity p, double min, double max, boolean front) {
        ServerWorld w = p.getServerWorld();
        Vec3d look = flatLook(p);
        for (int i = 0; i < 12; i++) {
            double dist = min + p.getRandom().nextDouble() * (max - min);
            Vec3d side = new Vec3d(-look.z, 0, look.x).multiply((p.getRandom().nextDouble() - 0.5) * 8);
            Vec3d pos = p.getPos().add(look.multiply(front ? dist : -dist)).add(side);
            BlockPos bp = findSpot(w, pos.x, p.getY(), pos.z);
            if (bp == null) continue;
            StalkerEntity s = ModEntities.STALKER.spawn(w, bp, SpawnReason.EVENT);
            if (s != null) { s.setTarget(p); return s; }
        }
        return null;
    }

    // ---------- eventos (usados pelos comandos) ----------

    public static void eventBlackout(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 240, 0, false, false, false));
        soundTo(p, SoundEvents.AMBIENT_CAVE.value(), p.getX(), p.getY(), p.getZ(), 1f, 0.6f);
    }

    public static void eventHeartbeat(ServerPlayerEntity p) {
        soundTo(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, p.getX(), p.getY(), p.getZ(), 1.5f, 1f);
    }

    public static void eventWhisper(ServerPlayerEntity p) {
        String[] msgs = {"§8Eu estou atrás de você.", "§8Não olhe.", "§8Ele sabe onde você está.", "§8Por que você está sozinho?"};
        p.sendMessage(Text.literal(msgs[p.getRandom().nextInt(msgs.length)]), true);
        soundTo(p, SoundEvents.ENTITY_ENDERMAN_STARE, p.getX(), p.getY(), p.getZ(), 0.6f, 0.5f);
    }

    public static void eventJumpscare(ServerPlayerEntity p) {
        spawnStalker(p, 5, 7, true);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0, false, false, false));
        soundTo(p, SoundEvents.ENTITY_ENDERMAN_SCREAM, p.getX(), p.getY(), p.getZ(), 1.5f, 0.6f);
        add(p, -10);
    }
}
