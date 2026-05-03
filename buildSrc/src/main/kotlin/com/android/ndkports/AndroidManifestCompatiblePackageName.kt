package com.android.ndkports

object AndroidManifestCompatiblePackageName {
    fun parse(nameString: String) : String {
        return nameString.replace("-", "_")
    }
}