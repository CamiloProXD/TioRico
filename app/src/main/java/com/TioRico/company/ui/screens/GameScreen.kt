package com.TioRico.company.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TioRico.company.models.*
import com.TioRico.company.ui.theme.*
import com.TioRico.company.viewmodel.AuthViewModel
import com.TioRico.company.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    authViewModel: AuthViewModel,
    gameViewModel: GameViewModel,
    onGameEnd: () -> Unit,
    onExit: () -> Unit
) {
    val room by gameViewModel.room.collectAsState()
    val userId = authViewModel.getCurrentUserId() ?: ""
    
    // Safety check
    if (room == null) {
        onExit()
        return
    }

    val currentRoom = room!!
    val currentPlayer = currentRoom.players[userId] ?: Player(name = "Jugador")
    val isMyTurn = currentRoom.currentTurnPlayerId == userId && currentRoom.status == GameStatus.PLAYING
    val progress = (currentPlayer.money.toFloat() / currentRoom.goal.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(currentRoom.status) {
        if (currentRoom.status == GameStatus.FINISHED) {
            onGameEnd()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Tío Rico: Meta Mode", color = Color.White, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = GreenSuccess.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "$${String.format("%,d", currentPlayer.money)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = GreenSuccess,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = BlueBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Event Banner
            if (currentRoom.lastEvent != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = YellowAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            currentRoom.lastEvent!!,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Round", currentRoom.turnCount.toString(), null)
                StatItem("Goal", "$${String.format("%,d", currentRoom.goal)}", "🎯")
                StatItem("Progress", "${(progress * 100).toInt()}%", null)
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp),
                color = GreenSuccess,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            if (currentRoom.status == GameStatus.WAITING) {
                // Lobby part of the screen if game hasn't started
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Waiting for players...", color = Color.White, fontSize = 20.sp)
                        Text("Room Code: ${currentRoom.code}", color = YellowAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connected players: ${currentRoom.players.size}", color = Color.White)
                        
                        if (currentRoom.players.size >= 1) { // Normal 2
                             Button(
                                 onClick = { gameViewModel.startGame(currentRoom.id, currentRoom.goal) },
                                 colors = ButtonDefaults.buttonColors(containerColor = YellowAccent)
                             ) {
                                 Text("Start Game", color = Color.Black)
                             }
                        }
                    }
                }
            } else {
                val me = currentRoom.players[userId]
                val isEliminated = me?.isEliminated == true

                if (isEliminated) {
                    // DEFEAT OVERLAY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DEFEAT", color = RedSpend, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("You ran out of money!", color = Color.White, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onExit,
                                colors = ButtonDefaults.buttonColors(containerColor = RedSpend)
                            ) {
                                Text("Exit to Lobby", color = Color.White)
                            }
                            TextButton(onClick = { /* Stay as spectator */ }) {
                                Text("Watch Game", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    // Actual Game actions
                    Text(
                        "What will you do this round?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        "Choose wisely, every decision counts.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ActionCard(
                        title = "SAVE",
                        description = "Gain $200 safely",
                        subtext = "Safe option",
                        color = GreenSuccess,
                        icon = Icons.Default.Savings,
                        enabled = isMyTurn,
                        onClick = { gameViewModel.performAction(currentRoom.id, userId, ActionType.SAVE) }
                    )

                    ActionCard(
                        title = "INVEST",
                        description = "Gain or lose up to $600",
                        subtext = "Risk: High",
                        color = BluePrimary,
                        icon = Icons.Default.TrendingUp,
                        enabled = isMyTurn,
                        onClick = { gameViewModel.performAction(currentRoom.id, userId, ActionType.INVEST) }
                    )

                    ActionCard(
                        title = "SPEND",
                        description = "Lose $300",
                        subtext = "Do you need it?",
                        color = RedSpend,
                        icon = Icons.Default.ShoppingBag,
                        enabled = isMyTurn,
                        onClick = { gameViewModel.performAction(currentRoom.id, userId, ActionType.SPEND) }
                    )

                    if (!isMyTurn && currentRoom.status == GameStatus.PLAYING) {
                        Text(
                            "Waiting for the other player...",
                            color = YellowAccent,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // History
                Text("RECENT HISTORY", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentRoom.history.reversed().take(10)) { item ->
                        HistoryRow(item)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: String?) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Text(icon, modifier = Modifier.padding(end = 4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    subtext: String,
    color: Color,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) color else color.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                Text(description, fontSize = 14.sp, color = Color.White)
                Text(subtext, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun HistoryRow(item: GameHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Round ${item.turn}: ${item.playerName} -> ${item.action}", color = Color.White, fontSize = 14.sp)
        val color = if (item.amountChange >= 0) GreenSuccess else RedSpend
        val sign = if (item.amountChange >= 0) "+" else ""
        Text("$sign$${item.amountChange}", color = color, fontWeight = FontWeight.Bold)
    }
}
