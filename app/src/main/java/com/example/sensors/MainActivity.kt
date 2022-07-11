package com.example.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sensors.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File

    //    SENSORES
    private lateinit var mSensorManager: SensorManager
    private var mAccelerometer: Sensor? = null
    private var mLight: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()

        if (allPermissionGranted()) {
            Toast.makeText(this, "We have Permissions", Toast.LENGTH_SHORT).show()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btPhoto.setOnClickListener {
            takePhoto()
        }

//        SENSORES
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto() {
        val imagecapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        imagecapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"
                    Toast.makeText(this@MainActivity, "${msg} ${savedUri}", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )

    }

    private fun takeZoom() {

    }

    private fun changeFlashState(state: Boolean) {

    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        cameraProviderFuture.addListener({
            val preview = Preview.Builder()
                .build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }

            imageCapture = ImageCapture.Builder().build()

            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.d(Constants.TAG, "startCamera Fail: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                //our code
                startCamera()
            } else {
                Toast.makeText(
                    this, "Permission not granted by the user!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }


    //    SENSORES
    var luz: Float = 0F
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event)
        } else if (event.sensor.type == Sensor.TYPE_LIGHT) {
            //luz 90 evento 200
            if (((luz - event.values[0]) > 100) || ((event.values[0] - luz) > 100)) {
                luz = event.values[0]

                if (event.values[0] < 300) {
                    makeToast(3)
                    changeFlashState(true)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    /**
     * funcões para movimento
     */
    var emQueda: Int = 0
    var emMoviementoRapido: Int = 0
    var estaEscuro: Int = 0

    private fun makeToast(tipo: Int) {
        if (emQueda == 0 && tipo == 1) {
            Toast.makeText(this, "Está em queda livre", Toast.LENGTH_LONG).show()
            emQueda = 1
        } else if (emMoviementoRapido == 0 && tipo == 2) {
            Toast.makeText(this, "Movimento violento", Toast.LENGTH_LONG).show()
            emMoviementoRapido = 1
            emQueda = 1
        } else if (estaEscuro == 0 && tipo == 3) {
            Toast.makeText(this, "Está escuro", Toast.LENGTH_LONG).show()
            estaEscuro = 1
        }
        Timer().schedule(object : TimerTask() {
            override fun run() {
                emQueda = 0
                emMoviementoRapido = 0
                estaEscuro = 0
                changeFlashState(false)
            }
        }, 5000)

    }

    private fun getAccelerometer(event: SensorEvent) {
        val values = event.values
        // Movement
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val accelationSquareRoot = ((x * x + y * y + z * z))
        /// (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH))
        if (accelationSquareRoot < 1 && emQueda == 0) {
            makeToast(1)
            takePhoto()
        } else if (accelationSquareRoot > 2000 && emMoviementoRapido == 0) {
            makeToast(2)
            takeZoom()
        }
    }

}