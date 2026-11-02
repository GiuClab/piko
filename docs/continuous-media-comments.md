# Continuous media and in-player comments

Work branch: `feature/continuous-media-comments`.
Build: `3.11.0-video-ui.15-dev.2` (development, not a public release).
Target: X `12.19.1-release.0` (`312191000`).

## Implemented

- The follow control has no visible text caption. Avatar, badge, native follow-only request, identity checks and accessibility descriptions remain.
- Photo-gallery entry from HomeTimelineFragment or ProfileMediaTimelineFragment can receive the matching source's mixed photo/video items, in source order. X's own gallery adapter and media request builders render them. This uses native horizontal gallery paging; immersive video playback remains its existing separate player.
- A source is accepted only when its visible fragment belongs to the previous activity, the account matches, and its native item provider contains the opened post. No recommendations are substituted. Source pagination requests use its existing bottom-page event (4).
- The immersive player's comment action mounts TweetDetailTimelineFragment in a bottom overlay on the same activity. It does not launch TweetDetailActivity. X's full conversation component may include the focal post as well as replies.
- Close button, outside tap, activity pause and the player's legacy Back handler remove the panel. Android predictive-back routing still requires device testing.

## APK analysis and validation

Analyzed the original APKM supplied by the maintainer. Native gallery controller:
`com.twitter.app.gallery.c0.I3(List, boolean)`, page callback `gallery.l0.c(int)`.
Source controller: `app.legacy.list.a0`, native provider `v.X1()` with `a()` and `getItem(int)`.
Conversation arguments match `tweetdetail.i0`: account-scoped builder plus contextual tweet and tweet ID.
R8 removed trivial subtype constructors; injected dex factories mirror the native base-constructor allocation pattern.

Compilation, patch application and device verification are distinct checks. Record actual results below before publishing. Do not claim runtime behavior was verified in the container.

## Required phone checks

- Open photos from Home timeline and a profile Media tab; page across different posts and mixed media, including multi-media posts and additional source pages.
- Verify each item's author, actions and native media controls match its actual post.
- Open and close comments on several videos, scroll replies, switch accounts, rotate, background/resume and test Back/predictive Back.
- Verify the panel preserves the current video and time, and does not navigate to the full post.
- Confirm follow has no caption and still opens the profile/follows only the correct eligible author.
- Recheck horizontal seeking, vertical video switching and both rail-side settings.

The existing public beta is unchanged. No APK, account credential or original app source is committed to this repository.
