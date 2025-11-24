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
    // เมท็อดสำหรับ Admin เพื่ออัปเดตข้อมูล
    public void setIngredients(List<ItemStack> ingredients) { this.ingredients = ingredients; }
    public void setResult(ItemStack result) { this.result = result; }
    
    // ตรวจสอบความถูกต้องของส่วนผสม
    public boolean matches(ItemStack[] inputItems) {
        if (inputItems.length != 12) return false;
        
        for (int i = 0; i < 12; i++) {
            ItemStack required = ingredients.get(i);
            ItemStack given = inputItems[i];
            
            // ตรวจสอบว่าไอเท็มที่ผู้เล่นใส่ตรงกับที่กำหนดไว้ในสูตรหรือไม่ (ไม่สนจำนวน)
            if (required != null && required.getType() != org.bukkit.Material.AIR) {
                if (given == null || !required.isSimilar(given) || given.getAmount() < required.getAmount()) {
                     return false; 
                }
            } else if (given != null && given.getType() != org.bukkit.Material.AIR) {
                // ถ้าสูตรไม่ต้องการอะไรในช่องนี้ แต่ผู้เล่นใส่
                return false;
            }
        }
        return true;
    }
}
