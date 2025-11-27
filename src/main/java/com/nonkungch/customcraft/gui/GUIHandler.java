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

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUIHandler (ปรับปรุง)
 * - ป้องกัน NullPointer ในกรณีสูตรชั่วคราว (Temp)
 * - เก็บ admin editing session ระหว่างที่ยังไม่บันทึกสูตร
 * - หา recipe โดย identifier ก่อน หาโดย name
 * - ตรวจสอบและลดไอเท็มอย่างเข้มงวด (ห้ามใส่ไอเท็มอื่นในช่องที่ต้องเป็น null)
 * - ป้องกัน shift-click ที่ทำให้ bypass การตรวจ
 */
public class GUIHandler implements Listener {
    private final CustomCraft plugin;
    private final Map<UUID, BukkitTask> craftingTasks = new HashMap<>();
    private final Map<UUID, String> renamingRecipes = new HashMap<>(); // playerUUID -> recipeIdentifier
    private final Map<UUID, CustomRecipe> adminEditingSessions = new HashMap<>(); // เก็บสูตรที่กำลังแก้ไข (ยังไม่บันทึก)

    public GUIHandler(CustomCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
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
                } else {
                    ChatUtil.sendMessage(player, "&cไม่พบสูตร (ID: " + id + ")");
                }
            }
        }

        // 2. GUI รายละเอียดการคราฟต์ (Recipe Detail GUI - ผู้เล่น)
        if (title.startsWith(PlayerCraftingGUI.DETAIL_TITLE_PREFIX)) {
            // อนุญาตให้คลิกได้เฉพาะในช่อง Input
            if (!Arrays.asList(PlayerCraftingGUI.INPUT_SLOTS).contains(event.getSlot())) {
                event.setCancelled(true);
            }

            // ห้าม shift-click เพื่อ prevent quick-move bypass
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }

            // Logic ปุ่ม Craft
            if (event.getSlot() == PlayerCraftingGUI.CRAFT_BUTTON_SLOT && currentItem != null && currentItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
                event.setCancelled(true);
                String recipeName = title.substring(PlayerCraftingGUI.DETAIL_TITLE_PREFIX.length());
                CustomRecipe recipe = getRecipeByTitleOrName(title, PlayerCraftingGUI.DETAIL_TITLE_PREFIX);
                if (recipe != null) {
                    startCrafting(player, inv, recipe);
                } else {
                    ChatUtil.sendMessage(player, "&cไม่พบสูตรที่เลือก.");
                }
            }
        }

        // 3. GUI แอดมิน (Admin Recipe List) - จัดการคลิกเพื่อสร้าง/แก้ไขสูตร
        if (title.equals(AdminRecipeGUI.ADMIN_MENU_TITLE)) {
            event.setCancelled(true);
            if (currentItem == null || currentItem.getType() == Material.AIR) return;

            // 3a. คลิกปุ่ม "Add New Recipe" (NETHER_STAR)
            if (currentItem.getType() == Material.NETHER_STAR) {
                // สร้างสูตรชั่วคราว และบันทึกเป็น session ก่อนเปิด GUI แก้ไข
                CustomRecipe newRecipe = new CustomRecipe(
                        "New Recipe (Temp)",
                        AdminRecipeGUI.createEmptyIngredientList(),
                        new ItemStack(Material.BOOK)
                );
                UUID playerId = player.getUniqueId();
                adminEditingSessions.put(playerId, newRecipe);
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
                    } else {
                        ChatUtil.sendMessage(player, "&cไม่พบสูตรที่เลือก (ID: " + id + ")");
                    }
                }
            }
        }

        // 4. GUI แอดมิน (Admin Edit Menu) - จัดการคลิกเพื่อใส่ไอเท็มและควบคุม
        if (title.startsWith(AdminRecipeGUI.EDIT_TITLE_PREFIX)) {

            // 4a. ถ้าคลิกใน Inventory ของผู้เล่น (ด้านล่าง): อนุญาตให้ทำได้ (แต่ห้าม shift-click)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true); // ห้าม shift-click มาใส่/เอา
                }
                return;
            }

            // 4b. ถ้าคลิกในช่อง Input (ส่วนผสม) หรือ Output (ผลลัพธ์) ของ GUI: อนุญาตให้ทำได้
            if (Arrays.asList(AdminRecipeGUI.EDIT_INPUT_SLOTS).contains(event.getSlot()) ||
                    event.getSlot() == AdminRecipeGUI.EDIT_OUTPUT_SLOT) {
                // ถ้าเป็น quick-move (shift) ให้ยกเลิก — ต้องการให้วางแบบปกติเท่านั้น
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            // 4c. ถ้าคลิกในช่องอื่นๆ ใน Top Inventory (ปุ่มควบคุม, กรอบ, ช่องว่าง): ยกเลิกการคลิก
            event.setCancelled(true);

            if (currentItem == null) return;

            // หา recipe จาก title: รองรับกรณีที่ title มี identifier หรือเป็นสูตรชั่วคราว
            String recipeKey = title.substring(AdminRecipeGUI.EDIT_TITLE_PREFIX.length());
            UUID playerId = player.getUniqueId();
            CustomRecipe recipe = getRecipeByTitleOrSession(recipeKey, playerId);

            // ถ้าเป็นสูตร Temp และ session ยังไม่มี ให้สร้าง session ใหม่ (ป้องกัน null)
            if (recipe == null && recipeKey.contains("Temp")) {
                // สร้างสูตรชั่วคราวใหม่ (ถ้าไม่มีใน session) และใช้มัน
                CustomRecipe sessionRecipe = adminEditingSessions.get(playerId);
                if (sessionRecipe == null) {
                    sessionRecipe = new CustomRecipe("New Recipe (Temp)", AdminRecipeGUI.createEmptyIngredientList(), new ItemStack(Material.BOOK));
                    adminEditingSessions.put(playerId, sessionRecipe);
                }
                recipe = sessionRecipe;
            }

            if (recipe == null && !recipeKey.contains("Temp")) {
                // ถ้าไม่ใช่ Temp และหาไม่เจอ ให้แจ้งและ return
                ChatUtil.sendMessage(player, "&cไม่พบสูตรที่กำลังแก้ไข.");
                return;
            }

            if (event.getSlot() == AdminRecipeGUI.BUTTON_SAVE) {
                handleAdminSave(player, inv, recipe);
                // หลัง save ให้ลบ session ถ้ามี
                adminEditingSessions.remove(playerId);
            } else if (event.getSlot() == AdminRecipeGUI.BUTTON_DELETE) {
                handleAdminDelete(player, recipe);
                adminEditingSessions.remove(playerId);
            } else if (event.getSlot() == AdminRecipeGUI.BUTTON_RENAME) {
                // เก็บ identifier ของสูตรที่กำลังแก้ไข
                String identifier = recipe.getIdentifier();
                if (identifier == null || identifier.isEmpty()) {
                    ChatUtil.sendMessage(player, "&cไม่สามารถเปลี่ยนชื่อสูตรที่ยังไม่ได้บันทึกได้!");
                    return;
                }
                ChatUtil.sendMessage(player, "&eโปรดพิมพ์ชื่อสูตรใหม่ลงในแชท.");
                renamingRecipes.put(player.getUniqueId(), identifier);
                // เก็บสูตรลง session เพื่อเรียกใช้งานหลัง rename
                adminEditingSessions.put(player.getUniqueId(), recipe);
                player.closeInventory();
            }
        }
    }

    // ⭐️ จัดการเมื่อผู้เล่นพิมพ์ในแชทเพื่อเปลี่ยนชื่อ (แก้บั๊ก UI หาย) ⭐️
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerID = event.getPlayer().getUniqueId();

        if (renamingRecipes.containsKey(playerID)) {
            event.setCancelled(true); // ป้องกันข้อความชื่อสูตรไม่ให้แสดงในแชท
            String newName = event.getMessage().trim();
            String recipeID = renamingRecipes.remove(playerID);

            // ต้องรันบน Main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                CustomRecipe recipe = plugin.getRecipeManager().getRecipe(recipeID);

                // ถ้าไม่พบใน manager ให้ตรวจ session (กรณียังไม่บันทึก)
                if (recipe == null) {
                    recipe = adminEditingSessions.get(playerID);
                }

                if (recipe == null) {
                    ChatUtil.sendMessage(player, "&cเกิดข้อผิดพลาด: ไม่พบสูตร.");
                    return;
                }

                // อัปเดตชื่อ
                recipe.setName(newName);

                // ถ้า recipe มี identifier และอยู่ใน manager: อัปเดต Bukkit recipe
                if (plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) != null) {
                    plugin.getRecipeManager().updateBukkitRecipe(recipe);
                }

                // ถ้าเป็น session (ยังไม่บันทึก) ให้เก็บไว้ใน session (ผู้ใช้ต้องกด Save เพื่อบันทึกจริง)

                plugin.getRecipeManager().saveRecipes(); // บันทึก (ถ้ามีการเปลี่ยนใน manager)
                ChatUtil.sendMessage(player, "&aเปลี่ยนชื่อสูตรเป็น '&f" + newName + "&a' เรียบร้อยแล้ว.");

                // เปิด GUI กลับไปที่หน้าแก้ไข (ถ้าเป็น session ให้ส่ง object ใน session)
                AdminRecipeGUI.openEditGUI(player, recipe, plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) == null);
            });
        }
    }

    // ... (เมท็อด startCrafting, verifyAndConsumeItems, และ onInventoryClose) ... (แก้ไขแล้ว)

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

    /**
     * ตรวจสอบความถูกต้องของไอเท็มในช่อง input ตาม recipe อย่างเข้มงวด
     * - หาก required == null แต่ given != null -> ผิด
     * - หาก required != null แต่ given == null -> ผิด
     * - หากทั้งคู่ไม่ null -> ตรวจ type และจำนวน แล้วค่อยลบ
     */
    private boolean verifyAndConsumeItems(Inventory inv, CustomRecipe recipe) {
        // สร้าง snapshot ก่อนเพื่อตรวจสอบ
        ItemStack[] currentInput = new ItemStack[PlayerCraftingGUI.INPUT_SLOTS.length];
        for (int i = 0; i < PlayerCraftingGUI.INPUT_SLOTS.length; i++) {
            currentInput[i] = inv.getItem(PlayerCraftingGUI.INPUT_SLOTS[i]);
        }

        // ใช้ recipe.matches เป็นการตรวจพื้นฐานก่อน (ถ้ามีการ implement)
        if (!recipe.matches(currentInput)) return false;

        // ตรวจและลดจำนวน (ทำเป็น atomic ต่อช่อง)
        for (int i = 0; i < PlayerCraftingGUI.INPUT_SLOTS.length; i++) {
            ItemStack required = null;
            if (i < recipe.getIngredients().size()) {
                required = recipe.getIngredients().get(i);
            }
            ItemStack given = inv.getItem(PlayerCraftingGUI.INPUT_SLOTS[i]);

            if (required == null && given == null) {
                // ช่องว่างตามคาด
                continue;
            }

            if (required == null && given != null) {
                // ผู้เล่นวางไอเท็มที่ไม่ควรมี -> ผิด
                return false;
            }

            if (required != null && given == null) {
                // ขาดไอเท็ม
                return false;
            }

            // ทั้งคู่ไม่ null -> ตรวจชนิดและจำนวน
            if (!areSameItemType(required, given)) {
                return false;
            }

            int remaining = given.getAmount() - required.getAmount();
            if (remaining < 0) {
                // จำนวนไม่พอ
                return false;
            } else if (remaining == 0) {
                inv.setItem(PlayerCraftingGUI.INPUT_SLOTS[i], null);
            } else {
                ItemStack clone = given.clone();
                clone.setAmount(remaining);
                inv.setItem(PlayerCraftingGUI.INPUT_SLOTS[i], clone);
            }
        }
        return true;
    }

    private boolean areSameItemType(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        // ถ้าต้องการเช็ค meta (เช่น displayname, enchant, nbt) ให้เพิ่มเงื่อนไขที่นี่
        return true;
    }

    private void handleAdminSave(Player player, Inventory inv, CustomRecipe recipe) {
        UUID playerId = player.getUniqueId();

        // ถ้า recipe เป็น null ให้พยายามดึงจาก session (กรณีสร้างใหม่)
        if (recipe == null) {
            recipe = adminEditingSessions.get(playerId);
        }

        List<ItemStack> ingredients = Arrays.stream(AdminRecipeGUI.EDIT_INPUT_SLOTS)
                .map(inv::getItem)
                .collect(Collectors.toList());

        ItemStack result = inv.getItem(AdminRecipeGUI.EDIT_OUTPUT_SLOT);

        if (result == null || result.getType() == Material.AIR) {
            ChatUtil.sendMessage(player, "&cต้องกำหนดไอเท็มผลลัพธ์ในการคราฟต์!");
            return;
        }

        if (recipe == null) {
            // สร้างสูตรใหม่ถ้ายังไม่มี (จะมี identifier ใหม่)
            recipe = new CustomRecipe("New Custom Recipe", ingredients, result);
        } else {
            recipe.setIngredients(ingredients);
            recipe.setResult(result);
        }

        // ถ้าเป็นสูตรใหม่ (ยังไม่มีใน manager)
        if (plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) == null) {
            // ถ้าชื่อยังเป็น Temp ให้ตั้งชื่อดีฟอลต์ (ถ้าผู้ใช้ไม่ได้ตั้ง)
            if (recipe.getName() == null || recipe.getName().contains("Temp")) {
                recipe.setName("New Custom Recipe");
            }
            plugin.getRecipeManager().addRecipe(recipe);
        } else {
            // อัปเดตสูตรเดิม
            plugin.getRecipeManager().updateBukkitRecipe(recipe);
        }

        plugin.getRecipeManager().saveRecipes();
        ChatUtil.sendMessage(player, "&aบันทึกสูตร '" + recipe.getName() + "' เรียบร้อยแล้ว.");
        player.closeInventory();
        adminEditingSessions.remove(playerId);
    }

    private void handleAdminDelete(Player player, CustomRecipe recipe) {
        if (recipe == null) {
            ChatUtil.sendMessage(player, "&cไม่พบสูตรที่จะลบ.");
            player.closeInventory();
            return;
        }

        if (plugin.getRecipeManager().getRecipe(recipe.getIdentifier()) != null) {
            plugin.getRecipeManager().removeRecipe(recipe.getIdentifier());
            plugin.getRecipeManager().saveRecipes();
            ChatUtil.sendMessage(player, "&cลบสูตร '" + recipe.getName() + "' ออกจากระบบแล้ว.");
            player.closeInventory();
            AdminRecipeGUI.openAdminMenu(player, plugin.getRecipeManager().getRecipes());
        } else {
            // ถ้าเป็น session (ยังไม่บันทึก) ให้ลบ session ทิ้ง
            adminEditingSessions.values().removeIf(r -> r == recipe);
            ChatUtil.sendMessage(player, "&cยกเลิกสูตรชั่วคราว '" + recipe.getName() + "'.");
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerID = player.getUniqueId();

        if (craftingTasks.containsKey(playerID)) {
            craftingTasks.get(playerID).cancel();
            craftingTasks.remove(playerID);
            ChatUtil.sendMessage(player, "&cการคราฟต์ถูกยกเลิก (ปิดเมนู).");
        }

        if (renamingRecipes.containsKey(playerID)) {
            renamingRecipes.remove(playerID);
            ChatUtil.sendMessage(player, "&eการตั้งชื่อสูตรถูกยกเลิก.");
        }

        // ถ้าปิดหน้าจอแก้ไขแอดมิน แต่เป็น session (ยังไม่บันทึก) ให้เก็บไว้หรือแจ้ง (ปัจจุบันลบ session เพื่อความปลอดภัย)
        if (adminEditingSessions.containsKey(playerID)) {
            adminEditingSessions.remove(playerID);
            ChatUtil.sendMessage(player, "&eการแก้ไขสูตรชั่วคราวถูกยกเลิก.");
        }
    }

    /**
     * พยายามหา recipe จาก title: รูปแบบที่รองรับ
     * - EDIT_TITLE_PREFIX + <identifier> - <name>
     * - EDIT_TITLE_PREFIX + <name> (fallback หาโดย name)
     */
    private CustomRecipe getRecipeByTitleOrSession(String titleSuffix, UUID playerId) {
        // หาก titleSuffix มีรูปแบบ "identifier - name"
        if (titleSuffix.contains(" - ")) {
            String possibleId = titleSuffix.split(" - ")[0].trim();
            CustomRecipe byId = plugin.getRecipeManager().getRecipe(possibleId);
            if (byId != null) return byId;
        }

        // ถ้า session มีของ player ให้คืน
        if (adminEditingSessions.containsKey(playerId)) {
            CustomRecipe session = adminEditingSessions.get(playerId);
            // ถ้า titleSuffix ตรงกับชื่อ session หรือ contains "Temp" ให้คืน session
            if (session != null && (titleSuffix.contains("Temp") || session.getName().equals(titleSuffix) || titleSuffix.contains(session.getIdentifier()))) {
                return session;
            }
        }

        // fallback: หาโดย name
        return plugin.getRecipeManager().getRecipes().stream()
                .filter(r -> r.getName().equals(titleSuffix) || (titleSuffix.contains(r.getName())))
                .findFirst().orElse(null);
    }

    /**
     * หา recipe สำหรับหน้าต่าง detail/player (รองรับทั้งรูปแบบ id - name หรือ name เดิม)
     */
    private CustomRecipe getRecipeByTitleOrName(String title, String prefix) {
        String suffix = title.substring(prefix.length());
        // ถ้ามี "id - name"
        if (suffix.contains(" - ")) {
            String id = suffix.split(" - ")[0].trim();
            CustomRecipe r = plugin.getRecipeManager().getRecipe(id);
            if (r != null) return r;
        }
        // fallback by name
        return plugin.getRecipeManager().getRecipes().stream()
                .filter(r -> r.getName().equals(suffix))
                .findFirst().orElse(null);
    }
}
