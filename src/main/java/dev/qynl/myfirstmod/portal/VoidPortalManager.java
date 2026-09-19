package dev.qynl.myfirstmod.portal;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.boss.NullWardenState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VoidPortalManager {
    public static final RegistryKey<World> NULL_REALM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MyFirstMod.MOD_ID, "null_realm"));

    private static final int TELEPORT_COOLDOWN = 80;
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private VoidPortalManager() {}

    public static boolean tryIgnite(ServerPlayerEntity player, BlockPos clicked, net.minecraft.util.Hand hand) {
        ServerWorld world=player.getServerWorld();
        if(player.isSpectator() || !world.canPlayerModifyAt(player,clicked))return false;
        if(!player.isCreative()) {
            var city=world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE)
                    .get(net.minecraft.util.Identifier.ofVanilla("ancient_city"));
            if(!world.getRegistryKey().equals(World.OVERWORLD) || city==null
                    || !world.getStructureAccessor().getStructureContaining(clicked,city).hasChildren()) {
                player.sendMessage(Text.translatable("message.myfirstmod.city_gate_location"),true);return false;
            }
        }
        var result=findGate(world,clicked);
        if(result.isEmpty()) {player.sendMessage(Text.translatable("message.myfirstmod.city_gate_incomplete"),false);return false;}
        var frame=result.get();
        // Validate every permission before changing any block or consuming a shard.
        boolean fresh=false;
        for(int y=1;y<AncientCityGate.HEIGHT-1;y++)for(int x=1;x<AncientCityGate.WIDTH-1;x++) {
            var p=blockPos(frame.at(x,y));if(!world.canPlayerModifyAt(player,p))return false;
            fresh|=!world.getBlockState(p).isOf(ModBlocks.VOID_PORTAL);
        }
        if(!fresh){player.sendMessage(Text.translatable("message.myfirstmod.city_gate_open"),true);return false;}
        openGate(world,frame);
        if(!player.isCreative())player.getStackInHand(hand).decrement(1);
        player.sendMessage(Text.translatable("message.myfirstmod.city_gate_awakened"),false);return true;
    }
    public static java.util.Optional<AncientCityGate.Frame> findGate(ServerWorld world,BlockPos clicked) {
        var cache=new java.util.HashMap<AncientCityGate.Point,AncientCityGate.Cell>();
        return AncientCityGate.find(new AncientCityGate.Point(clicked.getX(),clicked.getY(),clicked.getZ()),p->cache.computeIfAbsent(p,key->{
            var pos=blockPos(key);if(!world.isChunkLoaded(pos))return AncientCityGate.Cell.BLOCKED;
            var state=world.getBlockState(pos);
            if(state.isOf(Blocks.REINFORCED_DEEPSLATE))return AncientCityGate.Cell.FRAME;
            if(state.isOf(ModBlocks.VOID_PORTAL))return AncientCityGate.Cell.PORTAL;
            return state.isAir()?AncientCityGate.Cell.EMPTY:AncientCityGate.Cell.BLOCKED;
        }));
    }
    private static BlockPos blockPos(AncientCityGate.Point p){return new BlockPos(p.x(),p.y(),p.z());}
    public static void openGate(ServerWorld world,AncientCityGate.Frame frame) {
        var state=ModBlocks.VOID_PORTAL.getDefaultState().with(dev.qynl.myfirstmod.block.VoidPortalBlock.AXIS,
                frame.alongX()?net.minecraft.util.math.Direction.Axis.X:net.minecraft.util.math.Direction.Axis.Z);
        for(int y=1;y<AncientCityGate.HEIGHT-1;y++)for(int x=1;x<AncientCityGate.WIDTH-1;x++)world.setBlockState(blockPos(frame.at(x,y)),state,net.minecraft.block.Block.NOTIFY_LISTENERS);
        var center=blockPos(frame.at(11,3));world.playSound(null,center,SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,SoundCategory.BLOCKS,1.6f,.55f);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,center.getX()+.5,center.getY()+.5,center.getZ()+.5,120,frame.alongX()?8:.3,2,frame.alongX()?.3:8,.04);
    }
    /** Operator fixture tool: validates the real shape without bypassing the normal Survival entry checks. */
    public static void registerGateCommand() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(
            net.minecraft.server.command.CommandManager.literal("nullgate").requires(s->s.hasPermissionLevel(2))
                .then(net.minecraft.server.command.CommandManager.argument("pos",net.minecraft.command.argument.BlockPosArgumentType.blockPos()).executes(ctx->{
                    var world=ctx.getSource().getWorld();var pos=net.minecraft.command.argument.BlockPosArgumentType.getLoadedBlockPos(ctx,"pos");
                    var frame=findGate(world,pos);if(frame.isEmpty()){ctx.getSource().sendError(Text.literal("GATE_INVALID"));return 0;}
                    openGate(world,frame.get());ctx.getSource().sendFeedback(()->Text.literal("GATE_OPEN"),false);return 1;
                }))));
    }

    public static void tryEnter(Entity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return;

        int cooldown = COOLDOWNS.getOrDefault(player.getUuid(), 0);
        if (cooldown > 0) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (player.getServerWorld().getRegistryKey().equals(NULL_REALM)) {
            COOLDOWNS.put(player.getUuid(), TELEPORT_COOLDOWN);
            returnPlayer(player);
            return;
        }

        ServerWorld target = server.getWorld(NULL_REALM);
        if (target == null) {
            player.sendMessage(Text.literal("The Null Realm cannot be reached."), true);
            return;
        }

        COOLDOWNS.put(player.getUuid(), TELEPORT_COOLDOWN);

        BlockPos returnPos = player.getBlockPos();
        RegistryKey<World> returnWorld = player.getServerWorld().getRegistryKey();
        float returnYaw = player.getYaw();
        float returnPitch = player.getPitch();

        // Return data is owned by the Null Realm encounter state, not the
        // origin world's state. This makes returning work across dimensions
        // and survives a server restart.
        NullWardenManager.saveReturnPoint(
                player, returnWorld, returnPos, returnYaw, returnPitch
        );

        dev.qynl.myfirstmod.realm.RealmExpedition.prepare(target);
        player.teleport(target, 0.5, 81.0, 160.5, 180, 0);
        dev.qynl.myfirstmod.realm.RealmExpedition.welcome(player);
    }

    public static void clear() { COOLDOWNS.clear(); }

    public static void tick(MinecraftServer server) {
        COOLDOWNS.replaceAll((uuid, value) -> Math.max(0, value - 1));
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() == 0);

        NullWardenManager.tick(server);
    }

    public static void returnPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if(server==null)return;
        NullWardenState.ReturnPointData point = NullWardenManager.takeReturnPoint(player);

        ServerWorld destination = server.getOverworld();
        if (point != null) {
            Identifier id = Identifier.tryParse(point.worldId());
            if (id != null) {
                ServerWorld stored = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
                if (stored != null) destination = stored;
            }
        }

        BlockPos spawn = point == null
                ? destination.getSpawnPos()
                : new BlockPos(point.x(), point.y(), point.z());

        destination.getChunk(spawn); // Deliberately load the recorded return chunk, never an unbounded search.
        var preferred=point==null?destination.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,spawn):spawn.up();
        BlockPos landing=safeLanding(destination,preferred);
        if(landing==null){
            if(point!=null)NullWardenManager.saveReturnPoint(player,destination.getRegistryKey(),spawn,point.yaw(),point.pitch());
            player.sendMessage(Text.literal("The return landing is obstructed. Clear a safe space near the original gateway and try again."),false);return;
        }
        player.teleport(destination,landing.getX()+.5,landing.getY(),landing.getZ()+.5,
                point==null?player.getYaw():point.yaw(),point==null?player.getPitch():point.pitch());
        player.sendMessage(Text.literal("You return through the ancient gateway."),true);
    }
    private static BlockPos safeLanding(ServerWorld world,BlockPos origin){
        for(int radius=0;radius<=8;radius++)for(int dy=0;dy<=12;dy++){
            int y=(dy+1)/2*(dy%2==0?-1:1);
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                var p=origin.add(dx,y,dz);if(!world.isChunkLoaded(p))continue;
                var feet=world.getBlockState(p);var head=world.getBlockState(p.up());
                if(feet.isOf(ModBlocks.VOID_PORTAL)||head.isOf(ModBlocks.VOID_PORTAL))continue;
                if(feet.getCollisionShape(world,p).isEmpty()&&head.getCollisionShape(world,p.up()).isEmpty()
                    &&feet.getFluidState().isEmpty()&&head.getFluidState().isEmpty()
                    &&world.getBlockState(p.down()).isSideSolidFullSquare(world,p.down(),net.minecraft.util.math.Direction.UP))return p;
            }
        }
        return null;
    }
}
