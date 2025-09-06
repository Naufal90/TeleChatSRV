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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;


import java.io.File;
import java.io.OutputStream; 
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeleSRV extends JavaPlugin implements Listener {

    private String notifyBotToken; // Token bot Telegram 1
    private String notifyBotChatId; // ID grup atau chat Telegram bot 1
    private String controlBotToken; // Token bot Telegram 2
    private String controlBotChatId; // ID grup atau chat Telegram bot 2
    private String serverIP;
    private int serverPort;
    private long lastUpdatedId = 0;
    private final ExecutorService telegramExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Boolean> blockNotifyFilter = new HashMap<>();
    private final Map<String, Integer> xrayThresholdMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> playerMiningCount = new HashMap<>();

    @Override
    public void onEnable() {
        createPluginFolderAndConfig();
        loadBlockFilterConfig();
        loadXrayThresholdConfig();
        // Memuat konfigurasi
        loadServerConfig();
        // Memuat konfigurasi untuk kedua bot
        notifyBotToken = getConfig().getString("notifyBot.token", "");
        notifyBotChatId = getConfig().getString("notifyBot.chat_id", "");
        controlBotToken = getConfig().getString("controlBot.token", "");
        controlBotChatId = getConfig().getString("controlBot.chat_id", "");
        Bukkit.getPluginManager().registerEvents(this, this);  // Daftarkan listener
        startTelegramCommandListener();
    }

    @Override
    public void onDisable() {
        if (telegramExecutor != null && !telegramExecutor.isShutdown()) {
            telegramExecutor.shutdown();
    }
    getLogger().info("Plugin dimatikan.");
}
    
    private void startTelegramCommandListener() {
    new BukkitRunnable() {
        @Override
        public void run() {
            try {
                String url = String.format(
                    "https://api.telegram.org/bot%s/getUpdates?offset=%d&allowed_updates=message", 
                    controlBotToken, 
                    lastUpdatedId + 1
                );

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    String response = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))
                        .lines().collect(Collectors.joining("\n"));

                    JSONObject json = new JSONObject(response);
                    JSONArray updates = json.getJSONArray("result");

                    long newLastId = lastUpdatedId;

                    for (int i = 0; i < updates.length(); i++) {
                        JSONObject update = updates.getJSONObject(i);
                        newLastId = update.getLong("update_id");

                        if (update.has("message") && !update.isNull("message")) {
                            JSONObject message = update.getJSONObject("message");

                            if (message.has("text") && 
                                String.valueOf(message.getJSONObject("chat").getLong("id")).equals(controlBotChatId)) {

                                String text = message.getString("text").trim();
                                if (text.equals("/status")) {
                                    getLogger().info("Perintah /status diterima dari Telegram, mengeksekusi...");
                                    Bukkit.getScheduler().runTask(TeleSRV.this, () -> {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "status");
                                    });
                                }
                            }
                        }
                    }

                    lastUpdatedId = newLastId;
                }
            } catch (Exception e) {
                getLogger().warning("Error checking Telegram updates: " + e.toString());
            }
        }
    }.runTaskTimerAsynchronously(this, 0L, 100L); // setiap 5 detik
}
    
    // Event handler untuk chat player
@EventHandler
public void onPlayerChat(AsyncPlayerChatEvent event) {
    if (!getConfig().getBoolean("log.chat", true)) return;
    String player = event.getPlayer().getName();
    String message = event.getMessage();
    String raw = String.format(
        "💬 *[Chat]*\n" +
        "👤 *%s*: %s", 
        player, 
        message
     );
    sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(raw));
}

// Event handler ketika player bergabung
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    if (!getConfig().getBoolean("log.join", true)) return;
    String player = event.getPlayer().getName();
    String raw = String.format(
        "🎉 *[Join]*\n" +
        "🟢 *%s* telah bergabung ke server!", 
        player
    );
    sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(raw));
}

// Event handler ketika player keluar
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    if (!getConfig().getBoolean("log.quit", true)) return;
    String player = event.getPlayer().getName();
    String raw = String.format(
        "🚪 *[Leave]*\n" +
        "🔴 *%s* telah keluar dari server.",
        player
    );    
    sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(raw));
    playerMiningCount.remove(player);
}

