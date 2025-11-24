package com.nonkungch.customcraft.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class CustomRecipe {
    
    private final String identifier;
    private String name;
    // ส่วนผสม 12 ช่อง (สำหรับ GUI 4x3)
    private List<ItemStack> ingredients; 
    private ItemStack result; 

    /**
     * Constructor สำหรับการสร้างสูตรใหม่ (จะสร้าง UUID ให้อัตโนมัติ)
     */
    public CustomRecipe(String name, List<ItemStack> ingredients, ItemStack result) {
        this(UUID.randomUUID().toString(), name, ingredients, result);
    }

    /**
     * Constructor สำหรับการโหลดสูตรจากไฟล์ (โดยใช้ identifier เดิม)
     */
    public CustomRecipe(String identifier, String name, List<ItemStack> ingredients, ItemStack result) {
        this.identifier = identifier;
        this.name = name;
        this.ingredients = ingredients;
        this.result = result;
    }
    
    // --- Getters ---
    public String getIdentifier() { return identifier; }
    public String getName() { return name; }
    public List<ItemStack> getIngredients() { return ingredients; }
    public ItemStack getResult() { return result; }
    
    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setIngredients(List<ItemStack> ingredients) { this.ingredients = ingredients; }
    public void setResult(ItemStack result) { this.result = result; }
    
    
    /**
     * ตรวจสอบว่า inputItems (ไอเท็มที่ผู้เล่นใส่ใน GUI) ตรงตามสูตรที่ต้องการหรือไม่
     * การตรวจสอบนี้มีความเข้มงวด (Exact Match)
     * * @param inputItems array ของ ItemStack จาก slot 12 ช่องของ GUI
     * @return true ถ้าส่วนผสมตรงกันทั้งหมด
     */
    public boolean matches(ItemStack[] inputItems) {
        // ตรวจสอบขนาด Input ว่าตรงกับ 12 ช่องหรือไม่
        if (inputItems.length != 12) return false;
        
        for (int i = 0; i < 12; i++) {
            ItemStack required = ingredients.get(i);
            ItemStack given = inputItems[i];
            
            // 1. กรณีที่สูตรต้องการไอเท็มในช่องนี้ (required ไม่ใช่ null และไม่ใช่ AIR)
            if (required != null && required.getType() != Material.AIR) {
                
                // ตรวจสอบว่าผู้เล่นใส่ไอเท็มหรือไม่
                if (given == null || given.getType() == Material.AIR) {
                    return false; // ต้องการ แต่ผู้เล่นไม่ได้ใส่
                }
                
                // ตรวจสอบชนิด, ชื่อ, lore, enchant (isSimilar)
                if (!required.isSimilar(given)) {
                    return false; // ไอเท็มไม่ตรงกัน (ไม่ใช่ชนิด/metadata เดียวกัน)
                }
                
                // ตรวจสอบจำนวน
                if (given.getAmount() < required.getAmount()) {
                    return false; // จำนวนไม่พอ
                }
                
            } 
            // 2. กรณีที่สูตรต้องการช่องว่าง (required เป็น null หรือ AIR)
            else { 
                // ถ้าผู้เล่นใส่ไอเท็มเข้ามาในช่องที่ต้องว่าง
                if (given != null && given.getType() != Material.AIR) {
                    return false; // ต้องว่าง แต่ผู้เล่นใส่ไอเท็มเข้ามา
                }
                // ถ้า given เป็น null หรือ AIR ก็ถือว่าตรงตามเงื่อนไข (ช่องว่าง)
            }
        }
        return true;
    }
}
