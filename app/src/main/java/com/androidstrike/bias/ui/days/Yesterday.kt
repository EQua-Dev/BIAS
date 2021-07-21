package com.androidstrike.bias.ui.days

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidstrike.bias.R
import com.androidstrike.bias.adapters.CourseAdapter
import com.androidstrike.bias.model.CourseDetail
//import com.androidstrike.tera.data.CourseDetail
//import com.androidstrike.tera.ui.adapter.CourseAdapter
import com.androidstrike.bias.utils.Common
import com.androidstrike.bias.utils.IRecyclerItemClickListener
//import com.androidstrike.tera.utils.IRecyclerItemClickListener
import com.androidstrike.bias.utils.toast
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_yesterday.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class Yesterday : Fragment() {

    var courseAdapter: FirestoreRecyclerAdapter<CourseDetail, CourseAdapter>? = null
    lateinit var lectureTime: String
//    val connectionCheck = History().isNetworkAvailable(context)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_yesterday, container, false)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext())
        rv_yesterday.layoutManager = layoutManager
        rv_yesterday.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )
        getRealTimeCourses()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getRealTimeCourses() {
        //if the 'yesterday' is Friday or Saturday, show the text below
        if (Common.dowYesGood == "Friday" || Common.dowYesGood == "Saturday") {
            txt_yes.visibility = View.VISIBLE
            txt_yes.text = "Lecture Free Day!"
        } else {
            //if the 'yesterday' is between Monday and Thursday, fetch the day of the week
            // ...and access its corresponding collection in the firestore
            //and then fetch the courses for that very day
            val firestore = FirebaseFirestore.getInstance()
            val yesterdayCourses = firestore.collection(Common.dowYesGood)
            val query = yesterdayCourses.whereEqualTo("course_time", "${Common.formattedYesterday}")
            val options = FirestoreRecyclerOptions.Builder<CourseDetail>()
                .setQuery(query, CourseDetail::class.java).build()

            //display the list of fetched courses in recyclerView using both firebase and custom adapters
            courseAdapter =
                object : FirestoreRecyclerAdapter<CourseDetail, CourseAdapter>(options) {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): CourseAdapter {
                        val itemView = LayoutInflater.from(parent.context)
                            .inflate(R.layout.custom_day_course, parent, false)
                        return CourseAdapter(itemView)
                    }

                    override fun onBindViewHolder(
                        holder: CourseAdapter,
                        position: Int,
                        model: CourseDetail
                    ) {
                        //set the views in the layout to the corresponding values from the cloud
                        holder.txtCourseCode.text = StringBuilder(model.course_code!!)
                        holder.txtCourseTitle.text = StringBuilder(model.course!!)
                        holder.txtCourseLecturer.text = StringBuilder(model.lecturer!!)

                        val fmt = model.time
                        val bla = LocalTime.of(
                            fmt?.hours!!,
                            fmt.minutes
                        )

                        val nameOfCourse = model.course.toString()
                        val nameOfLecturer = model.lecturer.toString()

                        val omo = nameOfLecturer.split(" ".toRegex())
                        val pureLecturerName = omo[1]

                        //display lecture start and end time
                        holder.txtCourseTime.text =
                            "${model.start_time} \n    to \n${model.end_time}"
                        lectureTime = model.time.toString()

                        holder.setClick(object : IRecyclerItemClickListener {
                            override fun onItemClickListener(view: View, position: Int) {
                                activity?.toast("You Cannot Mark Attendance By This Time")
                            }
                        })
                    }

                }

            rv_yesterday.adapter = courseAdapter

        }
    }
}