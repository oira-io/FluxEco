# Contributing to FluxEco

Thank you for your interest in contributing to FluxEco. Contributions of all kinds are welcome, including bug reports, feature requests, documentation improvements, and code changes.

By contributing, you agree that your contributions will be licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## 📋 Table of Contents

* [Getting Started](#-getting-started)
* [Development Setup](#-development-setup)
* [Making Changes](#-making-changes)
* [Style Guide](#-style-guide)
* [Reporting Bugs](#-reporting-bugs)
* [Suggesting Features](#-suggesting-features)
* [Pull Request Process](#-pull-request-process)
* [Commit Guidelines](#-commit-guidelines)
* [Questions](#-questions)
* [Code of Conduct](#-code-of-conduct)

---

## 🚀 Getting Started

1. Fork the repository on GitHub
2. Clone your fork:

   ```bash
   git clone https://github.com/YOUR_USERNAME/FluxEco.git
   ```
3. Add the upstream remote:

   ```bash
   git remote add upstream https://github.com/oira-io/FluxEco.git
   ```
4. Create a branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

---

## 💻 Development Setup

### Prerequisites

* Java 21+
* Gradle (included via `gradlew`)
* Git
* IntelliJ IDEA or another Kotlin-compatible IDE

### Setup

1. Open the project in your IDE
2. Let Gradle sync
3. Build the project:

   ```bash
   ./gradlew build
   ```

---

## ✏️ Making Changes

* Keep changes focused. One fix or feature per branch.
* Use descriptive branch names:

  * `feature/add-economy-events`
  * `fix/balance-sync-issue`
  * `docs/update-api-docs`
  * `refactor/simplify-cache-logic`

---

## 🎨 Style Guide

### Kotlin Code Style

**Naming**

* Classes / Objects: `PascalCase`
* Functions / Variables: `camelCase`
* Constants: `UPPER_SNAKE_CASE`
* Companion object constants: `UPPER_SNAKE_CASE`

**Formatting**

* 4 spaces indentation
* Max line length: 120 characters
* Avoid abbreviations unless widely understood

**Functions**

```kotlin
// Avoid
fun getPB(p: Player): Double {
    return em.getBal(p.id)
}

// Prefer
fun getPlayerBalance(player: Player): Double {
    return economyManager.getBalance(player.uniqueId)
}
```

**Documentation**

```kotlin
/**
 * Transfers money from one player to another.
 *
 * @param from The player sending money
 * @param to The player receiving money
 * @param amount The amount to transfer
 * @return true if successful, false otherwise
 * @throws IllegalArgumentException if amount is negative
 */
fun transferMoney(from: Player, to: Player, amount: Double): Boolean {
    // implementation
}
```

**Error Handling**

```kotlin
// Avoid
try {
    database.saveTransaction(transaction)
} catch (e: Exception) {
    e.printStackTrace()
    return false
}

// Prefer
try {
    database.saveTransaction(transaction)
} catch (e: DatabaseException) {
    logger.warning("Failed to save transaction: ${e.message}")
    return false
}
```

---

## 🐛 Reporting Bugs

Open a GitHub Issue and include:

* Clear title
* Steps to reproduce
* Expected behavior
* Actual behavior
* Server version (Paper/Spigot)
* Java version
* Relevant config snippets
* Error logs or stack traces
* Plugins list (`/plugman list`)

**Example**

```text
Title: Balance not syncing across servers with Redis

Steps to reproduce:
1. Set up two Paper servers with FluxEco and Redis
2. Run `/eco give Player 100` on Server A
3. Run `/balance Player` on Server B

Expected: Balance shows 100
Actual: Balance shows 0
```

---

## ✨ Suggesting Features

Open a GitHub Issue with the `feature` label and include:

* Clear title
* Description of the feature
* Why it is useful
* How it would work from a user perspective
* Possible implementation ideas
* Related plugins or systems (if any)

---

## 🔄 Pull Request Process

1. Rebase on latest `main`:

   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. Push your branch:

   ```bash
   git push origin feature/your-feature-name
   ```
3. Open a Pull Request on GitHub

### PR Checklist

* Code builds without errors
* Follows the style guide
* No hardcoded values
* Documentation updated if needed
* Tested locally
* Comments explain complex logic

Maintainers will review and request changes if needed.

---

## 📝 Commit Guidelines

**Format**

```text
[TYPE] Short description (50 chars max)

Optional longer explanation of what changed and why.

Fixes #123
```

**Types**

* `feat:` new feature
* `fix:` bug fix
* `refactor:` code refactor
* `docs:` documentation
* `style:` formatting
* `perf:` performance
* `test:` tests

---

## ❓ Questions

* Check existing GitHub Issues
* Read the documentation: [https://oira.io/docs](https://oira.io/docs)
* Use GitHub Discussions
* Join Discord: [https://discord.gg/oira](https://discord.gg/oira)

---

## 🤝 Code of Conduct

This project follows a Code of Conduct. Be respectful and collaborative. Report unacceptable behavior to the maintainers.
