// SPDX-License-Identifier: MPL-2.0
package org.libreoffice.kit

import android.content.res.AssetManager
import java.nio.ByteBuffer

object LibreOfficeKit {
    @JvmStatic
    external fun putenv(string: String)

    @JvmStatic
    external fun redirectStdio(redirect: Boolean)

    @JvmStatic
    external fun initializeNative(
        dataDir: String,
        cacheDir: String,
        apkFile: String,
        assetManager: AssetManager?
    ): Boolean

    @JvmStatic
    external fun getLibreOfficeKitHandle(): ByteBuffer?
}
