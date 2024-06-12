package com.androidstrike.bias.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.bias.R
import com.androidstrike.bias.databinding.CustomAttendanceRecordBinding
import com.androidstrike.bias.utils.Common

class RecordsAdapter: RecyclerView.Adapter<RecordsAdapter.RecordsViewHolder>() {

    inner class RecordsViewHolder(private val binding: CustomAttendanceRecordBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(keys: ArrayList<String>, values: ArrayList<Boolean?>, position: Int) {
            val txtCourseDate = binding.txtCourseDate
            val imgCourseAttendance = binding.courseImgAttendance

            txtCourseDate.text = keys[position]
            if (values[position]!!){
                //if the value of that position is true, set the corresponding image
                imgCourseAttendance.setImageResource(R.drawable.ic_check)
            }else if (!values[position]!!){
                //if the value of that position is false, set the corresponding image
                imgCourseAttendance.setImageResource(R.drawable.ic_absent)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecordsAdapter.RecordsViewHolder {
        return RecordsViewHolder(
            CustomAttendanceRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecordsAdapter.RecordsViewHolder, position: Int) {
        //fetches the values in the local hashMap
        val hashKeyArray = ArrayList(Common.attendanceMap.keys)
        val hashValueArray = ArrayList(Common.attendanceMap.values)
        Log.d("EQua", "onBindViewHolder: $hashValueArray")

        holder.bind(hashKeyArray, hashValueArray, position)

    }

    override fun getItemCount(): Int {
        return Common.attendanceMap.size
    }
}