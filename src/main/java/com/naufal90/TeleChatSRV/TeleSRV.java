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

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TeleSRV extends JavaPlugin implements Listener {

    private String bot1Token; // Token bot Telegram 1
    private String bot1ChatId; // ID grup atau chat Telegram bot 1
    private String bot2Token; // Token bot Telegram 2
    private String bot2ChatId; // ID grup atau chat Telegram bot 2
    private String serverIP;
    private int serverPort;

    @Override
    public void onEnable() {
        createPluginFolderAndConfig();

        // Memuat konfigurasi
        loadServerConfig();

        // Memuat konfigurasi untuk kedua bot
        bot1Token = getConfig().getString("bot1.token", "");
        bot1ChatId = getConfig().getString("bot1.chat_id", "");
        bot2Token = getConfig().getString("bot2.token", "");
        bot2ChatId = getConfig().getString("bot2.chat_id", "");

        Bukkit.getPluginManager().registerEvents(this, this);  // Daftarkan listener
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin dimatikan.");
    }
    
    // Event handler untuk chat player
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Chat]*\n*%s*: %s", player, message));
    }

    // Event handler ketika player bergabung
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Join]*\n*%s* telah bergabung ke server!", player));
    }

    // Event handler ketika player keluar
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Leave]*\n*%s* telah keluar dari server.", player));
    }

    // Event handler ketika player mati
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String reason = event.getDeathMessage();
        String coordinates = String.format("X: %d, Y: %d, Z: %d",
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Death]*\n*%s* mati karena: %s\nKoordinat: %s", player.getName(), reason, coordinates));
    }

    // Event handler untuk block break (mining)
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().toString();
        String coordinates = String.format("X: %d, Y: %d, Z: %d",
                event.getBlock().getLocation().getBlockX(),
                event.getBlock().getLocation().getBlockY(),
                event.getBlock().getLocation().getBlockZ());
        sendToTelegram(bot1Token, bot1ChatId, String.format("*[Mining]*\n*%s* menambang: %s\nKoordinat: %s", player.getName(), blockType, coordinates));
    }

    // Fungsi untuk mengirim pesan ke Telegram
    private void sendToTelegram(String botToken, String chatId, String message) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            getLogger().severe("Token atau ID grup Telegram belum dikonfigurasi!");
            return;
        }

        try {
            String apiUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            String payload = String.format("chat_id=%s&text=%s&parse_mode=Markdown",
                    URLEncoder.encode(chatId, "UTF-8"),
                    URLEncoder.encode(message, "UTF-8"));

            // Kirim permintaan POST ke API Telegram
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            // Kirim payload
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            // Cek respons
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                getLogger().info("Pesan terkirim ke Telegram: " + message);
            } else {
                getLogger().severe("Gagal mengirim pesan ke Telegram. Kode respons: " + responseCode);
            }
        } catch (Exception e) {
            getLogger().severe("Error saat mengirim pesan ke Telegram: " + e.getMessage());
        }
    }

    // Membuat folder plugin dan konfigurasi jika belum ada
    private void createPluginFolderAndConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
    }

    // Membaca IP dan Port dari config.yml
    private void loadServerConfig() {
        serverIP = getConfig().getString("server.ip", "default_ip");
        serverPort = getConfig().getInt("server.port", 19132);
    }

    // Perintah untuk mengatur token dan chat ID untuk bot1 dan bot2
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

                sender.sendMessage("Token dan ID grup Telegram untuk kedua bot berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("Penggunaan: /settg <bot1_token> <bot1_chat_id> <bot2_token> <bot2_chat_id>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("setserver")) {
            if (args.length == 2) {
                String newIP = args[0];
                int newPort = Integer.parseInt(args[1]);

                // Mengupdate config.yml
                getConfig().set("server.ip", newIP);
                getConfig().set("server.port", newPort);
                saveConfig();

                // Memperbarui variabel di plugin
                serverIP = newIP;
                serverPort = newPort;

                sender.sendMessage("Server IP dan Port berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("Penggunaan: /setserver <ip> <port>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("reloadtg")) {
            reloadConfig();
            bot1Token = getConfig().getString("bot1.token", "");
            bot1ChatId = getConfig().getString("bot1.chat_id", "");
            bot2Token = getConfig().getString("bot2.token", "");
            bot2ChatId = getConfig().getString("bot2.chat_id", "");
            sender.sendMessage("Konfigurasi Telegram telah di-reload!");
            return true;
        }

        return false;
    }
    }
