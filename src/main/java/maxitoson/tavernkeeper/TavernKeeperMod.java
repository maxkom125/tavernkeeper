package maxitoson.tavernkeeper;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.datagen.DataGenerators;
import maxitoson.tavernkeeper.areas.AreaCommand;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.items.WalletItem;
import maxitoson.tavernkeeper.items.MarkingCane;
import maxitoson.tavernkeeper.items.TavernItem;
import maxitoson.tavernkeeper.network.NetworkHandler;
import maxitoson.tavernkeeper.network.SyncAreasPacket;
import maxitoson.tavernkeeper.tavern.Tavern;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import maxitoson.tavernkeeper.events.CustomerPaymentEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
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

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

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

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    
    // Handle level tick for tavern lifecycle
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event)
    {
        // Only tick if it's server-side and Overworld
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel 
            && serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            Tavern tavern = Tavern.get(serverLevel);
            tavern.tick();
        }
    }

    // Welcome message when player joins the world
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        Player player = event.getEntity();
        player.sendSystemMessage(Component.literal("§6[Tavern Keeper] §rWelcome to your tavern! Your journey as a keeper begins! 🍺"));
        LOGGER.info("Player {} joined - mod is working!", player.getName().getString());
        
        // Sync areas to the joining player
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.server.level.ServerLevel level = serverPlayer.serverLevel();
            Tavern tavern = Tavern.get(level);
            // Convert spaces to TavernArea for network packet
            java.util.List<maxitoson.tavernkeeper.areas.TavernArea> areas = tavern.getAllSpaces().stream()
                .map(space -> space.getArea())
                .toList();
            NetworkHandler.sendToPlayer(new SyncAreasPacket(areas), serverPlayer);
        }
    }
    
    // Handle right-click on block - HIGH PRIORITY to intercept sign interactions before editor opens
    // Cancel the event if the player is holding a marking cane and clicking on a sign
    // If the player is clicking on a tavern sign, toggle the tavern sign
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        Player player = event.getEntity();
        ItemStack heldItem = player.getItemInHand(event.getHand());
        net.minecraft.core.BlockPos pos = event.getPos();
        net.minecraft.world.level.Level level = event.getLevel();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        // Client-side: prevent sign editor from opening if holding marking cane
        if (level.isClientSide) {
            if (state.getBlock() instanceof net.minecraft.world.level.block.SignBlock 
                && heldItem.getItem() instanceof MarkingCane) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
            return;
        }
        
        // Server-side routing logic
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        Tavern tavern = Tavern.get(serverLevel);
        
        // Route 1: Holding marking cane + clicking sign → delegate to MarkingCane
        if (heldItem.getItem() instanceof MarkingCane 
            && state.getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
            MarkingCane.handleSignClick(serverLevel, pos, player);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        
        // Route 2: Clicking on tavern sign (without marking cane) → delegate to Tavern
        if (tavern.isTavernSign(pos)) {
            tavern.toggleOpenClosed(player);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
    }
    
    // Handle left-click on block with Marking Cane (clears selection or deletes area)
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof MarkingCane) {
            if (!player.level().isClientSide) {
                MarkingCane.handleLeftClick(player, event.getPos(), 
                    (net.minecraft.server.level.ServerLevel) player.level());
            }
            event.setCanceled(true); // Prevent block breaking
        }
    }
    
    // Register commands
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        AreaCommand.register(event.getDispatcher());
        LOGGER.info("Registered tavern area commands");
    }

    // Handle block placement to update furniture in areas
    @SubscribeEvent
    public void onBlockPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos pos = event.getPos();
            Tavern tavern = Tavern.get(serverLevel);
            
            // If placing a block at the tavern sign position, clear it (sign was replaced)
            if (tavern.isTavernSign(pos) && !(event.getState().getBlock() instanceof net.minecraft.world.level.block.SignBlock)) {
                LOGGER.info("Block placed at tavern sign position {} - clearing tavern sign", pos);
                tavern.clearTavernSign();
            }
            
            tavern.getSpacesAt(pos).forEach(space -> space.onBlockUpdated(pos, event.getState()));
        }
    }

    // Handle block breaking to remove furniture from areas
    @SubscribeEvent
    public void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos pos = event.getPos();
            Tavern tavern = Tavern.get(serverLevel);
            
            // If breaking the tavern sign block, clear the reference
            if (tavern.isTavernSign(pos)) {
                LOGGER.info("Tavern sign at {} was broken - clearing tavern sign", pos);
                tavern.clearTavernSign();
            }
            
            // Pass the OLD state to handle multi-block removal correctly
            tavern.getSpacesAt(pos).forEach(space -> space.onBlockBroken(pos, event.getState()));
        }
    }
    
    // Make skeletons target customers (zombies already target AbstractVillager naturally)
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton && !event.getLevel().isClientSide()) {
            // Add a goal to target customers (priority 3, same as zombie's villager targeting)
            skeleton.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                skeleton, 
                CustomerEntity.class, 
                true
            ));
        }
    }
    
    // Handle player right-clicking customer to serve them
    @SubscribeEvent
    public void onPlayerInteractEntity(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        
        if (event.getTarget() instanceof CustomerEntity customer) {
            // TODO: Put in a handler class (?)
            Player player = event.getEntity();
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            // Check if customer is waiting for service
            if (customer.getCustomerState() != maxitoson.tavernkeeper.entities.ai.CustomerState.WAITING_SERVICE) {
                return;
            }
            
            maxitoson.tavernkeeper.tavern.economy.FoodRequest request = customer.getFoodRequest();
            if (request == null) {
                LOGGER.warn("Customer {} is waiting for service but has no food request!", customer.getId());
                return;
            }
            
            // Check if player has the right food
            if (request.isSatisfiedBy(heldItem)) {
                // Remove food from player
                heldItem.shrink(request.getRequestedAmount());
                
                // Give player the payment (full coin breakdown) via event
                // WalletHandler will add to wallet or fall back to inventory
                NeoForge.EVENT_BUS.post(new CustomerPaymentEvent(player, customer, request));
                
                // Customer received food - transition to next state
                customer.setCustomerState(maxitoson.tavernkeeper.entities.ai.CustomerState.FINDING_SEAT);
                customer.setFoodRequest(null);
                
                // Success message
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Served customer " + request.getDisplayName() + "! +" + request.getPrice().getDisplayName()),
                    true // action bar
                );
                
                // Play sound
                customer.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F); // TODO: Make cash sound
                
                LOGGER.info("Player {} served customer {} with {} and received {}", 
                    player.getName().getString(), customer.getId(), request.getDisplayName(), request.getPrice().getDisplayName());
                
                event.setCanceled(true);
            } else {
                // Wrong item or not enough
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Customer wants " + request.getDisplayName() + "!"),
                    true // action bar
                );
                
                // Play sound
                customer.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            }
        }
    }
    

}

