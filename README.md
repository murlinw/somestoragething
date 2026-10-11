# Sided Storage

A NeoForge 1.21.1 mod adding a storage block whose inventory is split into
one partition per face. Items inserted through a face can only be taken out
through that same face — including by hoppers and pipes.

Three tiers, differing in slots per face. Each tier upgrades from the
previous one — there are no shortcuts:

| Block | Per face | Total | Recipe |
|---|---|---|---|
| Sided Storage | 9 | 54 | redstone corners, iron sides, chest center |
| Dense Sided Storage | 18 | 108 | gold ring around a basic Sided Storage |
| Capacity Sided Storage | 27 | 162 | diamond ring around a dense Sided Storage |

Right-click a block to open its tabbed GUI (one tab per face: Down, Up,
North, South, West, East). Breaking a block spills its contents.

Recipe overview: `docs/recipes.png`. Compatibility notes (Iron Chests,
Sophisticated Storage, Functional Storage): `docs/COMPATIBILITY.md`.

## Build

Requires JDK 21:

```
./gradlew build
```

The mod jar lands in `build/libs/`. Drop it into a NeoForge 1.21.1
instance's `mods` folder.

## License

MIT — see `LICENSE`.
