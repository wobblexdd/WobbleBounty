# ⚔️ WobbleBounty

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.21-F6F6F6?style=for-the-badge)
![Vault](https://img.shields.io/badge/Vault-Economy-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-Embedded-003B57?style=for-the-badge&logo=sqlite)

A modern, production-ready **bounty system plugin** for Minecraft servers running **Paper 1.21+**.

WobbleBounty provides a complete PvP bounty system with a clean GUI, efficient SQLite storage, Vault economy support, and a polished user experience.

---

## ✨ Features

### 🎯 Core Bounty System
- Place bounties on players
- Stackable rewards (multiple contributors per target)
- Automatic payout on kill
- Optional removal of bounty after kill
- Configurable minimum bounty amount

---

### 💰 Economy Integration (Vault)
- Full support for **Vault**
- Compatible with EssentialsX, CMI, and other economy plugins
- Configurable tax system
- Balance validation before placing bounty

---

### 🖥️ GUI System
- Clean, modern GUI (54 slots)
- Pagination support
- Sorting system:
  - Highest bounty first
  - Lowest bounty first
- Player search (chat-based input)
- Reset filter button
- Smooth navigation with sound feedback

---

### 🎨 UI / UX
- Small caps styled interface
- Minimalistic design
- Hover hints and clean lore
- Compact number formatting:
  - `1,000 → 1K`
  - `1,000,000 → 1M`
  - `1,000,000,000 → 1B`

---

### 🔎 Search System
- Click search button in GUI
- Type player name in chat
- Supports partial matching
- Instant filtered results
- Shift-click or reset button to clear

---

### 📊 Sorting System
- Toggle sorting with one click
- Visual feedback in GUI
- Works together with pagination and search

---

### 🔊 Sound System
- Configurable sounds:
  - Click
  - Success
  - Error
- Toggle enabled/disabled
- Fully customizable

---

### 💾 Database (SQLite)
- Embedded SQLite (no setup required)
- Automatic table creation
- Stores:
  - Active bounties
  - Contributions
- Fast and lightweight

---

## ⚙️ Commands

| Command | Description |
|--------|-------------|
| `/bounty` | Open bounty GUI |
| `/bounty place <player> <amount>` | Place a bounty |
| `/bounty check <player>` | Check bounty |
| `/bounty top` | Show top bounties |
| `/bounty remove <player>` | Remove bounty (admin) |
| `/bounty reload` | Reload config |

---

## 🔐 Permissions

| Permission | Description |
|-----------|------------|
| `wobble.bounty.admin` | Full admin access |
| `wobble.bounty.reload` | Reload plugin |

---

## 📦 Installation

1. Download the latest `.jar`
2. Place it into `/plugins`
3. Install dependencies:
   - Vault
   - Economy plugin (EssentialsX / CMI / similar)
4. Restart server

---

## 📁 Configuration (config.yml)

```yml
bounty:
  min-amount: 100.0
  tax-percent: 5.0
  allow-self-bounty: false
  remove-bounty-on-kill: true
```

---

## 🔊 Sounds (config.yml)

```yml
sounds:
  enabled: true
  success: ENTITY_EXPERIENCE_ORB_PICKUP
  error: ENTITY_VILLAGER_NO
  click: UI_BUTTON_CLICK
```

---

## 🧱 Requirements

- Paper 1.21+
- Java 21
- Vault
- Economy plugin

---

## 🚀 Performance

- Lightweight SQLite backend
- Optimized GUI rendering
- Minimal memory footprint
- Designed for survival and economy servers

---

## 🧩 Architecture

The plugin follows a clean structure:

- Service Layer → `BountyService`
- Repository Layer → `BountyRepository`
- Database → `SQLiteManager`
- GUI → `BountyGUI + Pagination`
- Listeners:
  - Player kill handling
  - GUI interactions
  - Chat search system

---

## 📌 Roadmap

- MongoDB support
- Cross-server sync
- API for external plugins
- WobbleAuction integration

---

## 👨‍💻 Author

**Wobble**  
GitHub: https://github.com/wobblexdd

---

## 📜 License

MIT License