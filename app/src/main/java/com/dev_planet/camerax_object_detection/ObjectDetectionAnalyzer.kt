package com.dev_planet.camerax_object_detection

import android.annotation.SuppressLint
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import java.lang.Exception
import java.util.concurrent.TimeUnit

class ObjectDetectionAnalyzer(
    private val listener: (detectedObjects: DetectedObjects) -> Unit
) : ImageAnalysis.Analyzer {


    private val detector: FirebaseVisionObjectDetector

    init {
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .build()


        detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

    }


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val rotationDegrees = 0
        val processingImage = imageProxy.image
        if (processingImage == null) {
            imageProxy.close()
            return
        }

        val rotation = when (rotationDegrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_0
        }

        val image = FirebaseVisionImage.fromMediaImage(processingImage, rotation)

        try {
            val results = Tasks.await(detector.processImage(image), 1, TimeUnit.SECONDS)

            debugPrint(results)

            listener(DetectedObjects(rotationDegrees, processingImage, results))

        } catch (e: Exception) {
            println("failure : $e")
        }

        imageProxy.close()
    }

    private fun debugPrint(visionObjects: List<FirebaseVisionObject>) {
        for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox

            println("Detected object: $idx")
            println("  Category: ${categoryNames[obj.classificationCategory]}")
            println("  trackingId: ${obj.trackingId}")
            println("  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                println("  Confidence: $confidence%")
            }
        }
    }
}



class DetectedObjects(
    rotationDegrees: Int,
    image: Image,
    val objects: List<FirebaseVisionObject>
) {
    val imageWidth: Int
    val imageHeight: Int

    init {
        when (rotationDegrees) {
            90, 270 -> {
                imageWidth = image.height
                imageHeight = image.width
            }
            else -> {
                imageWidth = image.width
                imageHeight = image.height
            }
        }
    }

}

val categoryNames: Map<Int, String> = mapOf(
    FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
    FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
    FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
    FirebaseVisionObject.CATEGORY_FOOD to "Food",
    FirebaseVisionObject.CATEGORY_PLACE to "Place",
    FirebaseVisionObject.CATEGORY_PLANT to "Plant"
)