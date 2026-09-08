# Aether cape artwork

The panel in [aether-cape-panel.png](aether-cape-panel.png) was created with the built-in image generation tool, using `src/main/resources/assets/aether/textures/gui/logo.png` as the reference. The original logo asset is unchanged.

The shipped texture is `src/main/resources/assets/aether/textures/cosmetic/aether_cape.png`. Its 128 × 64 atlas follows Minecraft's cape UV layout, with 20 × 32 front and back panels and two-pixel edges. It uses Minecraft's cape model, lighting, and animation.

Repack the panel with JDK 25:

```sh
java scripts/PackCapeTexture.java art/aether-cape-panel.png src/main/resources/assets/aether/textures/cosmetic/aether_cape.png
```

Generation prompt:

> Use case: precise-object-edit. Asset type: Minecraft vanilla-style cape flat texture artwork, NOT a 3D render or mockup. Input image 1 is the existing Aether logo, an invariant emblem: preserve its exact angular upward chevron and separated lower diamond, proportions and pale periwinkle blue. Turn this into a simple flat rectangular cape back panel in a 10:16 portrait aspect ratio. Center the logo horizontally in the upper-middle of the cape, occupying about 70% of the width. Extend the surrounding background into opaque deep midnight navy cloth, with extremely subtle blocky pixel-art tonal variation and a thin muted blue hem along the very bottom. Keep it clean and understated, in the visual language of official vanilla Minecraft capes. Broad flat colors, no glow, no gradients on the emblem, no folds, no perspective, no text, no additional emblems, no straps, no ornamental border. All corners square; the entire canvas is the cape panel, no margins or outside background. Return one flat image, ideally 640 by 1024 pixels. This will be downsampled into a small pixel texture and mapped onto the actual vanilla cape, so the emblem must be bold and legible.
