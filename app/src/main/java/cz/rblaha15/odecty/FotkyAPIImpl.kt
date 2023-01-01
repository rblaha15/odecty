package cz.rblaha15.odecty

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File

internal class FotkyAPIImpl(private val activity: ComponentActivity) : DefaultLifecycleObserver, FotkyAPI {

    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private lateinit var requestPermission: ActivityResultLauncher<String>

    override fun onCreate(owner: LifecycleOwner) {
        takePicture = activity.activityResultRegistry.register("", owner, ActivityResultContracts.TakePicture()) {
            println(it)
            if (!it) return@register

            val file = File(activity.filesDir, "/$fileName.jpg")
            callback(file)
        }
        requestPermission = activity.activityResultRegistry.register("2", owner, ActivityResultContracts.RequestPermission()) {
            println(it)
            if (!it) return@register

            val file = File(activity.filesDir, "/$fileName.jpg")
            val imageUri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)

            takePicture.launch(imageUri)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {

        takePicture.unregister()
        requestPermission.unregister()
    }

    private lateinit var fileName: String
    private lateinit var callback: (File) -> Unit

    override fun vyfotitFotku(fileName: String, callback: (File) -> Unit) {
        this.fileName = fileName
        this.callback = callback

        val iHavePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        else
            checkSelfPermission(activity, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED

        if (!iHavePermission) {
            requestPermission.launch(Manifest.permission.CAMERA)
        } else {

            val file = File(activity.filesDir, "/$fileName.jpg")
            val imageUri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)

            takePicture.launch(imageUri)
        }
    }
}