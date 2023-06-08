package com.androidstrike.bias.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class CourseDetail(
    var course: String? = null,
    var course_time: String? = null,
    var lecturer: String? = null,
    var time: Date? = null,
    var course_code: String? = "",
    var start_time: String? = "",
    var end_time: String? = "",
    var attendance_question: String? = "",
    var attendance_answer: String? = ""
//    var newTime:
): Parcelable