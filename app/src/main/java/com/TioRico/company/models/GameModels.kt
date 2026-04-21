package com.TioRico.company.models

enum class GameStatus {
    WAITING,
    PLAYING,
    FINISHED
}

enum class ActionType {
    SAVE,
    INVEST,
    SPEND
}

data class Player(
    val id: String = "",
    val name: String = "",
    val money: Int = 1000,
    val isReady: Boolean = false,
    val isEliminated: Boolean = false
)

data class GameHistoryItem(
    val turn: Int = 0,
    val playerName: String = "",
    val action: ActionType = ActionType.SAVE,
    val amountChange: Int = 0,
    val finalMoney: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameRoom(
    val id: String = "",
    val code: String = "",
    val goal: Int = 5000,
    val players: Map<String, Player> = emptyMap(),
    val currentTurnPlayerId: String = "",
    val status: GameStatus = GameStatus.WAITING,
    val winnerId: String? = null,
    val turnCount: Int = 1,
    val history: List<GameHistoryItem> = emptyList(),
    val lastEvent: String? = null
)
