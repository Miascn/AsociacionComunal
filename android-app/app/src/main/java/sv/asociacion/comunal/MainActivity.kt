package sv.asociacion.comunal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import sv.asociacion.comunal.ui.AsociacionRoot
import sv.asociacion.comunal.ui.theme.AsociacionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as AsociacionApp).container.authRepository
        setContent {
            AsociacionTheme {
                val vm: MainViewModel = viewModel(factory = SimpleViewModelFactory { MainViewModel(repository, (application as AsociacionApp).container.updateRepository) })
                AsociacionRoot(vm)
            }
        }
    }
}
