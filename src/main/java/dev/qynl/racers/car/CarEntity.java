package dev.qynl.racers.car;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.util.Cars;

/**
 * The car: physics, damage, boost and driving, on both logical sides.
 *
 * <p>Server authoritative. The client never simulates the car, it only sends
 * {@link CarControls} and renders the tracked state, which keeps the two sides
 * from ever disagreeing about where a car is.
 *
 * <p>Physics model (one tick = 1/20 s):
 * <ol>
 *   <li>project world velocity into the body frame (forward / lateral)</li>
 *   <li>longitudinal forces: engine, brakes, drag, rolling resistance</li>
 *   <li>lateral tyre force, clamped by the surface grip circle</li>
 *   <li>steering with a bicycle model whose yaw rate is limited by grip</li>
 *   <li>integrate and move with sub-stepping so 46 m/s cannot tunnel</li>
 * </ol>
 */
public abstract class CarEntity extends Entity {
    protected static final TrackedData<String> DATA_TYPE =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.STRING);
    protected static final TrackedData<Integer> DATA_PRIMARY =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Integer> DATA_SECONDARY =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Integer> DATA_ACCENT =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Integer> DATA_LEVELS =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Float> DATA_SPEED =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> DATA_SLIP =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> DATA_STEER =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> DATA_BOOST =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> DATA_HEALTH =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Boolean> DATA_BOOSTING =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Boolean> DATA_DRIFTING =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<String> DATA_DRIVER =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.STRING);
    protected static final TrackedData<Integer> DATA_LIGHT =
            DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // ---- physics state ----------------------------------------------------
    protected double velX;
    protected double velY;
    protected double velZ;
    protected double steerAngle;
    protected double vForward;
    protected double vLateral;
    protected double slip;
    protected double boostCharge = 0.4;
    protected int boostTicks;
    protected float engineHealth = 1.0F;
    protected float tyreHealth = 1.0F;
    protected boolean controlLocked;
    protected boolean aiDriving;
    protected int lastCrashTick = -100;
    protected int lastPadTick = -100;
    protected int stuckTicks;
    protected double odo;
    protected double driftScore;
    protected int engineSoundCooldown;
    protected float bodyRoll;
    protected float bodyPitch;
    protected int airborneTicks;
    /** 0 = free roam, 1 = on the grid / racing, 2 = finished (AI brings it home). */
    protected int raceState;
    protected LivingEntity lastSeenDriver;
    protected CarData cachedData;
    protected UUID storedOwner;
    protected String storedOwnerName = "";
    /** Driver input received from the network (player cars only). */
    protected CarControls driverInput = CarControls.none();

    protected CarEntity(EntityType<? extends CarEntity> type, World world) {
        super(type, world);
        this.setVelocity(Vec3d.ZERO);
    }

    // ------------------------------------------------------------------
    // data plumbing
    // ------------------------------------------------------------------
    public CarData data() {
        if (cachedData == null) {
            cachedData = new CarData(CarType.byId(getDataTracker().get(DATA_TYPE)),
                    getDataTracker().get(DATA_PRIMARY), getDataTracker().get(DATA_SECONDARY),
                    getDataTracker().get(DATA_ACCENT), unpackLevels(getDataTracker().get(DATA_LEVELS)),
                    engineHealth, tyreHealth, storedOwner, storedOwnerName, aiDriving);
        }
        return cachedData;
    }

    public CarType carType() {
        return CarType.byId(getDataTracker().get(DATA_TYPE));
    }

    public CarStats stats() {
        return data().stats();
    }

    public void initFromData(CarData data) {
        this.cachedData = null;
        this.storedOwner = data.owner;
        this.storedOwnerName = data.ownerName == null ? "" : data.ownerName;
        this.engineHealth = MathHelper.clamp(data.engineHealth, 0.0F, 1.0F);
        this.tyreHealth = MathHelper.clamp(data.tyreHealth, 0.05F, 1.0F);
        getDataTracker().set(DATA_TYPE, data.type.id);
        getDataTracker().set(DATA_PRIMARY, data.primaryColor);
        getDataTracker().set(DATA_SECONDARY, data.secondaryColor);
        getDataTracker().set(DATA_ACCENT, data.accentColor);
        getDataTracker().set(DATA_LEVELS, packLevels(data.levels));
        getDataTracker().set(DATA_HEALTH, healthFraction());
        this.boostCharge = Math.min(1.0, 0.35 + 0.15 * data.level(Tune.BOOST));
        getDataTracker().set(DATA_BOOST, (float) boostCharge);
        updateDisplayName();
    }

    public void applyData(CarData data) {
        initFromData(data);
    }

    public static int packLevels(int[] levels) {
        int packed = 0;
        for (int i = 0; i < Math.min(levels.length, Tune.values().length); i++) {
            packed |= MathHelper.clamp(levels[i], 0, 15) << (i * 4);
        }
        return packed;
    }

    public static int[] unpackLevels(int packed) {
        int[] levels = new int[Tune.values().length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (packed >> (i * 4)) & 0xF;
        }
        return levels;
    }

    public int[] levels() {
        return unpackLevels(getDataTracker().get(DATA_LEVELS));
    }

    public UUID owner() {
        return storedOwner;
    }

    public String ownerName() {
        return storedOwnerName;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DATA_TYPE, CarType.SPORT.id);
        builder.add(DATA_PRIMARY, PaintColors.RED);
        builder.add(DATA_SECONDARY, PaintColors.BLACK);
        builder.add(DATA_ACCENT, PaintColors.WHITE);
        builder.add(DATA_LEVELS, 0);
        builder.add(DATA_SPEED, 0.0F);
        builder.add(DATA_SLIP, 0.0F);
        builder.add(DATA_STEER, 0.0F);
        builder.add(DATA_BOOST, 0.4F);
        builder.add(DATA_HEALTH, 1.0F);
        builder.add(DATA_BOOSTING, false);
        builder.add(DATA_DRIFTING, false);
        builder.add(DATA_DRIVER, "");
        builder.add(DATA_LIGHT, 0);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("CarData")) {
            initFromData(CarData.fromNbt(nbt.getCompound("CarData")));
        }
        this.velX = nbt.getDouble("VelX");
        this.velY = nbt.getDouble("VelY");
        this.velZ = nbt.getDouble("VelZ");
        this.steerAngle = nbt.getDouble("Steer");
        this.driftScore = nbt.getDouble("DriftScore");
        this.odo = nbt.getDouble("Odo");
        this.aiDriving = nbt.getBoolean("AiDriving");
        String owner = nbt.getString("Owner");
        if (!owner.isEmpty()) {
            try {
                storedOwner = UUID.fromString(owner);
            } catch (IllegalArgumentException ignored) {
                storedOwner = null;
            }
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("CarData", data().toNbt());
        nbt.putDouble("VelX", velX);
        nbt.putDouble("VelY", velY);
        nbt.putDouble("VelZ", velZ);
        nbt.putDouble("Steer", steerAngle);
        nbt.putDouble("DriftScore", driftScore);
        nbt.putDouble("Odo", odo);
        nbt.putBoolean("AiDriving", aiDriving);
        nbt.putString("Owner", storedOwner == null ? "" : storedOwner.toString());
    }

    @Override
    public Text getName() {
        return carType().label();
    }

    // ------------------------------------------------------------------
    // main tick
    // ------------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();
        syncDriverState();
        if (getWorld().isClient()) {
            clientTick();
            return;
        }
        if (aiDriving) {
            driverInput = aiTick();
        } else if (getDriver() == null) {
            driverInput = CarControls.none();
        }
        CarControls controls = driverInput;
        if (controlLocked) {
            controls = CarControls.none();
        }
        physicsTick(controls);
        collideWithCars();
        serverEffects();
        if (getWorld().getTime() % 200 == 0) {
            getDataTracker().set(DATA_LIGHT, 0);
        }
    }

    /** Keeps mount/dismount bookkeeping in one place (no fragile hooks needed). */
    protected void syncDriverState() {
        LivingEntity driver = getDriver();
        if (driver != lastSeenDriver) {
            if (driver != null) {
                onDriverMount(driver);
            } else if (lastSeenDriver != null) {
                onDriverDismount();
            }
            lastSeenDriver = driver;
        }
    }

    protected CarControls aiTick() {
        if (aiDriver == null) {
            aiDriver = new dev.qynl.racers.ai.AiDriver();
        }
        return aiDriver.tick(this);
    }

    protected dev.qynl.racers.ai.AiDriver aiDriver;
    protected double aiSkill = 0.72;
    protected boolean aiRubberBand = true;

    public dev.qynl.racers.ai.AiDriver aiDriver() {
        return aiDriver;
    }

    public double aiSkill() {
        return aiSkill;
    }

    public void setAiSkill(double skill) {
        this.aiSkill = MathHelper.clamp(skill, 0.2, 1.0);
    }

    public boolean aiRubberBand() {
        return aiRubberBand;
    }

    public void setAiRubberBand(boolean value) {
        this.aiRubberBand = value;
    }

    // ------------------------------------------------------------------
    // physics
    // ------------------------------------------------------------------
    protected void physicsTick(CarControls controls) {
        CarStats stats = stats();
        final double dt = 0.05;
        SurfaceSample surface = sampleSurface();

        Vec3d forward = Vec3d.fromPolar(0.0F, getYaw());
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double fx = forward.x;
        double fz = forward.z;
        double rx = right.x;
        double rz = right.z;

        double vf = velX * fx + velZ * fz;
        double vl = velX * rx + velZ * rz;

        boolean grounded = isOnGround() || airborneTicks == 0;
        airborneTicks = grounded ? 0 : airborneTicks + 1;

        boolean boosting = boostTicks > 0 && engineHealth > 0.12F;
        double engineMul = surface.power().mult * engineHealth * (0.55 + 0.45 * tyreHealth);
        if (!grounded) {
            engineMul *= 0.2;
        }
        double maxSpeed = stats.topSpeed() * surface.speed().mult * (0.55 + 0.45 * engineHealth)
                * (boosting ? 1.22 : 1.0);

        // --- longitudinal --------------------------------------------------
        double accel = 0.0;
        double throttle = controls.throttle();
        double brake = controls.brake();
        if (engineHealth > 0.12F && throttle > 0.0) {
            double norm = MathHelper.clamp(vf / Math.max(5.0, maxSpeed), 0.0, 1.2);
            accel += stats.power() * throttle * engineMul * (1.0 - norm * norm * 0.9);
            if (vf < -0.4) {
                accel += stats.brakeForce() * 0.9;
            }
        }
        if (brake > 0.0) {
            if (vf > 0.5) {
                accel -= stats.brakeForce() * brake * (0.4 + 0.6 * tyreHealth) * surface.grip().mult;
            } else {
                accel -= stats.power() * 0.55 * brake * engineMul;
            }
        }
        if (controls.handbrake) {
            accel -= Math.signum(vf) * stats.brakeForce() * 0.5;
        }
        accel -= stats.drag() * vf * Math.abs(vf) * 40.0;
        accel -= 0.33 * vf * surface.roll().mult;
        if (!grounded) {
            accel *= 0.15;
        }
        vf += accel * dt;
        if (Math.abs(vf) < 0.05 && throttle == 0.0 && brake == 0.0) {
            vf = 0.0;
        }
        vf = MathHelper.clamp(vf, -7.0, maxSpeed);

        // --- steering ------------------------------------------------------
        double steerInput = controls.steer();
        double speedRatio = Math.min(1.0, Math.abs(vf) / Math.max(9.0, stats.topSpeed()));
        double maxSteer = stats.maxSteerDeg() * (1.0 - 0.52 * speedRatio);
        double steerTarget = steerInput * maxSteer;
        double steerResponse = controls.handbrake ? 0.40 : 0.28;
        if (surface.slippery()) {
            steerResponse *= 0.7;
        }
        steerAngle += (steerTarget - steerAngle) * steerResponse;

        double steerRad = Math.toRadians(steerAngle) * (vf < 0.0 ? -1.0 : 1.0);
        double wheelbase = Math.max(1.0, stats.wheelbase());
        double desiredYawRate = vf / wheelbase * Math.tan(steerRad);
        double grip = stats.grip() * surface.grip().mult * (0.45 + 0.55 * tyreHealth)
                * (controls.handbrake ? 0.32 : 1.0);
        if (boosting) {
            grip *= 1.05;
        }
        double maxYawRate = grip / Math.max(1.8, Math.abs(vf));
        double yawRate = MathHelper.clamp(desiredYawRate, -maxYawRate, maxYawRate);
        if (controls.handbrake && Math.abs(vf) > 2.0) {
            double lean = steerInput == 0.0 ? 1.0 : Math.signum(steerInput);
            yawRate += lean * Math.min(1.3, 0.075 * Math.abs(vf)) * stats.driftBias();
        }
        if (!grounded) {
            yawRate += steerInput * 0.5;
        }
        setYaw(getYaw() + (float) Math.toDegrees(yawRate * dt));

        // --- lateral tyre force --------------------------------------------
        double latStiffness = stats.latStiffness() * (grounded ? 1.0 : 0.2);
        double latAccel = MathHelper.clamp(-latStiffness * vl, -grip, grip);

        // --- integrate ------------------------------------------------------
        velX += (fx * accel + rx * latAccel) * dt;
        velZ += (fz * accel + rz * latAccel) * dt;
        velY -= 0.5;
        if (grounded && velY < 0.0) {
            velY = 0.0;
        }
        if (surface.bouncy() && grounded && velY <= -0.05) {
            velY = -velY * 0.65 + 0.6;
        }

        if (controls.boost) {
            activateBoost();
        }
        if (boosting) {
            boostTicks--;
            if (boostTicks <= 0) {
                getDataTracker().set(DATA_BOOSTING, false);
            }
        }

        moveWithCollisions(new Vec3d(velX * dt, MathHelper.clamp(velY, -1.8, 1.8) * dt, velZ * dt));

        // --- bookkeeping ----------------------------------------------------
        Vec3d forward2 = Vec3d.fromPolar(0.0F, getYaw());
        Vec3d right2 = new Vec3d(-forward2.z, 0.0, forward2.x);
        this.vForward = velX * forward2.x + velZ * forward2.z;
        this.vLateral = velX * right2.x + velZ * right2.z;
        double speed = Math.hypot(velX, velZ);
        this.slip = speed < 0.4 ? 0.0 : MathHelper.clamp(Math.abs(vLateral) / speed, 0.0, 1.0);
        this.odo += speed * dt;

        boolean drifting = slip > 0.30 && speed > 5.5;
        if (drifting) {
            driftScore += slip * speed * dt * 14.0;
            boostCharge = Math.min(1.0, boostCharge + slip * speed * dt * 0.05);
        } else if (boostTicks <= 0) {
            boostCharge = Math.min(1.0, boostCharge + dt * 0.014);
        }
        tyreHealth = MathHelper.clamp((float) (tyreHealth
                - Math.abs(vLateral) * dt * 0.0012 * (1.0 - 0.06 * data().level(Tune.TIRES))
                - (drifting ? 0.00035 : 0.0)), 0.05F, 1.0F);

        getDataTracker().set(DATA_SPEED, (float) vForward);
        getDataTracker().set(DATA_SLIP, (float) slip);
        getDataTracker().set(DATA_STEER, (float) (steerAngle / 32.0));
        getDataTracker().set(DATA_BOOST, (float) boostCharge);
        getDataTracker().set(DATA_DRIFTING, drifting);
        getDataTracker().set(DATA_HEALTH, healthFraction());
        getDataTracker().set(DATA_LIGHT, drifting || boostTicks > 0 ? 1 : 0);

        double targetRoll = MathHelper.clamp(-vLateral * 0.028, -0.2, 0.2);
        double targetPitch = MathHelper.clamp((grounded ? 0.0 : -velY * 0.06) + accel * -0.007, -0.25, 0.25);
        this.bodyRoll += (float) ((targetRoll - bodyRoll) * 0.2);
        this.bodyPitch += (float) ((targetPitch - bodyPitch) * 0.2);

        if (speed < 0.45) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
    }

    protected void moveWithCollisions(Vec3d delta) {
        int steps = MathHelper.clamp((int) Math.ceil(delta.length() / 0.4), 1, 24);
        Vec3d step = delta.multiply(1.0 / steps);
        for (int i = 0; i < steps; i++) {
            this.horizontalCollision = false;
            this.move(MovementType.SELF, step);
            if (this.horizontalCollision) {
                double speed = Math.hypot(velX, velZ);
                if (speed < 5.0 && tryStepUp()) {
                    continue;
                }
                handleCrash(speed);
                break;
            }
        }
    }

    /** Cars climb a 1 block ledge at low speed - kerbs, bumps, garage floors. */
    protected boolean tryStepUp() {
        Vec3d before = getPos();
        Vec3d step = Vec3d.fromPolar(0.0F, getYaw()).multiply(0.7);
        this.horizontalCollision = false;
        this.move(MovementType.SELF, new Vec3d(step.x, 1.05, step.z));
        if (!this.horizontalCollision && this.getY() > before.y + 0.35) {
            return true;
        }
        this.refreshPositionAndAngles(before.x, before.y, before.z, getYaw(), 0.0F);
        return false;
    }

    protected void handleCrash(double speed) {
        double impact = Math.max(speed, Math.hypot(velX, velZ));
        double nx = -velX * 0.2;
        double nz = -velZ * 0.2;
        boolean hard = impact > 6.0 && (this.age - lastCrashTick) > 5;
        velX = nx;
        velZ = nz;
        if (hard) {
            lastCrashTick = this.age;
            double damage = (impact - 6.0) * 0.02 * (1.0 - 0.06 * data().level(Tune.CHASSIS));
            damageCar((float) damage);
            if (getWorld() instanceof ServerWorld world) {
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ITEM_BREAK,
                        SoundCategory.NEUTRAL, 0.75F, 0.75F + random.nextFloat() * 0.2F);
                world.spawnParticles(ParticleTypes.CRIT, getX(), getY() + 0.6, getZ(),
                        8, 0.35, 0.3, 0.35, 0.15);
            }
        }
    }

    public void hitBoostPad(BlockPos pos) {
        if (getWorld().isClient() || this.age - lastPadTick <= 3) {
            return;
        }
        lastPadTick = this.age;
        boostTicks = Math.max(boostTicks, 30);
        boostCharge = Math.min(1.0, boostCharge + 0.3);
        getDataTracker().set(DATA_BOOSTING, true);
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    SoundCategory.NEUTRAL, 0.45F, 1.5F);
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(),
                    5, 0.25, 0.1, 0.25, 0.05);
        }
    }

    public void hitBounceBlock(double power) {
        this.velY = Math.max(velY, 3.4 * power);
        this.airborneTicks = 1;
    }

    public void activateBoost() {
        if (getWorld().isClient() || boostTicks > 0 || boostCharge < 0.28 || engineHealth < 0.15F) {
            return;
        }
        boostTicks = 34 + 12 * data().level(Tune.BOOST);
        boostCharge = Math.max(0.0, boostCharge - 0.32);
        getDataTracker().set(DATA_BOOSTING, true);
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FIRECHARGE_USE,
                    SoundCategory.NEUTRAL, 0.6F, 1.15F);
        }
    }

    protected void serverEffects() {
        if (!(getWorld() instanceof ServerWorld world)) {
            return;
        }
        double speed = Math.hypot(velX, velZ);
        if (slip > 0.32 && speed > 4.0 && this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() - velX * 0.06,
                    getY() + 0.15, getZ() - velZ * 0.06, 1, 0.22, 0.05, 0.22, 0.012);
            if (slip > 0.62) {
                world.spawnParticles(ParticleTypes.CLOUD, getX() - velX * 0.09, getY() + 0.1,
                        getZ() - velZ * 0.09, 2, 0.2, 0.02, 0.2, 0.0);
            }
        }
        if (boostTicks > 0) {
            world.spawnParticles(ParticleTypes.FLAME, getX() - velX * 0.11, getY() + 0.42,
                    getZ() - velZ * 0.11, 2, 0.09, 0.05, 0.09, 0.02);
        }
        if (engineHealth <= 0.05F && this.age % 25 == 0) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.7, getZ(),
                    2, 0.3, 0.2, 0.3, 0.005);
        }
        if (engineSoundCooldown-- <= 0) {
            engineSoundCooldown = 7;
            double absV = Math.abs(vForward);
            if (absV > 0.6 || boostTicks > 0 || engineHealth < 0.4F) {
                double pitch = 0.6 + 0.8 * Math.min(1.0, absV / Math.max(7.0, stats().topSpeed()));
                if (engineHealth < 0.4F) {
                    pitch *= 0.65;
                }
                world.playSound(null, getX(), getY(), getZ(),
                        engineHealth < 0.25F ? SoundEvents.BLOCK_FIRE_EXTINGUISH
                                : (boostTicks > 0 ? SoundEvents.ENTITY_MINECART_RIDING
                                : SoundEvents.ENTITY_MINECART_INSIDE),
                        SoundCategory.NEUTRAL,
                        (float) Math.min(0.6, 0.1 + absV * 0.013), (float) pitch);
            }
        }
    }

    protected void clientTick() {
        Vec3d forward = Vec3d.fromPolar(0.0F, getYaw());
        this.vForward = velX * forward.x + velZ * forward.z;
        if (Math.abs(vForward) < 0.01) {
            this.vForward = getDataTracker().get(DATA_SPEED);
        }
        this.vLateral = getDataTracker().get(DATA_SLIP);
    }

    // ------------------------------------------------------------------
    // status accessors used by renderer + HUD
    // ------------------------------------------------------------------
    public int raceState() {
        return raceState;
    }

    public void setRaceState(int state) {
        this.raceState = state;
    }

    /** Ticks the car has been wedged against something; read by the AI driver. */
    public int stuckTicks() {
        return stuckTicks;
    }

    public double getSpeed() {
        return getDataTracker().get(DATA_SPEED);
    }

    public double getSlip() {
        return getDataTracker().get(DATA_SLIP);
    }

    public double getSteerFactor() {
        return getDataTracker().get(DATA_STEER);
    }

    public double getBoostCharge() {
        return getDataTracker().get(DATA_BOOST);
    }

    public boolean isBoosting() {
        return getDataTracker().get(DATA_BOOSTING);
    }

    public boolean isDrifting() {
        return getDataTracker().get(DATA_DRIFTING);
    }

    public float getBodyRoll() {
        return bodyRoll;
    }

    public float getBodyPitch() {
        return bodyPitch;
    }

    public double getDriftScore() {
        return driftScore;
    }

    public double getOdometer() {
        return odo;
    }

    public float healthFraction() {
        return MathHelper.clamp(engineHealth * 0.65F + tyreHealth * 0.35F, 0.0F, 1.0F);
    }

    public float engineHealth() {
        return engineHealth;
    }

    public float tyreHealth() {
        return tyreHealth;
    }

    public boolean isBroken() {
        return engineHealth < 0.12F;
    }

    public void damageCar(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        this.engineHealth = MathHelper.clamp(engineHealth - amount * 0.72F, 0.0F, 1.0F);
        this.tyreHealth = MathHelper.clamp(tyreHealth - amount * 0.28F, 0.05F, 1.0F);
        getDataTracker().set(DATA_HEALTH, healthFraction());
    }

    public void resetHealth() {
        this.engineHealth = 1.0F;
        this.tyreHealth = 1.0F;
        getDataTracker().set(DATA_HEALTH, 1.0F);
    }

    public ActionResult repair(PlayerEntity player, ItemStack wrench) {
        if (!data().ownedBy(player)) {
            player.sendMessage(Text.translatable("message.fabricracers.not_your_car",
                    Text.literal(storedOwnerName)).formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        boolean damaged = engineHealth < 0.999F || tyreHealth < 0.999F;
        if (!damaged) {
            player.sendMessage(Text.translatable("message.fabricracers.already_fine")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }
        float before = engineHealth;
        this.engineHealth = MathHelper.clamp(engineHealth + 0.45F, 0.0F, 1.0F);
        this.tyreHealth = MathHelper.clamp(tyreHealth + 0.5F, 0.05F, 1.0F);
        getDataTracker().set(DATA_HEALTH, healthFraction());
        this.engineHealth = Math.max(before, engineHealth);
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_USE,
                    SoundCategory.NEUTRAL, 0.6F, 1.3F);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.5, getZ(),
                    8, 0.4, 0.3, 0.4, 0.02);
        }
        if (!player.isCreative() && wrench != null && wrench.isDamageable()) {
            wrench.damage(1, player, EquipmentSlot.MAINHAND);
        }
        player.sendMessage(Text.translatable("message.fabricracers.repaired")
                .formatted(Formatting.GREEN), true);
        return ActionResult.CONSUME;
    }

    public void setPaint(int primary, int secondary) {
        this.cachedData = null;
        getDataTracker().set(DATA_PRIMARY, primary);
        getDataTracker().set(DATA_SECONDARY, secondary);
    }

    /** Used by the carrot key to claim an ownerless car. */
    public void setOwner(UUID owner, String name) {
        this.storedOwner = owner;
        this.storedOwnerName = name == null ? "" : name;
        this.cachedData = null;
        updateDisplayName();
    }

    public void setLevels(int[] levels) {
        this.cachedData = null;
        getDataTracker().set(DATA_LEVELS, packLevels(levels));
    }

    // ------------------------------------------------------------------
    // driver plumbing
    // ------------------------------------------------------------------
    public abstract boolean isPlayerDriven();

    public LivingEntity getDriver() {
        for (Entity passenger : getPassengerList()) {
            if (passenger instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public boolean isRidden() {
        return !getPassengerList().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getDriver();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger,
                                              net.minecraft.entity.EntityDimensions dimensions,
                                              float tickDelta) {
        return new Vec3d(0.0, 0.62, 0.0);
    }

    public void setAiDriving(boolean value) {
        this.aiDriving = value;
        if (value) {
            this.aiDriver = new dev.qynl.racers.ai.AiDriver();
        } else {
            this.aiDriver = null;
        }
        this.cachedData = null;
        updateDisplayName();
    }

    public boolean isAiDriving() {
        return aiDriving;
    }

    public void setControlLocked(boolean locked) {
        this.controlLocked = locked;
    }

    public boolean isControlLocked() {
        return controlLocked;
    }

    public void setDriverInput(CarControls controls) {
        this.driverInput = controls;
    }

    public CarControls getDriverInput() {
        return driverInput;
    }

    public void onDriverMount(LivingEntity driver) {
        if (driver instanceof PlayerEntity player) {
            storedOwnerName = player.getName().getString();
            getDataTracker().set(DATA_DRIVER, storedOwnerName);
            if (storedOwner == null) {
                storedOwner = player.getUuid();
            }
        }
        aiDriving = false;
        aiDriver = null;
        updateDisplayName();
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                    SoundCategory.NEUTRAL, 0.5F, 1.6F);
        }
    }

    public void onDriverDismount() {
        getDataTracker().set(DATA_DRIVER, "");
        updateDisplayName();
        driverInput = CarControls.none();
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_IRON_DOOR_OPEN,
                    SoundCategory.NEUTRAL, 0.5F, 1.4F);
        }
    }

    public String driverName() {
        return getDataTracker().get(DATA_DRIVER);
    }

    protected void updateDisplayName() {
        String driver = getDataTracker().get(DATA_DRIVER);
        Text label = Text.literal("").append(carType().label())
                .append(driver.isEmpty()
                        ? (aiDriving ? Text.literal(" [AI]").formatted(Formatting.AQUA)
                        : Text.literal(" [parked]").formatted(Formatting.DARK_GRAY))
                        : Text.literal(" " + driver).formatted(Formatting.WHITE));
        if (!storedOwnerName.isEmpty()) {
            label = label.copy().append(Text.literal(" (" + storedOwnerName + ")")
                    .formatted(Formatting.DARK_GRAY));
        }
        setCustomName(label);
        setCustomNameVisible(!driver.isEmpty() || aiDriving);
    }

    /** Teleports with all physical state reset - used by the race engine. */
    public void teleportCleanly(double x, double y, double z, float yaw) {
        this.velX = 0.0;
        this.velY = 0.0;
        this.velZ = 0.0;
        this.steerAngle = 0.0;
        this.bodyRoll = 0.0F;
        this.bodyPitch = 0.0F;
        this.boostTicks = 0;
        this.airborneTicks = 0;
        this.driverInput = CarControls.none();
        this.slip = 0.0;
        getDataTracker().set(DATA_SPEED, 0.0F);
        getDataTracker().set(DATA_BOOSTING, false);
        if (getWorld() instanceof ServerWorld serverWorld) {
            this.teleport(serverWorld, x, y, z, java.util.Set.of(), yaw, 0.0F);
        } else {
            this.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
        }
    }

    // ------------------------------------------------------------------
    // interactions
    // ------------------------------------------------------------------
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (getWorld().isClient() || hand != Hand.MAIN_HAND) {
            return ActionResult.SUCCESS;
        }
        if (isRidden()) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof dev.qynl.racers.item.ToolItems.Wrench
                || stack.getItem() instanceof dev.qynl.racers.item.ToolItems.PaintCan) {
            return ActionResult.PASS;
        }
        if (!data().ownedBy(player) && !player.isCreative()) {
            player.sendMessage(Text.translatable("message.fabricracers.not_your_car",
                    Text.literal(storedOwnerName)).formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        player.startRiding(this);
        return ActionResult.CONSUME;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        if (!getWorld().isClient() && amount > 0.5F) {
            damageCar(Math.min(0.3F, amount * 0.04F));
            if (source.getAttacker() instanceof LivingEntity attacker) {
                double dx = getX() - attacker.getX();
                double dz = getZ() - attacker.getZ();
                double len = Math.max(0.2, Math.hypot(dx, dz));
                velX += dx / len * 0.4;
                velZ += dz / len * 0.4;
            }
        }
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    /** Keeps cars from stacking inside each other, with momentum exchange. */
    protected void collideWithCars() {
        List<CarEntity> others = getWorld().getEntitiesByClass(CarEntity.class,
                getBoundingBox().expand(1.4), other -> other != this && !other.isRemoved());
        for (CarEntity other : others) {
            Box a = getBoundingBox();
            Box b = other.getBoundingBox();
            if (!a.intersects(b)) {
                continue;
            }
            double dx = other.getX() - getX();
            double dz = other.getZ() - getZ();
            double dist = Math.max(0.2, Math.hypot(dx, dz));
            double overlap = 1.75 - dist;
            if (overlap <= 0) {
                continue;
            }
            double nx = dx / dist;
            double nz = dz / dist;
            double m1 = Math.max(0.3, stats().mass());
            double m2 = Math.max(0.3, other.stats().mass());
            double share1 = m2 / (m1 + m2);
            double push = overlap * 0.5;
            this.velX -= nx * push * share1;
            this.velZ -= nz * push * share1;
            other.velX += nx * push * (1.0 - share1);
            other.velZ += nz * push * (1.0 - share1);

            double relative = (velX - other.velX) * nx + (velZ - other.velZ) * nz;
            if (relative > 1.5) {
                double impulse = relative * 0.3;
                this.velX -= nx * impulse * share1 * 2.0;
                this.velZ -= nz * impulse * share1 * 2.0;
                other.velX += nx * impulse * (1.0 - share1) * 2.0;
                other.velZ += nz * impulse * (1.0 - share1) * 2.0;
                if (relative > 7.0) {
                    this.damageCar((float) (relative * 0.01));
                    other.damageCar((float) (relative * 0.01));
                    if (getWorld() instanceof ServerWorld world) {
                        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ITEM_BREAK,
                                SoundCategory.NEUTRAL, 0.5F, 1.0F);
                        world.spawnParticles(ParticleTypes.CRIT, (getX() + other.getX()) / 2.0,
                                getY() + 0.6, (getZ() + other.getZ()) / 2.0, 6, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // surface sampling
    // ------------------------------------------------------------------
    public record Multiplier(double mult) {
    }

    public record SurfaceSample(Multiplier power, Multiplier speed, Multiplier grip,
                                Multiplier roll, boolean bouncy, boolean slippery) {
    }

    protected SurfaceSample sampleSurface() {
        SurfaceType front = probeSurface(offsetAhead(0.55F));
        SurfaceType rear = probeSurface(offsetAhead(-0.55F));
        SurfaceType center = probeSurface(getBlockPos());
        double power = (front.powerMul + rear.powerMul * 1.3 + center.powerMul) / 3.3;
        double speed = (front.speedMul + rear.speedMul * 1.3 + center.speedMul) / 3.3;
        double grip = (front.gripMul * 1.2 + rear.gripMul * 1.4 + center.gripMul) / 3.6;
        double roll = (front.rollMul + rear.rollMul + center.rollMul) / 3.0;
        boolean bouncy = front.bouncy || rear.bouncy;
        boolean slippery = front == SurfaceType.ICE || rear == SurfaceType.ICE
                || center == SurfaceType.ICE;
        return new SurfaceSample(new Multiplier(power), new Multiplier(speed),
                new Multiplier(grip), new Multiplier(roll), bouncy, slippery);
    }

    private BlockPos offsetAhead(float blocks) {
        Vec3d forward = Vec3d.fromPolar(0.0F, getYaw()).multiply(blocks);
        return BlockPos.ofFloored(getX() + forward.x, getY() - 0.35, getZ() + forward.z);
    }

    private SurfaceType probeSurface(BlockPos pos) {
        var state = getWorld().getBlockState(pos);
        if (!state.isAir()) {
            return SurfaceType.of(state);
        }
        var below = getWorld().getBlockState(pos.down());
        if (!below.isAir()) {
            return SurfaceType.of(below);
        }
        return SurfaceType.AIR;
    }

    // ------------------------------------------------------------------
    // velocity bridge (Entity keeps its own copy, we keep ours)
    // ------------------------------------------------------------------
    @Override
    public Vec3d getVelocity() {
        return new Vec3d(velX, velY, velZ);
    }

    @Override
    public void setVelocity(Vec3d velocity) {
        this.velX = velocity.x;
        this.velY = velocity.y;
        this.velZ = velocity.z;
        super.setVelocity(velocity);
        this.velocityDirty = true;
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        this.velX += deltaX;
        this.velY += deltaY;
        this.velZ += deltaZ;
        super.setVelocity(new Vec3d(velX, velY, velZ));
        this.velocityDirty = true;
    }

    @Override
    public void setYaw(float yaw) {
        super.setYaw(yaw);
    }

    // ------------------------------------------------------------------
    // race integration helpers
    // ------------------------------------------------------------------
    public RaceManager raceManager() {
        if (getWorld() instanceof ServerWorld serverWorld) {
            return RaceManager.get(serverWorld.getServer());
        }
        return null;
    }

    public BlockPos spawnBlock() {
        return new BlockPos((int) Math.floor(getX()), (int) Math.floor(getY()), (int) Math.floor(getZ()));
    }

    public double distanceTo(double x, double z) {
        return Math.hypot(getX() - x, getZ() - z);
    }

    /** Distance travelled since the car was created, in blocks. */
    public double odometer() {
        return odo;
    }
}
