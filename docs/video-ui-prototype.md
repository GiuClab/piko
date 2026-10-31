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

## Validation performed, 2026-10-31

Source commit: `560f5f898e3c06dd72fb6775f147f8d68d449055`.
[Successful build and downloadable bundle](https://github.com/gcapurro89/piko/actions/runs/41188348289).

- Standalone gesture cases passed, including post changes, swipes, long presses,
  cancelled gestures and recognition at second DOWN before native double-tap seeking.
- Morphe Desktop 1.20.0 applied all 72 default Piko patches plus this patch to the
  supplied original X `12.19.1-release.0` APKM: 73 applied, zero failed.
- Full DEX and resource rebuilding succeeded. The resulting diagnostic APK is
  unsigned; it is not the installation deliverable.
- Single video taps are deferred for the platform double-tap timeout. A completed
  double tap suppresses the native tap gesture and invokes the native like view.
- Device behavior remains unverified. Compilation and APK rebuilding do not
  establish correct visual layout, playback or live account behavior.

## Prima prova sul telefono

1. Scarica `piko-patch-bundle-2` dalla compilazione collegata sopra ed estrai il `.mpp`.
2. In Morphe apri **Fonti → + → Locale** e importa il file.
3. Per X usa questa fonte locale: contiene anche le patch originali di Piko.
4. In modalità esperto seleziona **Experimental vertical video controls**, oltre
   alle patch abituali, e applicale all'APKM originale della versione corrispondente.
5. Usa il Morphe già presente sul telefono per firmare l'aggiornamento. Se
   l'installazione richiede di disinstallare X, fermati e verifica prima la firma.
6. Prova disposizione dei comandi, scorrimento, tocco singolo e doppio tocco.
   Un video già apprezzato deve restare tale dopo altri doppi tocchi.

Il pulsante Segui e l'avatar nella colonna destra non sono ancora implementati.
