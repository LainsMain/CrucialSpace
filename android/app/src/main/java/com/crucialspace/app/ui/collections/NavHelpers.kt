package com.crucialspace.app.ui.collections

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openDetail(context: Context, id: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("app://detail/$id"))
    intent.setPackage(context.packageName)
    context.startActivity(intent)
}


