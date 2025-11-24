package com.nonkungch.customcraft.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ChatUtil {
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[CustomCraft] " + message));
    }
}
