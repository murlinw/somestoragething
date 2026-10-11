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

The GUI opens on the face you clicked. Use Tab or the arrow keys to focus
a face button, then Enter or Space to select it. Each player views their
own face independently. Automation must supply a direction; unsided
capability queries return no inventory.

Mine any tier with a pickaxe to recover the block. Bare hands spill the
contents but do not drop the block. Comparators measure the combined
fullness of all six faces, from 0 (empty) to 15 (full).

To upgrade a placed block, right-click Basic while holding at least eight
gold ingots, or Dense while holding at least eight diamonds. The upgrade
consumes eight materials and preserves every face's items and slot positions.
Creative players pay no materials. Existing viewers must reopen the upgraded
block. Breaking and crafting still works, but spills contents before crafting.

Recipe overview: `docs/recipes.png`. Compatibility notes (Iron Chests,
Sophisticated Storage, Functional Storage): `docs/COMPATIBILITY.md`.

## Build

Requires JDK 21:

```
./gradlew build
```

The mod jar lands in `build/libs/`. Drop it into a NeoForge 1.21.1
instance's `mods` folder.

Supported target: Minecraft **1.21.1**, NeoForge **21.1.176 or newer within
21.1.x**, Java **21**. Other Minecraft versions are not declared compatible.

Run the Minecraft regression suite with `./gradlew runGameTestServer`.
It checks face isolation, hopper extraction, saving/reloading, pickaxe
harvest eligibility and loot, comparator output, upgrades, and independent
player menus. See `docs/TESTING.md` for remaining interactive checks.

GitHub Actions builds and tests every push to main and pull request. Jars
are available as workflow artifacts. Pushing a version tag such as `v1.1.0`
publishes a GitHub release only after the build and Minecraft tests pass.

## License

MIT — see `LICENSE`.
