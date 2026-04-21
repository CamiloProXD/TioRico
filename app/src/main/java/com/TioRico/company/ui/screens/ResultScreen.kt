package com.TioRico.company.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TioRico.company.ui.theme.*
import com.TioRico.company.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    gameViewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val room by gameViewModel.room.collectAsState()

    // Safety check - if room vanished, go back
    if (room == null) {
        onExit()
        return
    }

    val currentRoom = room!!
    val winner = currentRoom.players[currentRoom.winnerId ?: ""]
    val isVictorious = currentRoom.winnerId != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                if (isVictorious) "GOAL REACHED!" else "GAME OVER!",
                color = YellowAccent,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                if (isVictorious) {
                    "Congratulations ${winner?.name ?: "Player"}! You reached your objective and became a true Tío Rico."
                } else {
                    "You ran out of funds. Better luck next time!"
                },
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Trophy / Icon
            Text(
                if (isVictorious) "🏆" else "💸",
                fontSize = 100.sp,
                modifier = Modifier.padding(vertical = 32.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultCard("FINAL MONEY", if (isVictorious) "$${String.format("%,d", winner?.money ?: 0)}" else "$0", GreenSuccess, Modifier.weight(1f))
                ResultCard("GOAL", "$${String.format("%,d", currentRoom.goal)}", RedSpend, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            ResultCard("Rounds played", currentRoom.turnCount.toString(), BluePrimary, Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YellowAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit to Menu", color = Color.White)
            }
        }
    }
}

@Composable
fun ResultCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        }
    }
}