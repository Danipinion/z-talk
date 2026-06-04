package com.danipinion.z_talk.ui.utils

import android.content.Context

object AvatarHelper {
    val AVATAR_LIST = listOf(
        "bear", "beaver", "bird", "camel", "elephant", "fox", "giraffe",
        "grey_racoon", "hippopotamus", "kangaroo", "koala", "lion",
        "monkey", "panda", "racoon", "rhino", "tiger", "wolf", "zebra"
    )

    fun getAvatarResourceId(context: Context, avatarName: String?): Int {
        if (avatarName.isNullOrEmpty()) {
            return context.resources.getIdentifier("panda", "drawable", context.packageName)
        }
        val resId = context.resources.getIdentifier(avatarName, "drawable", context.packageName)
        return if (resId != 0) resId else context.resources.getIdentifier("panda", "drawable", context.packageName)
    }
}
