package app.crimera.patches.twitter.video.immersiveui

import app.crimera.patches.twitter.utils.Constants.PATCHES_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/** Rebuilt on v14: no startup initializer, global constructor interception or activity override. */
internal val continuousMediaPatch = bytecodePatch {
    execute {
        val hooks = "$PATCHES_DESCRIPTOR/SafeMediaHooks;"
        val source = mutableClassDefBy("Lcom/twitter/app/legacy/list/a0;")
        val loaded = source.methods.singleOrNull {
            it.name == "U" && it.parameterTypes == listOf("Lcom/twitter/model/common/collection/e;") && it.returnType == "V"
        } ?: throw PatchException("Native source-loaded callback changed")
        // The receiver is fully initialized here. Do not inject into a shared <init>.
        loaded.instructions.mapIndexedNotNull { i, instruction -> i.takeIf { instruction.opcode == Opcode.RETURN_VOID } }
            .reversed().forEach { i -> loaded.addInstructions(i,
                "invoke-static/range {p0 .. p0}, $hooks->sourceReady(Ljava/lang/Object;)V") }

        val gallery = mutableClassDefBy("Lcom/twitter/app/gallery/c0;")
        val update = gallery.methods.singleOrNull {
            it.name == "I3" && it.parameterTypes == listOf("Ljava/util/List;", "Z") && it.returnType == "V"
        } ?: throw PatchException("Native gallery update changed")
        update.addInstructions(0, """
            invoke-static/range {p0 .. p1}, $hooks->galleryItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
            move-result-object p1
        """.trimIndent())
        val listener = mutableClassDefBy("Lcom/twitter/app/gallery/l0;")
        if (listener.fields.none { it.name == "a" && it.type == gallery.type }) throw PatchException("Native gallery listener changed")
        val page = listener.methods.singleOrNull { it.name == "c" && it.parameterTypes == listOf("I") && it.returnType == "V" }
            ?: throw PatchException("Native page callback changed")
        page.instructions.mapIndexedNotNull { i, instruction -> i.takeIf { instruction.opcode == Opcode.RETURN_VOID } }
            .reversed().forEach { i -> page.addInstructions(i,
                "invoke-static/range {p0 .. p1}, $hooks->galleryPage(Ljava/lang/Object;I)V") }

        // Restore ordinary subtype constructors, each calling its DIRECT superclass.
        // The v15 grandparent-constructor factories are deliberately not reapplied.
        val mediaArgs = listOf("Lcom/twitter/model/core/e;", "Lcom/twitter/model/core/entity/c0;",
            "Lcom/twitter/media/request/a\$a;", "Ljava/lang/String;")
        val itemBase = "Lcom/twitter/app/gallery/item/a;"
        if (classDefBy(itemBase).methods.none { it.name == "<init>" && it.parameterTypes == mediaArgs }) throw PatchException("Native gallery base constructor changed")
        for (kind in listOf("b", "c")) {
            val item = mutableClassDefBy("Lcom/twitter/app/gallery/item/$kind;")
            if (item.superclass != itemBase) throw PatchException("Native gallery subtype changed")
            if (item.methods.none { it.name == "<init>" && it.parameterTypes == mediaArgs }) {
                item.methods.add(ImmutableMethod(item.type, "<init>",
                    mediaArgs.map { ImmutableMethodParameter(it, emptySet(), null) }, "V",
                    AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value,
                    emptySet(), emptySet(), MutableMethodImplementation(5)
                ).toMutable().apply {
                    addInstructions(0, """
                        invoke-direct/range {p0 .. p4}, $itemBase-><init>(${mediaArgs.joinToString("")})V
                        return-void
                    """.trimIndent())
                })
            }
        }
        val ancestor = "Lcom/twitter/app/common/m\$a;"
        if (classDefBy(ancestor).methods.none { it.name == "<init>" && it.parameterTypes == listOf("Landroid/os/Bundle;") }) throw PatchException("Native args ancestor constructor changed")
        for ((childName, parentName) in listOf(
            "Lcom/twitter/timeline/t\$a;" to ancestor,
            "Lcom/twitter/tweetdetail/g0\$a;" to "Lcom/twitter/timeline/t\$a;"
        )) {
            val child = mutableClassDefBy(childName)
            if (child.superclass != parentName) throw PatchException("Native args builder hierarchy changed")
            if (child.methods.none { it.name == "<init>" && it.parameterTypes == listOf("Landroid/os/Bundle;") }) {
                child.methods.add(ImmutableMethod(child.type, "<init>",
                    listOf(ImmutableMethodParameter("Landroid/os/Bundle;", emptySet(), null)), "V",
                    AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value,
                    emptySet(), emptySet(), MutableMethodImplementation(2)
                ).toMutable().apply {
                    addInstructions(0, """
                        invoke-direct/range {p0 .. p1}, $parentName-><init>(Landroid/os/Bundle;)V
                        return-void
                    """.trimIndent())
                })
            }
        }
        classDefBy("Lcom/twitter/tweetdetail/TweetDetailTimelineFragment;")
    }
}
