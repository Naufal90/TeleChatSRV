package com.example.WAChatSRV;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyPlugin extends JavaPlugin implements Listener {

    private String botEndpoint; // Untuk menyimpan endpoint dari config.yml

    @Override
    public void onEnable() {
        // Membuat folder dan file config.yml jika belum ada
        createPluginFolderAndConfig();

        // Membaca konfigurasi
        botEndpoint = getConfig().getString("bot.endpoint", "http://botmcwasrv.duckdns.org:8080/minecraft"); // Membaca endpoint dari config.yml
        getLogger().info("MyPlugin aktif! Endpoint bot: " + botEndpoint);
        
        // Daftarkan event listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin dimatikan!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        sendToBot("[Chat] " + player + ": " + message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("Pemain " + player + " bergabung ke server!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("Pemain " + player + " keluar dari server.");
    }

    private void sendToBot(String message) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(botEndpoint);  // Menggunakan endpoint dari config.yml
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonPayload = "{\"message\":\"" + message + "\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    getLogger().info("Pesan berhasil dikirim ke bot!");
                } else {
                    getLogger().warning("Gagal mengirim pesan ke bot. Kode respons: " + responseCode);
                }
            } catch (Exception e) {
                getLogger().severe("Error mengirim pesan ke bot: " + e.getMessage());
            }
        });
    }

    // Fungsi untuk membuat folder plugin dan file config.yml jika belum ada
    private void createPluginFolderAndConfig() {
        File pluginFolder = new File(getDataFolder(), "config.yml");
        if (!pluginFolder.exists()) {
            // Jika folder belum ada, buat direktori plugin
            getDataFolder().mkdirs();
            
            // Salin file default config.yml dari plugin JAR ke folder plugin
            saveDefaultConfig();
        }
    }

    // Menangani perintah untuk mengganti endpoint melalui perintah in-game
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setwebsocket")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("wasrv.setwebsocket")) {
                    if (args.length == 1) {
                        String newEndpoint = args[0];
                        // Mengubah endpoint dan menyimpan ke config
                        botEndpoint = newEndpoint;
                        getConfig().set("bot.endpoint", newEndpoint);
                        saveConfig();
                        player.sendMessage("Endpoint WebSocket berhasil diperbarui menjadi: " + newEndpoint);
                        getLogger().info("Endpoint WebSocket diperbarui menjadi: " + newEndpoint);
                    } else {
                        player.sendMessage("Penggunaan yang benar: /setwebsocket <endpoint>");
                    }
                } else {
                    player.sendMessage("Anda tidak memiliki izin untuk mengubah endpoint.");
                }
            } else {
                getLogger().warning("Perintah hanya bisa dijalankan oleh pemain.");
            }
            return true;
        }
        return false;
    }
}
