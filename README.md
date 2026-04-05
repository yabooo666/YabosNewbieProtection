# YabosNewbieProt

<p align="center">
  <img src="https://img.shields.io/badge/platform-Paper-525252?style=for-the-badge" alt="Paper">
  <img src="https://img.shields.io/badge/java-21-6B7280?style=for-the-badge" alt="Java 21">
  <img src="https://img.shields.io/badge/config-Fully%20Configurable-4B5563?style=for-the-badge" alt="Fully Configurable">
  <img src="https://img.shields.io/badge/PlaceholderAPI-Supported-374151?style=for-the-badge" alt="PlaceholderAPI">
  <img src="https://img.shields.io/badge/made%20for-play.macegate.ge-1F2937?style=for-the-badge" alt="Made for play.macegate.ge">
  <img src="https://img.shields.io/badge/license-Personal%20Server%20Use%20Only-111827?style=for-the-badge" alt="Personal Server Use Only">
</p>

A clean, polished newbie protection plugin built for modern Paper servers.

YabosNewbieProt is designed to give new players a safe start without making the server feel cheap, noisy, or overcomplicated. The plugin focuses on smooth protection logic, strong configurability, and a cleaner premium-style experience for admins and players.

**Made for `play.macegate.ge`.**

---

## Overview

YabosNewbieProt gives new players temporary protection when they join the server, helping them survive their first moments without being instantly targeted in PvP.

Protection behavior, timings, messages, UI, sounds, world restrictions, AFK handling, storage, and command flow can all be controlled from `config.yml`.

This version is built around one main goal:

**Everything important should be configurable.**

---

## Features

- Temporary newbie protection for new players
- Fully configurable protection duration
- Configurable PvP protection behavior
- Action bar support
- BossBar support
- Title and subtitle support
- Configurable sounds and notifications
- AFK-aware protection handling
- World whitelist / blacklist support
- Offline player admin support
- Dirty-save and auto-save storage system
- PlaceholderAPI support
- Configurable command aliases and permissions
- Fully customizable messages in `config.yml`

---

## Why this plugin

Many newbie protection plugins feel basic, outdated, or hardcoded.

YabosNewbieProt was made to feel more polished and practical:

- cleaner admin experience
- less hardcoded behavior
- more control from config
- lightweight logic for normal Paper servers
- easier to style for your own network

---

## Commands

Default commands include:

```text
/newbieprot status
/newbieprot check <player>
/newbieprot grant <player>
/newbieprot set <player> <time>
/newbieprot addtime <player> <time>
/newbieprot removetime <player> <time>
/newbieprot remove <player>
/newbieprot list
/newbieprot reload
```

Command aliases, permissions, usage text, and feedback are configurable.

---

## Configuration

The plugin is centered around `config.yml`.

You can configure things like:

- protection durations
- save intervals
- warning thresholds
- status formatting
- boss bar style
- sounds
- titles
- action bar text
- PlaceholderAPI output
- world restrictions
- AFK pause logic
- admin permissions
- command aliases
- storage behavior
- all player and admin messages

If you want to match your server branding, the plugin is designed so the experience can be styled directly from config without needing code edits for normal usage.

---

## Performance

YabosNewbieProt is designed to stay lightweight for regular Paper usage.

It avoids unnecessary heavy systems and keeps the protection logic straightforward. The updated version also uses a cleaner save flow with auto-save and dirty tracking instead of forcing full data writes in the wrong places.

---

## PlaceholderAPI

Placeholder support is included for servers that want to display player protection data in scoreboards, tab, chat, or GUIs.

Example use cases:

- remaining protection time
- whether a player is protected
- formatted protection status text

---

## Compatibility

- Paper
- Java 21
- PlaceholderAPI

---

## Installation

1. Build the plugin or place the compiled jar into your server's `plugins` folder
2. Start the server once to generate `config.yml`
3. Adjust the configuration to match your server
4. Reload or restart the server
5. Configure your messages, UI, timings, and behavior entirely from `config.yml`

---

## License

This project is licensed under a **Custom Personal Server Use Only License**.

### You may:
- use this plugin on your own server or network
- edit the files for your own private/internal use
- keep modified versions for your own private/internal use

### You may not:
- redistribute this project
- reupload this project anywhere
- resell this project
- sublicense this project
- share modified versions publicly
- claim this project as your own work
- use this project as part of another public or commercial release without explicit permission

See the full `LICENSE` file for exact terms.

---

## GitHub Topics

```text
minecraft
minecraft-plugin
paper
papermc
spigot
java
java-21
placeholderapi
pvp
newbie-protection
configurable
macegate
```

---

## Branding

This plugin was made for **play.macegate.ge**.

If you are using it on your own server, you can still fully restyle the experience through `config.yml` to match your own branding and tone.

---

## Notes

This project focuses on a cleaner premium-style feel rather than bloated features.

The goal is simple:

**protect new players, keep the experience smooth, and let server owners control everything that matters from config.**

---

## Support

yabo@macegate.ge
