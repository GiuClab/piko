# v14-based media rebuild

Branch: `fix/v14-media-rebuild`.
Version: `3.11.0-video-ui.16-dev.1`.
Base: the exact v14 compiled source, `3356c987418212b811091c02aa53a55f5ba9c9b4`.

## Why rebuild

The maintainer reported that v15-dev.2 crashed at launch. A crash stack trace was not available, so the precise cause is unconfirmed. Successful compilation/APK rebuilding did not prove Android startup or runtime verification.

## Changes from v14

- Remove the visible follow caption, retaining avatar, badge, native action and accessibility state.
- Add separate opt-in settings under Piko > Customisation for continuous source media and in-player comments. Both are OFF by default. Enable and test individually; restart X after changing the gallery setting.
- Retain the source gallery bridge and native conversation overlay as experimental integrations.

## Startup precautions

- ImmersiveVideoUi.enable remains the v14 boolean-only initializer. It does not load media modules or access context.
- No shared constructor is intercepted. Source registration runs after the ordinary source-loaded callback, behind an opt-in check.
- Opt-in checks use the caller's real Android Context and SharedPreferences, not an early shared Piko context singleton.
- The optional module's Handler is lazy. New functionality is not initialized unless explicitly enabled.
- SafeMediaHooks catches RuntimeException/LinkageError and preserves the original native gallery list when integration fails.
- The v15 base/grandparent-constructor factories are removed. Restored subtype constructors call their direct superclass, with constructor access flags and ordinary instance register layouts.
- No onBackPressed override is injected into X's activity. Comments use a view key listener and, where available, the Android platform back callback.

## Behavior and limits

Continuous gallery is entered from photos in the Home timeline or a profile Media tab. It preserves the matched source's ordering and uses X's native gallery rendering and horizontal paging. The existing immersive video feed remains separate. Native source identity, account and exact media ID are checked; unsupported sources retain X's original list.

Comments use X's native conversation fragment in a panel on the player activity. The focal post can appear above replies. Device verification is required for fragment integration, reply scrolling, back gestures and lifecycle handling.

## Checks

OptionalMediaGuardTest covers failed class initialization, subsequent NoClassDefFoundError, VerifyError, runtime failures, a failing error reporter and continued native fallback. MediaSequenceTest covers item identity/order, pagination duplicates and non-mutation of caller-owned lists. Existing gesture and relationship tests remain in the build.

Compilation and device verification must be reported separately. The original APKM is no longer present in the current scratch workspace, so a new APK application/rebuild cannot be claimed for this iteration. Earlier v15 rebuild results do not validate this version. No crash-free guarantee is made.
