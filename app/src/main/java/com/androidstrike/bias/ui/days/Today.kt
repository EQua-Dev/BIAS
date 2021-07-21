package com.androidstrike.bias.ui.days

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidstrike.bias.BuildConfig
import com.androidstrike.bias.R
import com.androidstrike.bias.adapters.CourseAdapter
import com.androidstrike.bias.model.CourseDetail
import com.androidstrike.bias.model.User
//import com.androidstrike.tera.data.CourseDetail
//import com.androidstrike.tera.ui.adapter.CourseAdapter
import com.androidstrike.bias.utils.Common
//import com.androidstrike.bias.utils.Common.caritasLocationLatitude
import com.androidstrike.bias.utils.Common.caritasLocationLatitudeNew
//import com.androidstrike.bias.utils.Common.caritasLocationLongitude
import com.androidstrike.bias.utils.Common.caritasLocationLongitudeNew
import com.androidstrike.bias.utils.Common.userCollectionRef
import com.androidstrike.bias.utils.Common.userId
import com.androidstrike.bias.utils.IRecyclerItemClickListener
//import com.androidstrike.tera.utils.IRecyclerItemClickListener
import com.androidstrike.bias.utils.toast
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
//import com.ramotion.fluidslider.FluidSlider
import kotlinx.android.synthetic.main.fragment_today.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.HashMap

class Today : Fragment() {

    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Represents a geographical location.
     */
    protected var mLastLocation: Location? = null

    private var mLatitudeLabel: String? = null
    private var mLongitudeLabel: String? = null

    lateinit var userLong: String
    lateinit var userLat: String
    private lateinit var container: View


    var courseAdapter: FirestoreRecyclerAdapter<CourseDetail, CourseAdapter>? = null
    lateinit var lectureTime: String
    lateinit var lectureTimeInMillis: String
    lateinit var todayRecyclerView: RecyclerView
    var user: User? = null

    lateinit var executor: Executor
    lateinit var biometricPrompt: androidx.biometric.BiometricPrompt
    lateinit var promptInfo: androidx.biometric.BiometricPrompt.PromptInfo

    var isAttended = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        isThereConnection()
        todayRecyclerView = rv_today
        container = layout_today


        getRealTimeCourses()

