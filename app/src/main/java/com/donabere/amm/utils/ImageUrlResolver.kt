package com.donabere.amm.utils

import android.content.Context
import com.donabere.amm.R

object ImageUrlResolver {
    fun resolve(context: Context, raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return null
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value
        }
        val base = context.getString(R.string.image_base_url).trimEnd('/')
        val normalized = value.trimStart('/')
        return if (normalized.startsWith("uploads/")) {
            "$base/$normalized"
        } else {
            "$base/uploads/$normalized"
        }
    }
}
