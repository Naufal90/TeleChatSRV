<p align="center">
  <img src="https://link-ke-logo-anda.com/logo.png" alt="TeleChatSRV Logo" width="200"/>
</p>

<h1 align="center">TeleChatSRV</h1>

<p align="center">
  Plugin integrasi <strong>Minecraft + Telegram</strong> untuk monitoring real-time aktivitas server.<br/>
  Awasi pemain, deteksi kecurangan, dan dapatkan notifikasi langsung ke Telegram Anda.
</p>

<p align="center">
  <a href="#fitur-utama">Fitur</a> •
  <a href="#contoh-notifikasi">Notifikasi</a> •
  <a href="#konfigurasi">Cara Konfigurasi</a> •
  <a href="#contoh-config">Contoh Config</a>
</p>

---

## Apa Itu TeleChatSRV?

**TeleChatSRV** adalah plugin untuk Spigot/Bukkit/Paper yang menghubungkan server Minecraft Anda dengan **Bot Telegram** secara langsung. Cocok untuk para admin yang ingin:

- Memantau server **dari mana saja**.
- Mendeteksi pemain yang mencurigakan (seperti penggunaan Xray).
- Mendapatkan notifikasi real-time langsung ke Telegram.

---

## Fitur Utama

- **Notifikasi Join/Quit**
  - Pemberitahuan saat pemain masuk dan keluar dari server.

- **Notifikasi Kematian & Pembunuhan**
  - Kirim pesan ketika ada pemain yang membunuh atau terbunuh.

- **Deteksi Xray Otomatis**
  - Notifikasi saat pemain menghancurkan block berharga.
  - Block yang terdeteksi dapat dikustomisasi di `config.yml`.

- **Integrasi Telegram yang Mudah**
  - Cukup masukkan token bot dan chat ID Anda.

---

## Contoh Notifikasi

- `[JOIN] Naufal90 baru saja masuk ke server.`
- `[KILL] Naufal90 membunuh Steve.`
- `[XRAY?] Naufal90 menghancurkan DIAMOND_ORE di X:120 Y:11 Z:-45.`

---

## Konfigurasi

1. Buat Bot Telegram dengan [@BotFather](https://t.me/BotFather).
2. Salin token dan isi di file `config.yml`.
3. Dapatkan `chat_id` Telegram Anda (gunakan bot seperti [@userinfobot](https://t.me/userinfobot)).
4. Jalankan server Anda, dan nikmati notifikasi otomatis ke Telegram!

---

## Contoh Config (`config.yml`)

```yaml
bot_token: "ISI_TOKEN_BOT_ANDA"
chat_id: "ISI_CHAT_ID_ANDA"

block_notify_filter:
  DIAMOND_ORE: true
  GOLD_ORE: true
  IRON_ORE: false
