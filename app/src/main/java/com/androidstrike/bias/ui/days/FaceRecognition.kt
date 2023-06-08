package com.androidstrike.bias.ui.days

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.androidstrike.bias.R
import com.androidstrike.bias.databinding.FragmentFaceRecognitionBinding
import com.androidstrike.bias.services.SimilarityClassifier
import com.androidstrike.bias.utils.toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.ReadOnlyBufferException
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.experimental.inv
import android.util.Pair
import android.util.Size
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.bias.model.CourseDetail
import com.androidstrike.bias.utils.Common
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FaceRecognition : Fragment() {


    private var _binding: FragmentFaceRecognitionBinding? = null
    private val binding get() = _binding!!

    val args: FaceRecognitionArgs by navArgs()
    lateinit var attendanceQuestion: String
    lateinit var attendanceAnswer: String
    lateinit var lectureTimeInMillis: String
    lateinit var courseDetail: CourseDetail

    var detector: FaceDetector? = null


    var cam_face = CameraSelector.LENS_FACING_BACK //Default Back Camera
    var start = true
    var flipX = false
    var cameraProvider: ProcessCameraProvider? = null
    var tfLite: Interpreter? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null


    var intValues: IntArray = intArrayOf()
    var inputSize = 112 //Input size for model

    var isModelQuantized = false
    var embeedings: Array<FloatArray> = arrayOf(floatArrayOf())
    var IMAGE_MEAN = 128.0f
    var IMAGE_STD = 128.0f
    var OUTPUT_SIZE = 192 //Output size of model

    private var registered: java.util.HashMap<String, SimilarityClassifier.Recognition> =
        HashMap<String, SimilarityClassifier.Recognition>() //saved Faces


    private val SELECT_PICTURE = 1


    var modelFile = "mobile_face_net.tflite" //model name
    var isAttended = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFaceRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attendanceQuestion = args.attendanceQuestion
        attendanceAnswer = args.attendanceAnswer
        lectureTimeInMillis = args.lectureTimeInMillis
        courseDetail = args.courseDetail
        registered = readFromSP()!! //Load saved faces from memory when app starts


        with(binding) {
            loanPreviewInfo.text = "\n      Recognized Face:"
            //On-screen switch to toggle between Cameras.
            loanCustomerCameraSwitch.setOnClickListener(View.OnClickListener {
                if (cam_face == CameraSelector.LENS_FACING_BACK) {
                    cam_face = CameraSelector.LENS_FACING_FRONT
                    flipX = true
                } else {
                    cam_face = CameraSelector.LENS_FACING_BACK
                    flipX = false
                }
                cameraProvider?.unbindAll()
                cameraBind()
//                val directions = LoanApplicationDirections.actionLoanApplicationToCustomerVerified(loanRecoName.text.toString())
//                findNavController().navigate(directions)
            })
        }
        //Load model
        try {
            tfLite = Interpreter(loadModelFile(requireActivity(), modelFile)!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //Initialize Face Detector
        //Initialize Face Detector
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)



        cameraBind()

    }

    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity, MODEL_FILE: String): MappedByteBuffer? {
        val fileDescriptor = activity.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    //Bind camera and preview view
    private fun cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture!!.addListener(Runnable {
            try {
                cameraProvider = cameraProviderFuture!!.get()
                bindPreview(cameraProvider!!)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this in Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    //Load Faces from Shared Preferences.Json String to Recognition object
    private fun readFromSP(): java.util.HashMap<String, SimilarityClassifier.Recognition>? {
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("HashMap", Context.MODE_PRIVATE)
        val defValue = Gson().toJson(java.util.HashMap<String, SimilarityClassifier.Recognition>())
        val json = sharedPreferences.getString("map", defValue)
        // System.out.println("Output json"+json.toString());
        val token: TypeToken<java.util.HashMap<String, SimilarityClassifier.Recognition>> =
            object : TypeToken<java.util.HashMap<String, SimilarityClassifier.Recognition>>() {}
        val retrievedMap: java.util.HashMap<String, SimilarityClassifier.Recognition> =
            Gson().fromJson<java.util.HashMap<String, SimilarityClassifier.Recognition>>(
                json,
                token.type
            )
        // System.out.println("Output map"+retrievedMap.toString());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        for (entry in retrievedMap) {
            val output = Array(1) {
                FloatArray(
                    OUTPUT_SIZE
                )
            }
            var arrayList = entry.value.extra as java.util.ArrayList<*>
            arrayList = arrayList[0] as java.util.ArrayList<*>
            for (counter in arrayList.indices) {
                output[0][counter] = (arrayList[counter] as Double).toFloat()
            }
            entry.value.extra = output

            //System.out.println("Entry output "+entry.getKey()+" "+entry.getValue().getExtra() );

        }
        //        System.out.println("OUTPUT"+ Arrays.deepToString(outut));
        requireContext().toast("Recognitions Loaded")
        return retrievedMap
    }


    @SuppressLint("UnsafeOptInUsageError")
    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        with(binding) {
            val preview = Preview.Builder()
                .build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build()
            preview.setSurfaceProvider(loanPreviewView.getSurfaceProvider())
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                .build()
            val executor: Executor = Executors.newSingleThreadExecutor()
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                var image: InputImage? = null
                @SuppressLint("UnsafeExperimentalUsageError") val mediaImage// Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)
                        = imageProxy.image
                if (mediaImage != null) {
                    image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    println("Rotation " + imageProxy.imageInfo.rotationDegrees)
                }
                println("ANALYSIS")

                //Process acquired image to detect faces
                val result: Task<List<Face>> =
                    detector!!.process(image)
                        .addOnSuccessListener(
                            OnSuccessListener<List<Face>> { faces ->
                                if (faces.size != 0) {
                                    val face = faces[0] //Get first face from detected faces
                                    println(face)

                                    //mediaImage to Bitmap
                                    val frame_bmp: Bitmap = toBitmap(mediaImage!!)!!
                                    val rot = imageProxy.imageInfo.rotationDegrees

                                    //Adjust orientation of Face
                                    val frame_bmp1: Bitmap =
                                        rotateBitmap(frame_bmp, rot, false, false)!!


                                    //Get bounding box of face
                                    val boundingBox = RectF(face.boundingBox)

                                    //Crop out bounding box from whole Bitmap(image)
                                    var cropped_face: Bitmap =
                                        getCropBitmapByCPU(frame_bmp1, boundingBox)!!
                                    if (flipX) cropped_face =
                                        rotateBitmap(cropped_face, 0, flipX, false)!!
                                    //Scale the acquired Face to 112*112 which is required input for model
                                    val scaled: Bitmap = getResizedBitmap(cropped_face, 112, 112)!!
                                    if (start) recognizeImage(scaled) //Send scaled bitmap to create face embeddings.
                                    println(boundingBox)
                                    try {
                                        Thread.sleep(10) //Camera preview refreshed every 10 millisec(adjust as required)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    if (registered.isEmpty()) loanRecoName.setText("Add Face") else loanRecoName.setText(
                                        "No Face Detected!"
                                    )
                                }
                            })
                        .addOnFailureListener(
                            OnFailureListener {
                                // Task failed with an exception
                                // ...
                            })
                        .addOnCompleteListener(OnCompleteListener<List<Face?>?> {
                            imageProxy.close() //v.important to acquire next frame for analysis
                        })
            }
            cameraProvider.bindToLifecycle(
                (requireActivity() as LifecycleOwner),
                cameraSelector,
                imageAnalysis,
                preview
            )
        }

    }


    private fun toBitmap(image: Image): Bitmap? {
        val nv21: ByteArray = YUV_420_888toNV21(image)!!
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        //System.out.println("bytes"+ Arrays.toString(imageBytes));

        //System.out.println("FORMAT"+image.getFormat());
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun rotateBitmap(
        bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean
    ): Bitmap? {
        val matrix = Matrix()

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees.toFloat())

        // Mirror the image along the X or Y axis.
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    //    public void register(String name, SimilarityClassifier.Recognition rec) {
    //        registered.put(name, rec);
    //    }
    private fun findNearest(emb: FloatArray): Pair<String, Float>? {
        var ret: Pair<String, Float>? = null
        for (entry in registered.entries) {
            val name = entry.key
            val knownEmb: FloatArray =
                ((entry.value.extra as Array<*>)[0] as FloatArray?)!!//(entry.value.extra as FloatArray)[0]
            var distance = 0f
            for (i in emb.indices) {
                val diff = emb[i] - knownEmb[i]
                distance += diff * diff
            }
            distance = Math.sqrt(distance.toDouble()).toFloat()
            if (ret == null || distance < ret.second) {
                ret = Pair(name, distance)
            }
        }
        return ret
    }


    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
        bm.recycle()
        return resizedBitmap
    }


    private fun getCropBitmapByCPU(source: Bitmap?, cropRectF: RectF): Bitmap? {
        val resultBitmap = Bitmap.createBitmap(
            cropRectF.width().toInt(),
            cropRectF.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val cavas = Canvas(resultBitmap)

        // draw background
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        cavas.drawRect( //from  w w  w. ja v  a  2s. c  om
            RectF(0F, 0F, cropRectF.width(), cropRectF.height()),
            paint
        )
        val matrix = Matrix()
        matrix.postTranslate(-cropRectF.left, -cropRectF.top)
        cavas.drawBitmap(source!!, matrix, paint)
        if (source != null && !source.isRecycled) {
            source.recycle()
        }
        return resultBitmap
    }


    //IMPORTANT. If conversion not done ,the toBitmap conversion does not work on some devices.
    private fun YUV_420_888toNV21(image: Image): ByteArray? {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[nv21, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong() // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer[nv21, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel = vBuffer[1]
            try {
                vBuffer.put(1, savePixel.inv().toByte())
                if (uBuffer[0] == savePixel.inv().toByte()) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer[nv21, ySize, 1]
                    uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
            }
        }
        return nv21
    }

    fun recognizeImage(bitmap: Bitmap) {

        with(binding) {
            // set Face to Preview
            loanFacePreview.setImageBitmap(bitmap)

            //Create ByteBuffer to store normalized image
            val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            imgData.order(ByteOrder.nativeOrder())
            intValues = IntArray(inputSize * inputSize)

            //get pixel values from Bitmap to normalize
            bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            imgData.rewind()
            for (i in 0 until inputSize) {
                for (j in 0 until inputSize) {
                    val pixelValue: Int = intValues.get(i * inputSize + j)
                    if (isModelQuantized) {
                        // Quantized model
                        imgData.put((pixelValue shr 16 and 0xFF).toByte())
                        imgData.put((pixelValue shr 8 and 0xFF).toByte())
                        imgData.put((pixelValue and 0xFF).toByte())
                    } else { // Float model
                        imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    }
                }
            }
            //imgData is input to our model
            val inputArray = arrayOf<Any>(imgData)
            val outputMap: MutableMap<Int, Any> = HashMap()
            embeedings =
                Array(1) { FloatArray(OUTPUT_SIZE) } //output of model will be stored in this variable
            outputMap[0] = embeedings
            tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap) //Run model
            var distance = Float.MAX_VALUE
            val id = "0"
            var label: String? = "?"

            //Compare new face with saved Faces.
            if (registered.size > 0) {
                val nearest: Pair<String, Float> =
                    findNearest(embeedings.get(0))!! //Find closest matching face
                if (nearest != null) {
                    val name = nearest.first
                    label = name
                    distance = nearest.second
                    if (distance < 1.000f) {
                        //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                        requireContext().toast("Student Recognized")
                        loanRecoName.setText(name)
                        cameraProvider!!.unbindAll()
                        showAttendanceDialog()

                        //val directions = LoanApplicationDirections.actionLoanApplicationToCustomerVerified(binding.loanRecoName.text.toString())
                        //findNavController().navigate(directions)

                    } else {
                        loanRecoName.setText("Unrecognized")
                    }
                    println("nearest: $name - distance: $distance")
                }
            }


//            final int numDetectionsOutput = 1;
//            final ArrayList<SimilarityClassifier.Recognition> recognitions = new ArrayList<>(numDetectionsOutput);
//            SimilarityClassifier.Recognition rec = new SimilarityClassifier.Recognition(
//                    id,
//                    label,
//                    distance);
//
//            recognitions.add( rec );


        }

    }

    private fun showAttendanceDialog() {

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
                activity?.toast("Attendance Taken")
                isAttended = true
                saveAttendance()
                dialog.dismiss()

            } else {
                //this block is supposed to set a limit to the number of attempts a uer has to answer the class question
                attendanceQuestionTrialCount = attendancePrintTrialCount+1
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
                    textInputLayout.error = "Answer is required"
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

    private fun saveAttendance() {
        //?? Trying to figure its purpose
        val sharedPref =
            requireActivity().getSharedPreferences(
                "Is_Attended",
                Context.MODE_PRIVATE
            )
        val editor = sharedPref.edit()
        editor.putBoolean(lectureTimeInMillis, true)
        editor.apply()
//        //?? still trying to figure out its purpose in this code
//        val sharedPref =
//            requireActivity().getSharedPreferences(
//                "VerificationStatus",
//                Context.MODE_PRIVATE
//            )
//        val editor = sharedPref.edit()
//        editor.putBoolean("isVerified", true)
//        editor.apply()
        var attendanceHashSubMapToAdd =
            HashMap<String, Map<String, Boolean>>()
        var attendanceHashMapToAdd =
            HashMap<String, Map<String, Map<String, Boolean>>>()
        attendanceHashSubMapToAdd["${courseDetail.course}"] =
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
                    Common.userId.toString()
                ).get().await()

            if (mapQuery.documents.isNotEmpty()) {
                for (document in mapQuery) {
                    try {
                        //sets the attendance reult hashmap to the cloud under that student's profile
                        Common.userCollectionRef.document(document.id)
                            .set(
                                attendanceHashMapToAdd,
                                SetOptions.merge()
                            ).await()
                        withContext(Dispatchers.Main){
                            val navBackToToday = FaceRecognitionDirections.actionFaceRecognitionToMenuToday()
                            findNavController().navigate(navBackToToday)
                        }
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

    fun attendanceMapFun(time: String, truth: Boolean): HashMap<String, Boolean> {
        var attendanceHashMap = HashMap<String, Boolean>()
        attendanceHashMap[time] = truth
        return attendanceHashMap
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



}