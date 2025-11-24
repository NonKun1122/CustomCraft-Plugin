package nonkungch.customcraft.gui;

import nonkungch.customcraft.model.CustomRecipe;
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
    public static final String ADMIN_MENU_TITLE = ChatColor.RED + "Admin: Recipe List";
    public static final String EDIT_TITLE_PREFIX = ChatColor.DARK_RED + "Admin Edit: ";
    
    public static final Integer[] EDIT_INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39}; 
    public static final int EDIT_OUTPUT_SLOT = 24; 
    
    public static final int BUTTON_SAVE = 47;
    public static final int BUTTON_RENAME = 49;
    public static final int BUTTON_DELETE = 51;
    
    public static void openAdminMenu(Player player, Collection<CustomRecipe> recipes) {
        Inventory gui = Bukkit.createInventory(player, 54, ADMIN_MENU_TITLE);
        
        gui.setItem(49, createAdminMenuItem(Material.NETHER_STAR, ChatColor.GREEN + "§lAdd New Recipe", ChatColor.YELLOW + "Click to create a new recipe."));
        
        int slot = 0;
        for (CustomRecipe recipe : recipes) {
            if (slot >= 45) break; 
            
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
    
    public static void openEditGUI(Player player, CustomRecipe recipe, boolean isNew) {
        Inventory gui = Bukkit.createInventory(player, 54, EDIT_TITLE_PREFIX + recipe.getName());
        
        ItemStack border = createBorderItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (!Arrays.asList(EDIT_INPUT_SLOTS).contains(i) && i != EDIT_OUTPUT_SLOT) {
                gui.setItem(i, border);
            }
        }
        
        for (int i = 0; i < EDIT_INPUT_SLOTS.length; i++) {
            gui.setItem(EDIT_INPUT_SLOTS[i], recipe.getIngredients().get(i));
        }
        gui.setItem(EDIT_OUTPUT_SLOT, recipe.getResult());
        
        gui.setItem(BUTTON_SAVE, createAdminMenuItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "§lSAVE RECIPE", ChatColor.GRAY + "บันทึกสูตรนี้"));
        gui.setItem(BUTTON_RENAME, createAdminMenuItem(Material.NAME_TAG, ChatColor.YELLOW + "§lRENAME", ChatColor.GRAY + "เปลี่ยนชื่อสูตร"));
        if (!isNew) {
            gui.setItem(BUTTON_DELETE, createAdminMenuItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "§lDELETE RECIPE", ChatColor.GRAY + "ลบสูตรนี้อย่างถาวร"));
        }
        
        player.openInventory(gui);
    }
    
    public static ItemStack createAdminMenuItem(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLine));
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

    public static List<ItemStack> createEmptyIngredientList() {
        return Arrays.asList(new ItemStack[12]);
    }
              }
          
