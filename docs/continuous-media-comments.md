# Continuous media and in-player comments

Work branch: `feature/continuous-media-comments`.
Status: partial implementation, not a public release.

## Requested behavior

- Open a photo or video from the timeline and browse adjacent photo/video media without returning to the feed.
- Do the same from a profile's Media section, preserving that source's ordering.
- Open comments in a bottom sheet over the video instead of navigating to the post screen.
- Remove the visible follow caption; retain the avatar, state badge and accessible state description.

## Implemented in this branch

Removed the follow TextView and its layout spacing. Avatar, follow badge, click handlers,
identity checks, native follow-only request and accessibility descriptions are retained.
This change does not add the comments sheet or continuous-media navigation.

## Blocked pending original X APKM

Target: com.twitter.android 12.19.1-release.0 (312191000).
The original APKM is not present in this workspace. It is required to inspect and validate:

1. Timeline and profile-media list models, their selection callbacks, item identities and pagination.
2. Photo/video viewer entry points and the selected media index within a multi-media post.
3. The native comments component, its activity/fragment dependencies, reply pagination and post binding.

No native method names or undocumented HTTP endpoints should be guessed to implement these features.
A link to the web post or opening the existing post activity does not satisfy the requested comments sheet.

## Integration requirements

Keep the source list (timeline versus profile media) and order stable while browsing.
Reuse native data and pagination; do not substitute recommendations for missing source items.
Preserve media IDs, per-post media ordering and the correct author/post for every action.
Preserve playback position and user-selected playback state when opening and dismissing comments.
Avoid duplicate fetches or actions after views recycle; discard stale source/account responses.
Retain photo zoom, horizontal video seeking, vertical navigation, and both column-side settings.
Do not replace the published beta until device verification.

## Device checks once implemented

Test both entry sources with photo-only, video-only and mixed posts, multiple media within one post,
end-of-list pagination, back navigation, comments open/close, keyboard handling, account switching,
and action targeting after repeated navigation. Confirm that the video does not navigate away when
comments open and that follow has no visible caption but remains accessible.
