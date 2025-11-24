package com.nonkungch.customcraft.gui;

import com.nonkungch.customcraft.CustomCraft; 
import com.nonkungch.customcraft.model.CustomRecipe;
import com.nonkungch.customcraft.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent; 
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUIHandler implements Listener {
    private final CustomCraft plugin; 
    private final Map<UUID, BukkitTask> craftingTasks = new HashMap<>(); 
    private final Map<UUID, String> renamingRecipes = new HashMap<>(); // สำหรับเก็บสถานะตั้งชื่อสูตร

    public GUIHandler(CustomCraft plugin) { 
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        ItemStack currentItem = event.getCurrentItem();

        // 1. GUI เลือกสูตร (PlayerCraftingGUI)
        if (title.equals(PlayerCraftingGUI.MENU_TITLE)) {
            event.setCancelled(true);
            if (currentItem == null || currentItem.getType() == Material.AIR || !currentItem.hasItemMeta()) return;

            List<String> lore = currentItem.getItemMeta().getLore();
            if (lore == null) return;
            
            String idLine = lore.stream().filter(line -> line.contains("ID:")).findFirst().orElse(null);
            if (idLine != null) {
                String id = idLine.split("ID: ")[1].trim();
                CustomRecipe recipe = plugin.getRecipeManager().getRecipe(id);
                if (recipe != null) {
                    PlayerCraftingGUI.openRecipeDetailGUI(player, recipe);
                }
            }
        }
        
        // 2. GUI รายละเอียดการคราฟต์ (Recipe Detail GUI - ผู้เล่น)
        if (title.startsWith(PlayerCraftingGUI.DETAIL_TITLE_PREFIX)) {
            // อนุญาตให้คลิกได้เฉพาะในช่อง Input 
            if (!Arrays.asList(PlayerCraftingGUI.INPUT_SLOTS).contains(event.getSlot())) {
                event.setCancelled(true);
            }
            
            // Logic ปุ่ม Craft
            if (event.getSlot() == PlayerCraftingGUI.CRAFT_BUTTON_SLOT && currentItem != null && currentItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
                event.setCancelled(true);
                String recipeName = title.substring(PlayerCraftingGUI.DETAIL_TITLE_PREFIX.length());
                CustomRecipe recipe = plugin.getRecipeManager().getRecipes().stream()
                        .filter(r -> r.getName().equals(recipeName))
                        .findFirst().orElse(null);
                if (recipe != null) {
                    startCrafting(player, inv, recipe);
                }
            }
        }
        
        // 3. GUI แอดมิน (Admin Recipe List) - จัดการคลิกเพื่อสร้าง/แก้ไขสูตร
        if (title.equals(AdminRecipeGUI.ADMIN_MENU_TITLE)) {
            event.setCancelled(true);
            if (currentItem == null || currentItem.getType() == Material.AIR) return;
            
            // 3a. คลิกปุ่ม "Add New Recipe" (NETHER_STAR)
            if (currentItem.getType() == Material.NETHER_STAR) {
                CustomRecipe newRecipe = new CustomRecipe(
                    "New Recipe (Temp)", 
                    AdminRecipeGUI.createEmptyIngredientList(), 
                    new ItemStack(Material.BOOK)
                );
                AdminRecipeGUI.openEditGUI(player, newRecipe, true);
                return;
            }
            
            // 3b. คลิกเพื่อแก้ไขสูตรเดิม
            if (currentItem.hasItemMeta() && currentItem.getItemMeta().hasLore()) {
                List<String> lore = currentItem.getItemMeta().getLore();
                String idLine = lore.stream().filter(line -> line.contains("ID:")).findFirst().orElse(null);
                if (idLine != null) {
                    String id = idLine.split("ID: ")[1].trim();
                    CustomRecipe recipe = plugin.getRecipeManager().getRecipe(id);
                    if (recipe != null) {
                        AdminRecipeGUI.openEditGUI(player, recipe, false);
                    }
                }
            }
        }

        // 4. GUI แอดมิน (Admin Edit Menu) - จัดการคลิกเพื่อใส่ไอเท็มและควบคุม
        if (title.startsWith(AdminRecipeGUI.EDIT_TITLE_PREFIX)) {
            
            // 4a. ถ้าคลิกใน Inventory ของผู้เล่น (ด้านล่าง): อนุญาตให้ทำได้ (ย้ายไอเท็มจากตัว)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                return;
            }
            
            // 4b. ถ้าคลิกในช่อง Input (ส่วนผสม) หรือ Output (ผลลัพธ์) ของ GUI: อนุญาตให้ทำได้ (วางไอเท็ม)
            if (Arrays.asList(AdminRecipeGUI.EDIT_INPUT_SLOTS).contains(event.getSlot()) || 
                event.getSlot() == AdminRecipeGUI.EDIT_OUTPUT_SLOT) {
                return;
            }

            // 4c. ถ้าคลิกในช่องอื่นๆ ใน Top Inventory (ปุ่มควบคุม, กรอบ, ช่องว่าง): ยกเลิกการคลิก
            event.setCancelled(true);
            
            if (currentItem == null) return;

            String recipeName = title.substring(AdminRecipeGUI.EDIT_TITLE_PREFIX.length());
            CustomRecipe recipe = plugin.getRecipeManager().getRecipes().stream()
                    .filter(r -> r.getName().equals(recipeName))
                    .findFirst().orElse(null);

            // จัดการสูตรชั่วคราว
            if (recipe == null && !recipeName.contains("Temp")) return; 

            if (event.getSlot() == AdminRecipeGUI.BUTTON_SAVE) {
                handleAdminSave(player, inv, recipe);
            } else if (event.getSlot() == AdminRecipeGUI.BUTTON_DELETE) {
                handleAdminDelete(player, recipe);
            } else if (event.getSlot() == AdminRecipeGUI.BUTTON_RENAME) {
                ChatUtil.sendMessage(player, "&eโปรดพิมพ์ชื่อสูตรใหม่ลงในแชท.");
                renamingRecipes.put(player.getUniqueId(), recipe.getIdentifier()); // ⭐️ บันทึกสถานะ
                player.closeInventory();
            }
        }
    }
    
    // ⭐️⭐️ จัดการเมื่อผู้เล่นพิมพ์ในแชทเพื่อเปลี่ยนชื่อ (แก้บั๊ก UI หาย) ⭐️⭐️
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        
        if (renamingRecipes.containsKey(playerID)) {
            event.setCancelled(true); // ป้องกันข้อความชื่อสูตรไม่ให้แสดงในแชท
            String newName = event.getMessage().trim();
            String recipeID = renamingRecipes.remove(playerID); // ลบสถานะทันที
            
            // ต้องรันบน Main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                CustomRecipe recipe = plugin.getRecipeManager().getRecipe(recipeID);
                
                if (recipe == null) {
                    ChatUtil.sendMessage(player, "&cเกิดข้อผิดพลาด: ไม่พบสูตร.");
                    return;
                }
                
                // อัปเดตชื่อ
                recipe.setName(newName);
                plugin.getRecipeManager().updateBukkitRecipe(recipe);
                plugin.getRecipeManager().saveRecipes();
                
                ChatUtil.sendMessage(player, "&aเปลี่ยนชื่อสูตรเป็น '&f" + newName + "&a' เรียบร้อยแล้ว.");
                
                // เปิด GUI กลับไปที่หน้าแก้ไข
                AdminRecipeGUI.openEditGUI(player, recipe, false); 
            });
        }
    }
    
    private void startCrafting(Player player, Inventory inv, CustomRecipe recipe) {
        if (craftingTasks.containsKey(player.getUniqueId())) {
            ChatUtil.sendMessage(player, "&cคุณกำลังคราฟต์อยู่ โปรดรอ!");
            return;
        }

        ItemStack[] inputItems = new ItemStack[PlayerCraftingGUI.INPUT_SLOTS.length];
        for (int i = 0; i < PlayerCraftingGUI.INPUT_SLOTS.length; i++) {
            inputItems[i] = inv.getItem(PlayerCraftingGUI.INPUT_SLOTS[i]);
        }
        
        if (!recipe.matches(inputItems)) {
            ChatUtil.sendMessage(player, "&cส่วนผสมไม่ถูกต้อง! โปรดตรวจสอบจำนวนและชนิดไอเท็ม.");
            return;
        }

        ChatUtil.sendMessage(player, "&aเริ่มคราฟต์ " + recipe.getName() + "... &7(รอ 5 วินาที)");
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            
            if (verifyAndConsumeItems(inv, recipe)) {
                player.getInventory().addItem(recipe.getResult().clone());
                ChatUtil.sendMessage(player, "&aคราฟต์ " + recipe.getName() + " &aสำเร็จแล้ว!");
            } else {
                ChatUtil.sendMessage(player, "&cการคราฟต์ถูกยกเลิก! ส่วนผสมมีการเปลี่ยนแปลงหรือถูกนำออก.");
            }
            
            craftingTasks.remove(player.getUniqueId());
        }, 5 * 20L);

        craftingTasks.put(player.getUniqueId(), task);
    }
    
    private boolean verifyAndConsumeItems(Inventory inv, CustomRecipe recipe) {
        ItemStack[] currentInput = new ItemStack[PlayerCraftingGUI.INPUT_SLOTS.length];
        for (int i = 0; i < PlayerCraftingGUI.INPUT_SLOTS.length; i++) {
            currentInput[i] = inv.getItem(PlayerCraftingGUI.INPUT_SLOTS[i]);
        }
        
        if (!recipe.matches(currentInput)) return false;

        for (int i = 0; i < PlayerCraftingGUI.INPUT_SLOTS.length; i++) {
            ItemStack required = recipe.getIngredients().get(i);
            ItemStack given = inv.getItem(PlayerCraftingGUI.INPUT_SLOTS[i]);
            
            if (required != null && given != null) {
                given.setAmount(given.getAmount() - required.getAmount());
                inv.setItem(PlayerCraftingGUI.INPUT_SLOTS[i], given.getAmount() <= 0 ? null : given);
            }
        }
        return true;
    }

    // ⭐️⭐️ แก้ไข: จัดการการบันทึกสูตรใหม่ให้ถูกต้อง (แก้บั๊กเซฟไม่ทำงาน) ⭐️⭐️
    private void handleAdminSave(Player player, Inventory inv, CustomRecipe recipe) {
        List<ItemStack> ingredients = Arrays.stream(AdminRecipeGUI.EDIT_INPUT_SLOTS)
                                    .map(inv::getItem)
                                    .collect(Collectors.toList());
        
        ItemStack result = inv.getItem(AdminRecipeGUI.EDIT_OUTPUT_SLOT);
        
        if (result == null || result.getType() == Material.AIR) {
            ChatUtil.sendMessage(player, "&cต้องกำหนดไอเท็มผลลัพธ์ในการคราฟต์!");
            return;
        }

        recipe.setIngredients(ingredients); 
        recipe.setResult(result);
        
        if (plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) == null) {
            // ถ้าเป็นสูตรใหม่ (ที่ถูกสร้างชั่วคราว)
            if (recipe.getName().contains("Temp")) {
                 recipe.setName("New Custom Recipe");
            }
            plugin.getRecipeManager().addRecipe(recipe); // บันทึกสูตรใหม่เข้า Map และลงทะเบียน
        } else {
            // อัปเดตสูตรเดิม
            plugin.getRecipeManager().updateBukkitRecipe(recipe); 
        }
        
        ChatUtil.sendMessage(player, "&aบันทึกสูตร '" + recipe.getName() + "' เรียบร้อยแล้ว.");
        plugin.getRecipeManager().saveRecipes(); // บันทึกข้อมูลลงไฟล์
        player.closeInventory();
    }

    private void handleAdminDelete(Player player, CustomRecipe recipe) {
        if (plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) != null) {
            plugin.getRecipeManager().removeRecipe(recipe.getIdentifier());
            plugin.getRecipeManager().saveRecipes();
            ChatUtil.sendMessage(player, "&cลบสูตร '" + recipe.getName() + "' ออกจากระบบแล้ว.");
            player.closeInventory();
            AdminRecipeGUI.openAdminMenu(player, plugin.getRecipeManager().getRecipes()); 
        } else {
            ChatUtil.sendMessage(player, "&cไม่พบสูตรที่จะลบ.");
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        
        // ยกเลิกการคราฟต์ที่กำลังดำเนินอยู่
        if (craftingTasks.containsKey(playerID)) {
            craftingTasks.get(playerID).cancel();
            craftingTasks.remove(playerID);
            ChatUtil.sendMessage((Player) event.getPlayer(), "&cการคราฟต์ถูกยกเลิก (ปิดเมนู).");
        }
        
        // ลบสถานะการตั้งชื่อ หากผู้เล่นปิดเมนูโดยไม่ตั้งชื่อ
        if (renamingRecipes.containsKey(playerID)) {
             renamingRecipes.remove(playerID);
             ChatUtil.sendMessage((Player) event.getPlayer(), "&eการตั้งชื่อสูตรถูกยกเลิก.");
        }
    }
}
