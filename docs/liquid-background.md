# Liquid background

The implementation was informed by the user's Music App branch at commit `a2f12c17b3af59714b6ee49da7ef88fb983284e7`:

- [GlowMesh.kt](https://github.com/centiox2/Music-App/blob/a2f12c17b3af59714b6ee49da7ef88fb983284e7/app/src/main/java/com/music/bitchord/ui/glow/GlowMesh.kt): slowly orbiting soft color layers without an expensive full-screen blur.
- [LiquidArtwork.kt](https://github.com/centiox2/Music-App/blob/a2f12c17b3af59714b6ee49da7ef88fb983284e7/app/src/main/java/com/music/bitchord/ui/glow/LiquidArtwork.kt): a reduced-size layer scaled to the screen, with a color-mesh fallback for older Android versions.

Home uses the liquid background only in its balance header; the transaction section below is solid. Other screens retain the liquid background. The shared implementation is in `shared/src/commonMain/kotlin/com/centiox/verdant/Appearance.kt`. It derives shades from the chosen color instead of loading artwork. Six layers orbit over an 80-second loop using integer harmonics so position and velocity match at the loop seam. A smaller offscreen canvas is scaled up. No image decoding, network access, full-screen blur, or Android 13-only shader APIs are required.

Animation state is read during drawing so the transaction list is not recomposed every frame. Compose infinite-animation support respects motion scaling and the Android window frame clock. Turning animation off disposes the moving transition and displays a still composition. Shades are luminance-limited for readable foreground text even with bright custom colors.

Settings offers Forest, Ocean, Amethyst, Ember, Rose, and Teal presets, a validated six-digit hex field, RGB sliders, and live previews. Preferences use the same private repository and encrypted Android storage as the ledger, survive restart, and are included in backups. Older version-1 backups still load with default appearance values.

Tests compare actual pixels at different animation times and verify that paused pixels stay unchanged. Further checks cover picker validation, RGB controls, backward-compatible backups, activity recreation, and a signed-release process restart.
