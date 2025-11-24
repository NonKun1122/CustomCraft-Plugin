package com.nonkungch.customcraft.model;

import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class CustomRecipe {
    
    private final String identifier;
    private String name;
    private List<ItemStack> ingredients; 
    private ItemStack result; 

    public CustomRecipe(String name, List<ItemStack> ingredients, ItemStack result) {
        this(UUID.randomUUID().toString(), name, ingredients, result);
    }

    public CustomRecipe(String identifier, String name, List<ItemStack> ingredients, ItemStack result) {
        this.identifier = identifier;
        this.name = name;
        this.ingredients = ingredients;
        this.result = result;
    }
    
    public String getIdentifier() { return identifier; }
    public String getName() { return name; }
    public List<ItemStack> getIngredients() { return ingredients; }
    public ItemStack getResult() { return result; }
    
    public void setName(String name) { this.name = name; }
    public void setIngredients(List<ItemStack> ingredients) { this.ingredients = ingredients; }
    public void setResult(ItemStack result) { this.result = result; }
    
    public boolean matches(ItemStack[] inputItems) {
        if (inputItems.length != 12) return false;
        
        for (int i = 0; i < 12; i++) {
            ItemStack required = ingredients.get(i);
            ItemStack given = inputItems[i];
            
            if (required != null && required.getType() != org.bukkit.Material.AIR) {
                if (given == null || !required.isSimilar(given) || given.getAmount() < required.getAmount()) {
                     return false; 
                }
            } else if (given != null && given.getType() != org.bukkit.Material.AIR) {
                return false;
            }
        }
        return true;
    }
}
