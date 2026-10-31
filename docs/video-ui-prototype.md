# Experimental vertical video controls

Base: Piko `v3.11.0-dev.3`, X `12.19.1-release.0` (`312191000`).

This opt-in patch changes only the portrait immersive video player. It keeps native
X action views, their click listeners and the currently bound tweet. It moves the
action bar to the right and adds double-tap-to-like with a heart overlay. A double
tap never intentionally removes a like. Native downloader support is retained.

This is the first prototype, not the completed TikTok-style interface. The avatar
remains in the original header; the direct Follow button and followed status are
not implemented. Counts still use X's original action-view layout.

## Build

The `Build video UI` workflow runs on pushes to `experimental/video-ui` and uploads
`patches/build/libs/*.mpp`. It first runs the standalone gesture tests. The output
is a **patch bundle**, not an installable X APK. The upstream `Release` workflow is
not needed. No signing keys, login tokens or original X APKs are committed here.

Select `Experimental vertical video controls` explicitly when applying this
bundle with Morphe to the original matching X APKM. The patch is disabled by
default. Keep other desired Piko patches selected. The immersive player must be
enabled in Piko settings. Portrait mode is the only changed orientation.

## Required verification before calling this usable

- Build the Java extensions and Kotlin patches.
- Apply the bundle to the exact X APKM locally; do not upload the original APKM to
  this public repository.
- Check the resulting DEX and resource output, including hook register counts.
- On device: advance through several videos and verify every action targets the
  visible tweet, including reposts and videos with multiple media items.
- Check counts, long labels, small screens, large font settings and rotation.
- Check single tap, double tap, repeated double tap, drag, long press, multi-touch,
  playback controls and scrubbing; already-liked videos must stay liked.
- Verify native downloads, bookmarks, replies, reposts and sharing still work.

A successful source build does not establish successful APK patching or on-device
behavior. Those checks must be reported separately.
