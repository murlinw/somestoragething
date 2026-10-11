# Compatibility record — Sided Storage in a pack with Iron Chests

Intent: Sided Storage is built to sit in a 1.21.1 NeoForge modpack alongside
Iron Chests (`ironchest`), Sophisticated Storage, Functional Storage, and other
storage mods.

## Base recipe under review

`sidedstorage:sided_storage_basic`, shaped:

```
R I R
I C I
R I R
```

- `R` = `minecraft:redstone` (corners)
- `I` = `minecraft:iron_ingot` (sides)
- `C` = `minecraft:chest` (center)

## Verdict: recipe is open — no conflicts found

Checked 2026-10-10 against published jars / data:

1. **Vanilla 1.21.1** (`data/minecraft/recipe`, from the NeoForge client-extra
   artifact). Only two shaped recipes use a chest at all: hopper (gapped
   layout, no redstone) and shulker box (shells, no iron). Zero recipes use
   the redstone + iron + chest set.
2. **Iron Chests 1.21 NeoForge (`ironchest-1.21-neoforge-16.0.7`).** No recipe
   in the mod mentions redstone and chest together. Its iron chest uses a
   full iron ring with no redstone (`MMM/MSM/MMM`); its copper→iron path uses
   iron + glass around its own copper chest; gold/diamond tiers center its
   own previous-tier chests. None match `RIR/ICI/RIR`.
3. **Sophisticated Storage 1.21.1 (`1.6.2.2159`).** Iron/gold/diamond chest
   upgrades center its own chest items (`sophisticatedstorage:chest`,
   `iron_chest`, `gold_chest`), never a vanilla chest. No match.
4. **Functional Storage 1.21.1 (`1.5.7`).** Every full-ring recipe enumerated:
   rings are planks, buckets, obsidian, or drawers; iron appears only as
   nuggets (framed drawer). No iron/gold/diamond-ingot ring around a chest.

Method: exact pattern-plus-ingredient comparison. A collision requires the
same grid with the same ingredients; none exists.

## Note on the previous recipe

The earlier base recipe (full 8-iron ring around a vanilla chest) *would*
have been ambiguous with Iron Chests' `vanilla_iron_chest` (full iron ring
around the `c:chests/wooden` tag, which includes vanilla chests). Both would
have shown in the recipe book for the same grid. The redstone-corner recipe
replaces it and removes even that ambiguity.

Even in a hypothetical future clash, the failure mode is two recipes sharing
one grid (both listed), never save corruption.

## Upgrade chain (linear, no shortcuts)

- `sidedstorage:sided_storage_basic`: redstone corners, iron sides,
  vanilla chest center.
- `sidedstorage:sided_storage_dense_from_basic`: gold ring around a basic
  Sided Storage.
- `sidedstorage:sided_storage_capacity_from_dense`: diamond ring around a
  dense Sided Storage.

Higher tiers are only reachable by upgrading the previous tier. There are no
direct from-vanilla-chest recipes for Dense or Capacity, so they cannot
collide with Iron Chests' own chest upgrade line either.
