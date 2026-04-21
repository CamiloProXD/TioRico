package com.TioRico.company.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TioRico.company.ui.theme.*
import com.TioRico.company.viewmodel.AuthViewModel
import com.TioRico.company.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    authViewModel: AuthViewModel,
    gameViewModel: GameViewModel,
    onGameCreated: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedGoal by remember { mutableStateOf(5000) }
    var isCustomMode by remember { mutableStateOf(false) }
    var customGoalText by remember { mutableStateOf("") }

    var joinCode by remember { mutableStateOf("") }
    val userId = authViewModel.getCurrentUserId() ?: ""
    val userName = authViewModel.getCurrentUserName()

    val room by gameViewModel.room.collectAsState()
    val error by gameViewModel.error.collectAsState()
    val loading by gameViewModel.loading.collectAsState()

    LaunchedEffect(room) {
        if (room != null) {
            onGameCreated()
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text("Tío Rico Lobby", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BluePrimary),
                navigationIcon = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color.White)
                    }
                }
            )
        },
        containerColor = BlueBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CHOOSE YOUR GOAL",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                GoalCard(
                    title = "EASY GOAL",
                    amount = 3000,
                    color = GreenSuccess,
                    selected = selectedGoal == 3000 && !isCustomMode,
                    onSelect = {
                        selectedGoal = 3000
                        isCustomMode = false
                    }
                )

                GoalCard(
                    title = "NORMAL GOAL",
                    amount = 5000,
                    color = BluePrimary,
                    selected = selectedGoal == 5000 && !isCustomMode,
                    badge = "POPULAR",
                    onSelect = {
                        selectedGoal = 5000
                        isCustomMode = false
                    }
                )

                GoalCard(
                    title = "HARD GOAL",
                    amount = 10000,
                    color = Color(0xFF673AB7),
                    selected = selectedGoal == 10000 && !isCustomMode,
                    onSelect = {
                        selectedGoal = 10000
                        isCustomMode = false
                    }
                )

                GoalCard(
                    title = "CUSTOM GOAL",
                    amount = if (isCustomMode) customGoalText.toIntOrNull() ?: 0 else 0,
                    color = Color.Gray,
                    selected = isCustomMode,
                    badge = "NEW",
                    onSelect = { isCustomMode = true }
                )

                if (isCustomMode) {
                    OutlinedTextField(
                        value = customGoalText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) customGoalText = it },
                        label = { Text("Enter Goal Amount (Min. 1500)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val finalGoal = if (isCustomMode) customGoalText.toIntOrNull() ?: 0 else selectedGoal
                val isValidGoal = !isCustomMode || (finalGoal >= 1500)

                Button(
                    onClick = { gameViewModel.createRoom(userId, userName, finalGoal) },
                    enabled = isValidGoal && !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Game", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                if (isCustomMode && customGoalText.isNotEmpty() && !isValidGoal) {
                    Text("Goal must be at least 1,500", color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Join Room Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Or join a room", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { if (it.length <= 6) joinCode = it },
                                label = { Text("Code") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { gameViewModel.joinRoom(joinCode, userId, userName) },
                                enabled = joinCode.length >= 4 && !loading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = { gameViewModel.clearError() }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    amount: Int,
    color: Color,
    selected: Boolean,
    badge: String? = null,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, color) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = 24.sp)
            }

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    if (badge != null) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            color = BluePrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(badge, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("$${String.format("%,d", amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
                Text("Reach this goal to win", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}