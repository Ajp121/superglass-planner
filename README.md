# Superglass Planner

A [RuneLite](https://runelite.net/) plugin for planning and tracking the Superglass Make crafting grind — built with ironmen in mind.

## Features

### Side Panel
- **Bank Materials** — Live counts of giant seaweed, sand, astrals, and molten glass (bank + inventory + rune pouch + looting bag)
- **Possible Casts & Estimated Glass** — How many casts you can do and how much glass you'll produce
- **Material Balance** — See deficits and surpluses across your materials
- **Goal Calculator** — Track progress toward a target level, XP amount, or glass count
- **Session Stats** — Casts, glass made, items blown, and XP gained this session

### Overlays
- **Superglass Make Overlay** — Shows casts available (or remaining to goal), session casts, and XP while casting
- **Glassblowing Overlay** — Shows items blown, items remaining to goal/level, and crafting XP while blowing glass

### Configuration
- Choose your glass item (unpowered orb, lantern lens, etc.) for accurate XP calculations
- Toggle pickup extra glass and set your expected glass per cast based on inventory space
- Factor existing glass into goal calculations
- Configurable overlay timeouts
- Optional session reset on logout

## Installation

Search for **Superglass Planner** in the RuneLite Plugin Hub.

## Building

```
./gradlew build
```

Requires Java 11+.
