package sv.asociacion.comunal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SimpleViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <R : ViewModel> create(modelClass: Class<R>): R = create() as R
}
