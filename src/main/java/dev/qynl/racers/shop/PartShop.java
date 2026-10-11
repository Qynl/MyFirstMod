package dev.qynl.racers.shop;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import dev.qynl.racers.car.CarType;
import dev.qynl.racers.car.Tune;
import dev.qynl.racers.registry.ModItems;

/**
 * Economy: mechanics and wandering traders sell tuning parts and complete cars
 * so a survival world has a path to the fast stuff. Race prizes pay in emeralds,
 * so winning literally funds the next upgrade.
 */
public final class PartShop {
    private PartShop() {
    }

    public static void init() {
        dev.qynl.racers.FabricRacers.LOGGER.info("Registering racing villager trades");
        // --- tier 1: starter parts and building blocks -------------------
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 1, factories -> {
            factories.add(buy(ModItems.partItem(Tune.TIRES), 2, 9, 8));
            factories.add(buy(ModItems.partItem(Tune.BRAKES), 1, 8, 8));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 1, factories -> {
            factories.add(buy(ModItems.BLOCK_ASPHALT_ITEM, 16, 7, 6));
            factories.add(buy(ModItems.BLOCK_KERB_ITEM, 16, 5, 6));
        });

        // --- tier 2: real tuning ----------------------------------------
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 2, factories -> {
            factories.add(buy(ModItems.partItem(Tune.ENGINE), 1, 18, 6));
            factories.add(buy(ModItems.partItem(Tune.GEARBOX), 1, 16, 6));
            factories.add(buy(ModItems.partItem(Tune.CHASSIS), 1, 14, 6));
        });

        // --- tier 3: aero, boost and the cars themselves -----------------
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 3, factories -> {
            factories.add(buy(ModItems.partItem(Tune.AERO), 1, 26, 5));
            factories.add(buy(ModItems.partItem(Tune.BOOST), 1, 24, 5));
            factories.add(PartShop::carOffer);
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.WEAPONSMITH, 3,
                factories -> factories.add(PartShop::carOffer));

        // --- the racing village -------------------------------------------------
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 2, factories -> {
            factories.add(buy(ModItems.RACE_PASS, 1, 12, 6));
            factories.add(buy(ModItems.STOPWATCH, 1, 10, 5));
            factories.add(buy(ModItems.WRENCH, 1, 16, 4));
        });

        // --- wandering trader: exotica -----------------------------------
        TradeOfferHelper.registerWanderingTraderOffers(1, factories -> {
            factories.add(buy(ModItems.WRENCH, 1, 30, 4));
            factories.add(buy(ModItems.BLOCK_BOOST_PAD_ITEM, 4, 22, 5));
            factories.add(PartShop::carOffer);
        });
        dev.qynl.racers.FabricRacers.LOGGER.info("Racing trades hooked: {} part systems, {} stock cars",
                Tune.values().length, CarType.stock().size());
    }

    /** A plain "give emeralds, get item" trade. */
    private static TradeOffers.Factory buy(Item item, int amount, int price, int maxUses) {
        return (entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, price),
                new ItemStack(item, amount), maxUses, 5, 0.05F);
    }

    /** Sells a random stock car, priced by its market value. */
    private static TradeOffer carOffer(net.minecraft.entity.Entity entity, Random random) {
        List<CarType> sellable = new ArrayList<>(CarType.stock());
        CarType type = sellable.get(random.nextInt(sellable.size()));
        return new TradeOffer(new TradedItem(Items.EMERALD, Math.max(4, type.price() * 3)),
                ModItems.carStack(type, null), 2, 12, 0.1F);
    }

    /** Parts needed to reach {@code level} of a system. */
    public static int costFor(Tune tune, int level) {
        return Math.max(1, level);
    }

    public static String costLabel(Tune tune, int level) {
        return costFor(tune, level) + "x " + tune.displayName + " part";
    }
}
