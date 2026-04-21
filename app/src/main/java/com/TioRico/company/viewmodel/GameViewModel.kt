
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TioRico.company.models.*
import com.TioRico.company.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository = GameRepository()) : ViewModel() {

    private val _room = MutableStateFlow<GameRoom?>(null)
    val room: StateFlow<GameRoom?> = _room.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createRoom(playerId: String, playerName: String, goal: Int = 5000) {
        if (playerId.isBlank()) {
            _error.value = "Error: User not correctly authenticated"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                val roomId = repository.createRoom(playerId, playerName, goal)
                observeRoom(roomId)
            } catch (e: Exception) {
                _error.value = "Error creating room: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun joinRoom(code: String, playerId: String, playerName: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val roomId = repository.joinRoom(code, playerId, playerName)
                if (roomId != null) {
                    observeRoom(roomId)
                } else {
                    _error.value = "Código de sala inválido o sala llena"
                }
            } catch (e: Exception) {
                _error.value = "Error al unirse: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startGame(roomId: String, goal: Int) {
        viewModelScope.launch {
            try {
                repository.startGame(roomId, goal)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun performAction(roomId: String, playerId: String, action: ActionType) {
        viewModelScope.launch {
            try {
                repository.performAction(roomId, playerId, action)
            } catch (e: Exception) {
                _error.value = "Error al realizar acción: ${e.message}"
            }
        }
    }

    private fun observeRoom(roomId: String) {
        viewModelScope.launch {
            repository.getRoom(roomId).collect {
                _room.value = it
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun leaveRoom(roomId: String, playerId: String) {
        viewModelScope.launch {
            try {
                repository.leaveRoom(roomId, playerId)
                _room.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}