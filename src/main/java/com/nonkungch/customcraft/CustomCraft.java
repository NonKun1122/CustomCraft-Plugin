package com.nonkungch.customcraft;

import com.nonkungch.customcraft.command.AdminRecipeCommand;
import com.nonkungch.customcraft.command.PlayerCraftCommand;
import com.nonkungch.customcraft.gui.GUIHandler;
import com.nonkungch.customcraft.manager.RecipeManager;
import com.nonkungch.customcraft.util.RecipeRemover;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomCraft extends JavaPlugin {

    private static CustomCraft instance;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); 
        
        this.recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes(); 
        
        this.getCommand("customcraft").setExecutor(new PlayerCraftCommand(this));
        this.getCommand("ccadmin").setExecutor(new AdminRecipeCommand(this));
        
        Bukkit.getPluginManager().registerEvents(new GUIHandler(this), this);
        
        RecipeRemover.removeVanillaRecipes(this); 
        
        getLogger().info("[CustomCraft] Plugin by nonkungch Enabled!");
    }

    @Override
    public void onDisable() {
        recipeManager.saveRecipes();
        getLogger().info("[CustomCraft] Plugin Disabled.");
    }

    public static CustomCraft getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
