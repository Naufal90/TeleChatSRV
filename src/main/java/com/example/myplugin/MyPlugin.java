package com.example.TELEChatSRV;

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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MyPlugin extends JavaPlugin implements Listener {

    private String botToken; // Token bot Telegram
    private String chatId;  // ID grup atau chat Telegram

    @Override
    public void onEnable() {
        createPluginFolderAndConfig();

        // Ambil konfigurasi dari file
        botToken = getConfig().getString("bot.token", "");  // Masukkan token bot Telegram Anda
        chatId = getConfig().getString("bot.chat_id", ""); // Masukkan ID grup atau pengguna Telegram Anda

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
        sendToTelegram(String.format("*[Chat]*\n*%s*: %s", player, message));
    }

    // Event handler ketika player bergabung
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(String.format("*[Join]*\n*%s* telah bergabung ke server!", player));
    }

    // Event handler ketika player keluar
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToTelegram(String.format("*[Leave]*\n*%s* telah keluar dari server.", player));
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
        sendToTelegram(String.format("*[Death]*\n*%s* mati karena: %s\nKoordinat: %s", player.getName(), reason, coordinates));
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
        sendToTelegram(String.format("*[Mining]*\n*%s* menambang: %s\nKoordinat: %s", player.getName(), blockType, coordinates));
    }

    // Fungsi untuk mengirim pesan ke Telegram
    private void sendToTelegram(String message) {
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

    // Perintah untuk mengatur token dan chat ID
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("settg")) {
            if (args.length == 2) {
                botToken = args[0];
                chatId = args[1];

                getConfig().set("bot.token", botToken);
                getConfig().set("bot.chat_id", chatId);
                saveConfig();

                sender.sendMessage("Token dan ID grup Telegram berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("Penggunaan: /settg <token> <chat_id>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("reloadtg")) {
            reloadConfig();
            botToken = getConfig().getString("bot.token", "");
            chatId = getConfig().getString("bot.chat_id", "");
            sender.sendMessage("Konfigurasi Telegram telah di-reload!");
            return true;
        }

        return false;
    }
    }
