package sv.asociacion.comunal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.asociacion.comunal.data.AuthRepository
import sv.asociacion.comunal.data.MeResponse

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val profile: MeResponse) : SessionState
    data class Error(val message: String) : SessionState
}

class MainViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init { viewModelScope.launch { _state.value = repository.restore()?.let(SessionState::SignedIn) ?: SessionState.SignedOut } }

    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = SessionState.Loading
        _state.value = runCatching { SessionState.SignedIn(repository.login(username, password)) }
            .getOrElse { SessionState.Error("No fue posible iniciar sesión. Verifica tus datos y conexión.") }
    }

    fun dismissError() { _state.value = SessionState.SignedOut }
    fun logout() = viewModelScope.launch { repository.logout(); _state.value = SessionState.SignedOut }
}
