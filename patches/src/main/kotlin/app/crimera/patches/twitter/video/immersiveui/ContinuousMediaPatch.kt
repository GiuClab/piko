package app.crimera.patches.twitter.video.immersiveui

import app.crimera.patches.twitter.utils.Constants.PATCHES_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable

/** Exact-version native source/gallery contracts, inspected in X 12.19.1. */
internal val continuousMediaPatch = bytecodePatch {
    execute {
        val extension = "$PATCHES_DESCRIPTOR/NativeMedia;"
        val source = mutableClassDefBy("Lcom/twitter/app/legacy/list/a0;")
        for ((name, type) in listOf("b" to "Lcom/twitter/app/common/inject/InjectedFragment;",
            "c" to "Lcom/twitter/util/user/UserIdentifier;", "v" to "Lcom/twitter/app/legacy/list/i0;")) {
            if (source.fields.none { it.name == name && it.type == type }) throw PatchException("Source-list contract changed: $name")
        }
        val constructors = source.methods.filter { it.name == "<init>" }
        if (constructors.isEmpty()) throw PatchException("Source constructor missing")
        constructors.forEach { method ->
            method.instructions.mapIndexedNotNull { index, instruction -> index.takeIf { instruction.opcode == Opcode.RETURN_VOID } }
                .reversed().forEach { index -> method.addInstructions(index,
                    "invoke-static/range {p0 .. p0}, $extension->onTimeline(Ljava/lang/Object;)V") }
        }
        val gallery = mutableClassDefBy("Lcom/twitter/app/gallery/c0;")
        val update = gallery.methods.singleOrNull { it.name == "I3" && it.parameterTypes == listOf("Ljava/util/List;", "Z") && it.returnType == "V" }
            ?: throw PatchException("Gallery list update contract changed")
        update.addInstructions(0, """
            invoke-static/range {p0 .. p1}, $extension->galleryItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """.trimIndent())
        val listener = mutableClassDefBy("Lcom/twitter/app/gallery/l0;")
        if (listener.fields.none { it.name == "a" && it.type == gallery.type }) throw PatchException("Gallery page listener contract changed")
        val page = listener.methods.singleOrNull { it.name == "c" && it.parameterTypes == listOf("I") && it.returnType == "V" }
            ?: throw PatchException("Gallery page callback changed")
        page.instructions.mapIndexedNotNull { index, instruction -> index.takeIf { instruction.opcode == Opcode.RETURN_VOID } }
            .reversed().forEach { index -> page.addInstructions(index,
                "invoke-static/range {p0 .. p1}, $extension->onGalleryPage(Ljava/lang/Object;I)V") }
        val playerActivity = mutableClassDefBy("Lcom/twitter/explore/immersivemediaplayer/ui/activity/ImmersiveMediaPlayerActivity;")
        if (playerActivity.methods.any { it.name == "onBackPressed" && it.parameterTypes.isEmpty() }) throw PatchException("Review native player back override")
        playerActivity.methods.add(ImmutableMethod(playerActivity.type, "onBackPressed", emptyList(), "V",
            AccessFlags.PUBLIC.value, emptySet(), emptySet(), MutableMethodImplementation(2)
        ).toMutable().apply {
            addInstructions(0, """
                invoke-static {p0}, $extension->closeComments(Landroid/app/Activity;)Z
                move-result v0
                if-eqz v0, :native_back
                return-void
                :native_back
                invoke-super {p0}, ${playerActivity.superclass}->onBackPressed()V
                return-void
            """.trimIndent())
        })
        // Reflection contracts are checked at patch time rather than failing later on the phone.
        classDefBy("Lcom/twitter/tweetdetail/TweetDetailTimelineFragment;")
        classDefBy("Lcom/twitter/tweetdetail/g0\$a;")
        // R8 removed the trivial subtype constructors. Mirror X's own new-instance +
        // base-constructor calls in verified dex factories; Java reflection cannot do this.
        val base = mutableClassDefBy("Lcom/twitter/app/gallery/item/a;")
        val args = listOf("Lcom/twitter/model/core/e;", "Lcom/twitter/model/core/entity/c0;",
            "Lcom/twitter/media/request/a\$a;", "Ljava/lang/String;")
        if (base.methods.none { it.name == "<init>" && it.parameterTypes == args }) throw PatchException("Gallery base constructor changed")
        base.methods.add(ImmutableMethod(base.type, "pikoCreate",
            (args + "Z").map { ImmutableMethodParameter(it, emptySet(), null) }, base.type,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, emptySet(), emptySet(), MutableMethodImplementation(6)
        ).toMutable().apply {
            addInstructions(0, """
                if-eqz p4, :photo
                new-instance v0, Lcom/twitter/app/gallery/item/c;
                invoke-direct {v0, p0, p1, p2, p3}, ${base.type}-><init>(${args.joinToString("")})V
                return-object v0
                :photo
                new-instance v0, Lcom/twitter/app/gallery/item/b;
                invoke-direct {v0, p0, p1, p2, p3}, ${base.type}-><init>(${args.joinToString("")})V
                return-object v0
            """.trimIndent())
        })
        val options = mutableClassDefBy("Lcom/twitter/tweetdetail/g0;")
        val builder = "Lcom/twitter/tweetdetail/g0\$a;"
        val parent = classDefBy("Lcom/twitter/app/common/m\$a;")
        if (parent.methods.none { it.name == "<init>" && it.parameterTypes == listOf("Landroid/os/Bundle;") }) throw PatchException("Timeline args constructor changed")
        options.methods.add(ImmutableMethod(options.type, "pikoNewCommentArgsBuilder", emptyList(), builder,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, emptySet(), emptySet(), MutableMethodImplementation(2)
        ).toMutable().apply {
            addInstructions(0, """
                new-instance v0, $builder
                const/4 v1, 0x0
                invoke-direct {v0, v1}, ${parent.type}-><init>(Landroid/os/Bundle;)V
                return-object v0
            """.trimIndent())
        })
    }
}
