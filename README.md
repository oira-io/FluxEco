# FluxEco

[![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Paper API](https://img.shields.io/badge/Paper-1.21-darkgreen?style=flat-square&logo=Papermc)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

> **A modern, optimized, and lightweight economy plugin for Minecraft servers**

FluxEco is a fully-featured economy plugin built with Kotlin, designed for Paper servers (Spigot-compatible). It provides a robust foundation for managing player economies with support for multiple databases, cross-server communication, and rich integrations with popular plugins.

## ✨ Features

- **Multi-Database Support**
  - SQLite (file-based, zero setup)
  - MySQL/MariaDB
  - MongoDB
  - H2 Database

- **Cross-Server Economy**
  - Redis-powered synchronization across multiple servers
  - Real-time balance updates with dedicated server IDs
  - 
- **Plugin Integrations**
  - **Vault** (required) - Standard economy interface
  - **PlaceholderAPI** (optional) - Placeholder support for other plugins
  - **MiniPlaceholders** (optional) - Alternative placeholder system
  - Built-in metrics with bStats

- **Highly Configurable**
  - Customizable currency names (singular/plural)
  - Flexible number formatting (abbreviated or comma-separated)
  - Command enable/disable with per-command permissions
  - Advanced thread pool configuration
  - Configurable date formatting

- **Developer-Friendly**
  - Dedicated API module for third-party developers
  - Economy and Transaction Manager interfaces
  - Custom event system
  - Well-structured command framework using Lamp

## 🚀 Getting Started

### Requirements

- Minecraft Server: Paper 1.21 (or newer Paper builds)
- Java: 21 or higher
- Vault plugin installed on your server

### Installation

1. **Download the plugin**
   - Get the latest `FluxEco-*.jar` release from the [Releases](https://github.com/oira-io/FluxEco/releases) page
   - ⚠️ Do **not** download the `FluxEco-API-*.jar` file (it's for developers only)

2. **Install the plugin**
   - Place the JAR file in your server's `plugins/` folder
   - If you haven't installed Vault yet, download and install it as well

3. **Start your server**
   ```bash
   ./start.sh  # or your server startup script
   ```
   
### Building from Source

If you want to build FluxEco yourself:

```bash
# Clone the repository
git clone https://github.com/oira-io/FluxEco.git
cd FluxEco

# Build the project
./gradlew build

# Built JARs will be in the dist/ directory
```

## 📋 Core Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/balance [player]` | `fluxeco.command.balance` | View your balance or another player's |
| `/pay <player> <amount>` | `fluxeco.command.pay` | Send money to another player |
| `/economy give <player> <amount>` | `fluxeco.command.economy.give` | Give money to a player |
| `/economy take <player> <amount>` | `fluxeco.command.economy.take` | Remove money from a player |
| `/economy set <player> <amount>` | `fluxeco.command.economy.set` | Set a player's exact balance |
| `/economy reset [player]` | `fluxeco.command.economy.reset` | Reset balance to default |
| `/baltop` | `fluxeco.command.baltop` | View the richest players |
| `/history` | `fluxeco.command.history` | View your transaction history |

For a complete list of commands and aliases, see `plugins/FluxEco/commands.yml`

## ⚙️ Configuration

### Quick Setup

**SQLite (Recommended for small servers):**
```yaml
# plugins/FluxEco/database.yml
database:
  type: sqlite
```

**MySQL:**
```yaml
# plugins/FluxEco/database.yml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: fluxeco
    username: root
    password: your-password
    poolSize: 6
```

**Cross-Server Redis:**
```yaml
# plugins/FluxEco/database.yml
redis:
  enabled: true
  server-id: "server1"  # Unique per server
  host: localhost
  port: 6379
  password: ""
  poolSize: 10
```

### Main Configuration

```yaml
# plugins/FluxEco/config.yml
general:
  starting-balance: 100
  date-format: "dd/MM/yyyy HH:mm"
  confirm-payments: false
  allow-offline-payments: true

currency:
  name: "Dollar"
  name-plural: "Dollars"

format:
  mode: abbreviated  # "commas" or "abbreviated"
  max-decimal-places: 2
  abbreviate: true
  uppercase: true
```

See the full configuration files in `plugins/FluxEco/` for all available options.

## 🔌 For Developers

### Using the FluxEco API

Add FluxEco API as a dependency in your plugin:

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.oira:FluxEco-API:beta1")
}
```

### Basic API Usage

```kotlin
import io.oira.fluxeco.api.IFluxEcoAPI

// Get the FluxEco API instance
val api = IFluxEcoAPI.getInstance()

// Get the economy manager
val economyManager = api.getEconomyManager()

// Check a player's balance
val balance = economyManager.getBalance(player)

// Modify a player's balance
economyManager.setBalance(player, 1000.0)

// Get transaction manager for advanced operations
val transactionManager = api.getTransactionManager()
```

### API Features

- **Economy Manager**: Manage player balances, give/take/set operations
- **Transaction Manager**: Create, retrieve, and manage transaction records
- **Custom Events**: Listen to economy events in your plugin
- **Data Models**: Access player profiles, transaction history, and statistics

For detailed API documentation, visit the [API module source code](api/src/main/kotlin/io/oira/fluxeco/api/)

## 🗄️ Database Support

| Database | Storage | Best For | Setup |
|----------|---------|----------|-------|
| **SQLite** | File-based (.db) | Development, small servers | Zero setup |
| **MySQL** | Client-server | Production, large networks | Requires MySQL server |
| **MongoDB** | Document-based | Flexible schemas, large scale | Requires MongoDB server |
| **H2** | Embedded | Testing, lightweight | Minimal setup |

All databases support full transaction history and player data persistence.

## 🔄 Cross-Server Features

Enable Redis to synchronize economy data across multiple Paper servers:

1. Set up a Redis server
2. Configure Redis in `database.yml`:
   ```yaml
   redis:
     enabled: true
     server-id: "server1"  # Unique per server
   ```
3. Requires MySQL or MongoDB as primary database

Benefits:
- Real-time balance synchronization
- Cross-server player lookups
- Unified transaction history
- Server-specific transaction logs

## 📊 Supported Placeholders

With PlaceholderAPI or MiniPlaceholders installed:

- `%fluxeco_balance%` - Player's current balance
- `%fluxeco_balance_formatted%` - Formatted balance
- `%fluxeco_rank%` - Player's rank in baltop
- `%fluxeco_currency%` - Currency name

## 🤝 Contributing

We welcome contributions from the community! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines on:
- Development setup
- Code style and standards
- Commit and PR processes
- Bug reporting and feature suggestions

### Development Setup

1. Clone the repository
2. Install IntelliJ IDEA or your preferred IDE with Kotlin support
3. Open the project and let Gradle sync dependencies
4. Use `./gradlew build` to build the project
5. Use `./gradlew runServer` to run a test Paper server

### Project Structure

```
FluxEco/
├── api/                 # Public API module for developers
├── core/                # Main plugin implementation
│   ├── command/         # Command implementations
│   ├── data/            # Database and data management
│   ├── gui/             # GUI implementations
│   ├── integration/     # Plugin integrations (Vault, PlaceholderAPI, etc.)
│   ├── manager/         # Core managers (config, economy, etc.)
│   └── listener/        # Event listeners
└── dist/                # Build output
```

## 📝 Configuration Files

After first run, the following files are created in `plugins/FluxEco/`:

- `config.yml` - Main plugin configuration
- `database.yml` - Database and Redis settings
- `commands.yml` - Command enable/disable and permissions
- `messages.yml` - Customizable plugin messages
- `gui/baltop-ui.yml` - Baltop GUI configuration
- `gui/history-ui.yml` - History GUI configuration
- `gui/stats-ui.yml` - Stats GUI configuration
- `gui/confirm-ui.yml` - Confirm payment GUI configuration

## 🐛 Troubleshooting

**Plugin fails to load:**
- Check server logs for detailed error messages
- Ensure Java 21+ is installed
- Verify Vault plugin is installed

**Database connection issues:**
- Check database credentials in `database.yml`
- Ensure database server is running and accessible
- Verify firewall/port settings

**Commands not working:**
- Check command permissions in `commands.yml`
- Verify player has required permission nodes
- Use `/fluxeco reload` to reload configuration

**GUI not displaying:**
- Ensure GUI is enabled in `config.yml`
- Check `gui/*.yml` configuration files

For more help, visit the [documentation](https://oira.io/docs) or join the [Discord server](https://discord.gg/oira)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Kotlin](https://kotlinlang.org/) and [Paper API](https://papermc.io/)
- Command framework powered by [Lamp](https://github.com/revxrsal/lamp)
- Database access via [Exposed](https://github.com/JetBrains/Exposed)
- Metrics by [bStats](https://bstats.org/)

## 🔗 Links

- **Website:** [oira.io](https://oira.io)
- **Documentation:** [oira.io/docs](https://oira.io/docs)
- **Discord:** [discord.gg/oira](https://discord.gg/oira)
- **GitHub:** [github.com/oira-io/FluxEco](https://github.com/oira-io/FluxEco)

---

**Questions?** Check the documentation or ask in the [Discord community](https://discord.gg/oira)!

