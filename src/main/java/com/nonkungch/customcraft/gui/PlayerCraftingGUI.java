package com.nonkungch.customcraft.gui;

import com.nonkungch.customcraft.model.CustomRecipe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;

public class PlayerCraftingGUI {
    
    public static final String MENU_TITLE = ChatColor.BLUE + "Custom Recipe Menu";
    public static final String DETAIL_TITLE_PREFIX = ChatColor.DARK_GREEN + "Craft: ";
    
    public static final Integer[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39}; 
    public static final int RESULT_DISPLAY_SLOT = 24; 
    public static final int CRAFT_BUTTON_SLOT = 43; 

    public static void openRecipeSelectionGUI(Player player, Collection<CustomRecipe> recipes) {
        Inventory gui = Bukkit.createInventory(player, 54, MENU_TITLE);
        
        int slot = 0;
        for (CustomRecipe recipe : recipes) {
            if (slot >= 54) break;
            
            ItemStack item = recipe.getResult() != null ? recipe.getResult().clone() : new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            
            List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
            lore.add(ChatColor.YELLOW + "--------------------");
            lore.add(ChatColor.GREEN + "Click to view recipe details.");
            lore.add(ChatColor.GRAY + "ID: " + recipe.getIdentifier()); 
            meta.setLore(lore);
            meta.setDisplayName(ChatColor.AQUA + "§l" + recipe.getName());
            item.setItemMeta(meta);
            
            gui.setItem(slot++, item);
        }
        
        player.openInventory(gui);
    }

    public static void openRecipeDetailGUI(Player player, CustomRecipe recipe) {
        Inventory gui = Bukkit.createInventory(player, 54, DETAIL_TITLE_PREFIX + recipe.getName());
        
        ItemStack border = createBorderItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (!isCraftingSlot(i)) {
                gui.setItem(i, border);
            }
        }
        
        gui.setItem(RESULT_DISPLAY_SLOT, createDisplayItem(recipe.getResult(), 
                ChatColor.GOLD + "§lRESULT", 
                ChatColor.YELLOW + "ไอเท็มที่จะได้รับ"));
        
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            ItemStack ingredient = recipe.getIngredients().get(i);
            gui.setItem(INPUT_SLOTS[i], ingredient != null ? ingredient.clone() : null); 
        }
        
        gui.setItem(CRAFT_BUTTON_SLOT, createCraftButton());
        
        player.openInventory(gui);
    }
    
    private static boolean isCraftingSlot(int slot) {
        for (int inputSlot : INPUT_SLOTS) {
            if (slot == inputSlot) return true;
        }
        return slot == RESULT_DISPLAY_SLOT || slot == CRAFT_BUTTON_SLOT;
    }
    
    private static ItemStack createCraftButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "§lCRAFT (5 วินาที)");
        meta.setLore(List.of(ChatColor.GRAY + "คลิกเพื่อเริ่มคราฟต์"));
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createBorderItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createDisplayItem(ItemStack source, String name, String loreLine) {
        ItemStack item = source.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLine));
        item.setItemMeta(meta);
        return item;
    }
}
