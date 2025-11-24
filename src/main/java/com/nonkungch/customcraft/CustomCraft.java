package com.nonkungch.customcraft; 

import nonkungch.customcraft.command.AdminRecipeCommand;
import nonkungch.customcraft.command.PlayerCraftCommand;
import nonkungch.customcraft.gui.GUIHandler;
import nonkungch.customcraft.manager.RecipeManager;
import nonkungch.customcraft.util.RecipeRemover;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomCraft extends JavaPlugin {

    private static NonkungchCustomCraft instance;
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

    public static NonkungchCustomCraft getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    }
