package sv.asociacion.comunal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.asociacion.comunal.data.AuthRepository
import sv.asociacion.comunal.data.MeResponse
import sv.asociacion.comunal.update.AppUpdate
import sv.asociacion.comunal.update.UpdateRepository
import sv.asociacion.comunal.update.UpdateState

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val profile: MeResponse) : SessionState
    data class PasswordChange(val profile: MeResponse, val temporaryPassword: String? = null) : SessionState
    data class Error(val message: String) : SessionState
}

class MainViewModel(private val repository: AuthRepository, private val updates: UpdateRepository) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init { viewModelScope.launch { val restored=repository.restore(); _state.value = restored?.let { if(it.user.passwordChangeRequired) SessionState.PasswordChange(it) else SessionState.SignedIn(it) } ?: SessionState.SignedOut; checkUpdates() } }

    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = SessionState.Loading
        _state.value = runCatching { val profile=repository.login(username, password); if(profile.user.passwordChangeRequired) SessionState.PasswordChange(profile, password) else SessionState.SignedIn(profile) }
            .getOrElse { SessionState.Error("No fue posible iniciar sesión. Verifica tus datos y conexión.") }
    }

    fun dismissError() { _state.value = SessionState.SignedOut }
    fun logout() = viewModelScope.launch { repository.logout(); _state.value = SessionState.SignedOut }
    fun changePassword(current: String, next: String) = viewModelScope.launch {
        val profile = (_state.value as? SessionState.PasswordChange)?.profile ?: return@launch
        _state.value = SessionState.Loading
        _state.value = runCatching { SessionState.SignedIn(repository.changePassword(current, next)) }
            .getOrElse { SessionState.PasswordChange(profile) }
    }

    fun checkUpdates() = viewModelScope.launch {
        if (_updateState.value is UpdateState.Downloading) return@launch
        _updateState.value = UpdateState.Checking
        _updateState.value = runCatching { updates.findUpdate()?.let(UpdateState::Available) ?: UpdateState.Idle }
            .getOrElse { UpdateState.Error("No se pudo comprobar la actualización.") }
    }
    fun downloadUpdate(update: AppUpdate) = viewModelScope.launch {
        _updateState.value = UpdateState.Downloading(update, 0)
        _updateState.value = runCatching {
            val file = updates.download(update) { percent -> _updateState.value = UpdateState.Downloading(update, percent) }
            UpdateState.Ready(update, file)
        }.getOrElse { UpdateState.Error("No se pudo descargar la actualización.") }
    }
    fun dismissUpdate() { if (_updateState.value !is UpdateState.Downloading) _updateState.value = UpdateState.Idle }
}