// Event handler ketika player mati
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    if (!getConfig().getBoolean("log.death", true)) return;
    Player player = event.getEntity();
    String reason = event.getDeathMessage();
    String coordinates = String.format("X: %d, Y: %d, Z: %d",
        player.getLocation().getBlockX(),
        player.getLocation().getBlockY(),
        player.getLocation().getBlockZ());
    String raw = String.format(
        "☠️ *[Death]*\n" +
        "⚫ *%s* mati karena: %s\n" +
        "📍 Koordinat: `%s`",
        player.getName(), 
        reason, 
        coordinates
    );
    sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(raw));
}

// Event handler untuk block break (mining)
@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    if (!getConfig().getBoolean("log.mining", true)) return;
    Player player = event.getPlayer();
    String blockType = event.getBlock().getType().toString().toUpperCase();
    String playerName = player.getName();

    // Filter notifikasi mining
    if (blockNotifyFilter.getOrDefault(blockType, false)) {
        String coordinates = String.format("X: %d, Y: %d, Z: %d",
            event.getBlock().getLocation().getBlockX(),
            event.getBlock().getLocation().getBlockY(),
            event.getBlock().getLocation().getBlockZ());
        String raw = String.format(
            "⛏️ *[Mining]*\n" +
            "👷 *%s* menambang: `%s`\n" +
            "📍 Koordinat: `%s`", 
            playerName, 
            blockType, 
            coordinates
        );
        sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(raw));
    }

    // Deteksi Xray berdasarkan ambang batas
    int threshold = xrayThresholdMap.getOrDefault(blockType, -1);
    if (threshold > 0) {
        Map<String, Integer> blockCounts = playerMiningCount.computeIfAbsent(playerName, k -> new HashMap<>());
        int currentCount = blockCounts.getOrDefault(blockType, 0) + 1;
        blockCounts.put(blockType, currentCount);

        if (currentCount == threshold) {
            String warning = String.format(
                "⚠️ *[Deteksi Xray]*\n" +
                "🧱 *%s* telah menambang *%d blok* `%s`",
                playerName, currentCount, blockType
            );
            sendToTelegram(notifyBotToken, notifyBotChatId, escapeMarkdownV2(warning));
        }
    }
}
    
    // Fungsi untuk mengirim pesan ke Telegram
    private void sendToTelegram(String botToken, String chatId, String message) {
    if (botToken.isEmpty() || chatId.isEmpty()) {
        getLogger().warning("Token atau Chat ID tidak valid!");
        return;
    }
    if (message == null || message.trim().isEmpty()) return;

    telegramExecutor.submit(() -> {
        try {
            // escape sekali di sini saja
            String cleanedMsg = escapeMarkdownV2(message);

            String payload = String.format(
                "{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"MarkdownV2\"}",
                chatId,
                cleanedMsg
            );

            HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://api.telegram.org/bot" + botToken + "/sendMessage"
            ).openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                getLogger().warning("Telegram API error: " + conn.getResponseMessage());
            }

            conn.disconnect();
        } catch (Exception e) {
            getLogger().warning("Gagal kirim Telegram: " + e.getMessage());
        }
    });
}
    
