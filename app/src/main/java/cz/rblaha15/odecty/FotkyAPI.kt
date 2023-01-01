package cz.rblaha15.odecty

import androidx.lifecycle.DefaultLifecycleObserver
import java.io.File

internal interface FotkyAPI : DefaultLifecycleObserver {

    fun vyfotitFotku(fileName: String, callback: (File) -> Unit)
}