package app.crimera.patches.twitter.video.immersiveui

import app.crimera.patches.twitter.misc.extension.twitterInitHook
import app.crimera.patches.twitter.misc.settings.settingsPatch
import app.crimera.patches.twitter.utils.Constants.COMPATIBILITY_X
import app.crimera.patches.twitter.utils.Constants.PATCHES_DESCRIPTOR
import app.crimera.patches.twitter.utils.enableSettings
import app.crimera.utils.changeFirstString
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val BAR = "Lcom/twitter/ui/tweet/inlineactions/InlineActionBar;"
private const val ACTION = "Lcom/twitter/ui/tweet/inlineactions/InlineActionView;"
private const val TWEET_VIEW = "Lcom/twitter/tweetview/core/TweetView;"
private const val EXTENSION = "$PATCHES_DESCRIPTOR/ImmersiveVideoUi;"

private object MeasureFingerprint : Fingerprint(definingClass = BAR, name = "onMeasure", returnType = "V")
private object LayoutFingerprint : Fingerprint(definingClass = BAR, name = "onLayout", returnType = "V")
private object ActionMeasureFingerprint : Fingerprint(definingClass = ACTION, name = "onMeasure", returnType = "V")
private object ActionLayoutFingerprint : Fingerprint(definingClass = ACTION, name = "onLayout", returnType = "V")
private object ToggleFingerprint : Fingerprint(definingClass = ACTION, name = "setToggleOn", returnType = "V")
private object ToggleNameFingerprint : Fingerprint(definingClass = EXTENSION, strings = listOf("pikoVideoToggleField"))
private object TweetNameFingerprint : Fingerprint(definingClass = EXTENSION, strings = listOf("pikoVideoTweetField"))

/** Opt-in portrait UI using native post actions, playback control and follow-only requests. */
@Suppress("unused")
val immersiveVideoUiPatch = bytecodePatch(
    name = "Experimental vertical video controls",
    description = "Portrait immersive player: vertical actions, single-tap play/pause, double-tap likes and a native follow control. Experimental.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_X)
    dependsOn(settingsPatch, immersiveVideoUiResourcePatch)

    execute {
        enableSettings("videoActionSide")
        // Fail while patching if the target's native follow-only API has changed.
        val followRequest = classDefBy("Lcom/twitter/api/legacy/request/user/f;")
        if (followRequest.fields.none { it.name == "M" && it.type == "Lcom/twitter/model/core/entity/k1;" } ||
            followRequest.fields.none { it.name == "K" && it.type == "[I" }) {
            throw PatchException("Native follow response contract changed")
        }
        val cache = classDefBy("Lcom/twitter/cache/twitteruser/a;")
        if (cache.methods.none { it.name == "c" && it.parameterTypes == listOf("J") && it.returnType == "Ljava/lang/Integer;" } ||
            cache.methods.none { it.name == "j" && it.parameterTypes == listOf("Lcom/twitter/model/core/entity/k1;") }) {
            throw PatchException("Native relationship cache contract changed")
        }

        val bar = classDefBy(BAR)
        val tweetField = bar.fields.singleOrNull { it.type == "Lcom/twitter/model/core/e;" }
            ?: throw PatchException("Cannot uniquely identify the current tweet field")
        TweetNameFingerprint.changeFirstString(tweetField.name)
        val toggleField = ToggleFingerprint.method.instructions.firstOrNull { it.opcode == Opcode.IPUT_BOOLEAN }
            ?.getReference<FieldReference>() ?: throw PatchException("Like toggle field not found")
        ToggleNameFingerprint.changeFirstString(toggleField.name)

        twitterInitHook.fingerprint.method.addInstruction(0, "invoke-static {}, $EXTENSION->enable()V")

        listOf(
            Triple(MeasureFingerprint, LayoutFingerprint, ""),
            Triple(ActionMeasureFingerprint, ActionLayoutFingerprint, "Action"),
        ).forEach { (measureFingerprint, layoutFingerprint, suffix) ->
            measureFingerprint.method.apply {
                val original = getInstruction(0)
                if (implementation!!.registerCount - 3 < 4) throw PatchException("Insufficient measurement scratch registers")
                addInstructionsWithLabels(0, """
                    invoke-static {p0, p1, p2}, $EXTENSION->measure$suffix(Landroid/view/ViewGroup;II)J
                    move-result-wide v0
                    const-wide/16 v2, -0x1
                    cmp-long v2, v0, v2
                    if-eqz v2, :native_measure
                    long-to-int v2, v0
                    const/16 v3, 0x20
                    ushr-long/2addr v0, v3
                    long-to-int v3, v0
                    invoke-virtual {p0, v2, v3}, Landroid/view/View;->setMeasuredDimension(II)V
                    return-void
                """.trimIndent(), ExternalLabel("native_measure", original))
            }
            layoutFingerprint.method.apply {
                val original = getInstruction(0)
                if (implementation!!.registerCount - 6 < 1) throw PatchException("Insufficient layout scratch registers")
                addInstructionsWithLabels(0, """
                    invoke-static {p0}, $EXTENSION->layout$suffix(Landroid/view/ViewGroup;)Z
                    move-result v0
                    if-eqz v0, :native_layout
                    return-void
                """.trimIndent(), ExternalLabel("native_layout", original))
            }
        }

        val view = mutableClassDefBy(TWEET_VIEW)
        if (view.methods.any { it.name == "dispatchTouchEvent" }) {
            throw PatchException("Unexpected native dispatchTouchEvent; review touch interception before patching")
        }
        view.methods.add(ImmutableMethod(
            TWEET_VIEW,
            "dispatchTouchEvent",
            listOf(ImmutableMethodParameter("Landroid/view/MotionEvent;", emptySet(), null)),
            "Z",
            AccessFlags.PUBLIC.value,
            emptySet(),
            emptySet(),
            MutableMethodImplementation(4),
        ).toMutable().apply {
            addInstructions(0, """
                invoke-static {p0, p1}, $EXTENSION->onTouch(Landroid/view/ViewGroup;Landroid/view/MotionEvent;)I
                move-result v0
                if-eqz v0, :native_dispatch
                const/4 v1, 0x2
                if-ne v0, v1, :consume_tap
                invoke-static {p1}, Landroid/view/MotionEvent;->obtain(Landroid/view/MotionEvent;)Landroid/view/MotionEvent;
                move-result-object v0
                const/4 v1, 0x3
                invoke-virtual {v0, v1}, Landroid/view/MotionEvent;->setAction(I)V
                invoke-super {p0, v0}, ${view.superclass}->dispatchTouchEvent(Landroid/view/MotionEvent;)Z
                invoke-virtual {v0}, Landroid/view/MotionEvent;->recycle()V
                invoke-static {p0}, $EXTENSION->afterNativeCancel(Landroid/view/ViewGroup;)V
                :consume_tap
                const/4 v0, 0x1
                return v0
                :native_dispatch
                invoke-super {p0, p1}, ${view.superclass}->dispatchTouchEvent(Landroid/view/MotionEvent;)Z
                move-result v0
                return v0
            """.trimIndent())
        })
    }
}
