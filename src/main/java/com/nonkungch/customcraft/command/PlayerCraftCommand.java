package com.nonkungch.customcraft.command;

import com.nonkungch.customcraft.CustomCraft; 
import com.nonkungch.customcraft.gui.PlayerCraftingGUI;
import com.nonkungch.customcraft.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCraftCommand implements CommandExecutor {
    private final CustomCraft plugin; 

    public PlayerCraftCommand(CustomCraft plugin) { 
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        
        if (plugin.getRecipeManager().getRecipes().isEmpty()) {
            ChatUtil.sendMessage(player, "&cยังไม่มีสูตรคราฟต์ที่กำหนดเองในระบบ.");
            return true;
        }
        
        PlayerCraftingGUI.openRecipeSelectionGUI(player, plugin.getRecipeManager().getRecipes()); 
        return true;
    }
}