        val layoutManager = LinearLayoutManager(requireContext())
        todayRecyclerView.layoutManager = layoutManager
        todayRecyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getRealTimeCourses() {
        //if the 'today' is Friday or Saturday, show the text below
        if (Common.dowGood == "Friday" || Common.dowGood == "Saturday") {
            txt.visibility = View.VISIBLE
            txt.text = "Lecture Free Day\nEnjoy the Weekend!"
        } else if (Common.dowGood == "Sunday") {
            //if the 'today' is Sunday, show the text below
            txt.visibility = View.VISIBLE
            txt.text = "Lecture Free Day \n Prepare for Tomorrow!"
        } else {
            //if the 'today' is between Monday and Thursday, fetch the day of the week
            // ...and access its corresponding collection in the firestore
            //and then fetch the courses for that very day
            val firestore = FirebaseFirestore.getInstance()
            val todayCourses = firestore.collection(Common.dowGood)
                .whereEqualTo("course_time", "${Common.formattedToday}")
//            val query = todayCourses.whereEqualTo("course_time", "${Common.formattedToday}")
            val options = FirestoreRecyclerOptions.Builder<CourseDetail>()
                .setQuery(todayCourses, CourseDetail::class.java).build()

            try {
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

                            val nameOfLecturer = model.lecturer.toString()

                            val omo = nameOfLecturer.split(" ".toRegex())
                            val pureLecturerName = omo[1]

                            val nameOfCourse = model.course.toString()

                            //display lecture start and end time
                            holder.txtCourseTime.text =
                                "${model.start_time} \n    to \n${model.end_time}"
                            lectureTime = model.time.toString()
                            val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
                            try {
                                val mDate = sdf.parse(lectureTime)
                                lectureTimeInMillis = mDate!!.time.toString()
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }
//

                            holder.setClick(object : IRecyclerItemClickListener {
                                override fun onItemClickListener(view: View, position: Int) {

                                    Log.d("LatTruth", "onItemClickListener: ${isLatitudeEqual()}")
                                    Log.d("LongTruth", "onItemClickListener: ${isLongitudeEqual()}")
                                    val c = Calendar.getInstance()
                                    val format = SimpleDateFormat("HH:mm")
                                    val currentTime = format.format(c.time)

                                    val classOver = model.end_time
                                    val classStart = model.start_time
                                    Log.d(
                                        "EQUALOCATION",
                                        "onItemClickListener: $mLatitudeLabel \n ${Common.caritasLocationLatitudeNew}"
                                    )
                                    //check if the current time is within the lecture time
                                    if (currentTime.compareTo(classStart.toString()) < 0 || currentTime.compareTo(
                                            classOver.toString()
                                        ) > 0
                                    ) {
                                        activity?.toast("You Cannot Mark Attendance By This Time")
                                    } else {
                                        //check if user location corresponds with lecture location
                                        if (!isLatitudeEqual() || !isLongitudeEqual()) {
                                            activity?.toast("You are not in the class")

                                        } else {

                                            val timeNow = LocalTime.now()
//                                    val fmtTimeNow =
//                                        timeNow.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

//                                            isRated()

                                            if (isRated()) {//(sharedPrefs.getBoolean("${model.time}", true)) {
                                                //check if the class has already been rated
                                                activity?.toast("Attendance Already Marked")
                                            } else {
                                                //fetches the class attendance verification question
                                                val attendanceQuestion =
                                                    model.attendance_question
                                                //..and answer
                                                val attendanceAnswer = model.attendance_answer

                                                //shows the question in a dialog
                                                showAttendanceDialog(
                                                    attendanceQuestion,
                                                    attendanceAnswer
                                                )

                                                //?? Trying to figure its purpose
                                                val sharedPref =
                                                    requireActivity().getSharedPreferences(
                                                        "Is_Rated",
                                                        Context.MODE_PRIVATE
                                                    )
                                                val editor = sharedPref.edit()
                                                editor.putBoolean(lectureTimeInMillis, false)
                                                editor.apply()

                                                //the code lines below prepare a hashmap to contain the result of the attendance take
                                                /*the structure is akin to
                                                Classes
                                                    CourseName
                                                        timeInMillis: isAttended(Boolean)
                                                    CourseName
                                                        timeInMillis: isAttended(Boolean)
                                                    CourseName
                                                        timeInMillis: isAttended(Boolean)
                                                 */

                                                var attendanceHashSubMapToAdd =
                                                    HashMap<String, Map<String, Boolean>>()
                                                var attendanceHashMapToAdd =
                                                    HashMap<String, Map<String, Map<String, Boolean>>>()
                                                attendanceHashSubMapToAdd["${model.course}"] =
                                                    attendanceMapFun(
                                                        lectureTimeInMillis,
                                                        isAttended
                                                    )
                                                attendanceHashMapToAdd["Classes"] =
                                                    attendanceHashSubMapToAdd
//                                                user?.classes = attendanceHashSubMapToAdd
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val mapQuery =
                                                        Common.userCollectionRef.whereEqualTo(
                                                            "uid",
                                                            userId.toString()
                                                        ).get().await()

                                                    if (mapQuery.documents.isNotEmpty()) {
                                                        for (document in mapQuery) {
                                                            try {
                                                                //sets the attendance reult hashmap to the cloud under that student's profile
                                                                userCollectionRef.document(document.id)
                                                                    .set(
                                                                        attendanceHashMapToAdd,
                                                                        SetOptions.merge()
                                                                    ).await()
                                                            } catch (e: Exception) {
                                                                activity?.toast(e.message.toString())
                                                                Log.d(
                                                                    "Equa",
                                                                    "onItemClickListener: ${e.message.toString()}"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
//                                            }
                                        }
                                    }
                                }
                            })
                        }

                    }

            } catch (e: Exception) {
                activity?.toast(e.message.toString())
            }


        }
        courseAdapter?.startListening()
        todayRecyclerView.adapter = courseAdapter
    }


    fun isLatitudeEqual(): Boolean {
        //function to check if the user latitude is same with class latitude
        if (userLat == caritasLocationLatitudeNew) {
            return true
        }
        return false
    }

    fun isLongitudeEqual(): Boolean {
        //function to check if the user longitude is same with class longitude
        if (userLong == caritasLocationLongitudeNew) {
            return true
        }
        return false
    }

    private fun showAttendanceDialog(attendanceQuestion: String?, attendanceAnswer: String?) {

        //the lines of code below set up an edittext layout programmatically
        val etBuilder = MaterialAlertDialogBuilder(requireContext())

        etBuilder.setTitle(attendanceQuestion)

        val constraintLayout = getEditTextLayout(requireContext())
        etBuilder.setView(constraintLayout)

        val textInputLayout =
            constraintLayout.findViewWithTag<TextInputLayout>("textInputLayoutTag")
        val textInputEditText =
            constraintLayout.findViewWithTag<TextInputEditText>("textInputEditTextTag")
        textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER

        val attendancePrintTrialCount = 0
        var attendanceQuestionTrialCount = 0
        // alert dialog positive button
        etBuilder.setPositiveButton("Submit") { dialog, which ->
            val userAnswer: String = textInputEditText.text.toString()
            if (userAnswer == attendanceAnswer) {
                //if the user's answer is correct, prompt for biometric authentication
                promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Take Attendance using fingerprint")
                    .setNegativeButtonText("Cancel")
                    .build()
                executor = ContextCompat.getMainExecutor(requireContext())


                biometricPrompt = androidx.biometric.BiometricPrompt(requireActivity(), executor,
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            activity?.toast("Error: $errString")
//                            biometricPrompt.cancelAuthentication()
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            activity?.toast("Attendance Taken")
                            isAttended = true
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            activity?.toast("Authentication Failed, Try After 1 Minute")
//                            biometricPrompt.cancelAuthentication()
//                            Handler().postDelayed({

//                            }, 100000)
                            dialog.dismiss()
                        }
                    })

                biometricPrompt.authenticate(promptInfo)

                //?? still trying to figure out its purpose in this code
                val sharedPref =
                    requireActivity().getSharedPreferences(
                        "VerificationStatus",
                        Context.MODE_PRIVATE
                    )
                val editor = sharedPref.edit()
                editor.putBoolean("isVerified", true)
                editor.apply()

            } else {
                //this block is supposed to set a limit to the number of attempts a uer has to answer the class question
                attendanceQuestionTrialCount++
                when (attendanceQuestionTrialCount) {
                    1 -> {
                        // TODO: 04/07/2021 2: If answer is wrong, give 2 more chances
                        activity?.toast("Code not valid. 2 more attempts!")
                    }
                    2 -> {
                        activity?.toast("Code not valid. 1 more attempt!")
                    }
                    3 -> {
                        // TODO: 04/07/2021 3: If answer is wrong 3x, try again after 1 minutes
                        activity?.toast("Too many attempts, Try again later!")
                        Handler().postDelayed({

                        }, 100000)
                        dialog.dismiss()
                    }
                }

            }
        }

        // alert dialog other buttons
        etBuilder.setNegativeButton("No", null)
        etBuilder.setNeutralButton("Cancel", null)

        // set dialog non cancelable
        etBuilder.setCancelable(false)

        // finally, create the alert dialog and show it
        val dialog = etBuilder.create()

        dialog.show()

        // initially disable the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        // edit text text change listener
        textInputEditText.addTextChangedListener(object : TextWatcher {
            //sets a watcher to the edit text and performs certain functions and validation
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(
                p0: CharSequence?, p1: Int,
                p2: Int, p3: Int
            ) {
            }

            override fun onTextChanged(
                p0: CharSequence?, p1: Int,
                p2: Int, p3: Int
            ) {
                if (p0.isNullOrBlank()) {
                    textInputLayout.error = "Code is required"
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = false
                } else {
                    textInputLayout.error = ""
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = true
                }
            }
        })

    }

    fun attendanceMapFun(time: String, truth: Boolean): HashMap<String, Boolean> {
        var attendanceHashMap = HashMap<String, Boolean>()
        attendanceHashMap[time] = truth
        return attendanceHashMap
    }

    //?? still trying to figure out its purpose in this code
    private fun isRated(): Boolean {
        val sharedPrefs = requireActivity().getSharedPreferences(
            "Is_Attended",
            Context.MODE_PRIVATE
        )
        return sharedPrefs.getBoolean(lectureTimeInMillis, false)
    }

    private fun getEditTextLayout(context: Context): ConstraintLayout {
        //builds the edit text layout programmatically
        val constraintLayout = ConstraintLayout(context)
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        constraintLayout.layoutParams = layoutParams
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            constraintLayout.id = View.generateViewId()
        }

        val textInputLayout = TextInputLayout(context)
        textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layoutParams.setMargins(
            32.toDp(context),
            8.toDp(context),
            32.toDp(context),
            8.toDp(context)
        )
        textInputLayout.layoutParams = layoutParams
        textInputLayout.hint = "Input Answer"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textInputLayout.id = View.generateViewId()
        }
        textInputLayout.tag = "textInputLayoutTag"


