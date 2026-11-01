# Piko Video UI

**An unofficial experimental Piko fork for the vertical video player in X on Android.**

Customizable video actions, double-tap likes and horizontal video seeking, with a simplified player layout.

[**Download latest experimental build (.14)**](https://github.com/GiuClab/piko/actions/runs/41285286715/artifacts/13075405878) · [Installation](#installation) · [Known issues](#status-and-known-issues) · [Source code](https://github.com/GiuClab/piko/tree/experimental/video-ui)

## Features

- Video actions: author avatar, follow status, likes, comments, reposts, bookmarks, sharing, downloads and post menu.
- Experimental left/right column selection in **Piko > Customisation > Video action buttons position** (Right by default; latest correction still needs device confirmation).
- Tap the avatar to open the profile, or use the follow badge without leaving the player.
- Single-tap play/pause; double-tap to like without removing an existing like.
- Horizontal dragging to seek; vertical swiping to switch videos.
- Playback controls that remain visible, with play/pause aligned beside the progress track.
- A profile-name footer without the post description or visible handle.

Changes apply to the portrait immersive player. This is a patch bundle, not a standalone app or an installable X APK.

## Compatibility

| Component | Version / status |
| --- | --- |
| X for Android | `12.19.1-release.0` (`312191000`) |
| Package | `com.twitter.android` |
| Latest experimental bundle | `3.11.0-video-ui.14` |
| Upstream base | Piko `3.11.0-dev.3` |
| Custom patch | `Experimental vertical video controls` |

Newer X versions are not verified. Both upstream patches and this fork's video changes need verification before updating.

## Installation

The latest experimental bundle is **3.11.0-video-ui.14**. The existing **beta 1 release still contains version 10**; these are separate downloads. The experimental Actions artifact requires GitHub sign-in and is retained until November 15, 2026. If it expires, rebuild from the development branch.

1. Download the [latest experimental ZIP](https://github.com/GiuClab/piko/actions/runs/41285286715/artifacts/13075405878) and extract `patches-3.11.0-video-ui.14.mpp`.
2. In Morphe Manager, import the file as a local patch source. Existing users can update the local source previously named **Piko mio**.
3. Use this source alone for X. It already contains the base Piko patches: do not combine it with another Piko source.
4. Select the original APKM for X `12.19.1-release.0`. Enable **Experimental vertical video controls** and your other preferred compatible patches.
5. Apply the patches and install the resulting APK. Updating an existing patched installation requires the same signing key.

The `.mpp` cannot be installed directly as an Android app. If a signature conflict occurs, preserve your data and existing signing setup before changing installations.

[Upstream Piko](https://github.com/crimera/piko) · [Morphe](https://morphe.software)

## Status and known issues

**Experimental development builds.** Version 10's player behavior was reported working on the development phone. Version 14 passed compilation and the existing automated tests, but its left/right column selection correction still needs device confirmation. Wider device and account compatibility is not established.

- Earlier builds sometimes displayed `VideoFollowControl: Video follow state unavailable…`. Its cause has not been conclusively diagnosed. Please report it if it returns.
- Follow state may be unavailable when author/account identity cannot be verified. A follow is confirmed only after X responds; network latency still applies.
- Version 9 blocked vertical switching; version 10 addressed it.
- Versions 11–13 had reported problems applying the left-side selection. Version 14 changes which layout container receives the setting; this correction is not yet confirmed on the phone.
- Android system back gestures are separate from app-level swipe-to-dismiss handling.
- Newer X versions are not supported by this release.

## Feedback

Use this fork's **Issues** tab when available for bugs specific to these changes, rather than upstream Piko's tracker. Include device, Android, X, Morphe and bundle versions, plus reproduction steps. See [SUPPORT.md](SUPPORT.md).

Never upload login tokens, passwords, signing keys or unredacted account credentials.

## Development

The custom source lives on [`experimental/video-ui`](https://github.com/GiuClab/piko/tree/experimental/video-ui). The default branch provides this landing page and support templates.

- [Implementation and verification notes](https://github.com/GiuClab/piko/blob/experimental/video-ui/docs/video-ui-prototype.md)
- [Build workflow](https://github.com/GiuClab/piko/blob/experimental/video-ui/.github/workflows/build-video-ui.yml)
- [Builds](https://github.com/GiuClab/piko/actions/workflows/build-video-ui.yml)

## Credits and license

Based on [Piko by crimera and contributors](https://github.com/crimera/piko). Independently maintained by [GiuClab](https://github.com/GiuClab).

Not affiliated with or endorsed by X, TikTok or the upstream Piko project.

The upstream project identifies its code as GPL-3.0-or-later. Preserve the original license, notices and attribution when redistributing. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
