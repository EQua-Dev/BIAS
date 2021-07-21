package com.androidstrike.bias.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.bias.R
import com.androidstrike.bias.utils.Common
import kotlinx.android.synthetic.main.custom_attendance_record.view.*

class RecordsAdapter: RecyclerView.Adapter<RecordsAdapter.RecordsViewHolder>() {

    inner class RecordsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecordsAdapter.RecordsViewHolder {
        return RecordsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.custom_attendance_record, parent,false))
    }

    override fun onBindViewHolder(holder: RecordsAdapter.RecordsViewHolder, position: Int) {
        //fetches the values in the local hashMap
        val hashKeyArray = ArrayList(Common.attendanceMap.keys)
        val hashValueArray = ArrayList(Common.attendanceMap.values)
        Log.d("EQua", "onBindViewHolder: $hashValueArray")

        holder.itemView.txt_course_date.text = hashKeyArray[position]
        if (hashValueArray[position]!!){
            //if the value of that position is true, set the corresponding image
            holder.itemView.course_img_attendance.setImageResource(R.drawable.ic_check)
        }else if (!hashValueArray[position]!!){
            //if the value of that position is false, set the corresponding image
            holder.itemView.course_img_attendance.setImageResource(R.drawable.ic_absent)
        }
    }

    override fun getItemCount(): Int {
        return Common.attendanceMap.size
    }
}