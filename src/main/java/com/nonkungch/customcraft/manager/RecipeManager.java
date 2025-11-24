package com.nonkungch.customcraft.manager;

import com.nonkungch.customcraft.CustomCraft; 
import com.nonkungch.customcraft.model.CustomRecipe;
import com.nonkungch.customcraft.util.ItemSerializer; 
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey; // ⭐️ New
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe; // ⭐️ New
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RecipeManager {
    private final CustomCraft plugin; 
    private final Map<String, CustomRecipe> recipes = new HashMap<>();
    private final File recipeFile;

    public RecipeManager(CustomCraft plugin) { 
        this.plugin = plugin;
        this.recipeFile = new File(plugin.getDataFolder(), "recipes.yml");
    }

    public void loadRecipes() {
        if (!recipeFile.exists()) {
            try { recipeFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        YamlConfiguration recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);
        
        int loadedCount = 0;
        for (String id : recipeConfig.getKeys(false)) {
            String name = recipeConfig.getString(id + ".name", "Untitled Recipe");
            ItemStack result = ItemSerializer.deserialize(recipeConfig.getString(id + ".result")); 
            
            List<ItemStack> ingredients = recipeConfig.getStringList(id + ".ingredients").stream()
                                            .map(ItemSerializer::deserialize) 
                                            .collect(Collectors.toList());
            
            CustomRecipe recipe = new CustomRecipe(id, name, ingredients, result);
            recipes.put(id, recipe);
            
            // ⭐️ ลงทะเบียน Bukkit Recipe เมื่อโหลด
            registerBukkitRecipe(recipe);
            loadedCount++;
        }
        plugin.getLogger().info("Loaded and registered " + loadedCount + " custom recipes.");
    }

    public void saveRecipes() {
        YamlConfiguration recipeConfig = new YamlConfiguration();
        for (CustomRecipe recipe : recipes.values()) {
            String id = recipe.getIdentifier();
            recipeConfig.set(id + ".name", recipe.getName());
            recipeConfig.set(id + ".result", ItemSerializer.serialize(recipe.getResult()));
            
            List<String> ingredientStrings = recipe.getIngredients().stream()
                                            .map(ItemSerializer::serialize)
                                            .collect(Collectors.toList());
            recipeConfig.set(id + ".ingredients", ingredientStrings);
        }
        
        try {
            recipeConfig.save(recipeFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save recipes.yml!");
            e.printStackTrace();
        }
    }

    // =======================================================
    // ⭐️ เมท็อดใหม่สำหรับการจัดการสูตรในระบบ Bukkit (1.13+)
    // =======================================================

    private void registerBukkitRecipe(CustomRecipe customRecipe) {
        if (customRecipe.getResult() == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, customRecipe.getIdentifier().toLowerCase());
        
        // ลบสูตรเดิมออกก่อน ถ้ามี (ใช้สำหรับอัปเดต)
        Bukkit.removeRecipe(key);
        
        ShapedRecipe recipe = new ShapedRecipe(key, customRecipe.getResult().clone());
        
        // กำหนดรูปแบบ 3x3 สำหรับ Bukkit Recipe (ใช้แค่ 9 ช่องแรกของ GUI)
        recipe.shape("ABC",
                     "DEF",
                     "GHI");

        char symbol = 'A';
        for (int i = 0; i < 9; i++) {
            ItemStack ingredient = customRecipe.getIngredients().size() > i ? customRecipe.getIngredients().get(i) : null;
            
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                // ใช้ Material เท่านั้นสำหรับ ShapedRecipe
                recipe.setIngredient(symbol, ingredient.getType()); 
            } else {
                 // ใส่ช่องว่าง
                 recipe.setIngredient(symbol, Material.AIR); 
            }
            symbol++;
        }
        
        try {
             Bukkit.addRecipe(recipe);
        } catch (IllegalStateException e) {
             plugin.getLogger().warning("Failed to register recipe key: " + customRecipe.getIdentifier() + ". " + e.getMessage());
        }
    }
    
    // สำหรับเรียกใช้เมื่อปิดปลั๊กอิน
    public void unregisterBukkitRecipes() {
        for (CustomRecipe recipe : recipes.values()) {
            NamespacedKey key = new NamespacedKey(plugin, recipe.getIdentifier().toLowerCase());
            Bukkit.removeRecipe(key);
        }
    }
    
    // สำหรับเรียกใชเมื่อ Admin กด Save
    public void updateBukkitRecipe(CustomRecipe recipe) {
        registerBukkitRecipe(recipe); 
    }

    // =======================================================
    // เมท็อด CRUD
    // =======================================================

    public void addRecipe(CustomRecipe recipe) { 
        recipes.put(recipe.getIdentifier(), recipe); 
        registerBukkitRecipe(recipe); 
    }
    
    public void removeRecipe(String identifier) { 
        CustomRecipe recipe = recipes.remove(identifier);
        if (recipe != null) {
            NamespacedKey key = new NamespacedKey(plugin, recipe.getIdentifier().toLowerCase());
            Bukkit.removeRecipe(key); 
        }
    }
    
    public Collection<CustomRecipe> getRecipes() { return recipes.values(); }
    public CustomRecipe getRecipe(String id) { return recipes.get(id); }
}
