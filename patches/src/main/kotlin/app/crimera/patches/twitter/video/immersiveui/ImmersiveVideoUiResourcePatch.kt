package app.crimera.patches.twitter.video.immersiveui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.findElementByAttributeValueOrThrow
import org.w3c.dom.Element

internal val immersiveVideoUiResourcePatch = resourcePatch {
    execute {
        document("res/layout/immersive_media_player_chrome_portrait.xml").use { editor ->
            fun node(id: String) = editor.childNodes.findElementByAttributeValueOrThrow("android:id", "@id/$id") as Element
            val actions = node("tweet_inline_actions")
            actions.setAttribute("android:layout_width", "72dp")
            actions.setAttribute("android:layout_height", "wrap_content")
            actions.setAttribute("android:layout_marginHorizontal", "0dp")
            actions.setAttribute("android:layout_marginEnd", "4dp")
            actions.setAttribute("android:layout_marginBottom", "64dp")
            actions.removeAttribute("app:layout_constraintStart_toStartOf")
            actions.setAttribute("app:layout_constraintEnd_toEndOf", "parent")
            actions.setAttribute("app:layout_constraintBottom_toBottomOf", "parent")

            val text = node("tweet_content_text")
            text.removeAttribute("app:layout_constraintBottom_toTopOf")
            text.setAttribute("app:layout_constraintBottom_toBottomOf", "parent")
            text.setAttribute("android:layout_marginBottom", "16dp")
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
