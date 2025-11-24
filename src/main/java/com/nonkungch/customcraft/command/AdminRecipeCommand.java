package nonkungch.customcraft.command;

import nonkungch.customcraft.CustomCraft;
import nonkungch.customcraft.gui.AdminRecipeGUI;
import nonkungch.customcraft.model.CustomRecipe;
import nonkungch.customcraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

public class AdminRecipeCommand implements CommandExecutor {
    private final CustomCraft plugin;

    public AdminRecipeCommand(CustomCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        
        if (!player.hasPermission("customcraft.admin")) {
            ChatUtil.sendMessage(player, "&cคุณไม่มีสิทธิ์ใช้คำสั่งนี้");
            return true;
        }
        
        if (args.length == 0) {
            AdminRecipeGUI.openAdminMenu(player, plugin.getRecipeManager().getRecipes());
            return true;
        }
        
        if (args[0].equalsIgnoreCase("new")) {
            // สร้างสูตรใหม่ว่างเปล่าพร้อมชื่อชั่วคราว
            CustomRecipe newRecipe = new CustomRecipe("New Recipe (Temp)", AdminRecipeGUI.createEmptyIngredientList(), new ItemStack(org.bukkit.Material.BOOK));
            AdminRecipeGUI.openEditGUI(player, newRecipe, true);
            return true;
        }
        
        ChatUtil.sendMessage(player, "&cการใช้งาน: /ccadmin [new]");
        return true;
    }
          }
