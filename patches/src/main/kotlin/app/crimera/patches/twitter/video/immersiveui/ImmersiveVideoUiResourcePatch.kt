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
            // A full-width host gives the rail real left/right layout bounds and native hit targets.
            // Moving only translationX can be clipped by X's original narrow containers.
            val host = actions.ownerDocument.createElement("FrameLayout").apply {
                setAttribute("android:id", "@+id/piko_video_action_container")
                setAttribute("android:layout_width", "0dp")
                setAttribute("android:layout_height", "wrap_content")
                setAttribute("android:layout_marginBottom", "16dp")
                setAttribute("app:layout_constraintStart_toStartOf", "parent")
                setAttribute("app:layout_constraintEnd_toEndOf", "parent")
                setAttribute("app:layout_constraintBottom_toTopOf", "@id/playback_speed_button")
            }
            actions.parentNode.replaceChild(host, actions)
            host.appendChild(actions)
            // Preserve any sibling constraints that previously targeted the native rail.
            val elements = host.ownerDocument.getElementsByTagName("*")
            for (i in 0 until elements.length) {
                val element = elements.item(i) as Element
                for (j in 0 until element.attributes.length) {
                    val attribute = element.attributes.item(j)
                    if (attribute.nodeName.startsWith("app:layout_constraint") &&
                        attribute.nodeValue == "@id/tweet_inline_actions") {
                        attribute.nodeValue = "@id/piko_video_action_container"
                    }
                }
            }
            val obsolete = (0 until actions.attributes.length).map { actions.attributes.item(it).nodeName }
                .filter { it.startsWith("app:layout_constraint") || it.startsWith("app:layout_goneMargin") ||
                    it.startsWith("android:layout_margin") }
            obsolete.forEach { actions.removeAttribute(it) }
            actions.setAttribute("android:layout_width", "56dp")
            actions.setAttribute("android:layout_height", "wrap_content")
            actions.setAttribute("android:layout_gravity", "right|top")

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
                seek.setAttribute("android:layout_margin$edge", if (edge == "End" || edge == "Right") "12dp" else "0dp")
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
                    setAttribute("app:layout_constraintEnd_toEndOf", "parent")
                    setAttribute("android:layout_marginHorizontal", "12dp")
                }
            }
        }
    }
}
