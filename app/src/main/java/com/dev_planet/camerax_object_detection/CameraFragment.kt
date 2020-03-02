package com.dev_planet.camerax_object_detection


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import kotlinx.android.synthetic.main.fragment_camera.*
import java.util.concurrent.Executors


class CameraFragment : Fragment() {


    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>


    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraProviderFuture = ProcessCameraProvider.getInstance(context.applicationContext)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        },ContextCompat.getMainExecutor(context))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            (activity as? MainActivity)?.moveToPermission()
        }
    }
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetName("Preview")
            .build()

        preview.setSurfaceProvider(previewView.previewSurfaceProvider)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            Executors.newSingleThreadExecutor(),
            ObjectDetectionAnalyzer {
                overlay?.post {
                    updateOverlay(it)
                }
            }
        )

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }
    private fun updateOverlay(detectedObjects: DetectedObjects) {

        if (detectedObjects.objects.isEmpty()) {
            overlay.set(emptyList())
            return
        }

        overlay.setSize(detectedObjects.imageWidth, detectedObjects.imageHeight)

        val list = mutableListOf<BoxData>()

        for (obj in detectedObjects.objects) {

            val box = obj.boundingBox

            val name = "${categoryNames[obj.classificationCategory]}"

            val confidence =
                if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                    val confidence: Int =
                        obj.classificationConfidence!!.times(100).toInt()
                    " $confidence%"
                } else {
                    ""
                }

            list.add(BoxData("$name$confidence", box))
        }

        overlay.set(list)
    }


}
