# TapToFlip browser prototype

First playable HTML5 port for evaluating browser-game distribution. Android files are unchanged. The original frog asset and GameEngine.kt mechanics are reused; obstacle artwork is currently represented by collision-sized colored blocks. This is a prototype, not a platform-approved or monetized release.

Run `python3 -m http.server 8000 --directory web/dist` from the repository root and open localhost:8000. Run `node --test web/test/engine.test.mjs` for engine checks. Package the contents of `web/dist` with index.html at ZIP root for later platform review.

Controls: tap/click/Space to jump; P or Pause to pause. Losing window focus pauses the run. Best score stays on this browser when storage is available. No third-party dependencies, trackers, live ads or SDK credentials are included.

## Distribution decision — 2026-09-07

CrazyGames is the first candidate because Basic Launch can test engagement without SDK integration. It does not generate revenue. Full Launch is selective and requires SDK integration and QA. Only platform-served ads are allowed; Android AdMob does not carry over. A submission or commercial agreement has NOT been made.

Official sources:
- https://docs.crazygames.com/requirements/intro/
- https://docs.crazygames.com/resources/basic-launch-metrics/
- https://developers.poki.com/guide/working-with-poki

Before submission: finish original obstacle artwork, verify actual mobile/desktop gameplay and frog rendering, prepare required covers, confirm developer account/payment eligibility and asset rights, review current platform terms, and upload for initial review. Basic Launch metrics determine whether to invest in full SDK work. No acceptance, traffic, or revenue is guaranteed.

Known difference: the prototype uses a fixed 432×768 world scaled to the viewport so all browser sizes share physics. Android uses measured layout dimensions. Random number sequences differ between Kotlin and JavaScript. Node tests validate logic only; browser visual/device QA remains outstanding.
