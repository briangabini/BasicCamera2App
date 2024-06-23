package com.bgcoding.camera2api

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bgcoding.camera2api.ui.theme.Camera2ApiTheme
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    lateinit var captureRequest: CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var imageReader: ImageReader

    private val permissionsRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                // Permission was denied, handle this situation
            } else {
                // Permission was granted, you can now proceed with your operations
                setCameraPreview()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            allPermissionsGranted -> {
                // All permissions already granted
                setCameraPreview()
            }
            else -> {
                // Some permissions not granted, request them
                permissionsRequest.launch(permissions)
            }
        }

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("video thread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                /*TODO("Not yet implemented")*/

                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

        }

        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(object: ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(p0: ImageReader?) {

                var image = p0?.acquireLatestImage()
                var buffer = image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                var file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "img.jpeg")
                var fileOutputStream = FileOutputStream(file)

                fileOutputStream.write(bytes)

                image.close()
                fileOutputStream.close()

                /*Toast.makeText(this@MainActivity, "Image captured", Toast.LENGTH_SHORT).show()*/
                Toast.makeText(this@MainActivity, "Image captured and saved at ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        }, handler)

        findViewById<Button>(R.id.capture).apply {
            setOnClickListener {
                captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureRequest.addTarget(imageReader.surface)
                cameraCaptureSession.capture(captureRequest.build(), null, null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun open_camera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                var surface = Surface(textureView.surfaceTexture)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object: CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }, handler)
    }

    private fun setCameraPreview() {
        /*setContent {
            Camera2ApiTheme {

            }
        }*/
        setContentView(R.layout.activity_main)
    }

}