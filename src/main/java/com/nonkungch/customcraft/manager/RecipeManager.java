package com.nonkungch.customcraft.manager;

import com.nonkungch.customcraft.CustomCraft;
import com.nonkungch.customcraft.model.CustomRecipe;
import vom.nonkungch.customcraft.util.ItemSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
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
        
        for (String id : recipeConfig.getKeys(false)) {
            String name = recipeConfig.getString(id + ".name", "Untitled Recipe");
            ItemStack result = ItemSerializer.deserialize(recipeConfig.getString(id + ".result"));
            
            List<ItemStack> ingredients = recipeConfig.getStringList(id + ".ingredients").stream()
                                            .map(ItemSerializer::deserialize)
                                            .collect(Collectors.toList());
            
            recipes.put(id, new CustomRecipe(id, name, ingredients, result));
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " custom recipes.");
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

    public void addRecipe(CustomRecipe recipe) { recipes.put(recipe.getIdentifier(), recipe); }
    public void removeRecipe(String identifier) { recipes.remove(identifier); }
    public Collection<CustomRecipe> getRecipes() { return recipes.values(); }
    public CustomRecipe getRecipe(String id) { return recipes.get(id); }
                        }
          
