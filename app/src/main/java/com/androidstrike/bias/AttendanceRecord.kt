package com.androidstrike.bias

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidstrike.bias.adapters.RecordsAdapter
import com.androidstrike.bias.databinding.FragmentAttendanceRecordBinding
import com.androidstrike.bias.databinding.FragmentYesterdayBinding
import com.androidstrike.bias.utils.Common
import com.androidstrike.bias.utils.getDate
import com.androidstrike.bias.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.SimpleFormatter


class AttendanceRecord : Fragment() {


    private var _binding: FragmentAttendanceRecordBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<AttendanceRecordArgs>()
    var dateNew: String? = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAttendanceRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //gets the selected course from the passed nav argument
        val course = args.courseName.course

        CoroutineScope(Dispatchers.IO).launch {
            //navigates to the user's profile
            val mapQuery = Common.userCollectionRef.whereEqualTo(
                "uid",
                Common.userId.toString()
            ).get().addOnSuccessListener { docsSnapshot ->
                for (doc in docsSnapshot.documents) {
                    if (doc != null) {
                        //navigates to the map of the selected course
                        val getDocs = doc.get("Classes.$course")
                        Log.d("EQua", "onActivityCreated: $course")
                        if (getDocs != null){
                            //fetches the key and values of the content and assigns it to a map
                            val mapNew: Map<String?, Any?> =
                                getDocs as Map<String?, Any?>
                            Log.d("EQua", mapNew.keys.toString())


                            for (key in mapNew.keys) {
                                //converts the key texts saved in milliseconds to date format
                                    //and saves it to the local hashmap
                                val dateLong: Long? = key?.toLong()
                                dateNew = getDate(dateLong, "dd/MM/yyyy").toString()
                                Common.attendanceMap[dateNew]
                            }

                            for (value in mapNew.values){
                                //saves the values of the date to their corresponding keys in the local map
                                    //PS: there could be a bug here where the value is not being assigned to its actual corresponding key
                                Common.attendanceMap[dateNew!!] = value as Boolean?
                                Log.d("EQua1", "onActivityCreated: $value")
                            }
                        }
                        else{
                            activity?.toast("No Attendance Record")
                        }

                    }
                    else {
                        activity?.toast("No Attendance Record")
                    }
                }
                Log.d("EQua", "onActivityCreated: ${Common.attendanceMap}")
            }
        }

        val layoutManager = LinearLayoutManager(requireContext())
        val recordAdapter = RecordsAdapter()
        binding.rvRecordHistory.adapter = recordAdapter

        binding.rvRecordHistory.layoutManager = layoutManager
        binding.rvRecordHistory.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )


    }

}