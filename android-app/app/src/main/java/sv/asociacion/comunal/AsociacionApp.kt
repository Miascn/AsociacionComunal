package sv.asociacion.comunal

import android.app.Application
import sv.asociacion.comunal.data.AppContainer

class AsociacionApp : Application() {
    val container by lazy { AppContainer(this) }
}
