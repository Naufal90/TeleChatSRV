package com.example.WAChatSRV;

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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class MyPlugin extends JavaPlugin implements Listener {

    private String botHost;
    private int botPort;
    private WebSocket botWebSocket;

    @Override
    public void onEnable() {
        createPluginFolderAndConfig();

        // Mengambil host dan port dari file konfigurasi
        botHost = getConfig().getString("bot.host", "localhost");  // Ganti dengan host Replit jika diperlukan
        botPort = getConfig().getInt("bot.port", 8080);  // Port bot di Replit

        connectToBot();  // Menyambungkan ke bot

        Bukkit.getPluginManager().registerEvents(this, this);  // Mendaftarkan event listener
    }

    @Override
    public void onDisable() {
        if (botWebSocket != null) {
            botWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin dimatikan");
        }
    }

    // Event handler untuk chat player
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        sendToBot("chat", player, message, null, null, null);
    }

    // Event handler ketika player bergabung
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("join", player, null, null, null, null);
    }

    // Event handler ketika player keluar
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("leave", player, null, null, null, null);
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
        sendToBot("death", player.getName(), null, reason, null, coordinates);
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
        sendToBot("mining", player.getName(), null, null, blockType, coordinates);
    }

    // Fungsi untuk mengirim data ke bot
    private void sendToBot(String type, String player, String message, String reason, String block, String coordinates) {
        if (botWebSocket != null) {
            String jsonPayload = String.format(
                    "{\"type\":\"%s\", \"player\":\"%s\", \"message\":\"%s\", \"reason\":\"%s\", \"block\":\"%s\", \"coordinates\":\"%s\"}",
                    type, player, message != null ? message : "",
                    reason != null ? reason : "", block != null ? block : "",
                    coordinates != null ? coordinates : "");

            botWebSocket.sendText(jsonPayload, true);
        } else {
            getLogger().severe("WebSocket tidak terhubung ke bot!");
        }
    }

    // Fungsi untuk menghubungkan plugin ke bot menggunakan WebSocket
    private void connectToBot() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Pastikan botHost diambil dari konfigurasi atau URL yang benar dari Replit
            String botURL = "wss://" + botHost + ":" + botPort; // Gunakan wss:// jika bot menggunakan HTTPS
            botWebSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(botURL), new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            getLogger().info("Koneksi WebSocket berhasil terhubung ke bot!");
                            webSocket.request(1);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            getLogger().severe("Error pada WebSocket: " + error.getMessage());
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            getLogger().info("Pesan diterima dari bot: " + data);
                            webSocket.request(1);
                            return null;
                        }
                    })
                    .join();
        } catch (Exception e) {
            getLogger().severe("Gagal menghubungkan WebSocket ke bot: " + e.getMessage());
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

    // Perintah untuk mengubah host dan port bot
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("settcp")) {
            if (args.length == 2) {
                try {
                    botHost = args[0];
                    botPort = Integer.parseInt(args[1]);

                    getConfig().set("bot.host", botHost);
                    getConfig().set("bot.port", botPort);
                    saveConfig();

                    connectToBot(); // Menyambungkan ulang ke bot

                    sender.sendMessage("Host dan port telah diperbarui ke: " + botHost + ":" + botPort);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Port harus berupa angka!");
                }
                return true;
            } else {
                sender.sendMessage("Penggunaan: /settcp <host> <port>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("reloadwasrv")) {
            reloadConfig();
            botHost = getConfig().getString("bot.host", "localhost");
            botPort = getConfig().getInt("bot.port", 8080);

            connectToBot(); // Menyambungkan ulang ke bot
            sender.sendMessage("Plugin telah berhasil di-reload!");
            return true;
        }

        return false;
    }
}
