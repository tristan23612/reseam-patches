package app.reseam.patches.dcinside.manager.userId

import app.reseam.dcinside.customInterceptor.bindings.CustomApplicationInterceptor
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.dcinside.showUserId.bindings.ShowUserId
import app.reseam.dcinside.floatingButton.gallScope.bindings.GallScope
import app.reseam.patch.*
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.returnType
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.types.AnnotationElement
import app.reseam.patch.types.AnnotationItem
import app.reseam.patch.types.EncodedVal
import app.reseam.patch.types.Instruction
import app.reseam.patch.types.NewField
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings
import app.reseam.patches.dcinside.customInterceptor.customInterceptor

internal const val POST_ITEM_CLASS = "com.dcinside.app.response.PostItem"
internal const val POST_INFO_CLASS = "com.dcinside.app.model.PostInfo"

@Suppress("unused")
val showUserId = patch(DcinsidePatchStrings.showUserId) {
    description(DcinsidePatchStrings.showUserIdDescription)
    compatibleWith(DCINSIDE)
    dependsOn(customInterceptor)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.Management,
            DcinsideStrings.sectionUserId,
            DcinsideSettings.showUserId,
            DcinsideSettings.enableUserIdDirectScope,
        )
    )

    execute {
        resources.addId("reseam_user_id")
            ?: error("Could not add id/reseam_user_id")
        resources.addId("reseam_gallery_data_space")
            ?: error("Could not add id/reseam_gallery_data_space")

        listOf("view_post_list_item_basic", "view_post_list_item_split").forEach { layout ->
            injectUserIdViews(
                layoutName = layout,
                leftAnchorId = "post_list_item_member_ic",
                rightAnchorId = "post_list_item_counts",
                verticalAnchorId = "post_list_item_nic",
                viewClass = "com.dcinside.app.view.ResizeTextView",
                textColorAttr = "?attr/dcPostReadSubColor",
                height = "wrap_content",
                extraAttributes = mapOf(
                    "android:gravity" to "center_vertical",
                    "app:layout_constraintTop_toTopOf" to "@id/post_list_item_nic",
                ),
            )
        }

        injectUserIdViews(
            layoutName = "view_read_header",
            leftAnchorId = "read_header_member_ic",
            rightAnchorId = "read_header_gallog",
            verticalAnchorId = "read_header_name",
            viewClass = "android.widget.TextView",
            textColorAttr = "?attr/colorPostExt",
            height = "wrap_content",
            extraAttributes = mapOf(
                "android:layout_marginStart" to "1dp",
                "app:layout_constraintBaseline_toBaselineOf" to "@id/read_header_name",
            ),
        )

        mapOf(
            "view_reply_item_image" to "31dp",
            "view_reply_item_image_big" to "31dp",
            "view_reply_item_tcon" to "31dp",
            "view_reply_item_text" to "26dp",
            "view_reply_item_voice" to "26dp",
            "view_reply_item_voice2" to "26dp",
        ).forEach { (layout, height) ->
            injectUserIdViews(
                layoutName = layout,
                leftAnchorId = "reply_member_ic",
                rightAnchorId = "reply_user_memo",
                verticalAnchorId = "reply_name",
                viewClass = "com.dcinside.app.view.ResizeTextView",
                textColorAttr = "?attr/dcPostReadSubColor",
                height = height,
                extraAttributes = mapOf(
                    "android:gravity" to "center_vertical",
                    "app:layout_constraintTop_toTopOf" to "@id/reply_name",
                ),
            )
        }

        appEntry {
            CustomApplicationInterceptor.showUserId(string(DcinsideSettings.showUserId.key), bool(DcinsideSettings.showUserId.default))
            ShowUserId.showUserId(string(DcinsideSettings.showUserId.key), bool(DcinsideSettings.showUserId.default))
            GallScope.enableUserIdDirectScope(string(DcinsideSettings.enableUserIdDirectScope.key), bool(DcinsideSettings.enableUserIdDirectScope.default))
        }

        val postList = getPostList()
        val postSearchList = getPostSearchList()
        val postHeader = getPostHeader()
        val comment = getComment()

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
                    EncodedVal.StringVal("reseam_user_id"),
                ),
            ),
        )

        listOf(
            klass(POST_ITEM_CLASS),
            klass(POST_INFO_CLASS),
            klass(comment.parameterTypes[2])
        ).forEach { klass ->
            klass.classDef.addField(
                NewField(
                    name = "reseamUserId",
                    fieldType = "Ljava/lang/String;",
                    accessFlags = AccessFlags.PUBLIC.toUInt()
                )
            )

            klass.classDef.addFieldAnnotation(
                "reseamUserId",
                annotationItem
            )
        }

        listOf(
            postList,
            postSearchList
        ).forEach { method ->
            method.point { invokeVirtual { name("getContext") } }
                .next { invokeVirtual { returns(POST_ITEM_CLASS) } }
                .next { resultOf(POST_ITEM_CLASS) }
                .captureAs("postItem", POST_ITEM_CLASS)
                .next { opcode(Opcode.RETURN_VOID) }
                .before {
                    val userIdField = klass(POST_ITEM_CLASS).field("reseamUserId")
                    val itemView = param(0).fieldOfType(Type.View)

                    call(ShowUserId.setUserId, itemView, capture("postItem").field(userIdField))
                }
        }

        postHeader.after {
            val userIdField = klass(POST_INFO_CLASS).field("reseamUserId")
            val userId = param(0).field(userIdField)

            call(ShowUserId.setUserId, thisObject, userId)
        }

        comment.point {
            invokeVirtual {
                owner(comment.parameterTypes[0])
                paramCount(0)
            }
        }.let {
            val viewMethod = (it.instruction as Instruction.Invoke).field0.method.name
            val viewType = (it.instruction as Instruction.Invoke).field0.method.returnType
            comment.before {
                val userIdField = klass(comment.parameterTypes[2]).field("reseamUserId")
                val userId = param(2).field(userIdField)
                val view = param(0).callVirtual(
                    comment.parameterTypes[0],
                    viewMethod,
                    "()$viewType",
                )

                call(ShowUserId.setUserId, view, userId)
            }
        }

        history().forEach {
            points {
                invokeStatic {
                    name("currentTimeMillis")
                }
            }.forEach {
                previous {
                    invokeVirtual {
                        owner(POST_INFO_CLASS)
                        returns(Type.String)
                    }
                }
                .captureArgumentAs("postInfo", 0, POST_INFO_CLASS)
                .let {
                    val savedPostInfo = it.method.reserveLocal("savedPostInfo", POST_INFO_CLASS)
                    it.before {
                        local(savedPostInfo).assign(capture("postInfo"))
                    }

                    it.next { resultOf(Type.String) }
                        .captureAs("name", Type.String)
                        .after {
                            val userIdField = klass(POST_INFO_CLASS).field("reseamUserId")
                            val userId = local(savedPostInfo).field(userIdField)
                            val name = capture("name")

                            name.assign(
                                call(
                                    ShowUserId.addUserId,
                                    name,
                                    userId,
                                ),
                            )
                        }
                }
            }
        }
    }
}

