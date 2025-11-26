package maxitoson.tavernkeeper.datagen;

import maxitoson.tavernkeeper.TavernKeeperMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Coin uncrafting (matches wallet's 100:1 auto-conversion rate)
        // Note: Crafting outputs max 64 items, so player loses 36 coins per uncraft
        
        // 1 Iron -> 64 Copper (100 value, 36 loss)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.COPPER_COIN.get(), 64)
                .requires(TavernKeeperMod.IRON_COIN.get())
                .unlockedBy("has_iron_coin", has(TavernKeeperMod.IRON_COIN.get()))
                .save(output, "tavernkeeper:copper_coin_from_iron");

        // 1 Gold -> 64 Iron (100 value, 36 loss)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.IRON_COIN.get(), 64)
                .requires(TavernKeeperMod.GOLD_COIN.get())
                .unlockedBy("has_gold_coin", has(TavernKeeperMod.GOLD_COIN.get()))
                .save(output, "tavernkeeper:iron_coin_from_gold");

        // 1 Diamond -> 64 Gold (100 value, 36 loss)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.GOLD_COIN.get(), 64)
                .requires(TavernKeeperMod.DIAMOND_COIN.get())
                .unlockedBy("has_diamond_coin", has(TavernKeeperMod.DIAMOND_COIN.get()))
                .save(output, "tavernkeeper:gold_coin_from_diamond");

        // 1 Netherite -> 64 Diamond (100 value, 36 loss)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.DIAMOND_COIN.get(), 64)
                .requires(TavernKeeperMod.NETHERITE_COIN.get())
                .unlockedBy("has_netherite_coin", has(TavernKeeperMod.NETHERITE_COIN.get()))
                .save(output, "tavernkeeper:diamond_coin_from_netherite");
                
        // Wallet Recipe
        // Leather left, right, and bottom of Copper Coin
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, TavernKeeperMod.WALLET.get())
                .pattern("LCL")
                .pattern(" L ")
                .define('L', Items.LEATHER)
                .define('C', TavernKeeperMod.COPPER_COIN.get())
                .unlockedBy("has_copper_coin", has(TavernKeeperMod.COPPER_COIN.get()))
                .save(output);
    }
}