        val textInputEditText = TextInputEditText(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textInputEditText.id = View.generateViewId()
        }
        textInputEditText.tag = "textInputEditTextTag"

        textInputLayout.addView(textInputEditText)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintLayout.addView(textInputLayout)
        return constraintLayout
    }

    // extension method to convert pixels to dp
    fun Int.toDp(context: Context): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
    ).toInt()

    private fun checkPermissions(): Boolean {
        //checks if the required app permissions are granted
        val permissionState = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        //prepares the perform the runtime permission
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }


    private fun requestPermissions() {
        //performs the runtime permission
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                View.OnClickListener {
                    // Request permission
                    startLocationPermissionRequest()
                })

        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation()
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                    View.OnClickListener {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action =
                            ACTION_APPLICATION_DETAILS_SETTINGS //BassBoost.Settings.//BassBoost.Settings.//.//ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            getLastLocation()
        }
    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     *
     *
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        //gets the location of the user using 'last known location' trick
        mFusedLocationClient!!.lastLocation
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful && task.result != null) {
                    mLastLocation = task.result

                    mLatitudeLabel = "${(mLastLocation)!!.latitude}"
                    mLongitudeLabel = "${(mLastLocation)!!.longitude}"

                    //gets the 1st 7 characters from the location
                    //this was in order to avoid making a comparison with the user's pin point location, which changes with even a change in static direction
                    userLong = mLongitudeLabel!!.take(7)
                    userLat = mLatitudeLabel!!.take(7)

                    Log.d("LocationHome", "getLastLocation: $mLatitudeLabel, $mLongitudeLabel")

                } else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    showMessage(getString(R.string.no_location_detected))
                }
            }
    }

    /**
     * Shows a [] using `text`.

     * @param text The Snackbar text.
     */
    private fun showMessage(text: String) {
        activity?.toast(text)
    }

    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {
        activity?.toast(getString(mainTextStringId))
    }

    companion object {

        private val TAG = "LocationProvider"

        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }

}