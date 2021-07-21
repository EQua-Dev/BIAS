package com.androidstrike.bias.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.bias.R
import com.androidstrike.bias.utils.IRecyclerItemClickListener

class CourseAdapter (itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener{

    var txtCourseTitle: TextView
    var txtCourseCode: TextView
    var txtCourseLecturer: TextView
    var txtCourseTime: TextView


    lateinit var iRecyclerItemClickListener: IRecyclerItemClickListener
    fun setClick(iRecyclerItemClickListener: IRecyclerItemClickListener){
        this.iRecyclerItemClickListener = iRecyclerItemClickListener
    }
    init {
        txtCourseTitle = itemView.findViewById(R.id.tv_course_title) as TextView
        txtCourseCode = itemView.findViewById(R.id.tv_course_code) as TextView
        txtCourseLecturer = itemView.findViewById(R.id.tv_course_lecturer) as TextView
        txtCourseTime = itemView.findViewById(R.id.tv_course_time) as TextView

        itemView.setOnClickListener(this)

    }
    override fun onClick(v: View?) {
        iRecyclerItemClickListener.onItemClickListener(v!!,adapterPosition)
    }

}