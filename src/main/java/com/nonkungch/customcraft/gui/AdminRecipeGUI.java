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

    // ================================
    // GUI Titles
    // ================================
    public static final String ADMIN_MENU_TITLE = ChatColor.RED + "Admin: Recipe List";
    public static final String EDIT_TITLE_PREFIX = ChatColor.DARK_RED + "Admin Edit: ";

    // ================================
    // Input / Output slots (4x3 grid = 12 items)
    // ================================
    public static final Integer[] EDIT_INPUT_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30,
            37, 38, 39
    };

    public static final int EDIT_OUTPUT_SLOT = 24;

    // ================================
    // Control Buttons
    // ================================
    public static final int BUTTON_SAVE = 47;
    public static final int BUTTON_RENAME = 49;
    public static final int BUTTON_DELETE = 51;

    // ปุ่ม Add New อยู่ในหน้า Admin Menu ตรงกลาง
    public static final int BUTTON_ADD_NEW = 49;

    // ================================
    // Admin Menu – Recipe List
    // ================================
    public static void openAdminMenu(Player player, Collection<CustomRecipe> recipes) {

        Inventory gui = Bukkit.createInventory(player, 54, ADMIN_MENU_TITLE);

        // ปุ่ม Create new recipe
        gui.setItem(BUTTON_ADD_NEW, createItem(
                Material.NETHER_STAR,
                ChatColor.GREEN + "§lAdd New Recipe",
                List.of(ChatColor.YELLOW + "Click to create a new recipe.")
        ));

        int slot = 0;

        for (CustomRecipe recipe : recipes) {
            if (slot >= 45) break; // แถวบน 5 แถวเท่านั้น

            ItemStack item = recipe.getResult() != null
                    ? recipe.getResult().clone()
                    : new ItemStack(Material.BOOK);

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

    // ================================
    // Edit GUI – เปิดแก้ไขสูตร
    // ================================
    public static void openEditGUI(Player player, CustomRecipe recipe, boolean isNew) {

        Inventory gui = Bukkit.createInventory(player, 54, EDIT_TITLE_PREFIX + recipe.getName());

        ItemStack border = createBorder(Material.GRAY_STAINED_GLASS_PANE);

        // เติม Border ทั้ง GUI
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }

        // ------------------------------
        // Section Labels (UI ป้ายบอกผู้ใช้)
        // ------------------------------
        gui.setItem(1, createItem(Material.PAPER, ChatColor.YELLOW + "§lIngredients", List.of("Place materials here")));
        gui.setItem(23, createItem(Material.PAPER, ChatColor.YELLOW + "§lOutput Item", List.of("Result of crafting")));
        gui.setItem(48, createItem(Material.PAPER, ChatColor.YELLOW + "§lControls", List.of("Save / Rename / Delete")));

        // ------------------------------
        // วางช่อง Input
        // ------------------------------
        for (int i = 0; i < EDIT_INPUT_SLOTS.length; i++) {
            ItemStack ing = recipe.getIngredients().size() > i ? recipe.getIngredients().get(i) : null;
            gui.setItem(EDIT_INPUT_SLOTS[i], ing);
        }

        // ------------------------------
        // ช่อง Output
        // ------------------------------
        gui.setItem(EDIT_OUTPUT_SLOT, recipe.getResult());

        // ------------------------------
        // Buttons
        // ------------------------------
        gui.setItem(BUTTON_SAVE, createItem(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "§lSAVE RECIPE",
                List.of(ChatColor.GRAY + "บันทึกสูตรนี้")
        ));

        gui.setItem(BUTTON_RENAME, createItem(
                Material.NAME_TAG,
                ChatColor.YELLOW + "§lRENAME",
                List.of(ChatColor.GRAY + "เปลี่ยนชื่อสูตร")
        ));

        if (!isNew) {
            gui.setItem(BUTTON_DELETE, createItem(
                    Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "§lDELETE RECIPE",
                    List.of(ChatColor.GRAY + "ลบสูตรนี้อย่างถาวร")
            ));
        }

        player.openInventory(gui);
    }

    // ================================
    // Helper – Create Item
    // ================================
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        if (lore != null) m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    // Border Item
    private static ItemStack createBorder(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(" ");
        i.setItemMeta(m);
        return i;
    }

    // ================================
    // ใช้ตอนสร้างสูตรใหม่
    // ================================
    public static List<ItemStack> createEmptyIngredientList() {
        ItemStack[] empty = new ItemStack[12]; // ทุกช่องเป็น null
        return Arrays.asList(empty);
    }
}
