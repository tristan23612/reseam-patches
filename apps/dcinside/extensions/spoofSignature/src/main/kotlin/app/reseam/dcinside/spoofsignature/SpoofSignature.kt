// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SpoofSignature")

package app.reseam.dcinside.spoofsignature

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import app.reseam.runtime.Export
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Field

private const val PACKAGE_NAME = "com.dcinside.app.android"
private const val CHANGED_PACKAGE_NAME = "com.dcinside.app.android.revanced"

private const val CERTIFICATE_DATA =
    "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" +
        "hHXTC3/WixFCEwmrGNtkTGohCbAje2Ry5gSXrYY="

@Volatile
private var installed = false

/** Installs the PackageInfo signature replacement once in this app process. */
@Export
@Synchronized
fun install() {
    if (installed) return

    val fakeSignature = Signature(Base64.decode(CERTIFICATE_DATA, Base64.DEFAULT))
    val creatorField = getFieldAccessible(PackageInfo::class.java, "CREATOR")
    @Suppress("UNCHECKED_CAST")
    val originalCreator = creatorField.get(null) as Parcelable.Creator<PackageInfo>

    val creator = object : Parcelable.Creator<PackageInfo> {
        @Suppress("DEPRECATION")
        override fun createFromParcel(source: Parcel): PackageInfo {
            val packageInfo = originalCreator.createFromParcel(source)
            if (packageInfo.packageName == PACKAGE_NAME || packageInfo.packageName == CHANGED_PACKAGE_NAME) {
                packageInfo.signatures?.takeIf { it.isNotEmpty() }?.set(0, fakeSignature)
                packageInfo.signingInfo?.apkContentsSigners
                    ?.takeIf { it.isNotEmpty() }
                    ?.set(0, fakeSignature)
            }
            return packageInfo
        }

        override fun newArray(size: Int): Array<PackageInfo?> = originalCreator.newArray(size)
    }

    HiddenApiBypass.addHiddenApiExemptions(
        "Landroid/os/Parcel;",
        "Landroid/content/pm",
        "Landroid/app",
    )
    creatorField.set(null, creator)

    clearPackageInfoCache()
    clearParcelCreatorCaches()
    installed = true
}

private fun clearPackageInfoCache() {
    runCatching {
        val cache = getFieldAccessible(PackageManager::class.java, "sPackageInfoCache").get(null)
        cache?.javaClass?.getMethod("clear")?.invoke(cache)
    }
}

private fun clearParcelCreatorCaches() {
    for (name in arrayOf("mCreators", "sPairedCreators")) {
        runCatching {
            val cache = getFieldAccessible(Parcel::class.java, name).get(null) as? MutableMap<*, *>
            cache?.clear()
        }
    }
}

private fun getFieldAccessible(type: Class<*>, fieldName: String): Field {
    var current: Class<*>? = type
    while (current != null && current != Any::class.java) {
        try {
            return current.getDeclaredField(fieldName).apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    throw NoSuchFieldException("${type.name}.$fieldName")
}
