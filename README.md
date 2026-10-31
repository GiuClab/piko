# Piko Video UI

**An unofficial experimental Piko fork for the vertical video player in X on Android.**

Left-side actions, double-tap likes and horizontal video seeking, with a simplified player layout.

[**Download the public beta**](https://github.com/gcapurro89/piko/releases/tag/video-ui-beta.1) · [Installation](#installation) · [Known issues](#status-and-known-issues) · [Source code](https://github.com/gcapurro89/piko/tree/experimental/video-ui)

## Features

- Left-side video actions: author avatar, follow status, likes, comments, reposts, bookmarks, sharing, downloads and post menu.
- Tap the avatar to open the profile, or use the follow badge without leaving the player.
- Single-tap play/pause; double-tap to like without removing an existing like.
- Horizontal dragging to seek; vertical swiping to switch videos.
- Playback controls that remain visible, with play/pause aligned beside the progress track.
- A profile-name footer without the post description or visible handle.

Changes apply to the portrait immersive player. This is a patch bundle, not a standalone app or an installable X APK.

## Compatibility

| Component | Public beta target |
| --- | --- |
| X for Android | `12.19.1-release.0` (`312191000`) |
| Package | `com.twitter.android` |
| Bundle | `3.11.0-video-ui.10` |
| Upstream base | Piko `3.11.0-dev.3` |
| Custom patch | `Experimental vertical video controls` |

Newer X versions are not verified. Both upstream patches and this fork's video changes need verification before updating.

## Installation

1. Download `patches-3.11.0-video-ui.10.mpp` from the [beta release](https://github.com/gcapurro89/piko/releases/tag/video-ui-beta.1).
2. In Morphe Manager, import the file as a local patch source. Existing users can update the local source previously named **Piko mio**.
3. Use this source alone for X. It already contains the base Piko patches: do not combine it with another Piko source.
4. Select the original APKM for X `12.19.1-release.0`. Enable **Experimental vertical video controls** and your other preferred compatible patches.
5. Apply the patches and install the resulting APK. Updating an existing patched installation requires the same signing key.

The `.mpp` cannot be installed directly as an Android app. If a signature conflict occurs, preserve your data and existing signing setup before changing installations.

[Upstream Piko](https://github.com/crimera/piko) · [Morphe](https://morphe.software)

## Status and known issues

**Public testing beta.** The maintainer reports the latest player behavior working on the development phone. Automated gesture/state tests and compilation passed. Wider device and account compatibility is not established.

- Earlier builds sometimes displayed `VideoFollowControl: Video follow state unavailable…`. Its cause has not been conclusively diagnosed. Please report it if it returns.
- Follow state may be unavailable when author/account identity cannot be verified. A follow is confirmed only after X responds; network latency still applies.
- Version 9 blocked vertical switching. Version 10 contains the correction and is distributed here.
- Android system back gestures are separate from app-level swipe-to-dismiss handling.
- Newer X versions are not supported by this release.

## Feedback

Use this fork's **Issues** tab when available for bugs specific to these changes, rather than upstream Piko's tracker. Include device, Android, X, Morphe and bundle versions, plus reproduction steps. See [SUPPORT.md](SUPPORT.md).

Never upload login tokens, passwords, signing keys or unredacted account credentials.

## Development

The custom source lives on [`experimental/video-ui`](https://github.com/gcapurro89/piko/tree/experimental/video-ui). The default branch provides this landing page and support templates.

- [Implementation and verification notes](https://github.com/gcapurro89/piko/blob/experimental/video-ui/docs/video-ui-prototype.md)
- [Build workflow](https://github.com/gcapurro89/piko/blob/experimental/video-ui/.github/workflows/build-video-ui.yml)
- [Builds](https://github.com/gcapurro89/piko/actions/workflows/build-video-ui.yml)

## Credits and license

Based on [Piko by crimera and contributors](https://github.com/crimera/piko). Independently maintained by [gcapurro89](https://github.com/gcapurro89).

Not affiliated with or endorsed by X, TikTok or the upstream Piko project.

The upstream project identifies its code as GPL-3.0-or-later. Preserve the original license, notices and attribution when redistributing. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
