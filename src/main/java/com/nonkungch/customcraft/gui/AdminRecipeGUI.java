package com.nonkungch.customcraft.gui;

import com.nonkungch.customcraft.model.CustomRecipe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AdminRecipeGUI {
    // ชื่อเมนู
    public static final String ADMIN_MENU_TITLE = ChatColor.RED + "Admin: Recipe List";
    public static final String EDIT_TITLE_PREFIX = ChatColor.DARK_RED + "Admin Edit: ";
    
    // ตำแหน่ง Slot สำหรับส่วนผสม (4 แถว x 3 คอลัมน์)
    public static final Integer[] EDIT_INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39}; 
    // ตำแหน่ง Slot สำหรับผลลัพธ์
    public static final int EDIT_OUTPUT_SLOT = 24; 
    
    // ตำแหน่งปุ่มควบคุม
    public static final int BUTTON_SAVE = 47;
    public static final int BUTTON_RENAME = 49;
    public static final int BUTTON_DELETE = 51;
    
    /**
     * เปิดเมนูแสดงรายการสูตรสำหรับผู้ดูแลระบบ
     * @param player ผู้เล่นที่เปิดเมนู
     * @param recipes รายการสูตรทั้งหมด
     */
    public static void openAdminMenu(Player player, Collection<CustomRecipe> recipes) {
        Inventory gui = Bukkit.createInventory(player, 54, ADMIN_MENU_TITLE);
        
        // ปุ่ม Add New Recipe (ตำแหน่งตรงกลางแถวล่าง)
        gui.setItem(BUTTON_RENAME, createAdminMenuItem(Material.NETHER_STAR, ChatColor.GREEN + "§lAdd New Recipe", ChatColor.YELLOW + "Click to create a new recipe."));
        
        int slot = 0;
        for (CustomRecipe recipe : recipes) {
            if (slot >= 45) break; // จำกัดสูตรไม่เกิน 45 ช่อง
            
            // ใช้ไอเท็มผลลัพธ์ในการแสดงสูตร
            ItemStack item = recipe.getResult() != null ? recipe.getResult().clone() : new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "§l" + recipe.getName());
            meta.setLore(List.of(
                    ChatColor.YELLOW + "ID: " + recipe.getIdentifier(),
                    ChatColor.GRAY + "Click to edit this recipe."
            ));
            item.setItemMeta(meta);
            
            gui.setItem(slot++, item);
        }
        
        player.openInventory(gui);
    }
    
    /**
     * เปิดเมนูแก้ไขสูตรเฉพาะ (Edit/Create)
     * @param player ผู้เล่นที่เปิดเมนู
     * @param recipe สูตรที่ต้องการแก้ไข
     * @param isNew เป็นสูตรใหม่หรือไม่ (เพื่อซ่อนปุ่ม Delete)
     */
    public static void openEditGUI(Player player, CustomRecipe recipe, boolean isNew) {
        // ชื่อเมนูจะมีชื่อสูตรต่อท้าย เช่น "Admin Edit: New Recipe (Temp)"
        Inventory gui = Bukkit.createInventory(player, 54, EDIT_TITLE_PREFIX + recipe.getName());
        
        ItemStack border = createBorderItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // วางกรอบและปุ่ม
        for (int i = 0; i < 54; i++) {
            // เว้นช่องสำหรับ Input, Output และปุ่มควบคุม
            if (!Arrays.asList(EDIT_INPUT_SLOTS).contains(i) && i != EDIT_OUTPUT_SLOT && 
                i != BUTTON_SAVE && i != BUTTON_RENAME && i != BUTTON_DELETE) {
                gui.setItem(i, border);
            }
        }
        
        // วางส่วนผสม (Input Slots)
        for (int i = 0; i < EDIT_INPUT_SLOTS.length; i++) {
            ItemStack ingredient = recipe.getIngredients().size() > i ? recipe.getIngredients().get(i) : null;
            gui.setItem(EDIT_INPUT_SLOTS[i], ingredient);
        }
        
        // วางผลลัพธ์ (Output Slot)
        gui.setItem(EDIT_OUTPUT_SLOT, recipe.getResult());
        
        // ปุ่ม Save
        gui.setItem(BUTTON_SAVE, createAdminMenuItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "§lSAVE RECIPE", ChatColor.GRAY + "บันทึกสูตรนี้"));
        
        // ปุ่ม Rename (ใช้ปุ่มตรงกลาง)
        gui.setItem(BUTTON_RENAME, createAdminMenuItem(Material.NAME_TAG, ChatColor.YELLOW + "§lRENAME", ChatColor.GRAY + "เปลี่ยนชื่อสูตร"));
        
        // ปุ่ม Delete (แสดงเฉพาะเมื่อไม่ใช่สูตรใหม่)
        if (!isNew) {
            gui.setItem(BUTTON_DELETE, createAdminMenuItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "§lDELETE RECIPE", ChatColor.GRAY + "ลบสูตรนี้อย่างถาวร"));
        }
        
        player.openInventory(gui);
    }
    
    /**
     * Helper Method สำหรับสร้างปุ่มควบคุม/เมนู
     */
    public static ItemStack createAdminMenuItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLine));
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Helper Method สำหรับสร้าง Item กรอบ (Border)
     */
    private static ItemStack createBorderItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * สร้าง List ของ ItemStack ว่างเปล่า 12 ช่อง สำหรับสูตรใหม่
     */
    public static List<ItemStack> createEmptyIngredientList() {
        ItemStack[] emptyArray = new ItemStack[12];
        // ไม่จำเป็นต้อง Arrays.fill(emptyArray, null) เพราะ Java กำหนดค่าเริ่มต้นเป็น null อยู่แล้ว
        return Arrays.asList(emptyArray);
    }
}
