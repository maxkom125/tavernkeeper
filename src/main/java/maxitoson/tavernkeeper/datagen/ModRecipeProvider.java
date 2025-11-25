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
        // Copper Coin <-> Iron Coin
        // 9 Copper -> 1 Iron
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TavernKeeperMod.IRON_COIN.get())
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', TavernKeeperMod.COPPER_COIN.get())
                .unlockedBy("has_copper_coin", has(TavernKeeperMod.COPPER_COIN.get()))
                .save(output, "tavernkeeper:iron_coin_from_copper");

        // 1 Iron -> 9 Copper
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.COPPER_COIN.get(), 9)
                .requires(TavernKeeperMod.IRON_COIN.get())
                .unlockedBy("has_iron_coin", has(TavernKeeperMod.IRON_COIN.get()))
                .save(output, "tavernkeeper:copper_coin_from_iron");

        // Iron Coin <-> Gold Coin
        // 9 Iron -> 1 Gold
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TavernKeeperMod.GOLD_COIN.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', TavernKeeperMod.IRON_COIN.get())
                .unlockedBy("has_iron_coin", has(TavernKeeperMod.IRON_COIN.get()))
                .save(output, "tavernkeeper:gold_coin_from_iron");

        // 1 Gold -> 9 Iron
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.IRON_COIN.get(), 9)
                .requires(TavernKeeperMod.GOLD_COIN.get())
                .unlockedBy("has_gold_coin", has(TavernKeeperMod.GOLD_COIN.get()))
                .save(output, "tavernkeeper:iron_coin_from_gold");

        // Gold Coin <-> Diamond Coin
        // 9 Gold -> 1 Diamond
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TavernKeeperMod.DIAMOND_COIN.get())
                .pattern("GGG")
                .pattern("GGG")
                .pattern("GGG")
                .define('G', TavernKeeperMod.GOLD_COIN.get())
                .unlockedBy("has_gold_coin", has(TavernKeeperMod.GOLD_COIN.get()))
                .save(output, "tavernkeeper:diamond_coin_from_gold");

        // 1 Diamond -> 9 Gold
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.GOLD_COIN.get(), 9)
                .requires(TavernKeeperMod.DIAMOND_COIN.get())
                .unlockedBy("has_diamond_coin", has(TavernKeeperMod.DIAMOND_COIN.get()))
                .save(output, "tavernkeeper:gold_coin_from_diamond");

        // Diamond Coin <-> Netherite Coin
        // 9 Diamond -> 1 Netherite
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TavernKeeperMod.NETHERITE_COIN.get())
                .pattern("DDD")
                .pattern("DDD")
                .pattern("DDD")
                .define('D', TavernKeeperMod.DIAMOND_COIN.get())
                .unlockedBy("has_diamond_coin", has(TavernKeeperMod.DIAMOND_COIN.get()))
                .save(output, "tavernkeeper:netherite_coin_from_diamond");

        // 1 Netherite -> 9 Diamond
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.DIAMOND_COIN.get(), 9)
                .requires(TavernKeeperMod.NETHERITE_COIN.get())
                .unlockedBy("has_netherite_coin", has(TavernKeeperMod.NETHERITE_COIN.get()))
                .save(output, "tavernkeeper:diamond_coin_from_netherite");

        // Emerald Coin Recipe
        // 8 Netherite Coins + 1 Emerald -> 8 Emerald Coins (Based on existing JSON)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TavernKeeperMod.EMERALD_COIN.get(), 8)
                .pattern("NNN")
                .pattern("NEN")
                .pattern("NNN")
                .define('N', TavernKeeperMod.NETHERITE_COIN.get())
                .define('E', Items.EMERALD)
                .unlockedBy("has_netherite_coin", has(TavernKeeperMod.NETHERITE_COIN.get()))
                .save(output);
                
        // Reversible: 1 Emerald Coin -> 1 Netherite Coin
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TavernKeeperMod.NETHERITE_COIN.get())
                .requires(TavernKeeperMod.EMERALD_COIN.get())
                .unlockedBy("has_emerald_coin", has(TavernKeeperMod.EMERALD_COIN.get()))
                .save(output, "tavernkeeper:netherite_coin_from_emerald");
    }
}

