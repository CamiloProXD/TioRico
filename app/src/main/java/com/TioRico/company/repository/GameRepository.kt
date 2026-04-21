package com.TioRico.company.repository

import com.TioRico.company.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val roomsCollection = db.collection("rooms")

    fun getRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val subscription = roomsCollection.document(roomId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    val room = snapshot.toObject<GameRoom>()
                    trySend(room)
                } catch (e: Exception) {
                    close(e)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun createRoom(playerId: String, playerName: String, goal: Int = 5000): String {
        val code = (100000..999999).random().toString()
        val roomId = roomsCollection.document().id
        val initialPlayer = Player(id = playerId, name = playerName, isReady = true)
        val room = GameRoom(
            id = roomId,
            code = code,
            goal = goal,
            players = mapOf(playerId to initialPlayer),
            currentTurnPlayerId = playerId,
            status = GameStatus.WAITING
        )
        roomsCollection.document(roomId).set(room).await()
        return roomId
    }

    suspend fun joinRoom(code: String, playerId: String, playerName: String): String? {
        val query = roomsCollection.whereEqualTo("code", code).get().await()
        if (query.isEmpty) return null
        
        val document = query.documents.first()
        val roomId = document.id
        val room = document.toObject<GameRoom>() ?: return null
        
        if (room.status != GameStatus.WAITING) return null

        val updatedPlayers = room.players.toMutableMap()
        updatedPlayers[playerId] = Player(id = playerId, name = playerName)
        
        roomsCollection.document(roomId).update("players", updatedPlayers).await()
        return roomId
    }

    suspend fun performAction(roomId: String, playerId: String, action: ActionType) {
        val roomRef = roomsCollection.document(roomId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val room = snapshot.toObject<GameRoom>() ?: return@runTransaction
            
            if (room.currentTurnPlayerId != playerId || room.status != GameStatus.PLAYING) return@runTransaction

            val player = room.players[playerId] ?: return@runTransaction
            var moneyChange = 0
            
            when (action) {
                ActionType.SAVE -> {
                    moneyChange = 200
                }
                ActionType.INVEST -> {
                    // Random between -200 and +600
                    moneyChange = Random.nextInt(-200, 601)
                }
                ActionType.SPEND -> {
                    moneyChange = -300
                }
            }

            val newMoney = player.money + moneyChange
            val isEliminated = newMoney <= 0
            val reachedGoal = newMoney >= room.goal

            val updatedPlayer = player.copy(
                money = newMoney,
                isEliminated = isEliminated
            )

            val updatedPlayers = room.players.toMutableMap()
            updatedPlayers[playerId] = updatedPlayer

            // Update history
            val historyItem = GameHistoryItem(
                turn = room.turnCount,
                playerName = player.name,
                action = action,
                amountChange = moneyChange,
                finalMoney = newMoney
            )
            val updatedHistory = room.history.toMutableList()
            updatedHistory.add(historyItem)

            // Determine next turn - skip eliminated players
            val updatedActivePlayers = updatedPlayers.filterValues { !it.isEliminated }
            val activePlayerIds = updatedActivePlayers.keys.toList().sorted()
            
            // Round synchronization: Round ends when the current player WAS the last one in the PREVIOUS active list
            val previousActivePlayerIds = room.players.filterValues { !it.isEliminated }.keys.toList().sorted()
            val currentIndexInOldList = previousActivePlayerIds.indexOf(playerId)
            val isEndOfRound = currentIndexInOldList == previousActivePlayerIds.size - 1

            var nextPlayerId = room.currentTurnPlayerId
            if (activePlayerIds.isNotEmpty()) {
                val nextIndex = (activePlayerIds.indexOf(playerId).takeIf { it != -1 } ?: -1) + 1
                nextPlayerId = if (nextIndex >= activePlayerIds.size) activePlayerIds[0] else activePlayerIds[nextIndex]
            }

            val nextRoundCount = if (isEndOfRound) room.turnCount + 1 else room.turnCount

            var newStatus = room.status
            var winnerId = room.winnerId
            
            if (reachedGoal) {
                newStatus = GameStatus.FINISHED
                winnerId = playerId
            } else {
                val remainingPlayers = updatedPlayers.values.filter { !it.isEliminated }
                
                if (remainingPlayers.isEmpty()) {
                    // Everyone lost
                    newStatus = GameStatus.FINISHED
                    winnerId = null
                } else if (remainingPlayers.size == 1 && room.players.size > 1) {
                    // Only one winner
                    newStatus = GameStatus.FINISHED
                    winnerId = remainingPlayers.first().id
                }
            }

            // -- Logic for Random Events (Every round end or 25% chance) --
            var eventText: String? = null
            if (Random.nextInt(4) == 0) {
                val eventType = Random.nextInt(5)
                var eventChange = 0
                
                when (eventType) {
                    0 -> {
                        eventText = "UNEXPECTED INHERITANCE! You get +$500"
                        eventChange = 500
                    }
                    1 -> {
                        eventText = "ECONOMIC CRISIS! You lose -$200"
                        eventChange = -200
                    }
                    2 -> {
                        eventText = "LUCKY DAY! You get +$100"
                        eventChange = 100
                    }
                    3 -> {
                        eventText = "TRAFFIC FINE! You pay -$150"
                        eventChange = -150
                    }
                    4 -> {
                        // CAPITAL TAX INCENTIVE: Only hits those who DID NOT spend
                        if (action != ActionType.SPEND) {
                            eventText = "CAPITAL TAX! Since you didn't spend, you pay -$400"
                            eventChange = -400
                        } else {
                            eventText = "TAX EXEMPT! You spent money, so you avoid the Capital Tax."
                            eventChange = 0
                        }
                    }
                }

                val playerAfterEvent = updatedPlayers[playerId]!!
                val finalMoneyWithEvent = (playerAfterEvent.money + eventChange).coerceAtLeast(0)
                updatedPlayers[playerId] = playerAfterEvent.copy(money = finalMoneyWithEvent, isEliminated = finalMoneyWithEvent <= 0)
            }

            transaction.update(roomRef, mapOf(
                "players" to updatedPlayers,
                "history" to updatedHistory,
                "currentTurnPlayerId" to nextPlayerId,
                "turnCount" to nextRoundCount,
                "status" to newStatus,
                "winnerId" to winnerId,
                "lastEvent" to eventText
            ))
        }.await()
    }

    suspend fun startGame(roomId: String, goal: Int) {
        roomsCollection.document(roomId).update(
            "status", GameStatus.PLAYING,
            "goal", goal
        ).await()
    }

    suspend fun leaveRoom(roomId: String, playerId: String) {
        val roomRef = roomsCollection.document(roomId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val room = snapshot.toObject<GameRoom>() ?: return@runTransaction

            val updatedPlayers = room.players.toMutableMap()
            updatedPlayers.remove(playerId)

            if (updatedPlayers.isEmpty()) {
                // Self-destruct: Delete room if empty
                transaction.delete(roomRef)
            } else {
                // If the player who left was the current turn holder, move turn to next active
                var nextTurnId = room.currentTurnPlayerId
                if (room.currentTurnPlayerId == playerId) {
                    val remainingActiveIds = updatedPlayers.filterValues { !it.isEliminated }.keys.toList().sorted()
                    if (remainingActiveIds.isNotEmpty()) {
                        nextTurnId = remainingActiveIds[0]
                    } else {
                        // If no active players left, just pick anyone or let game end
                        nextTurnId = updatedPlayers.keys.first()
                    }
                }
                
                transaction.update(roomRef, mapOf(
                    "players" to updatedPlayers,
                    "currentTurnPlayerId" to nextTurnId
                ))
            }
        }.await()
    }
}
