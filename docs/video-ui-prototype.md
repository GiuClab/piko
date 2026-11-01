# Experimental vertical video controls

Current bundle: **3.11.0-video-ui.14**, based on upstream Piko `v3.11.0-dev.3`.
Target: X `12.19.1-release.0` (`312191000`), portrait immersive video player.

[Build and downloadable patch bundle](https://github.com/gcapurro89/piko/actions/runs/41285286715)

## Host-child targeting correction (version 14)

Version 13 was reported to remain on the right. Its runtime code required the
hooked InlineActionBar to be a direct child of the new FrameLayout. That requirement
is removed: find the host within portrait chrome and change gravity on its direct
XML child, which may itself contain the hooked native bar. Right remains the default;
the saved preference and gesture implementation are unchanged. Compilation and
existing tests passed, but this nesting correction requires device verification.

## Previous rail host correction (version 13)

Version 12 was reported to leave the rail invisible when Left was selected. The
translation-based approach is removed. A full-width FrameLayout is now constrained
in the portrait chrome; the original native rail remains inside it with its ID,
listeners and bindings. Its real FrameLayout gravity is LEFT/TOP or RIGHT/TOP,
using the same saved preference (Right by default). Existing sibling constraints
target the host, and playback controls and gestures are unchanged.

Compilation and existing tests passed; APK resource application and actual visibility
and touch behavior on both sides require device verification for this build.

## Previous position attempt (version 12, superseded)

The maintainer reported version 11 remaining on the right after selecting Left.
Positioning now runs from the action rail's layout hook and before the separate
TweetView visibility guard in the pre-draw policy. The target edge is calculated
against the portrait chrome rather than the immediate parent, and intermediate
wrappers do not clip the translated rail. Preference key and gesture code are
unchanged. Compilation and existing tests passed; both sides still need phone
verification for this correction.

## Action column position (introduced in version 11)

Only the column position is changed in this iteration. The default is now physical
**Right**, independent of UI language. In X, open **Piko > Customisation > Video
action buttons position** and select **Right** or **Left**. In Italian, the existing
section is named **Customizzazione**, with **Posizione dei pulsanti video**.

The choice uses Piko's persisted settings and applies when the portrait player
is drawn again, including reopening it. It moves the complete action rail and its
native touch bounds together. Playback controls and gesture handling are unchanged.
The option is exposed only when Experimental vertical video controls is patched.

Compilation and existing automated tests passed; selecting both sides, persistence
after restart and touch targeting still require phone verification for version 11.
The published beta 1 remains version 10 until the new option is device-verified.

## Device feedback

After version 10, the maintainer reported the player working on the development phone. This supersedes the earlier pending confirmation for vertical switching, but is not broad device/account validation. The previously reported follow-state diagnostic remains documented for recurrence reports.

## Current behavior

- Filled white heart, comment bubble, bookmark and share arrow, styled for the
  vertical video rail. Native checked state makes the heart pink/red and a saved
  bookmark yellow. Repost and download remain available as X actions.
- The author's already loaded native profile image is mirrored at the top of the
  rail, with a pink Follow badge. Tapping the photo invokes the native profile
  listener; the badge and Follow label keep the verified follow-only action.
- The rail is left-aligned, 56dp wide with no start margin; icon sizes remain unchanged.
- Native play/pause occupies a 48dp cell on the left of the progress row. The
  progress widget fills the remaining width with a 12dp right inset; duration and
  scrubbing remain native. The runtime policy centers play/pause against the
  actual native AbsSeekBar track rather than its time-label wrapper, clears track
  horizontal padding and fits it to its immediate parent. Unknown track types
  retain native behavior; device confirmation is required.
- The lower post menu is proxied in the left-hand rail below Download. It invokes
  the current video's original bound menu listener; the footer original is transparent.
- The footer shows the profile display name only. The post description and handle
  are hidden in this portrait player, not removed from the post's underlying data.
- A portrait-only header copy preserves all native binding IDs. The transparent,
  one-pixel handle view remains available to the follow identity check. The original
  footer avatar remains bound and sized but transparent, supplying the image and
  original profile listener for the left-hand avatar.
- A lifecycle-scoped pre-draw policy reasserts hidden descriptions after recycled
  views are rebound. It also restores portrait controls after native auto-hide,
  scoped to the visible TweetView, and never cancels a frame in onPreDraw.
  It stores weak references and removes its listener on detach.
- Single-tap play/pause, double-tap likes, native counts, downloads and follow status
  retain the previous iteration's behavior. Native action listeners are not replaced
  by the icon changes. The changes apply to the portrait immersive player only.

## Horizontal seeking

A video-surface DOWN immediately reserves interception. Tentative movement is not
forwarded to X's dismiss listener. Vertical-dominant motion releases interception and hands native paging a complete
DOWN/MOVE sequence in activity-content coordinates. Replay bypasses patch gesture
recognition; subsequent events remain native until the next DOWN. The original
second-touch DOWN is retained as well. Horizontal motion remains owned until UP/CANCEL even if
the scrubber is unavailable. The injected native CANCEL path reasserts ownership
after cancellation callbacks that could release it. Controls, including the top
back arrow, bypass surface reservation. Android system back gestures are outside
this app-level gesture change.

A one-finger horizontal drag starting on the video surface dispatches native
AbsSeekBar touch callbacks. One video width corresponds to one full duration;
positions are clamped to the start/end. Vertical-dominant motion stays native for
video paging. Control-originated gestures, multi-touch and cancelled gestures do
not start seeking. Double-tap likes and delayed single-tap playback remain enabled.
The gesture requires a native AbsSeekBar; unsupported track types retain X behavior.
Direction thresholds and progress/clamping arithmetic are covered by JVM tests;
actual touch routing requires device testing.

## Follow behavior

The control reads the activity-scoped native friendship cache, with a fallback to
that account's cached author record. It verifies that the native author ID and
screen name agree with the header currently shown in this video.

It displays `Segui`, `Segui già`, `Richiesta`, `Bloccato`, `Tu`, or `Stato…`. A cache
miss or unknown native flag is never treated as proof that a profile is unfollowed.
The control is disabled until a known eligible state is available. Even a disabled
status tile absorbs touches instead of triggering video gestures underneath it.

The click rechecks the account, author and relationship. Confirmation is polled
every 100ms, and completion refreshes the initiating tile immediately rather than
waiting for its one-second refresh. Network latency remains unchanged. It queues X's native
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
  likes and follow. Version 9 was reported to block vertical paging on the phone. This iteration
  restores a complete native gesture handoff for vertical movement. Device
  verification remains required; successful compilation does not verify routing.
- Gesture, horizontal-seek math and relationship-state tests passed in GitHub Actions for this version.
- Java extensions and Kotlin patches compiled successfully; the `.mpp` artifact was
  generated. The final tested source is commit `3356c987418212b811091c02aa53a55f5ba9c9b4`.
- APK application/rebuilding was not repeated for this version because the original
  APKM is no longer present in the current workspace. The preceding version's
  successful rebuild does not establish this version's resource compatibility.
- Device QA is required: filled/checked icons, avatar after scrolling, photo versus
  badge taps, seeking beside play/pause, controls remaining visible after waiting,
  post-menu placement and target after swiping, and name-only footer after rebinding.

## Aggiornamento sul telefono

1. Scarica `piko-patch-bundle-18` dalla compilazione collegata sopra ed estrai
   `patches-3.11.0-video-ui.14.mpp`.
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
