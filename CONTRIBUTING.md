# Contributing to FluxEco
Thank you for contributing to FluxEco! 🎉

---

By contributing to FluxEco, you agree that your contributions will be licensed under the MIT License. See [LICENSE](LICENSE) for details.

## 📜 License

- **Issues:** Look through existing issues for similar questions
- **Documentation:** Check [oira.io/docs](https://oira.io/docs)
- **Discussions:** Use GitHub Discussions for questions
- **Discord:** Join our [Discord server](https://discord.gg/oira)

## ❓ Questions?

- Use headers and formatting for readability
- Keep related information together
- Include code examples
- Use clear, concise language

### Documentation

```
  date-format: "dd/MM/yyyy HH:mm"
  # Date format (Java SimpleDateFormat)
  
  starting-balance: 100
  # Starting balance for new players
general:
# Use descriptive comments
```yaml

- Provide default values
- Group related settings
- Include descriptive comments
- Use 2-space indentation

### Configuration Files

```
}
    e.printStackTrace()
} catch (e: Exception) {
    database.saveTransaction(transaction)
try {
// Avoid

}
    return false
    logger.warning("Failed to save transaction: ${e.message}")
} catch (e: DatabaseException) {
    database.saveTransaction(transaction)
try {
// Prefer
```kotlin
**Error Handling:**

```
}
    // implementation
fun transferMoney(from: Player, to: Player, amount: Double): Boolean {
 */
 * @throws IllegalArgumentException if amount is negative
 * @return true if transfer was successful, false otherwise
 * @param amount The amount to transfer
 * @param to The player receiving money
 * @param from The player sending money
 *
 * Transfers money from one player to another.
/**
```kotlin
**Documentation:**

```
}
    return em.getBal(p.id)
fun getPB(p: Player): Double {
// Avoid

}
    return economyManager.getBalance(player.uniqueId)
fun getPlayerBalance(player: Player): Double {
// Good
```kotlin
**Functions:**

- Avoid abbreviations unless widely understood
- Use meaningful variable names
- Max line length: 120 characters
- Use 4 spaces for indentation
**Formatting:**

- `companion object` constants - UPPER_SNAKE_CASE
- `constants` - UPPER_SNAKE_CASE: `MAX_TRANSACTION_AMOUNT`
- `functions` and `variables` - camelCase: `getBalance()`, `playerName`
- `classes` and `objects` - PascalCase: `PlayerBalance`, `EconomyManager`
**Naming Conventions:**

### Kotlin Code Style

## 🎨 Style Guide

```
- Transfer tax amount to configurable account
- Check tax on payment events
- Add tax percentage to config.yml
Suggested implementation:

This helps regulate wealth distribution on economy servers.
Use case:

payment would go to a server account.
Admins could set a tax percentage, and a portion of each
It would be useful to have an automatic tax on transactions.
Description:

Title: Add economy tax system
```
**Example:**

- Any related plugins or systems
- Possible implementation approaches
- How it would work from a user perspective
- Why this feature would be useful
- Clear title describing the feature
**Include:**

Have an idea for a new feature? Open a GitHub Issue with the `feature` label:

## ✨ Suggesting Features

```
Config: Using MySQL + Redis (see attached config.yml)
Java: 21.0.2
Server: Paper 1.21.1

Actual: Balance shows 0 on Server B
Expected: Balance shows 100 on Server B

3. Check balance on Server B: `/balance Player`
2. Give money to player on Server A: `/eco give Player 100`
1. Set up two Paper servers with FluxEco and Redis
Steps to reproduce:

Title: Balance not syncing across servers with Redis
```
**Example:**

- Plugins list (use `/plugman list`)
- Error logs or stack traces
- Relevant configuration snippets
- Java version
- Server version (Spigot/Paper version)
- Actual behavior
- Expected behavior
- Steps to reproduce
- Clear title describing the issue
**Include:**

Found a bug? Please report it by opening a GitHub Issue:

## 🐛 Reporting Bugs

   - Approved PRs will be merged
   - Address feedback and push updates
   - Maintainers will review your PR
5. **Review process:**

   - [ ] Commits are clean and descriptive
   - [ ] Documentation updated if needed
   - [ ] Tested locally
   - [ ] No hardcoded values (use config instead)
   - [ ] Comments explain complex logic
   - [ ] Follows the style guide
   - [ ] Code builds without errors
4. **PR Checklist:**

   - Include any testing or configuration changes
   - Provide context about what changed and why
   - Reference any related issues (#123)
   - Use a clear title describing the change
3. **Open a Pull Request on GitHub:**

   ```
   git push origin feature/your-feature-name
   ```bash
2. **Push your changes:**

   ```
   git push origin feature/your-feature-name --force-with-lease
   git rebase upstream/main
   git fetch upstream
   ```bash
1. **Keep commits clean** - Rebase and squash if needed:

## 🔄 Pull Request Process

```
from 500ms to <100ms average.
Improved connection pooling to reduce sync latency

fix: Resolve balance sync delay with Redis
```

```
Fixes #456

and amount using the TransactionManager interface.
Users can now filter transactions by type, date range,

feat: Add transaction filters to API
```
**Examples:**

- `test:` - Test additions/changes
- `perf:` - Performance improvement
- `style:` - Code style/formatting
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `fix:` - Bug fix
- `feat:` - New feature
**Types:**

```
Fixes #123 (if applicable)

Explain what changed and why, not how.
Longer explanation if needed (wrap at 72 chars).

[TYPE] Brief description (50 chars max)
```
**Format:**

Write clear, descriptive commit messages:

## 📝 Commit Guidelines

```
./gradlew compileKotlin
# Check for Kotlin warnings

./gradlew test
# Run any tests

./gradlew build
# Build and check for errors
```bash

Before submitting changes:

### Testing

5. **Update documentation** - Reflect changes in configuration files and docs
4. **Add tests** - If applicable, add tests for new features
3. **Write clear code** - Use meaningful variable names and comments
2. **Follow the style guide** - See [Style Guide](#style-guide) below
1. **Keep changes focused** - One feature or fix per branch/PR

### Code Changes

- `refactor/simplify-cache-logic` - Code improvements
- `docs/update-api-docs` - Documentation
- `fix/balance-sync-issue` - Bug fixes
- `feature/add-economy-events` - New features
Use descriptive branch names:

### Branch Naming Convention

## ✏️ Making Changes

```
└── dist/                             # Build output directory
├── settings.gradle.kts               # Project settings
├── build.gradle.kts                  # Root build configuration
│
│   └── build.gradle.kts
│   │   └── *.yml                     # Configuration files
│   ├── src/main/resources/
│   │       └── util/                 # Utility classes
│   │       ├── redis/                # Redis support
│   │       ├── manager/              # Core managers
│   │       ├── listener/             # Event listeners
│   │       ├── integration/          # Plugin integrations
│   │       ├── gui/                  # GUI implementations
│   │       ├── data/                 # Database management
│   │       ├── command/              # Command handlers
│   │       ├── cache/                # Caching layer
│   │       ├── api/                  # API implementations
│   │   └── fluxeco/core/
│   ├── src/main/kotlin/io/oira/
├── core/                             # Main plugin implementation
│
│   └── build.gradle.kts
│   │       └── IFluxEcoAPI.kt        # Main API interface
│   │       ├── transaction/          # Transaction interfaces
│   │       ├── model/                # Data models
│   │       ├── event/                # Custom events
│   │       ├── economy/              # Economy interfaces
│   │   └── fluxeco/api/
│   ├── src/main/kotlin/io/oira/
├── api/                              # Public API module
FluxEco/
```

### Project Structure

   ```
   ./gradlew test
   ```bash
4. **Run tests** (if available):

   ```
   ./gradlew build
   ```bash
3. **Build the project:**

   - Let Gradle sync automatically
   - IntelliJ IDEA: File → Open → Select FluxEco folder
2. **Open in your IDE:**

   ```
   cd FluxEco
   git clone https://github.com/YOUR_USERNAME/FluxEco.git
   ```bash
1. **Clone the repository:**

### Setup Steps

- IntelliJ IDEA (recommended) or any Kotlin-compatible IDE
- Git
- Gradle 8.0+ (included with `gradlew`)
- Java 21 or higher

### Prerequisites

## 💻 Development Setup

4. **Create a branch** - `git checkout -b feature/your-feature-name`
3. **Add upstream remote** - `git remote add upstream https://github.com/oira-io/FluxEco.git`
2. **Clone your fork** - `git clone https://github.com/YOUR_USERNAME/FluxEco.git`
1. **Fork the repository** - Click the "Fork" button on GitHub

## 🚀 Getting Started

**Be respectful, inclusive, and collaborative.** We're all here to make FluxEco better.

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the maintainers.

## 🤝 Code of Conduct

- [Questions?](#questions)
- [Style Guide](#style-guide)
- [Suggesting Features](#suggesting-features)
- [Reporting Bugs](#reporting-bugs)
- [Pull Request Process](#pull-request-process)
- [Commit Guidelines](#commit-guidelines)
- [Making Changes](#making-changes)
- [Development Setup](#development-setup)
- [Getting Started](#getting-started)
- [Code of Conduct](#code-of-conduct)

## 📋 Table of Contents

Thank you for your interest in contributing to FluxEco! We welcome contributions from the community, whether it's bug reports, feature suggestions, or code improvements.