// Method untuk escape karakter khusus MarkdownV2
private String escapeMarkdownV2(String text) {
    return text.replaceAll("([_\\[\\]()~`>#+\\-=|{}\\.!])", "\\\\$1");
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

    private void loadBlockFilterConfig() {
    blockNotifyFilter.clear();
    ConfigurationSection filterSection = getConfig().getConfigurationSection("block_notify_filter");
    if (filterSection != null) {
        for (String key : filterSection.getKeys(false)) {
            blockNotifyFilter.put(key.toUpperCase(), filterSection.getBoolean(key));
        }
    }
}

private void loadXrayThresholdConfig() {
    xrayThresholdMap.clear();
    ConfigurationSection thresholdSection = getConfig().getConfigurationSection("count_block_destroy_xray");
    if (thresholdSection != null) {
        for (String key : thresholdSection.getKeys(false)) {
            xrayThresholdMap.put(key.toUpperCase(), thresholdSection.getInt(key));
        }
    }
}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("settg")) {
            if (!sender.hasPermission("telechat.admin")) {
                sender.sendMessage("§cAnda tidak memiliki izin untuk perintah ini!");
                return true;
            }

            if (args.length == 4) {
                notifyBotToken = args[0];
                notifyBotChatId = args[1];
                controlBotToken = args[2];
                controlBotChatId = args[3];

                getConfig().set("notifyBot.token", notifyBotToken);
                getConfig().set("notifyBot.chat_id", notifyBotChatId);
                getConfig().set("controlBot.token", controlBotToken);
                getConfig().set("controlBot.chat_id", controlBotChatId);
                saveConfig();

                sender.sendMessage("§aToken dan ID grup Telegram untuk kedua bot berhasil diperbarui!");
                return true;
            } else {
                sender.sendMessage("§cPenggunaan: /settg <notifyBot_token> <notifyBot_chat_id> <controlBot_token> <controlBot_chat_id>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("setserver")) {
            if (!sender.hasPermission("telechat.admin")) {
                sender.sendMessage("§cAnda tidak memiliki izin untuk perintah ini!");
                return true;
            }

            if (args.length == 2) {
                try {
                    String newIP = args[0];
                    int newPort = Integer.parseInt(args[1]);

                    if (newPort < 1 || newPort > 65535) {
                        sender.sendMessage("§cPort harus antara 1-65535");
                        return false;
                    }

                    getConfig().set("server.ip", newIP);
                    getConfig().set("server.port", newPort);
                    saveConfig();

                    serverIP = newIP;
                    serverPort = newPort;

                    sender.sendMessage("§aServer IP dan Port berhasil diperbarui!");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cPort harus berupa angka!");
                    return false;
                }
            } else {
                sender.sendMessage("§cPenggunaan: /setserver <ip> <port>");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("reloadtg")) {
            if (!sender.hasPermission("telechat.admin")) {
                sender.sendMessage("§cAnda tidak memiliki izin untuk perintah ini!");
                return true;
            }

            reloadConfig();
            notifyBotToken = getConfig().getString("notifyBot.token", "");
            notifyBotChatId = getConfig().getString("notifyBot.chat_id", "");
            controlBotToken = getConfig().getString("controlBot.token", "");
            controlBotChatId = getConfig().getString("controlBot.chat_id", "");
            sender.sendMessage("§aKonfigurasi Telegram telah di-reload!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("status")) {
    int onlinePlayers = Bukkit.getOnlinePlayers().size();
    int maxPlayers = Bukkit.getMaxPlayers();

    // Builder & data final agar bisa dipakai dalam runnable
    StringBuilder playerList = new StringBuilder();
    long totalPing = 0;
    int counted = 0;

    for (Player p : Bukkit.getOnlinePlayers()) {
        try {
            int ping = p.getPing();
            totalPing += ping;
            counted++;
            playerList.append("- ")
                    .append(p.getName())
                    .append(" (")
                    .append(ping)
                    .append("ms)\n");
        } catch (Exception ignored) {}
    }

    // Buat data final supaya bisa dipakai di dalam run()
    final String playersText = playerList.length() > 0 ? playerList.toString() : "- Tidak ada pemain online\n";
    final long averagePing = counted > 0 ? totalPing / counted : 0;

    new BukkitRunnable() {
        @Override
        public void run() {
            String message = String.format(
                "📊 *[Status Server]*\n" +
                "🟢 *Online:* %d/%d\n" +
                "👥 *Pemain:*\n%s\n" +
                "🌐 *IP:* `%s`\n" +
                "🔌 *Port:* `%d`\n" +
                "⏱️ *Ping Rata-rata:* `%dms`",
                onlinePlayers, 
                maxPlayers, 
                playersText,  // Daftar pemain (format: "- Name (pingms)\n")
                serverIP, 
                serverPort, 
                averagePing
            );

            sendToTelegram(controlBotToken, controlBotChatId, escapeMarkdownV2(message));
        }
    }.runTaskAsynchronously(this);

    sender.sendMessage("§aStatus server telah dikirim ke Telegram.");
    return true;
}
        return false;
    }
}
