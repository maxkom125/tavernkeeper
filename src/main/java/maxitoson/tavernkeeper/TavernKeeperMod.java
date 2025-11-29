package maxitoson.tavernkeeper;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.datagen.DataGenerators;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.items.MarkingCane;
import maxitoson.tavernkeeper.items.WalletItem;
import maxitoson.tavernkeeper.items.TavernItem;
import maxitoson.tavernkeeper.network.NetworkHandler;
// import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
// import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TavernKeeperMod.MODID)
public class TavernKeeperMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "tavernkeeper";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "tavernkeeper" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "tavernkeeper" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "tavernkeeper" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold Entities which will all be registered under the "tavernkeeper" namespace
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    // Creates a new Block with the id "tavernkeeper:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "tavernkeeper:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "tavernkeeper:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerItem("example_item", TavernItem::new, new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));
    
    // Creates the Marking Cane tool for area selection
    public static final DeferredItem<Item> MARKING_CANE = ITEMS.registerItem("marking_cane", MarkingCane::new, new Item.Properties().stacksTo(1));
    
    // Currency System - Coins (in order: lowest to highest tier)
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> IRON_COIN = ITEMS.registerSimpleItem("iron_coin", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin", new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DIAMOND_COIN = ITEMS.registerSimpleItem("diamond_coin", new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> NETHERITE_COIN = ITEMS.registerSimpleItem("netherite_coin", new Item.Properties().stacksTo(64).rarity(Rarity.EPIC));
    
    public static final DeferredItem<Item> WALLET = ITEMS.registerItem("wallet", WalletItem::new, new Item.Properties().stacksTo(1));
    
    // Register Customer Entity (uses Villager's properties)
    public static final DeferredHolder<EntityType<?>, EntityType<CustomerEntity>> CUSTOMER = ENTITY_TYPES.register("customer", 
        () -> EntityType.Builder.of(
            (EntityType<CustomerEntity> type, net.minecraft.world.level.Level level) -> new CustomerEntity(type, level),
            MobCategory.CREATURE
        )
        .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
        .clientTrackingRange(EntityType.VILLAGER.clientTrackingRange())
        .build("customer"));
    
    // Customer spawn egg for testing
    public static final DeferredItem<Item> CUSTOMER_SPAWN_EGG = ITEMS.register("customer_spawn_egg",
        () -> new DeferredSpawnEggItem(CUSTOMER, 0x7B3F00, 0xFFFFFF, new Item.Properties()));

    // Creates a creative tab with the id "tavernkeeper:tavernkeeper_tab" for all tavern keeper items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAVERNKEEPER_TAB = CREATIVE_MODE_TABS.register("tavernkeeper_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tavernkeeper.tavernkeeper_tab"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MARKING_CANE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MARKING_CANE.get()); // Area selection tool
                // Coins in order (lowest to highest tier)
                output.accept(COPPER_COIN.get());
                output.accept(IRON_COIN.get());
                output.accept(GOLD_COIN.get());
                output.accept(DIAMOND_COIN.get());
                output.accept(NETHERITE_COIN.get());
                output.accept(WALLET.get());
                output.accept(CUSTOMER_SPAWN_EGG.get()); // Customer spawn egg for testing
                output.accept(EXAMPLE_ITEM.get()); // Example item
            }).build());

    public TavernKeeperMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get registered
        ENTITY_TYPES.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        
        // Register entity attributes
        modEventBus.addListener(this::registerEntityAttributes);
        
        // Register network handlers
        modEventBus.addListener(NetworkHandler::register);

        // Register data generators
        modEventBus.addListener(DataGenerators::gatherData);

        // Register our mod's config so that NeoForge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean())
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }
    
    // Register entity attributes
    private void registerEntityAttributes(EntityAttributeCreationEvent event)
    {
        event.put(CUSTOMER.get(), CustomerEntity.createAttributes().build());
    }
    
    // ========== Event Handlers ==========
    // Located in the events package.
    //
    // - events/PlayerInteractionHandler.java
    //   * onRightClickBlock - Marking cane area selection, tavern sign interactions
    //   * onLeftClickBlock - Marking cane area deletion
    //   * onPlayerInteractEntity - Customer serving with food
    //
    // - events/WorldUpdateHandler.java
    //   * onBlockPlace - Update furniture when blocks are placed
    //   * onBlockBreak - Remove furniture when blocks are broken
    //   * onEntityJoinLevel - Configure mob AI (skeletons target customers)
    //
    // - events/TavernLifecycleHandler.java
    //   * onServerStarting - Server initialization
    //   * onLevelTick - Tavern tick (customer spawning)
    //   * onPlayerJoin - Welcome message + sync areas
    //   * onRegisterCommands - Register tavern area commands
}

