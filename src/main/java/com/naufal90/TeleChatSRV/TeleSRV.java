package com.naufal90.TeleChatSRV;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class TeleChatSRV extends JavaPlugin implements Listener {

    private String bot1Token;
    private String bot1ChatId;
    private String bot2Token;
    private String bot2ChatId;
    private String serverIP;
    private int serverPort;
    private Map<String, Boolean> blockNotifyFilter = new HashMap<>(); // Filter block mining

    @Override
    public void onEnable() {
        createPluginFolderAndConfig();
        loadServerConfig();

        bot1Token = getConfig().getString("bot1.token", "");
        bot1ChatId = getConfig().getString("bot1.chat_id", "");
        bot2Token = getConfig().getString("bot2.token", "");
        bot2ChatId = getConfig().getString("bot2.chat_id", "");

        loadBlockFilterConfig(); // Memuat filter block dari config.yml

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin berhasil diaktifkan!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin dimatikan.");
    }

    // Memuat filter block dari config.yml
    private void loadBlockFilterConfig() {
        blockNotifyFilter.clear();
        ConfigurationSection filterSection = getConfig().getConfigurationSection("block_notify_filter");
        if (filterSection != null) {
            for (String blockType : filterSection.getKeys(false)) {
                blockNotifyFilter.put(blockType.toUpperCase(), filterSection.getBoolean(blockType));
            }
        }

        // Default filter jika config kosong
        if (blockNotifyFilter.isEmpty()) {
            blockNotifyFilter.put("DIAMOND_ORE", true);
            blockNotifyFilter.put("EMERALD_ORE", true);
            blockNotifyFilter.put("ANCIENT_DEBRIS", true);
            getLogger().info("Default block filter digunakan!");
        }
    }

    // Event handler untuk block break (dengan filter)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        String blockType = event.getBlock().getType().toString();
        Boolean shouldNotify = blockNotifyFilter.get(blockType);

        if (shouldNotify == null || !shouldNotify) {
            return; // Skip jika block tidak ada di filter atau false
        }

        Player player = event.getPlayer();
        String coordinates = String.format("X: %d, Y: %d, Z: %d",
                event.getBlock().getLocation().getBlockX(),
                event.getBlock().getLocation().getBlockY(),
                event.getBlock().getLocation().getBlockZ());

        sendToTelegram(bot1Token, bot1ChatId, 
            String.format("*[Mining]*\n*%s* menambang: %s\nKoordinat: %s", 
            player.getName(), blockType, coordinates));
    }

    // [FITUR LAMA TETAP ADA] //
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Chat]*\n*%s*: %s", player, message));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Join]*\n*%s* telah bergabung ke server!", player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Leave]*\n*%s* telah keluar dari server.", player));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String reason = event.getDeathMessage();
        String coordinates = String.format("X: %d, Y: %d, Z: %d",
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        sendToTelegram(bot1Token, bot1ChatId, 
            String.format("*[Death]*\n*%s* mati karena: %s\nKoordinat: %s", 
            player.getName(), reason, coordinates));
    }

    private void sendToTelegram(String botToken, String chatId, String message) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            getLogger().severe("Token/ID Telegram belum diatur!");
            return;
        }

        try {
            String apiUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            String payload = String.format("chat_id=%s&text=%s&parse_mode=Markdown",
                    URLEncoder.encode(chatId, "UTF-8"),
                    URLEncoder.encode(message, "UTF-8"));

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                getLogger().severe("Gagal mengirim pesan! Kode: " + responseCode);
            }
        } catch (Exception e) {
            getLogger().severe("Error: " + e.getMessage());
        }
    }

    private void createPluginFolderAndConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
    }

    private void loadServerConfig() {
        serverIP = getConfig().getString("server.ip", "default_ip");
        serverPort = getConfig().getInt("server.port", 19132);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("settg")) {
            if (args.length == 4) {
                bot1Token = args[0];
                bot1ChatId = args[1];
                bot2Token = args[2];
                bot2ChatId = args[3];

                getConfig().set("bot1.token", bot1Token);
                getConfig().set("bot1.chat_id", bot1ChatId);
                getConfig().set("bot2.token", bot2Token);
                getConfig().set("bot2.chat_id", bot2ChatId);
                saveConfig();

                sender.sendMessage("Token dan Chat ID berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("Penggunaan: /settg <bot1Token> <bot1ChatId> <bot2Token> <bot2ChatId>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("setserver")) {
            if (args.length == 2) {
                serverIP = args[0];
                serverPort = Integer.parseInt(args[1]);

                getConfig().set("server.ip", serverIP);
                getConfig().set("server.port", serverPort);
                saveConfig();

                sender.sendMessage("IP dan Port server berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("Penggunaan: /setserver <IP> <Port>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("reloadtg")) {
            reloadConfig();
            bot1Token = getConfig().getString("bot1.token", "");
            bot1ChatId = getConfig().getString("bot1.chat_id", "");
            bot2Token = getConfig().getString("bot2.token", "");
            bot2ChatId = getConfig().getString("bot2.chat_id", "");
            loadBlockFilterConfig(); // Reload filter block
            sender.sendMessage("Konfigurasi Telegram dan filter block di-reload!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("listblocks")) {
            StringBuilder message = new StringBuilder("Daftar Block yang Di-notifikasi:\n");
            for (Map.Entry<String, Boolean> entry : blockNotifyFilter.entrySet()) {
                message.append("- ").append(entry.getKey())
                      .append(": ").append(entry.getValue() ? "AKTIF" : "NON-AKTIF")
                      .append("\n");
            }
            sender.sendMessage(message.toString());
            return true;
        }

        return false;
    }
}
