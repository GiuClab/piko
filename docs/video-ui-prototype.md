# Experimental vertical video controls

Current bundle: **3.11.0-video-ui.4**, based on upstream Piko `v3.11.0-dev.3`.
Target: X `12.19.1-release.0` (`312191000`), portrait immersive video player.

[Build and downloadable patch bundle](https://github.com/gcapurro89/piko/actions/runs/41192271872)

## Current behavior

- A single video tap confirms through X's actual `video_play_toggle` button after
  the double-tap timeout. Replaying a native surface tap did not pause video on
  device, so the implementation now invokes the native playback action directly.
- A double tap invokes the bound native like view. Already-liked posts stay liked;
  moves, long presses, multiple pointers and post changes cancel the gesture.
- Main icons are 34dp with counts underneath in 64dp cells. Like is first among
  the social actions. Native listeners, checked state and count views are retained.
- The action column ends above the playback toolbar. Speed, mute and PiP are
  grouped on the left below it, rather than overlapping share and download.
- A person/plus Follow control appears above the social actions. The original
  author's avatar remains in the footer header; it has not been moved to the rail.

## Follow behavior

The control reads the activity-scoped native friendship cache, with a fallback to
that account's cached author record. It verifies that the native author ID and
screen name agree with the header currently shown in this video.

It displays `Segui`, `Segui già`, `Richiesta`, `Bloccato`, `Tu`, or `Stato…`. A cache
miss or unknown native flag is never treated as proof that a profile is unfollowed.
The control is disabled until a known eligible state is available. Even a disabled
status tile absorbs touches instead of triggering video gestures underneath it.

The click rechecks the account, author and relationship. It queues X's native
**follow-only** request; it does not use the native follow/unfollow toggle action.
`Invio…` is shown while waiting. A followed state is published into the native cache
only after the request supplies a successful matching user response. Protected
profile requests remain `Richiesta`; native errors and restrictions remain handled
by X. There is no automatic account action when opening or scrolling videos.

This runtime account behavior has not been exercised in the container. Device
verification is required, including known-followed, not-followed, protected,
blocked and own profiles, reposts and account switching. No real follow request
was sent by the development or APK verification tools.

## Verification

- Gesture bookkeeping tests passed, including rejection at UP without a preceding
  MOVE event and recognition at the second DOWN before native seeking can run.
- Relationship tests passed: unknown, followed-by-only, already followed, pending,
  blocked and own-account states are distinguished.
- Java extensions and Kotlin patches compiled successfully on GitHub Actions.
- Morphe Desktop applied 73 patches to the original matching APKM with no failed
  patches and rebuilt the full DEX/resources successfully. This unsigned diagnostic
  APK is not an installation deliverable; patching on device retains the existing
  Morphe signing key.
- This is still an experimental layout. Visual behavior, real playback and account
  actions must be checked on device. The first device test confirmed the double
  tap, but exposed the single-tap and horizontal-icon-layout issues fixed here.

## Aggiornamento sul telefono

1. Scarica `piko-patch-bundle-4` dalla compilazione collegata sopra ed estrai
   `patches-3.11.0-video-ui.4.mpp`.
2. In Morphe apri la scheda della fonte locale **Piko mio**, premi **Aggiorna** e
   seleziona il nuovo file. Mantieni disabilitata la fonte Piko originale per X.
3. Lascia selezionata **Experimental vertical video controls**, insieme alle patch
   abituali, e applicale all'APKM originale della versione corrispondente. Il
   pacchetto contiene anche le patch originali: non combinarlo con una seconda Piko.
4. Installa come aggiornamento usando il Morphe già presente sul telefono e la
   stessa chiave di firma. Il file `.mpp` è un bundle di patch, non un APK installabile.
5. Controlla tocco singolo, doppio tocco, scorrimento, spazio fra i comandi e stato
   su un profilo che segui già. Prova Segui soltanto su un profilo che vuoi seguire.

The `Build video UI` workflow uploads `.mpp` artifacts; the upstream `Release`
workflow is not required. Original X APKMs, signed X APKs and signing keys are not
stored in this public repository.
