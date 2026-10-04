# TapToFlip 1.3 (version code 7)

- Slower beginner physics: jump impulse 510 (previously 690), gravity 980 (previously 1750), and initial scroll speed 170 (previously 245). Score-based difficulty still increases progressively.
- Interstitials are eligible after three completed rounds, with at least 45 seconds between actual impressions. Missing ads do not reset eligibility. Ads appear only at game over; loading callbacks never launch an ad during gameplay.
- Retry failed ad requests with a 30-second to 4-minute backoff. Expired cached ads are discarded after one hour. Debug builds use Google's test interstitial/banner IDs; release builds retain the existing AdMob IDs.
- Remove the share action from game-over UI, avoiding accidental launch during repeated gameplay taps.
- Simplify gameplay HUD to score, personal best, menu, pause, and a daily-goal progress bar. Keep the goal label on one line to avoid resizing gameplay when text changes.

## Validation

26 unit tests passed, including beginner reaction time, initial speed, first interstitial eligibility, missing inventory, and impression cooldown. Debug build, release AAB, and lint completed successfully. AdaptiveLayoutTest and FrogRenderingTest also passed on Pixel 9/API 37 after restarting the emulator with 4 GB RAM. The emulator initially killed the app due to LOW_MEMORY; System UI also showed an ANR during screenshot capture, so final visual confirmation on a physical device is still needed. Real release ad availability also depends on the AdMob account and inventory.

## Next publication

Generate a fresh signed 1.3 AAB with the existing upload key. Version code 7 is greater than the published code 6. Test before uploading.

## Follow-up ad verification

- Updated Google Mobile Ads SDK from 24.5.0 to 25.5.0.
- Restored sharing from the home menu only; there is no share action during gameplay or game over.
- Pixel 9 persistent RAM increased from 2048 MB to 4096 MB, without wiping data.
- A dedicated Google test-ad integration check passed. The SDK reported Interstitial ready followed by Interstitial displayed after three simulated completed rounds. No renderer crash occurred in this run; real release ad fill remains dependent on AdMob.

## Rewarded continue

- Added an opt-in rewarded continue offer at game over, once per run. Earned rewards retain the score and grant a three-second collision shield. Early dismissal does not revive the frog.
- Normal interstitials are deferred until New run/Main menu is chosen; a rewarded-ad attempt suppresses the normal interstitial for that round. A rewarded impression resets the interstitial cooldown.
- Game-over actions wait 650 ms before becoming clickable, to avoid accidental taps carried over from gameplay.
- 28 unit tests and ContinueOfferTest passed. The reward offer was visually checked and Google rewarded test inventory opened successfully.
- Live rewarded ad unit OdulluGecis (ca-app-pub-5287725227601079/4442460771) is configured in app/src/main/res/values/ads.xml. AdMob reward settings: amount 1, item Reward. Debug uses Google test inventory; release uses the provided live unit. Reward is granted only after the SDK earned-reward callback. Live inventory delivery has not been verified by clicking real ads.

## Reminder frequency update

- Removed the home reminder switch. Reminders are scheduled automatically every 48 hours from setup or the previous notification; returning to the game does not restart this cadence.
- Android 13+ notification permission is requested once. Denials, disabled notifications and disabled channels are respected; users control notifications in Android settings.
- Daytime sending window is 10:00 through 19:59. Android battery scheduling may delay delivery. Existing unique work is updated to a two-day initial delay.
- Message text rotates between personal best and daily challenge. Opening the game clears the displayed reminder.
