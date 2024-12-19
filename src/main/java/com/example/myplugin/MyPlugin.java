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

    private String botHost;
    private int botPort;

    @Override
    public void onEnable() {
        // Membuat folder dan file config.yml jika belum ada
        createPluginFolderAndConfig();

        // Membaca konfigurasi dari file config.yml
        botHost = getConfig().getString("bot.host", "localhost");
        botPort = getConfig().getInt("bot.port", 8080);
        
        getLogger().info("MyPlugin aktif! Host: " + botHost + ", Port: " + botPort);
        
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
        sendToBot("chat", player, message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("join", player, "Bergabung ke server");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        sendToBot("leave", player, "Keluar dari server");
    }

    private void sendToBot(String type, String player, String message) {
    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
        try {
            // Membuat koneksi ke bot melalui TCP (bukan HTTP)
            Socket socket = new Socket(botHost, botPort);
            OutputStream os = socket.getOutputStream();
            
            // Membuat payload JSON dengan type, player, dan message
            String jsonPayload = String.format("{\"type\":\"%s\", \"player\":\"%s\", \"message\":\"%s\"}", type, player, message);
            
            // Mengirimkan data ke bot
            os.write(jsonPayload.getBytes());
            os.flush();
            
            // Menutup koneksi setelah data dikirim
            socket.close();
            
            getLogger().info("Pesan berhasil dikirim ke bot!");

        } catch (Exception e) {
            getLogger().severe("Error mengirim pesan ke bot: " + e.getMessage());
        }
    });
}

    // Fungsi untuk membuat folder plugin dan file config.yml jika belum ada
    private void createPluginFolderAndConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            // Jika folder belum ada, buat direktori plugin
            pluginFolder.mkdirs();
        }

        File configFile = new File(pluginFolder, "config.yml");
        if (!configFile.exists()) {
            // Jika config.yml belum ada, salin file default config.yml dari plugin JAR ke folder plugin
            saveDefaultConfig();
        }
    }

    // Fungsi untuk reload plugin
    public void reloadPlugin() {
        // Memuat ulang konfigurasi
        reloadConfig();
        botHost = getConfig().getString("bot.host", "localhost");
        botPort = getConfig().getInt("bot.port", 8080);

        getLogger().info("Konfigurasi diperbarui! Host: " + botHost + ", Port: " + botPort);
    }

    // Menangani perintah untuk mengganti host dan port melalui perintah in-game atau console
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("settcp")) {
            if (args.length == 2) {
                String newHost = args[0];
                int newPort;
                try {
                    newPort = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Port harus berupa angka.");
                    return false;
                }

                // Perbarui host dan port di konfigurasi
                botHost = newHost;
                botPort = newPort;
                getConfig().set("bot.host", newHost);
                getConfig().set("bot.port", newPort);
                saveConfig();

                sender.sendMessage("Host dan port TCP diperbarui menjadi: " + newHost + ":" + newPort);
                return true;
            } else {
                sender.sendMessage("Penggunaan yang benar: /settcp <host> <port>");
                return false;
            }
        }

        // Perintah untuk mereload plugin
        if (cmd.getName().equalsIgnoreCase("reloadwasrv")) {
            // Pastikan yang menjalankan perintah adalah player atau console
            if (sender instanceof Player && !sender.hasPermission("plugin.reload")) {
                sender.sendMessage("Anda tidak memiliki izin untuk mereload plugin.");
                return false;
            }

            // Melakukan reload plugin
            reloadPlugin();
            sender.sendMessage("Plugin berhasil direload!");
            return true;
        }

        return false;
    }
                }
