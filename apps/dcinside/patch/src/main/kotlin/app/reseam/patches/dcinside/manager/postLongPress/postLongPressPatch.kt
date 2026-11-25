package app.reseam.patches.dcinside.manager.postLongPress

import app.reseam.dcinside.customInterceptor.bindings.CustomApplicationInterceptor
import app.reseam.dcinside.postLongPress.bindings.PostLongPress
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.*
import app.reseam.patch.dex.Opcode
import app.reseam.patch.settings.section
import app.reseam.patch.types.AnnotationElement
import app.reseam.patch.types.AnnotationItem
import app.reseam.patch.types.EncodedVal
import app.reseam.patch.types.NewField
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings
import app.reseam.patches.dcinside.customInterceptor.customInterceptor

internal const val POST_ITEM_CLASS = "com.dcinside.app.response.PostItem"

@Suppress("unused")
val postLongPress = patch(DcinsidePatchStrings.postLongPress) {
    description(DcinsidePatchStrings.postLongPressDescription)
    compatibleWith(DCINSIDE)
    dependsOn(customInterceptor)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.Management,
            DcinsideStrings.sectionPostManagement,
            DcinsideSettings.postLongPress,
        )
    )

    execute {
        appEntry {
            CustomApplicationInterceptor.postLongPress(string(DcinsideSettings.postLongPress.key), bool(DcinsideSettings.postLongPress.default))
            PostLongPress.postLongPress(string(DcinsideSettings.postLongPress.key), bool(DcinsideSettings.postLongPress.default))
        }

        val annotationDescriptor = klass("annotation class") {
            implements("java.lang.annotation.Annotation")
            custom {
                val names = methods.map { it.name }.toSet()
                names.containsAll(setOf("value", "alternate"))
            }
        }.descriptor

        val annotationItem = AnnotationItem(
            visibility = 1u.toUByte(),
            annotationType = annotationDescriptor,
            elements = listOf(
                AnnotationElement(
                    "value",
                    EncodedVal.StringVal("reseam_no"),
                ),
            ),
        )

        klass(POST_ITEM_CLASS).classDef.let {
            it.addField(
                NewField(
                    name = "reseamNo",
                    fieldType = "I",
                    accessFlags = AccessFlags.PUBLIC.toUInt()
                )
            )

            it.addFieldAnnotation(
                "reseamNo",
                annotationItem
            )
        }

        val postItemOnBindViewHolder = method("postItemOnBindViewHolder method") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.Void)
            name("onBindViewHolder")
            literals(
                resources.id("string", "post_section_text")?.toLong() ?: error("Could not find string/post_section_text"),
                resources.id("string", "post_is_auto_deleted")?.toLong() ?: error("Could not find string/post_is_auto_deleted"),
            )
        }.point { invokeVirtual { name("getContext") } }
            .next { invokeVirtual { returns(POST_ITEM_CLASS) } }
            .next { resultOf(POST_ITEM_CLASS) }
            .captureAs("postItem", POST_ITEM_CLASS)
            .next { opcode(Opcode.RETURN_VOID) }
            .before {
                val noField = klass(POST_ITEM_CLASS).field("reseamNo")
                val itemView = param(0).fieldOfType(Type.View)

                call(PostLongPress.setLongClickListener, itemView, capture("postItem").field(noField))
            }
    }
}