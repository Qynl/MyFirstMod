package dev.qynl.myfirstmod.boss;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.world.World;

public class NullWardenEntity extends WardenEntity {
    private boolean defeatPending;
    private static final TrackedData<Byte> CINEMATIC =
            DataTracker.registerData(NullWardenEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> PHASE =
            DataTracker.registerData(NullWardenEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> ATTACK =
            DataTracker.registerData(NullWardenEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> ATTACK_PROGRESS =
            DataTracker.registerData(NullWardenEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> DEFEATED =
            DataTracker.registerData(NullWardenEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public NullWardenEntity(EntityType<? extends WardenEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CINEMATIC, (byte) 0);
        builder.add(PHASE, (byte) 1);
        builder.add(ATTACK, (byte) 0);
        builder.add(ATTACK_PROGRESS, (byte) 0);
        builder.add(DEFEATED, false);
    }

    public void setVisualState(int phase, int attack, int attackProgress, boolean defeated) {
        getDataTracker().set(PHASE, (byte) Math.max(1, Math.min(4, phase)));
        getDataTracker().set(ATTACK, (byte) Math.max(0, Math.min(7, attack)));
        getDataTracker().set(ATTACK_PROGRESS, (byte) Math.max(0, Math.min(127, attackProgress)));
        getDataTracker().set(DEFEATED, defeated);
    }

    public void setCinematic(int stage) { getDataTracker().set(CINEMATIC, (byte) stage); }
    public int getCinematic() { return getDataTracker().get(CINEMATIC); }
    public boolean isDefeatPending() { return defeatPending; }

    @Override public void onDeath(net.minecraft.entity.damage.DamageSource source) {
        // Keep the actor alive for the finale; vanilla death removal used to truncate it after 20 ticks.
        defeatPending = true;
        setHealth(1);
        setInvulnerable(true);
        setAiDisabled(true);
    }
    @Override public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("NullDefeatPending", defeatPending);
    }
    @Override public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        defeatPending = nbt.getBoolean("NullDefeatPending");
    }

    public int getVisualPhase() {
        return getDataTracker().get(PHASE);
    }

    public int getVisualAttack() {
        return getDataTracker().get(ATTACK);
    }

    public float getVisualAttackProgress() {
        return getDataTracker().get(ATTACK_PROGRESS) / 100.0f;
    }

    public boolean isVisualDefeated() {
        return getDataTracker().get(DEFEATED);
    }
}
