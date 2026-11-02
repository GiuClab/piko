# v14-based media rebuild

Branch: `fix/v14-media-rebuild`.
Current development version: `3.11.0-video-ui.16-dev.3`.
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

## Build result (v16-dev.1)

Source: `b9f0fc5709bfc0b50ae07f12ffc851c89300418b`.
GitHub Actions: https://github.com/GiuClab/piko/actions/runs/41468985193
Compilation and the gesture, relationship, media-sequence and optional-initialization guard tests passed.
The build is a development candidate; Android startup and the optional features have not been verified on a device.

## Comments diagnostic (v16-dev.2)

The maintainer reached the player and reported the handled message `NativeComments: In-player comments unavailable`. This does not identify the underlying exception. Version 16-dev.2 adds operation-stage tracking and a dialog with the unwrapped root cause and stack trace. The user can explicitly copy the diagnostic with Copy details; no file permission or PC is needed. No functional comments fix is claimed by this diagnostic build.

Source: `94e9a1381c2643d9e584407729eb22c42ce3988b`.
Build and existing tests passed: https://github.com/GiuClab/piko/actions/runs/41532424524
Android runtime and the actual failure cause still require the user's diagnostic.

## Conversation argument fix (v16-dev.3)

A device diagnostic reached `build-native-args` and reported the native builder configuration invalid. The builder contained the account owner and timeline tag but lacked `timeline_arg_tweet_id`. Code had attempted build() before putting the required conversation fields into a separate result Bundle.

The fix inserts `timeline_arg_contextual_tweet`, `timeline_arg_tweet_id` and `arg_timeline_type` into the builder's Bundle BEFORE invoking its native build method. The timeline tag remains a separate field. No user/account/post identifiers from the diagnostic are committed here. The copyable diagnostic is retained for any subsequent integration error.

Source: `8d32d5f742a7c8d922801e9ef21006742935be2a`.
Build: https://github.com/GiuClab/piko/actions/runs/41534407905
Compilation and existing tests passed. The validation-order bug is corrected in code; full comments-panel behavior still requires device confirmation.
