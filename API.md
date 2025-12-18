# FluxEco API Guide

This guide explains how to use the FluxEco API to integrate economy functionality into your own Minecraft plugins.

## 📋 Table of Contents

- [Setup](#setup)
- [Basic Usage](#basic-usage)
- [Economy Manager](#economy-manager)
- [Transaction Manager](#transaction-manager)
- [Events](#events)
- [Data Models](#data-models)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

## 🔧 Setup

### Add FluxEco as a Dependency

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.oira:FluxEco-API:beta1")
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}
```

**Gradle (Groovy):**
```groovy
repositories {
    mavenCentral()
    maven { url "https://repo.papermc.io/repository/maven-public/" }
}

dependencies {
    compileOnly 'io.oira:FluxEco-API:beta1'
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
}
```

### Plugin Dependencies

Add FluxEco to your `plugin.yml`:
```yaml
name: YourPlugin
main: com.example.YourPlugin
version: 1.0.0
depend: [FluxEco]  # FluxEco must be loaded first
```

## 🚀 Basic Usage

### Get the API Instance

```kotlin
import io.oira.fluxeco.api.IFluxEcoAPI

// Get the API instance (throws exception if FluxEco not loaded)
val api = IFluxEcoAPI.getInstance()

// Get the version
val version = api.getVersion()
println("FluxEco version: $version")
```

### Safety Check

```kotlin
import io.oira.fluxeco.api.IFluxEcoAPI

// Check if FluxEco API is available
try {
    val api = IFluxEcoAPI.getInstance()
    // Use the API
} catch (e: IllegalStateException) {
    logger.warning("FluxEco plugin is not loaded!")
}
```

## 💰 Economy Manager

The Economy Manager handles player balance operations.

### Get the Economy Manager

```kotlin
val api = IFluxEcoAPI.getInstance()
val economyManager = api.getEconomyManager()
```

### Check Player Balance

```kotlin
import org.bukkit.entity.Player

fun checkBalance(player: Player) {
    val economyManager = IFluxEcoAPI.getInstance().getEconomyManager()
    
    // Get balance asynchronously
    economyManager.getBalance(player.uniqueId) { balance ->
        player.sendMessage("Your balance: $$balance")
    }
}
```

### Add Money to Player

```kotlin
fun giveMoney(player: Player, amount: Double) {
    val economyManager = IFluxEcoAPI.getInstance().getEconomyManager()
    
    economyManager.addBalance(player.uniqueId, amount) { success ->
        if (success) {
            player.sendMessage("You received $$amount")
        } else {
            player.sendMessage("Failed to give money")
        }
    }
}
```

### Remove Money from Player

```kotlin
fun takeMoney(player: Player, amount: Double) {
    val economyManager = IFluxEcoAPI.getInstance().getEconomyManager()
    
    economyManager.removeBalance(player.uniqueId, amount) { success ->
        if (success) {
            player.sendMessage("You lost $$amount")
        } else {
            player.sendMessage("You don't have enough money")
        }
    }
}
```

### Set Exact Balance

```kotlin
fun setBalance(player: Player, amount: Double) {
    val economyManager = IFluxEcoAPI.getInstance().getEconomyManager()
    
    economyManager.setBalance(player.uniqueId, amount) { success ->
        if (success) {
            player.sendMessage("Your balance is now $$amount")
        } else {
            player.sendMessage("Failed to set balance")
        }
    }
}
```

## 💸 Transaction Manager

The Transaction Manager handles money transfers and transaction history.

### Get the Transaction Manager

```kotlin
val api = IFluxEcoAPI.getInstance()
val transactionManager = api.getTransactionManager()
```

### Create a Transaction

```kotlin
import io.oira.fluxeco.api.transaction.Transaction
import java.util.UUID

fun transferMoney(fromUUID: UUID, toUUID: UUID, amount: Double, reason: String) {
    val transactionManager = IFluxEcoAPI.getInstance().getTransactionManager()
    
    val transaction = Transaction(
        from = fromUUID,
        to = toUUID,
        amount = amount,
        reason = reason,
        timestamp = System.currentTimeMillis()
    )
    
    transactionManager.createTransaction(transaction) { success ->
        if (success) {
            println("Transaction created successfully")
        } else {
            println("Failed to create transaction")
        }
    }
}
```

### Get Player's Transaction History

```kotlin
fun getTransactionHistory(player: Player) {
    val transactionManager = IFluxEcoAPI.getInstance().getTransactionManager()
    
    transactionManager.getPlayerTransactions(player.uniqueId) { transactions ->
        transactions.forEach { transaction ->
            println("${transaction.amount} - ${transaction.reason} - ${transaction.timestamp}")
        }
    }
}
```

### Get Transaction by ID

```kotlin
fun getTransaction(transactionId: String) {
    val transactionManager = IFluxEcoAPI.getInstance().getTransactionManager()
    
    transactionManager.getTransaction(transactionId) { transaction ->
        if (transaction != null) {
            println("Transaction: ${transaction.amount} from ${transaction.from} to ${transaction.to}")
        } else {
            println("Transaction not found")
        }
    }
}
```

## 📡 Events

Listen to FluxEco events in your plugin to react to economy changes.

### Setup Event Listeners

```kotlin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import io.oira.fluxeco.api.event.BalanceChangeEvent
import org.bukkit.plugin.java.JavaPlugin

class YourPlugin : JavaPlugin() {
    override fun onEnable() {
        // Register event listener
        server.pluginManager.registerEvents(EconomyListener(), this)
    }
}

class EconomyListener : Listener {
    @EventHandler
    fun onBalanceChange(event: BalanceChangeEvent) {
        // Handle balance changes
        println("${event.player.name}'s balance changed by ${event.amount}")
    }
}
```

### Listen to Transaction Events

```kotlin
import io.oira.fluxeco.api.event.TransactionCompleteEvent

class EconomyListener : Listener {
    @EventHandler
    fun onTransaction(event: TransactionCompleteEvent) {
        val fromPlayer = event.transaction.from
        val toPlayer = event.transaction.to
        val amount = event.transaction.amount
        
        println("$fromPlayer sent $$amount to $toPlayer")
    }
}
```

## 📊 Data Models

### Player Profile Model

```kotlin
import io.oira.fluxeco.api.model.PlayerProfile

// Get player profile
val profile = playerProfile
profile.uuid          // UUID of player
profile.balance       // Current balance
profile.totalEarned   // Total money earned
profile.totalSpent    // Total money spent
profile.joinDate      // When player first joined
profile.lastActive    // Last activity timestamp
```

### Transaction Model

```kotlin
import io.oira.fluxeco.api.transaction.Transaction

val transaction = transaction
transaction.id          // Unique transaction ID
transaction.from        // Sender's UUID
transaction.to          // Receiver's UUID
transaction.amount      // Amount transferred
transaction.reason      // Reason/description
transaction.timestamp   // When transaction occurred
transaction.serverId    // Which server (if cross-server)
```

## 📚 Examples

### Example 1: Simple Payment System

```kotlin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import io.oira.fluxeco.api.IFluxEcoAPI

class PayCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) return false
        if (args.size < 2) {
            sender.sendMessage("Usage: /customerpay <player> <amount>")
            return false
        }
        
        val recipient = sender.server.getPlayer(args[0])
        if (recipient == null) {
            sender.sendMessage("Player not found")
            return false
        }
        
        val amount = args[1].toDoubleOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage("Invalid amount")
            return false
        }
        
        val economyManager = IFluxEcoAPI.getInstance().getEconomyManager()
        val transactionManager = IFluxEcoAPI.getInstance().getTransactionManager()
        
        // Check sender has enough money
        economyManager.getBalance(sender.uniqueId) { balance ->
            if (balance < amount) {
                sender.sendMessage("You don't have enough money")
                return@getBalance
            }
            
            // Remove from sender
            economyManager.removeBalance(sender.uniqueId, amount) { success ->
                if (!success) {
                    sender.sendMessage("Failed to process payment")
                    return@removeBalance
                }
                
                // Add to recipient
                economyManager.addBalance(recipient.uniqueId, amount) { success ->
                    if (!success) {
                        // Refund sender
                        economyManager.addBalance(sender.uniqueId, amount) { }
                        sender.sendMessage("Failed to process payment")
                        return@addBalance
                    }
                    
                    // Log transaction
                    val transaction = Transaction(
                        from = sender.uniqueId,
                        to = recipient.uniqueId,
                        amount = amount,
                        reason = "Player payment",
                        timestamp = System.currentTimeMillis()
                    )
                    transactionManager.createTransaction(transaction) { }
                    
                    sender.sendMessage("Sent $$amount to ${recipient.name}")
                    recipient.sendMessage("Received $$amount from ${sender.name}")
                }
            }
        }
        
        return true
    }
}
```

### Example 2: Reward System

```kotlin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLevelChangeEvent
import io.oira.fluxeco.api.IFluxEcoAPI
import io.oira.fluxeco.api.transaction.Transaction

class LevelUpRewardListener : Listener {
    @EventHandler
    fun onPlayerLevelUp(event: PlayerLevelChangeEvent) {
        val player = event.player
        val newLevel = event.getNewLevel()
        
        if (newLevel > event.oldLevel) {
            // Player leveled up, give reward
            val rewardAmount = (newLevel * 10).toDouble()
            val api = IFluxEcoAPI.getInstance()
            
            api.getEconomyManager().addBalance(player.uniqueId, rewardAmount) { success ->
                if (success) {
                    player.sendMessage("§aLevel up! You earned $$rewardAmount")
                    
                    // Log transaction
                    val transaction = Transaction(
                        from = null,  // System reward
                        to = player.uniqueId,
                        amount = rewardAmount,
                        reason = "Level up to $newLevel",
                        timestamp = System.currentTimeMillis()
                    )
                    api.getTransactionManager().createTransaction(transaction) { }
                }
            }
        }
    }
}
```

### Example 3: Check Player Stats

```kotlin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import io.oira.fluxeco.api.IFluxEcoAPI

class StatsCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) return false
        
        val api = IFluxEcoAPI.getInstance()
        val economyManager = api.getEconomyManager()
        val transactionManager = api.getTransactionManager()
        
        economyManager.getBalance(sender.uniqueId) { balance ->
            sender.sendMessage("§6=== Economy Stats ===")
            sender.sendMessage("§eBalance: §f$$balance")
            
            transactionManager.getPlayerTransactions(sender.uniqueId) { transactions ->
                sender.sendMessage("§eTotal Transactions: §f${transactions.size}")
                
                val totalReceived = transactions
                    .filter { it.to == sender.uniqueId }
                    .sumOf { it.amount }
                
                val totalSent = transactions
                    .filter { it.from == sender.uniqueId }
                    .sumOf { it.amount }
                
                sender.sendMessage("§eTotal Received: §f$$totalReceived")
                sender.sendMessage("§eTotal Sent: §f$$totalSent")
            }
        }
        
        return true
    }
}
```

## 🐛 Troubleshooting

### "FluxEco API is not initialized yet"

**Problem:** You're trying to access the API before FluxEco has loaded.

**Solution:**
```kotlin
// Check if FluxEco is loaded before using API
try {
    val api = IFluxEcoAPI.getInstance()
    // Use API
} catch (e: IllegalStateException) {
    logger.warning("FluxEco is not loaded")
}
```

### Plugin doesn't start

**Problem:** Missing `depend: [FluxEco]` in plugin.yml

**Solution:**
```yaml
# plugin.yml
name: YourPlugin
depend: [FluxEco, Vault]  # Add FluxEco as dependency
```

### Callbacks never execute

**Problem:** Using synchronous methods in async context

**Solution:** All Economy Manager and Transaction Manager methods are async and use callbacks:
```kotlin
// Good - uses callback
economyManager.getBalance(uuid) { balance ->
    // This executes when result is ready
}

// Don't expect return value on next line
val balance = economyManager.getBalance(uuid)  // balance is null!
```

### Balance not saving

**Problem:** Not handling the success callback

**Solution:**
```kotlin
// Good - check if operation succeeded
economyManager.setBalance(uuid, amount) { success ->
    if (!success) {
        logger.warning("Failed to set balance")
    }
}

// Avoid - ignoring success
economyManager.setBalance(uuid, amount) { }
```

## 📖 Additional Resources

- **Main Documentation:** [oira.io/docs](https://oira.io/docs)
- **Source Code:** [github.com/oira-io/FluxEco](https://github.com/oira-io/FluxEco)
- **Discord:** [discord.gg/oira](https://discord.gg/oira)

## 📄 License

The FluxEco API is provided under the MIT License. See [LICENSE](LICENSE) for details.

---

Happy coding! If you have questions or issues with the API, please reach out on Discord or create an issue on GitHub.

