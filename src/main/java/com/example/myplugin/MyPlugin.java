package com.example.myplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyPlugin extends JavaPlugin implements Listener {

    // URL endpoint bot WhatsApp
    private static final String BOT_ENDPOINT = "http://localhost:3000/webhook"; // Ganti sesuai kebutuhan Anda

    @Override
    public void onEnable() {
        getLogger().info("MyPlugin aktif!");
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
                URL url = new URL(BOT_ENDPOINT);
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
}