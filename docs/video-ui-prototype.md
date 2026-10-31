# Experimental vertical video controls

Current bundle: **3.11.0-video-ui.7**, based on upstream Piko `v3.11.0-dev.3`.
Target: X `12.19.1-release.0` (`312191000`), portrait immersive video player.

[Build and downloadable patch bundle](https://github.com/gcapurro89/piko/actions/runs/41207995237)

## Current behavior

- Filled white heart, comment bubble, bookmark and share arrow, styled for the
  vertical video rail. Native checked state makes the heart pink/red and a saved
  bookmark yellow. Repost and download remain available as X actions.
- The author's already loaded native profile image is mirrored at the top of the
  rail, with a pink Follow badge. Tapping the photo invokes the native profile
  listener; the badge and Follow label keep the verified follow-only action.
- The rail is 56dp wide with no end margin; icon sizes remain unchanged.
- Native play/pause occupies a 48dp cell on the left of the progress row. The
  progress widget fills the remaining width to the right screen edge; duration and
  scrubbing remain native. The runtime policy centers play/pause against the
  actual native AbsSeekBar track rather than its time-label wrapper, clears track
  horizontal padding and fits it to its immediate parent. Unknown track types
  retain native behavior; device confirmation is required.
- The lower post menu is proxied in the right-hand rail below Download. It invokes
  the current video's original bound menu listener; the footer original is transparent.
- The footer shows the profile display name only. The post description and handle
  are hidden in this portrait player, not removed from the post's underlying data.
- A portrait-only header copy preserves all native binding IDs. The transparent,
  one-pixel handle view remains available to the follow identity check. The original
  footer avatar remains bound and sized but transparent, supplying the image and
  original profile listener for the right-hand avatar.
- A lifecycle-scoped pre-draw policy reasserts hidden descriptions after recycled
  views are rebound. It also restores portrait controls after native auto-hide,
  scoped to the visible TweetView, and never cancels a frame in onPreDraw.
  It stores weak references and removes its listener on detach.
- Single-tap play/pause, double-tap likes, native counts, downloads and follow status
  retain the previous iteration's behavior. Native action listeners are not replaced
  by the icon changes. The changes apply to the portrait immersive player only.

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

- The preceding version was confirmed on device to have working playback gestures,
  likes and follow. This iteration adjusts the rail end spacing, progress width and alignment
  against the actual progress track.
- Gesture and relationship-state tests passed in GitHub Actions for this version.
- Java extensions and Kotlin patches compiled successfully; the `.mpp` artifact was
  generated. The final tested source is commit `ebe2751bd708e9f497eacc3ef0e27d470af546d3`.
- APK application/rebuilding was not repeated for this version because the original
  APKM is no longer present in the current workspace. The preceding version's
  successful rebuild does not establish this version's resource compatibility.
- Device QA is required: filled/checked icons, avatar after scrolling, photo versus
  badge taps, seeking beside play/pause, controls remaining visible after waiting,
  post-menu placement and target after swiping, and name-only footer after rebinding.

## Aggiornamento sul telefono

1. Scarica `piko-patch-bundle-8` dalla compilazione collegata sopra ed estrai
   `patches-3.11.0-video-ui.7.mpp`.
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
