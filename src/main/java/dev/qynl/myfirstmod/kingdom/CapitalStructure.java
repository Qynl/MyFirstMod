package dev.qynl.myfirstmod.kingdom;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.*;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.heightprovider.ConstantHeightProvider;
import net.minecraft.world.gen.structure.*;
import java.util.Optional;
/** The Silent Capital is unique: its structure type only ever starts in one fixed chunk,
 * then delegates to vanilla jigsaw placement for the actual template drop. */
public final class CapitalStructure extends Structure {
    public static final MapCodec<CapitalStructure> CODEC=createCodec(CapitalStructure::new);
    public static final StructureType<CapitalStructure> TYPE=()->CODEC;
    public static final Identifier POOL=Identifier.of("myfirstmod","silent_capital");
    public static final int CHUNK_X=0,CHUNK_Z=-22;
    private final Config settings;private volatile JigsawStructure delegate;
    public CapitalStructure(Config config){super(config);settings=config;}
    public static void register(){Registry.register(Registries.STRUCTURE_TYPE,Identifier.of("myfirstmod","silent_capital"),TYPE);}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        if(context.chunkPos().x!=CHUNK_X||context.chunkPos().z!=CHUNK_Z)return Optional.empty();
        var jigsaw=delegate;
        if(jigsaw==null){
            var pools=context.dynamicRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
            var pool=pools.getEntry(RegistryKey.of(RegistryKeys.TEMPLATE_POOL,POOL))
                .orElseThrow(()->new IllegalStateException("Missing capital template pool: "+POOL));
            jigsaw=delegate=new JigsawStructure(settings,pool,0,ConstantHeightProvider.ZERO,false,Heightmap.Type.WORLD_SURFACE_WG);
        }
        return jigsaw.getValidStructurePosition(context);
    }
    @Override public StructureType<?> getType(){return TYPE;}
}
