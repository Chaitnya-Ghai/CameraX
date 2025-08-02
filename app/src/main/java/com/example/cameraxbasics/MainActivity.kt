package com.example.cameraxbasics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cameraxbasics.ui.theme.CameraXBasicsTheme
import com.example.cameraxbasics.ui.theme.PhotoBottomSheetContent
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    var permissionBoolean : Boolean ?= false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (permissionBoolean==false){
                requestPermissions()
            }
            CameraXBasicsTheme {
                val viewModel : MainViewModel = viewModel<MainViewModel>()
                val bitmaps by viewModel.bitmaps.collectAsState()
                val scope= rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val controller = LifecycleCameraController(applicationContext).apply {
                    setEnabledUseCases(
                        CameraController.IMAGE_CAPTURE
                                    or
                                        CameraController.VIDEO_CAPTURE
                    )
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetContent = {
                        PhotoBottomSheetContent(
                            modifier = Modifier
                                .fillMaxWidth(),
                            bitmaps
                        )
                    },
                    sheetPeekHeight = 0.dp
                ) { padding->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                    ){
                        CameraPreview(controller= controller , modifier = Modifier.fillMaxSize())

                        IconButton(
                            onClick = {
                                controller.cameraSelector =
                                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else CameraSelector.DEFAULT_BACK_CAMERA
                            },
                            modifier = Modifier.offset(21.dp, 21.dp))
                        {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch camera"
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            )
                            {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Switch camera"
                                )
                            }
                            IconButton(
                                onClick = {
                                    takePhoto(
                                        controller = controller,
                                        onPhotoTaken = { byteArray ->
                                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                            viewModel.capturePhoto(bitmap)
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take photo"
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (ByteArray) -> Unit
    ){
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback(){
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply{
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                    val stream = ByteArrayOutputStream()
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val byteArray = stream.toByteArray()

                    onPhotoTaken(byteArray)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)

                }
            }

        )
    }


//    handling permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
    val allPermissionsGranted = permissions.all { it.value }
    if (allPermissionsGranted) {
        permissionBoolean = true
    } else {
        // Do something if not all permissions granted
        requestPermissions()
    }
    }
    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            )
        )
    }
}
