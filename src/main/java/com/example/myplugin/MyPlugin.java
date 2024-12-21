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

        botHost = getConfig().getString("bot.host", "localhost");
        botPort = getConfig().getInt("bot.port", 8080);

        connectToBot();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (botWebSocket != null) {
            botWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin dimatikan");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        sendToBot("chat", player, message, null, null, null);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("join", player, null, null, null, null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("leave", player, null, null, null, null);
    }

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

    private void connectToBot() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            botWebSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://" + botHost + ":" + botPort), new WebSocket.Listener() {
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

    private void createPluginFolderAndConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
    }

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
