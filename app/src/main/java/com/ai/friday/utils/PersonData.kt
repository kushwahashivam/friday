package com.ai.friday.utils

import android.graphics.Bitmap
import java.io.Serializable

data class PersonData(
    var id: String,
    var name: String,
    var image: Bitmap,
    var data: String,
    var star: Int
) : Serializable