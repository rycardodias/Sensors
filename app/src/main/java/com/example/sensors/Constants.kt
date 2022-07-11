package com.example.sensors

import android.Manifest

object Constants {

    const val TAG= "cameraX"
    const val FILE_NAME = "yy-MM-dd-HH-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}