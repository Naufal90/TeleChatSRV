# TeleChatSRV

**TeleChatSRV** adalah plugin Spigot/Bukkit yang menghubungkan server Minecraft Anda dengan **Bot Telegram**, untuk memantau aktivitas pemain secara real-time langsung dari ponsel Anda.

Plugin ini cocok untuk admin server yang ingin **mengawasi pemain secara jarak jauh** dan **mendeteksi aktivitas mencurigakan**, seperti penggunaan xray.

---

## Fitur Utama

- **Notifikasi Pemain Masuk/Keluar**
  - Mendapatkan pemberitahuan setiap kali pemain bergabung atau keluar dari server.

- **Notifikasi Pembunuhan**
  - Kirim notifikasi ketika pemain membunuh atau terbunuh oleh pemain lain.

- **Anti-Xray Deteksi**
  - Mendeteksi pemain yang mencurigakan saat menghancurkan block berharga (ore).
  - Daftar block yang akan memicu notifikasi dapat diatur di `config.yml`.

- **Integrasi Telegram**
  - Hanya perlu memasukkan token Bot Telegram Anda untuk mulai menerima notifikasi.

---

## Contoh Notifikasi di Telegram

- `[JOIN] Naufal90 baru saja masuk ke server.`
- `[KILL] Naufal90 membunuh Steve.`
- `[XRAY?] Naufal90 menghancurkan DIAMOND_ORE pada koordinat X:120 Y:11 Z:-45.`

---

## Cara Konfigurasi

1. Buat Bot Telegram menggunakan [@BotFather](https://t.me/BotFather).
2. Dapatkan **token** dan masukkan ke file `config.yml`.
3. Atur block mana yang perlu dikirim notifikasinya di bagian `block_notify_filter`.
4. Jalankan server dan nikmati notifikasi langsung ke Telegram!

---

## Contoh Config (config.yml)

```yaml
bot_token: "ISI_TOKEN_BOT_ANDA"
chat_id: "ISI_CHAT_ID_ANDA"

block_notify_filter:
  DIAMOND_ORE: true
  GOLD_ORE: true
  IRON_ORE: false
