package sv.asociacion.comunal.update

import java.io.File

data class AppUpdate(val version: String, val downloadUrl: String, val notes: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate, val percent: Int) : UpdateState
    data class Ready(val update: AppUpdate, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}
