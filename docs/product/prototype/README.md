# SubMask Interactive Prototype

This directory contains a static product prototype for SubMask. It is intended for product review, QA alignment, and interaction discussion. It does not depend on Android runtime code.

## Open

Open `index.html` in a browser:

```text
docs/product/prototype/index.html
```

## Covered Flows

- Home screen with permission status, orientation switch, preview, quick layouts, and manual sliders.
- Right-side menu with only `使用帮助` and `关于`.
- Quick layouts for `TikTok 全屏`, `YouTube 半屏`, and `横屏全屏`.
- Floating mask unlocked and locked states.
- Runtime opacity adjustment.
- Quick Settings Tile active and inactive states.

## Out Of Scope

- Android service behavior.
- Real overlay permission flow.
- Real notification permission flow.
- Automatic subtitle detection, OCR, AI analysis, multiple masks, per-app configuration, account sync, and built-in player.
