# Exorcist

A 2D infinite climbing platformer built in **Java** (Swing). Jump between procedurally generated platforms, fight four enemy types with a responsive combat system, and climb as high as you can. Built on an object-oriented entity hierarchy with state-machine AI, custom collision physics, and a sprite-sheet animation system.

[![Demo](thumbnail.jpg)](https://youtu.be/cbCoB_rXNRQ)

## Features

- **Procedural generation** — platforms, hazards, and enemy placements are generated on the fly with increasing difficulty as you ascend
- **Four enemy types** with distinct AI behaviors: melee aggression, ranged cursing, aerial pursuit, and heavy tanking
- **Fluid combat** — attack, double jump, and a stamina-based shield that rewards timing
- **Persistent high score** saved between sessions
- **Polished animations** — per-entity sprite sheets with state-driven animation (idle, walk, attack, death)
- **Responsive game feel** — fall damage, knockback, curse DoT with visual feedback, half-heart regeneration

## How It Works

The game is organised around an object-oriented entity model: a shared `Entity` base provides movement, health, and animation state, and each enemy (`Wolf`, `Golem`, `Witch`, `Bat`) and the `Player` extends it with its own state-machine AI. `GamePanel` drives the update/render loop, `CollisionChecker` resolves platform and combat collisions, and `TileManager` builds the world from map files while scrolling infinitely upward and generating fresh content as you climb.

Each entity animates from sprite sheets driven by its current state (idle, walk, attack, death). Physics covers gravity, jumping and double-jumping, knockback, and fall damage for long drops. Input is split between `KeyHandler` and `MouseHandler`, audio plays through `javax.sound.sampled` via a dedicated `Sound` class, and the high score persists to disk between sessions.

## Enemies

| Enemy | Behavior |
|-------|----------|
| **Wolf** | Fast and aggressive — lunges at the player on contact |
| **Golem** | Slow and tanky — heavy melee hits, high HP |
| **Witch** | Ranged — casts a stackable curse (DoT) and kites the player |
| **Bat** | Aerial — spawns periodically and pursues from any direction |

## Player

- Move, double jump, attack, and shield
- 5 hearts HP with 2 bonus overflow hearts (earned from kills)
- Shield with stamina bar — blocks frontal attacks, depletes on sustained use
- Half-heart regen every 20 seconds
- Fall damage for drops over 10 tiles

## Skills Demonstrated

- Object-oriented design — `Entity` base with `Wolf` / `Golem` / `Witch` / `Bat` / `Player` subclasses
- Inheritance & polymorphism — shared entity behaviour specialised per enemy
- State-machine AI — per-entity behaviour and animation states
- Game AI — four distinct enemy behaviours (melee lunge, ranged kiting, aerial pursuit, tank)
- Procedural generation — infinite upward platform, hazard, and enemy generation with scaling difficulty
- Collision detection & physics — `CollisionChecker`, gravity, double jump, knockback, fall damage
- Sprite animation system — state-driven sprite-sheet animation (idle/walk/attack/death)
- Game loop architecture — `GamePanel` update/render cycle
- Custom 2D rendering — Java Swing `Graphics2D` with `AlphaComposite` effects
- Input handling — separate keyboard and mouse handlers
- Audio playback — sound effects via `javax.sound.sampled`
- Tile-map system — `TileManager` loading map files with infinite scrolling
- File I/O — persistent high score and map loading
- Combat systems design — stamina shield, curse DoT, half-heart regen, overflow hearts
- Java Platform Module System — modular build with `module-info.java`
- JAR packaging — runnable modular Java application

## Tech Stack

- Java 17
- Java Swing / AWT (`Graphics2D`, `AlphaComposite`, 2D rendering)
- `javax.sound.sampled` (audio)
- Java Platform Module System (`module-info.java`)
- Packaged as a runnable modular JAR (`Exorcist.jar`)
- Eclipse IDE

## Demo & Links

- ⬇️ [Download the latest release](https://github.com/TheYellowDuck/Exorcist/releases/latest)

## Getting Started

Requires Java 17+.

```bash
java -jar Exorcist.jar
```

Download the latest release [here](https://github.com/TheYellowDuck/Exorcist/releases/latest).

### Controls

| Key | Action |
|-----|--------|
| A / D or Arrow Keys | Move |
| W / Space / Up | Jump (double jump supported) |
| Z / J or Left Click | Attack |
| E / K or Right Click | Shield |
| Escape | Pause |
