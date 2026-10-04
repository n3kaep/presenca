package com.presenca;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A Presença: humanoide de olhos brancos que some se você encara e caça quem está isolado. */
public class StalkerEntity extends HostileEntity {
    private final Map<UUID, Integer> watched = new HashMap<>();
    private int life = 0;

    public StalkerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        ServerWorld w = (ServerWorld) getWorld();
        life++;
        if (age % 20 == 0) pickTarget(w);
        checkWatched(w);
        if (life > Config.get().stalkerLifetimeTicks && getTarget() == null) vanish(w);
    }

    /** Escolhe o jogador mais vulnerável: sanidade baixa e longe dos amigos. */
    private void pickTarget(ServerWorld w) {
        ServerPlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (ServerPlayerEntity p : w.getPlayers(pl -> !pl.isSpectator() && !pl.isCreative() && pl.squaredDistanceTo(this) < 64 * 64)) {
            double isolation = 100;
            for (ServerPlayerEntity o : w.getPlayers(x -> x != p && !x.isSpectator())) {
                isolation = Math.min(isolation, p.distanceTo(o));
            }
            double score = Sanity.get(p) - isolation * 0.5;
            if (score < bestScore) { bestScore = score; best = p; }
        }
        if (best != null) setTarget(best);
    }

    /** Se alguém encara por muito tempo, ele desaparece (e quem olhou perde sanidade). */
    private void checkWatched(ServerWorld w) {
        for (ServerPlayerEntity p : w.getPlayers(pl -> !pl.isSpectator() && pl.squaredDistanceTo(this) < 40 * 40)) {
            Vec3d to = getEyePos().subtract(p.getEyePos()).normalize();
            boolean looking = p.getRotationVec(1f).dotProduct(to) > 0.96 && p.canSee(this);
            int t = watched.getOrDefault(p.getUuid(), 0);
            t = looking ? t + 1 : Math.max(0, t - 2);
            watched.put(p.getUuid(), t);
            if (t > Config.get().lookTicks && distanceTo(p) > 4) {
                Sanity.add(p, -4);
                vanish(w);
                return;
            }
        }
    }

    private void vanish(ServerWorld w) {
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.02);
        w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, SoundCategory.HOSTILE, 1f, 0.7f);
        discard();
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof PlayerEntity p) Sanity.add(p, -(float) Config.get().hitSanityLoss);
        return hit;
    }

    @Override public boolean isFireImmune() { return true; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource s) { return SoundEvents.ENTITY_ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDERMAN_DEATH; }
}
