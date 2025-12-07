# FluxEco

The best free modern economy plugin for Minecraft servers.

## Description

FluxEco is a modern economy plugin built with Kotlin, supporting various databases and integrations like Vault, PlaceholderAPI, and MiniPlaceholders. It provides commands for managing balances, transactions, and more.

## Installation

1. Download the latest FluxEco-ver.jar release file. (Do not download the API jar, as it is for developers only.)
2. Place the JAR in your server's `plugins` folder.
3. Restart the server.

## Building from Source

To build the plugin yourself:

1. Clone the repository.
2. Run `./gradlew build` in the root directory.
3. The built JARs will be in `dist/`.

## Dependencies

- Vault
- PlaceholderAPI (optional)
- MiniPlaceholders (optional)

## TODO

- [x] Add Redis cross server support
- [x] Add MongoDB support (untested)
- [x] Implement Stats GUI (optional)
- [x] Implement Confirm GUI (optional)
- [x] Make more configurable
- [ ] Add Vault migration
- [ ] Add database migration

For more information, visit [https://oira.io](https://oira.io).
