# Minecraft 1.21.1 / NeoForge 21.1.176 validation

Run `./gradlew build runGameTestServer` with Java 21. GameTests run inside
Minecraft, not against mocked inventory classes. The suite covers all three
tiers' directional capabilities and save/load, pickaxe harvest checks and
block loot, comparator endpoints, both upgrade transitions and their material
cost, a real hopper extracting the bottom partition, and independent menus
that become invalid when their block entity is replaced. It also checks opening
all six clicked faces, rejecting tab packets with no matching menu, invalid
faces, a different position or excessive distance, and returning cursor items
exactly once on a valid tab switch.

Interactive release checks (not claimed as automated coverage):

- Right-click each physical face and confirm that face opens.
- Navigate face buttons using Tab, arrow keys, Enter, and Space; check
  narrator labels and tooltips, and confirm switching does not reset the mouse.
- Switch faces with an item on the cursor: the item must return to the
  player inventory or drop safely when that inventory is full, never duplicate.
- Have two connected players shift-click items on different faces at once.
- Mine all tiers in survival and check that each block and every stored stack
  drop once. Repeat after a world restart and a chunk unload/reload.
- In a pack with claim protection, verify denied interactions stay denied.
- Check recipe-book unlocks after receiving the required chest/previous tier.

Upgrades use ordinary right-click, without sneaking, because vanilla suppresses
block use for many held items while sneaking. Hold eight gold ingots for Basic
to Dense or eight diamonds for Dense to Capacity. Existing menus close after
replacement, so stale slot indices cannot access the new inventory.
