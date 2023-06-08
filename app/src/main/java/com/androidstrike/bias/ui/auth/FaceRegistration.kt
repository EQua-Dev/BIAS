package com.androidstrike.bias.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.androidstrike.bias.R
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.media.Image
import android.text.InputType
import android.util.Pair
import android.util.Size
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.androidstrike.bias.services.SimilarityClassifier
import com.androidstrike.bias.ui.Landing
import com.androidstrike.bias.utils.Common
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
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
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

import com.google.mlkit.vision.face.FaceDetector

class FaceRegistration : Fragment() {

    lateinit var cameraSwitch: Button
    lateinit var addFace: Button
    lateinit var recoName: TextView
    lateinit var facePreview: ImageView
    lateinit var previewView: PreviewView

    val args: FaceRegistrationArgs by navArgs()
    lateinit var studentRegNo: String



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

    private val registered: java.util.HashMap<String, SimilarityClassifier.Recognition> =
        HashMap<String, SimilarityClassifier.Recognition>() //saved Faces


    private val SELECT_PICTURE = 1


    var modelFile = "mobile_face_net.tflite" //model name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_face_registration, container, false)

        cameraSwitch = view.findViewById(R.id.camera_switch)
        addFace = view.findViewById(R.id.add_face)
        recoName = view.findViewById(R.id.reco_name)
        facePreview = view.findViewById(R.id.face_preview)
        previewView = view.findViewById(R.id.previewView)

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studentRegNo = args.regNumber

        //Request permissions
        Dexter.withActivity(requireActivity()) //Dexter makes runtime permission easier to implement
            .withPermission(android.Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                }
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    requireContext().toast("Accept Permission")
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }
            }).check()

        //On-screen switch to toggle between Cameras.


//            previewInfo.text = "\n      Recognized Face:"
            //On-screen switch to toggle between Cameras.
            cameraSwitch.setOnClickListener(View.OnClickListener {
                if (cam_face == CameraSelector.LENS_FACING_BACK) {
                    cam_face = CameraSelector.LENS_FACING_FRONT
                    flipX = true
                } else {
                    cam_face = CameraSelector.LENS_FACING_BACK
                    flipX = false
                }
                cameraProvider?.unbindAll()
                cameraBind()
            })

            addFace.setOnClickListener{
                addFace()
            }



        //Load model

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

    private fun addFace() {
        run {
            start = false
            val builder =
                AlertDialog.Builder(
                    requireContext()
                )
            builder.setTitle("Face Reg No")

            // Set up the input
            val input = TextView(context)
            //input.inputType = InputType.TYPE_CLASS_TEXT
            input.text = studentRegNo
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton(
                "REGISTER"
            ) { dialog, which -> //Toast.makeText(context, input.getText().toString(), Toast.LENGTH_SHORT).show();

                //Create and Initialize new object with Face embeddings and Name.
                val result = SimilarityClassifier.Recognition(
                    "0", "", -1f
                )
                result.extra = embeedings
                registered[input.text.toString()] = result
                Common.new_student_reg_no = input.text.toString().trim()
                start = true

                insertToSP(registered, false)

//                cameraProvider!!.unbindAll()

                val i = Intent(requireContext(), Landing::class.java)
                startActivity(i)

                //val directions = FaceRegistrationDirections.actionFaceRegistrationToCreateCustomer(input.text.toString().trim())
                //findNavController().navigate(directions)

            }
            builder.setNegativeButton(
                "Cancel"
            ) { dialog, which ->
                start = true
                dialog.cancel()
            }
            builder.show()
        }
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


    //Save Faces to Shared Preferences.Conversion of Recognition objects to json string
    private fun insertToSP(jsonMap: java.util.HashMap<String, SimilarityClassifier.Recognition>, clear: Boolean) {
        if (clear) jsonMap.clear() else jsonMap.putAll(readFromSP()!!)
        val jsonString = Gson().toJson(jsonMap)
        //        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : jsonMap.entrySet())
//        {
//            System.out.println("Entry Input "+entry.getKey()+" "+  entry.getValue().getExtra());
//        }
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("HashMap", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("map", jsonString)
        //System.out.println("Input josn"+jsonString.toString());
        editor.apply()
        Toast.makeText(context, "Recognitions Saved", Toast.LENGTH_SHORT).show()
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
            Gson().fromJson<java.util.HashMap<String, SimilarityClassifier.Recognition>>(json, token.type)
        // System.out.println("Output map"+retrievedMap.toString());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        for (entry in retrievedMap){
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
        Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT).show()
        return retrievedMap
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

    @SuppressLint("UnsafeOptInUsageError")
    fun bindPreview(cameraProvider: ProcessCameraProvider) {

            val preview = Preview.Builder()
                .build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build()
            preview.setSurfaceProvider(previewView.getSurfaceProvider())
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
                    image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
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
                                    if (registered.isEmpty()) recoName.setText("Add Face") else recoName.setText(
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
        for (entry in registered.entries){
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

            // set Face to Preview
            facePreview.setImageBitmap(bitmap)

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
                    if (distance < 1.000f) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                        recoName.setText(name) else recoName.setText("Unknown")
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