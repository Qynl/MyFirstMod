package dev.qynl.myfirstmod.kingdom;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.*;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.heightprovider.ConstantHeightProvider;
import net.minecraft.world.gen.structure.*;
import java.util.Optional;
/** Vanilla jigsaw placement, but it never starts inside the fixed sanctuary approach. */
public final class MonasteryStructure extends Structure {
    public static final MapCodec<MonasteryStructure> CODEC=createCodec(MonasteryStructure::new);
    public static final StructureType<MonasteryStructure> TYPE=()->CODEC;
    public static final Identifier POOL=Identifier.of("myfirstmod","rootbound_monastery");
    private final Config settings;private volatile JigsawStructure delegate;
    public MonasteryStructure(Config config){super(config);settings=config;}
    public static void register(){Registry.register(Registries.STRUCTURE_TYPE,Identifier.of("myfirstmod","rootbound_monastery"),TYPE);}
    public static boolean reserved(BlockPos pos){return Math.abs((long)pos.getX())<160&&pos.getZ()>-160&&pos.getZ()<256;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        var start=new BlockPos(context.chunkPos().getStartX(),0,context.chunkPos().getStartZ());
        if(reserved(start))return Optional.empty();
        var jigsaw=delegate;
        if(jigsaw==null){
            var pools=context.dynamicRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
            var pool=pools.getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL,POOL))
                .orElseThrow(()->new IllegalStateException("Missing monastery template pool: "+POOL));
            jigsaw=delegate=new JigsawStructure(settings,pool,0,ConstantHeightProvider.ZERO,false,Heightmap.Type.WORLD_SURFACE_WG);
        }
        // Public in 1.21.1; JigsawStructure itself is final, so it is delegated to rather than extended.
        return jigsaw.getValidStructurePosition(context);
    }
    @Override public StructureType<?> getType(){return TYPE;}
}
