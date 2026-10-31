# Piko Video UI — Public testing beta 1

An unofficial Piko fork with a redesigned portrait video player for X on Android.

## Included

Left-side actions, author avatar and follow control; single-tap play/pause; double-tap likes; horizontal seeking; vertical switching; persistent playback controls and a simplified profile-name footer.

## Compatibility and verification

- X: **12.19.1-release.0** (`312191000`).
- Bundle: **3.11.0-video-ui.10**, based on Piko **3.11.0-dev.3**.
- Built source: `743b4b69a3338c999a2c41068f078bb61d33b9b7`.
- Automated tests and compilation passed in run `41244490449`.
- The maintainer reports the latest behavior working on the development phone. Wider device/account coverage is still needed.

## Install

Download `patches-3.11.0-video-ui.10.mpp`, import it as a local source in Morphe Manager, and enable **Experimental vertical video controls**. Use this Piko source alone for X and patch the original APKM of the supported version.

Existing users can update the source previously called **Piko mio**. Use the same signing key when updating an existing patched installation. The `.mpp` is a patch bundle, not an Android APK.

## Known issues

Earlier builds occasionally displayed `Video follow state unavailable`. Its cause has not been conclusively diagnosed; report it if it returns. Follow confirmation depends on X's response. Other X versions are not verified. Android system back gestures are separate from app-level swipe handling.

Version 10 addresses version 9's vertical-paging regression. Please test vertical switching and horizontal seeking.

## Feedback and credits

Use this fork's Issues tab when available. Include device, Android, X, Morphe and bundle versions plus reproduction steps. Remove credentials from logs.

Based on [Piko by crimera and contributors](https://github.com/crimera/piko). Independently maintained fork; not affiliated with X, TikTok or upstream Piko. LICENSE and NOTICE are included alongside the patch bundle.
