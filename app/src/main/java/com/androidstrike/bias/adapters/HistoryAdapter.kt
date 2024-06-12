package com.androidstrike.bias.adapters

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.bias.AttendanceOverviewDirections
import com.androidstrike.bias.R
import com.androidstrike.bias.databinding.CustomHistoryItemBinding
import com.androidstrike.bias.utils.Common
import com.androidstrike.bias.utils.IRecyclerItemClickListener
import kotlinx.parcelize.Parcelize

class HistoryAdapter (val context: Context, val courses: List<Course>) :
RecyclerView.Adapter<HistoryAdapter.MyViewHolder>(), View.OnClickListener {

    lateinit var iRecyclerItemClickListener: IRecyclerItemClickListener
    var adapterPosition: Int? = 0

    fun setClick(iRecyclerItemClickListener: IRecyclerItemClickListener) {
        this.iRecyclerItemClickListener = iRecyclerItemClickListener
    }

    inner class MyViewHolder(private val binding: CustomHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(day: Course?, position: Int) {
            binding.txtCourseName.text = day!!.course
            Common.clickedCourse = day.course

            this.currentDay = day
            this.currentPosition = position
        }

        var currentDay: Course? = null
        var currentPosition: Int? = 0

        init {
            itemView.setOnClickListener {
                // launch the attendance record fragment passing the args
                val currentCourse = courses[currentPosition!!]
                val action = AttendanceOverviewDirections.actionAttendanceOverviewToAttendanceRecord(currentCourse)
                itemView.findNavController().navigate(action)
//                val navController = Navigation.findNavController(itemView)
//                navController.navigate(R.id.action_attendanceOverview_to_attendanceRecord)
            }

        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = CustomHistoryItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val course = courses[position]
        holder.setData(course, position)
        adapterPosition = position    }

    override fun getItemCount(): Int {
        return courses.size
    }

    override fun onClick(v: View?) {
        iRecyclerItemClickListener.onItemClickListener(v!!, adapterPosition!!)
    }

    object Supplier {
        val courses = listOf<Course>(
            Course("C#"),
            Course("Python"),
            Course("Java"),
            Course("Ruby"),
            Course("JavaScript"),
            Course("Basic"),
            Course("Php"),
        )
    }

}
@Parcelize
data class Course(var course: String): Parcelable
