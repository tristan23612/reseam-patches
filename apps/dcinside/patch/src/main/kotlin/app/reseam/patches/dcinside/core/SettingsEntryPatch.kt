package app.reseam.patches.dcinside.core

import app.example.patches.DcinsidePatchStrings
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.openSettings

private const val CATEGORY = "reseam_settings_category"
private const val ROW = "reseam_settings_entry"

@Suppress("unused")
val settingsEntry = patch(DcinsidePatchStrings.settingsEntry) {
    compatibleWith(DCINSIDE)
    dependsOn(dcinsideSettings)

    execute {
        resources.addString(CATEGORY, "Reseam")
            ?: error("Could not add the Reseam settings category title")
        resources.addString(ROW, "Reseam Settings")
            ?: error("Could not add the Reseam settings row title")
        val rowId = resources.addId(ROW)?.toInt()
            ?: error("Could not add id/$ROW")

        settingsFragment.classDef.addInterface($$"Landroid/view/View$OnClickListener;")
        settingsFragment.addMethod("onClick", "(Landroid/view/View;)V") {
            openSettings()
            returnVoid()
        }

        resources.editXml("layout", "fragment_settings") {
            val scroll = root.children.single { it.tag == "ScrollView" }
            val list = scroll.children.single { it.tag == "LinearLayout" }

            val header = createElement("TextView").apply {
                this["android:textAppearance"] = "?attr/textTypeSub"
                this["android:textColor"] = "?attr/colorPrimaryText"
                this["android:gravity"] = "center_vertical"
                this["android:background"] = "?attr/windowBackgroundInverse"
                this["android:paddingTop"] = "5dp"
                this["android:paddingBottom"] = "5dp"
                this["android:layout_width"] = "match_parent"
                this["android:layout_height"] = "wrap_content"
                this["android:text"] = "@string/$CATEGORY"
                this["android:paddingStart"] = "10dp"
                this["android:paddingEnd"] = "10dp"
            }
            val group = createElement("LinearLayout").apply {
                this["android:orientation"] = "vertical"
                this["android:layout_width"] = "match_parent"
                this["android:layout_height"] = "wrap_content"
                this["android:divider"] = "?attr/divide_n"
                this["android:showDividers"] = "middle"
            }
            val row = createElement("LinearLayout").apply {
                this["android:id"] = "@id/$ROW"
                this["android:gravity"] = "center_vertical"
                this["android:orientation"] = "horizontal"
                this["android:background"] = "?attr/selectableItemBackground"
                this["android:layout_width"] = "match_parent"
                this["android:layout_height"] = "50dp"

                appendChild(createElement("TextView").apply {
                    this["android:textAppearance"] = "?attr/textTypeDefault"
                    this["android:layout_gravity"] = "center_vertical"
                    this["android:layout_width"] = "wrap_content"
                    this["android:layout_height"] = "wrap_content"
                    this["android:text"] = "@string/$ROW"
                    this["android:layout_marginStart"] = "10dp"
                })
                appendChild(createElement("ImageView").apply {
                    this["android:padding"] = "10dp"
                    this["android:layout_width"] = "wrap_content"
                    this["android:layout_height"] = "wrap_content"
                    this["android:src"] = "@drawable/list_arrow"
                    this["android:scaleType"] = "centerInside"
                    this["android:contentDescription"] = "@null"
                    this["app:tint"] = "?attr/icTintNormal"
                })
            }
            group.appendChild(row)

            val first = list.children.firstOrNull()
            if (first == null) {
                list.appendChild(header)
                list.appendChild(group)
            } else {
                list.insertBefore(header, first)
                list.insertBefore(group, first)
            }
        }

        settingsFragment.method("onViewCreated") {
            params(Type.View, "android.os.Bundle")
        }.after {
            val row = param(0).callVirtual(
                "android.view.View",
                "findViewById",
                "(I)Landroid/view/View;",
                int(rowId),
            )
            row.callVirtual(
                "android.view.View",
                "setOnClickListener",
                $$"(Landroid/view/View$OnClickListener;)V",
                thisObject,
            )
        }
    }
}
