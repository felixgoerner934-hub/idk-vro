# SpawnerBeacon 1.2.0

Client-side Fabric mod for Minecraft **26.1.2** with a Cherry Blossom themed configuration screen and visible spawner beams.

## Features

- Client-side spawner tracking using loaded `SpawnerBlockEntity` instances.
- Through-wall beacon-style beams.
- Per-spawner-type visibility and thickness.
- Per-type colors with visual color swatches and RGB sliders; no hex input required.
- Master beam toggle, animation, opacity, height, render distance and distance fade.
- Rainbow mode.
- Overworld / Nether / End filters.
- Nearby spawner list with distance and coordinate copy.
- Cherry Blossom GUI with branch/blossom artwork, responsive centered layout, hover/click styling and a decorative cherry car illustration.
- German and English translations.

## Build on GitHub

1. Upload the project contents to a repository.
2. Make sure `.github/workflows/build.yml` exists.
3. Open **Actions → Build SpawnerBeacon → Run workflow**.
4. Download the `SpawnerBeacon-1.2.0` artifact.
5. Put `spawnerbeacon-1.2.0.jar` into the Fabric `mods` folder together with Fabric API.

## In game

Press the **Ü** key on a German keyboard (the physical key represented by GLFW's US `[` / `LBRACKET`) to open the menu.

Configuration is saved to `.minecraft/config/spawnerbeacon.json`.

## Notes

The beam renderer uses the modern Minecraft 26.1 extraction/render pipeline. The GUI uses `GuiGraphicsExtractor` and bundled PNG artwork. The decorative car is an original Cherry Blossom themed illustration; it can be replaced by a specific reference image later if desired.