context(patchRuntime: PatchRuntime)
private fun injectUserIdViews(
    layoutName: String,
    leftAnchorId: String,
    rightAnchorId: String,
    verticalAnchorId: String,
    viewClass: String,
    textColorAttr: String,
    height: String,
    extraAttributes: Map<String, String> = emptyMap(),
) {
    val leftAnchorResource = patchRuntime.resources.id("id", leftAnchorId)
        ?: error("Could not find id/$leftAnchorId")
    val rightAnchorResource = patchRuntime.resources.id("id", rightAnchorId)
        ?: error("Could not find id/$rightAnchorId")

    patchRuntime.resources.editXml("layout", layoutName) {
        declareNamespace("app", "http://schemas.android.com/apk/res-auto")

        val elements = root.descendants()
        val leftAnchor = elements.firstOrNull {
            resourceRef(it["android:id"] ?: "") == leftAnchorResource
        } ?: error("Could not find @$leftAnchorId in layout/$layoutName")

        val rightAnchor = elements.firstOrNull {
            resourceRef(it["android:id"] ?: "") == rightAnchorResource
        } ?: error("Could not find @$rightAnchorId in layout/$layoutName")

        val userId = createElement(viewClass).apply {
            this["android:textAppearance"] = "?attr/textTypeSub"
            this["android:textColor"] = textColorAttr
            this["android:id"] = "@id/reseam_user_id"
            this["android:layout_width"] = "wrap_content"
            this["android:layout_height"] = height
            this["android:singleLine"] = "true"
            this["android:includeFontPadding"] = "false"
            this["android:visibility"] = "gone"
            this["app:layout_constraintBottom_toBottomOf"] = "@id/$verticalAnchorId"
            this["app:layout_constraintStart_toEndOf"] = "@id/$leftAnchorId"
            this["app:layout_constraintEnd_toStartOf"] = "@id/$rightAnchorId"
            extraAttributes.forEach { (attribute, value) -> this[attribute] = value }
        }

        leftAnchor["app:layout_constraintEnd_toStartOf"] = "@id/reseam_user_id"
        rightAnchor["app:layout_constraintStart_toEndOf"] = "@id/reseam_user_id"
        leftAnchor.parent?.insertBefore(userId, rightAnchor)
            ?: error("The left anchor has no parent in layout/$layoutName")

        root.appendChild(createElement("Space").apply {
            this["android:id"] = "@id/reseam_gallery_data_space"
            this["android:layout_width"] = "0dp"
            this["android:layout_height"] = "0dp"
            this["android:visibility"] = "gone"
        })
    }
}

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }
