package app.reseam.patches.dcinside.manager.floatingButton

import app.reseam.patch.*
import app.reseam.dcinside.floatingButton.bindings.FloatingButton
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.customInterceptor.customInterceptor

private object FloatingButtonResources

internal fun buttonResource(name: String): ByteArray =
    FloatingButtonResources::class.java.getResourceAsStream("/buttons/$name")
        ?.use { it.readBytes() }
        ?: error("Button resource is missing: $name")

private fun PatchRuntime.floatingButtonContainerResourcePatch() {
    resources.addId("reseam_floating_button_container")
    resources.addId("reseam_floating_button_sub_container")
    resources.addId("reseam_floating_button_toggle")

    resources.editXml("layout", "fragment_post_list") {
        val quickWriteId = resources.id("id", "post_list_quick_write")
            ?: error("Could not find id/post_list_quick_write")
        val quickWrite = root.descendants()
            .firstOrNull { resourceRef(it["android:id"] ?: "") == quickWriteId }
            ?: error("Could not find @id/post_list_quick_write in layout/fragment_post_list")
        val parent = quickWrite.parent
            ?: error("post_list_quick_write has no parent in layout/fragment_post_list")

        val container = createElement("LinearLayout").apply {
            this["android:id"] = "@id/reseam_floating_button_container"
            this["android:layout_width"] = "wrap_content"
            this["android:layout_height"] = "wrap_content"
            this["android:layout_gravity"] = "end|bottom"
            this["android:layout_marginBottom"] = "134.0dp"
            this["android:layout_marginEnd"] = "15.0dp"
            this["android:orientation"] = "vertical"
        }

        val subContainer = createElement("LinearLayout").apply {
            this["android:id"] = "@id/reseam_floating_button_sub_container"
            this["android:layout_width"] = "wrap_content"
            this["android:layout_height"] = "wrap_content"
            this["android:orientation"] = "vertical"
            this["android:visibility"] = "gone"
        }
        container.appendChild(subContainer)

        val toggleButton = quickWrite.clone().apply {
            this["android:id"] = "@id/reseam_floating_button_toggle"
            removeAttribute("android:layout_gravity")
            removeAttribute("android:layout_marginBottom")
            removeAttribute("android:layout_marginEnd")
            this["android:layout_marginTop"] = "10.0dp"
        }
        toggleButton.children
            .firstOrNull { it.tag == "androidx.appcompat.widget.AppCompatImageView" }
            ?.apply {
                this["android:padding"] = "6.0dp"
                this["android:src"] = "@drawable/reseam_floating_button_toggle"
                removeAttribute("android:tint")
                removeAttribute("app:tint")
            }
        container.appendChild(toggleButton)

        val children = parent.children
        val quickWriteIndex = children.indexOfFirst {
            resourceRef(it["android:id"] ?: "") == quickWriteId
        }
        check(quickWriteIndex >= 0) { "post_list_quick_write is not a direct child of its reported parent" }
        val nextSibling = children.getOrNull(quickWriteIndex + 1)
        if (nextSibling == null) parent.appendChild(container) else parent.insertBefore(container, nextSibling)
    }
}

internal fun PatchRuntime.addFloatingButtonResource(settingId: String) {
    val buttonName = "reseam_${settingId}_button"
    resources.addFile(
        "drawable",
        buttonName,
        "res/drawable/$buttonName.png",
        buttonResource("$buttonName.png"),
    )
    resources.addId(buttonName) ?: error("Could not add id/$buttonName")

    resources.editXml("layout", "fragment_post_list") {
        val quickWriteId = resources.id("id", "post_list_quick_write")
            ?: error("Could not find id/post_list_quick_write")
        val subContainerId = resources.id("id", "reseam_floating_button_sub_container")
            ?: error("Could not find id/reseam_floating_button_sub_container")
        val quickWrite = root.descendants()
            .firstOrNull { resourceRef(it["android:id"] ?: "") == quickWriteId }
            ?: error("Could not find @id/post_list_quick_write in layout/fragment_post_list")
        val subContainer = root.descendants()
            .firstOrNull { resourceRef(it["android:id"] ?: "") == subContainerId }
            ?: error("Could not find @id/reseam_floating_button_sub_container in layout/fragment_post_list")

        val floatingButton = quickWrite.clone().apply {
            this["android:id"] = "@id/$buttonName"
            removeAttribute("android:layout_gravity")
            removeAttribute("android:layout_marginBottom")
            removeAttribute("android:layout_marginEnd")
            this["android:layout_marginTop"] = "10.0dp"
        }
        floatingButton.children
            .firstOrNull { it.tag == "androidx.appcompat.widget.AppCompatImageView" }
            ?.apply {
                this["android:padding"] = "6.0dp"
                this["android:src"] = "@drawable/$buttonName"
                removeAttribute("android:tint")
                removeAttribute("app:tint")
            }
        subContainer.appendChild(floatingButton)
    }
}

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }

@Suppress("unused")
val floatingButton = patch {
    compatibleWith(DCINSIDE)
    dependsOn(customInterceptor)

    execute {
        resources.addFile(
            "drawable",
            "reseam_floating_button_toggle",
            "res/drawable/reseam_floating_button_toggle.xml",
            buttonResource("reseam_floating_button_toggle.xml"),
        )
        floatingButtonContainerResourcePatch()

        klass("fragment post list class") {
            strings("PostFilterType|", "useQuickWrite")
        }.methods("quick write button visibility method") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params(Type.Boolean)
            returns(Type.Void)
            calls {
                name("setVisibility")
                name("getVisibility")
            }
        }.single().point { invokeVirtual { name("setVisibility") } }
            .captureArgumentAs("view", 0, Type.View)
            .previous { opcode(Opcode.IF_EQZ) }
            .before {
                call(
                    FloatingButton.setFloatingButtonToggleVisibility,
                    capture("view"),
                    param(0),
                    string("reseam_floating_button"),
                )
            }
    }
}

