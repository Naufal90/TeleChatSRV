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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MyPlugin extends JavaPlugin implements Listener {

    private String botHost; // Host bot
    private int botPort;    // Port bot

    @Override
    public void onEnable() {
        // Membaca konfigurasi
        botHost = getConfig().getString("bot.host", "localhost"); // Host bot, misal 'localhost' atau IP
        botPort = getConfig().getInt("bot.port", 12345);           // Port bot, misal 12345
        getLogger().info("WAChatSRV aktif! Host bot: " + botHost + " Port bot: " + botPort);
        
        // Daftarkan event listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("WAChatSRV dimatikan!");
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
            try (Socket socket = new Socket(botHost, botPort);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                // Kirim pesan ke bot melalui TCP
                out.writeUTF(message); // Kirim pesan dalam format UTF
                out.flush(); // Pastikan data terkirim
                getLogger().info("Pesan berhasil dikirim ke bot: " + message);
            } catch (IOException e) {
                getLogger().severe("Error mengirim pesan ke bot melalui TCP: " + e.getMessage());
            }
        });
    }

    // Menangani perintah untuk mengganti host dan port bot
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("reloadwasrv")) {
            if (sender.hasPermission("wasrv.reload")) {
                reloadConfig();
                botHost = getConfig().getString("bot.host", "localhost");
                botPort = getConfig().getInt("bot.port", 12345);
                sender.sendMessage("WAChatSRV konfigurasi berhasil dimuat ulang!");
                getLogger().info("WAChatSRV konfigurasi dimuat ulang.");
                return true;
            } else {
                sender.sendMessage("Anda tidak memiliki izin untuk memuat ulang konfigurasi.");
                return false;
            }
        }

        if (cmd.getName().equalsIgnoreCase("settcp")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("wasrv.settcp")) {
                    if (args.length == 2) {
                        String newHost = args[0];
                        int newPort = Integer.parseInt(args[1]);
                        // Mengubah host dan port bot, dan menyimpan ke config
                        botHost = newHost;
                        botPort = newPort;
                        getConfig().set("bot.host", newHost);
                        getConfig().set("bot.port", newPort);
                        saveConfig();
                        player.sendMessage("Host dan port TCP berhasil diperbarui menjadi: " + newHost + ":" + newPort);
                        getLogger().info("Host dan port TCP diperbarui menjadi: " + newHost + ":" + newPort);
                    } else {
                        player.sendMessage("Penggunaan yang benar: /settcp <host> <port>");
                    }
                } else {
                    player.sendMessage("Anda tidak memiliki izin untuk mengubah host dan port.");
                }
            } else {
                getLogger().warning("Perintah hanya bisa dijalankan oleh pemain.");
            }
            return true;
        }

        return false;
    }
}
