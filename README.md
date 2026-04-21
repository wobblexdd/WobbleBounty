# ⚔️ KBountyHunters

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.21-F6F6F6?style=for-the-badge)
![Vault](https://img.shields.io/badge/Vault-Economy-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-Embedded-003B57?style=for-the-badge&logo=sqlite)

A production-ready **bounty system plugin** for Minecraft servers running **Paper 1.21+**.

KBountyHunters delivers a complete PvP bounty experience with a polished GUI, advanced filtering, admin tools, and a reliable SQLite backend.

---

## ✨ Features

### 🎯 Core System
- Place bounties on players
- Stackable rewards (multiple contributors per target)
- Automatic payout on kill
- Optional removal after kill
- Configurable minimum bounty and tax system

---

### 💰 Economy (Vault)
- Full Vault integration
- Compatible with EssentialsX, CMI, and similar plugins
- Tax handling on bounty placement
- Balance validation before transactions

---

### 🖥️ GUI System
- 54-slot modern GUI
- Pagination system
- Confirm GUI for actions
- Integrated search and filtering
- Real-time updates

---

### 🔎 Search & Filtering
- Chat-based search input
- Partial name matching
- Exact match priority
- Result counter in GUI
- Reset filter system

---

### 📊 Sorting Modes
- Highest First
- Lowest First
- Newest First
- Oldest First
- Alphabetical

---

### 🎨 UI / UX
- Small caps styled interface
- Clean layout and spacing
- Hover hints and structured lore
- Consistent visual system across KBountyHunters

---

### 🔊 Sound System
- Click / success / error sounds
- Fully configurable
- Toggle support

---

### 🛠️ Admin Features
- `/bounty set <player> <amount>`
- `/bounty remove <player>`
- `/bounty info <player>`
- `/bounty clearall`
- Admin actions available via GUI

---

### 🛡️ Inventory Safety
- Blocks:
  - Shift-click
  - Drag
  - Hotbar swap
  - Double click
- Prevents GUI abuse and item movement

---

### 💾 Database (SQLite)
- Embedded database (no setup required)
- Automatic schema creation
- Stores:
  - Active bounties
  - Contributions
  - Creation timestamps
- Fast and lightweight

---

## ⚙️ Commands

| Command | Description |
|--------|-------------|
| `/bounty` | Open bounty GUI |
| `/bounty place <player> <amount>` | Place a bounty |
| `/bounty check <player>` | Check bounty |
| `/bounty top` | Show top bounties |
| `/bounty set <player> <amount>` | Set bounty (admin) |
| `/bounty remove <player>` | Remove bounty (admin) |
| `/bounty info <player>` | View bounty details (admin) |
| `/bounty clearall` | Clear all bounties (admin) |
| `/bounty reload` | Reload configuration |

---

## 🔐 Permissions

| Permission | Description |
|-----------|------------|
| `klouse.bounty.admin` | Full admin access |
| `klouse.bounty.reload` | Reload plugin |

---

## 📦 Installation

1. Download the latest `.jar`
2. Place it into `/plugins`
3. Install dependencies:
   - Vault
   - Economy plugin (EssentialsX / CMI / similar)
4. Restart server

---

## 📁 Configuration

```yml
bounty:
  min-amount: 100.0
  tax-percent: 5.0
  allow-self-bounty: false
  remove-bounty-on-kill: true
  broadcast-on-place: true
  broadcast-on-claim: true

sounds:
  enabled: true
  success: ENTITY_EXPERIENCE_ORB_PICKUP
  error: ENTITY_VILLAGER_NO
  click: UI_BUTTON_CLICK
```
---

## 🧱 Requirements

* Paper 1.21+
* Java 21
* Vault
* Economy plugin

---

## 🚀 Performance

* Lightweight SQLite backend
* Optimized GUI updates
* Minimal memory usage
* Suitable for survival and economy servers

---

## 🧩 Architecture

* Service Layer → `BountyService`
* Repository Layer → `BountyRepository`
* Database → `SQLiteManager`
* GUI → `BountyGUI`, `BountyConfirmGUI`, `Pagination`
* Listeners:

  * PlayerKillListener
  * BountyMenuListener
  * PlayerSearchListener
  * InventorySafetyListener

---

## 📌 Roadmap

* MongoDB support
* Cross-server sync
* Public API for integrations
* Public API integration support (planned)

---

## 👨‍💻 Author

**Klouse**
GitHub: https://github.com/klousex
