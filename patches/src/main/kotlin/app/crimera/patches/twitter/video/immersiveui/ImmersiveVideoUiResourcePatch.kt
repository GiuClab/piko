package app.crimera.patches.twitter.video.immersiveui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.findElementByAttributeValueOrThrow
import org.w3c.dom.Element

internal val immersiveVideoUiResourcePatch = resourcePatch {
    execute {
        // Keep the standard header used by landscape and other screens untouched.
        document("res/layout/tweet_header_layout.xml").use { }
        get("res/layout/tweet_header_layout.xml").copyTo(
            get("res/layout").resolve("piko_video_header.xml"), overwrite = true,
        )
        document("res/layout/piko_video_header.xml").use { editor ->
            fun header(id: String) = editor.childNodes.findElementByAttributeValueOrThrow("android:id", "@id/$id") as Element
            for (id in listOf("text_separator", "promoted_badge", "tweet_edit_badge")) {
                header(id).setAttribute("android:visibility", "gone")
            }
            header("tweet_profile_image_container").apply {
                setAttribute("android:alpha", "0")
                setAttribute("android:layout_width", "48dp")
                setAttribute("android:layout_height", "48dp")
                removeAttribute("app:layout_constraintEnd_toStartOf")
                removeAttribute("app:layout_constraintBottom_toBottomOf")
                setAttribute("app:layout_constraintTop_toTopOf", "parent")
            }
            // Retain a bound, visible username view for native identity checks, without drawing it.
            header("username").apply {
                setAttribute("android:alpha", "0")
                setAttribute("android:layout_width", "1px")
                setAttribute("android:layout_height", "1px")
                setAttribute("android:importantForAccessibility", "no")
                removeAttribute("app:layout_constraintTop_toBottomOf")
                removeAttribute("app:layout_constraintBottom_toBottomOf")
                setAttribute("app:layout_constraintTop_toTopOf", "parent")
            }
            header("tweet_curation_action").apply {
                setAttribute("android:alpha", "0")
                setAttribute("android:layout_width", "1px")
                setAttribute("android:importantForAccessibility", "no")
            }
            header("name").apply {
                removeAttribute("app:layout_constraintStart_toEndOf")
                setAttribute("app:layout_constraintStart_toStartOf", "parent")
                setAttribute("android:layout_marginStart", "0dp")
                removeAttribute("app:layout_constraintBottom_toTopOf")
                setAttribute("app:layout_constraintBottom_toBottomOf", "parent")
                setAttribute("app:layout_constraintTop_toTopOf", "parent")
                setAttribute("app:layout_constraintVertical_bias", "0.5")
            }
        }
        document("res/layout/immersive_media_player_chrome_portrait.xml").use { editor ->
            fun node(id: String) = editor.childNodes.findElementByAttributeValueOrThrow("android:id", "@id/$id") as Element
            val actions = node("tweet_inline_actions")
            actions.setAttribute("android:layout_width", "56dp")
            actions.setAttribute("android:layout_height", "wrap_content")
            actions.setAttribute("android:layout_marginHorizontal", "0dp")
            actions.setAttribute("android:layout_marginEnd", "0dp")
            actions.setAttribute("android:layout_marginBottom", "16dp")
            actions.removeAttribute("app:layout_constraintStart_toStartOf")
            actions.setAttribute("app:layout_constraintEnd_toEndOf", "parent")
            actions.removeAttribute("app:layout_constraintBottom_toBottomOf")
            actions.setAttribute("app:layout_constraintBottom_toTopOf", "@id/playback_speed_button")

            // Keep the playback toolbar on the left, below the last social action.
            for ((id, previous) in listOf(
                "playback_speed_button" to null,
                "video_mute_toggle" to "playback_speed_button",
                "video_pip_button" to "video_mute_toggle",
            )) {
                node(id).apply {
                    removeAttribute("app:layout_constraintEnd_toStartOf")
                    removeAttribute("app:layout_constraintEnd_toEndOf")
                    if (previous == null) {
                        setAttribute("app:layout_constraintStart_toStartOf", "parent")
                        setAttribute("android:layout_marginStart", "12dp")
                    } else {
                        setAttribute("app:layout_constraintStart_toEndOf", "@id/$previous")
                    }
                }
            }

            node("tweet_header").setAttribute("layout", "@layout/piko_video_header")
            val seek = node("seekbar")
            seek.setAttribute("android:layout_width", "0dp")
            seek.removeAttribute("app:layout_constraintEnd_toStartOf")
            seek.removeAttribute("app:layout_constraintStart_toStartOf")
            seek.setAttribute("app:layout_constraintStart_toEndOf", "@id/video_play_toggle")
            seek.removeAttribute("app:layout_constraintEnd_toEndOf")
            seek.setAttribute("android:layout_marginStart", "0dp")
            seek.setAttribute("app:layout_constraintEnd_toEndOf", "parent")
            seek.setAttribute("android:layout_marginHorizontal", "0dp")
            seek.setAttribute("android:paddingHorizontal", "0dp")
            for (edge in listOf("Left", "Right", "Start", "End")) {
                seek.setAttribute("android:layout_margin$edge", "0dp")
                seek.setAttribute("android:padding$edge", "0dp")
            }
            // The native widget contains both its progress track and time label.
            node("video_play_toggle").apply {
                setAttribute("android:layout_width", "48dp")
                setAttribute("android:layout_height", "48dp")
                setAttribute("android:alpha", "1")
                setAttribute("android:importantForAccessibility", "auto")
                setAttribute("android:layout_marginStart", "0dp")
                removeAttribute("app:layout_constraintStart_toStartOf")
                setAttribute("app:layout_constraintStart_toStartOf", "parent")
            }
            node("tweet_inline_actions").setAttribute("android:importantForAccessibility", "auto")

            val text = node("tweet_content_text")
            text.removeAttribute("app:layout_constraintBottom_toTopOf")
            text.setAttribute("app:layout_constraintBottom_toBottomOf", "parent")
            text.setAttribute("android:layout_marginBottom", "0dp")
            text.setAttribute("android:visibility", "gone")
            text.setAttribute("android:layout_height", "0dp")
            for (id in listOf("tweet_content_text", "tweet_header")) {
                node(id).apply {
                    removeAttribute("app:layout_constraintEnd_toEndOf")
                    setAttribute("app:layout_constraintEnd_toStartOf", "@id/tweet_inline_actions")
                    setAttribute("android:layout_marginHorizontal", "12dp")
                }
            }
        }
    }
}
