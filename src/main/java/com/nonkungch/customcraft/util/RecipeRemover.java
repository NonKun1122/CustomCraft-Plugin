package com.nonkungch.customcraft.util;

import com.nonkungch.customcraft.CustomCraft; 
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;
import java.util.Iterator;

public class RecipeRemover {
    
    public static void removeVanillaRecipes(CustomCraft plugin) { 
        String[] materialsToRemove = {
            "IRON_INGOT", "IRON_BLOCK", "IRON_SWORD", "IRON_PICKAXE", "IRON_AXE", "IRON_CHESTPLATE",
            "GOLD_INGOT", "GOLD_BLOCK", "GOLDEN_SWORD", "GOLDEN_PICKAXE",
            "DIAMOND", "DIAMOND_BLOCK", "DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_CHESTPLATE",
            "NETHERITE_INGOT", "NETHERITE_BLOCK", "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_CHESTPLATE"
        };

        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe == null || recipe.getResult() == null) continue;
            
            String resultName = recipe.getResult().getType().name();
            
            for (String matName : materialsToRemove) {
                if (resultName.equals(matName)) {
                    it.remove();
                    count++;
                    break;
                }
            }
        }
        plugin.getLogger().info("Removed " + count + " vanilla recipes.");
    }
}
