package com.ultratv.tv.nativeapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KOBANI 4K",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your activation code",
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Code Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code.ifEmpty { "_____" },
                    color = if (code.isEmpty()) Color.Gray else Color.White,
                    fontSize = 24.sp,
                    letterSpacing = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMsg != null) {
                Text(text = errorMsg!!, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
            }

            // Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("DEL", "0", "OK")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in keys) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (key in row) {
                            Button(
                                onClick = {
                                    errorMsg = null
                                    when (key) {
                                        "DEL" -> if (code.isNotEmpty()) code = code.dropLast(1)
                                        "OK" -> {
                                            if (code.isBlank()) {
                                                errorMsg = "Please enter a code"
                                                return@Button
                                            }
                                            loading = true
                                            scope.launch {
                                                try {
                                                    val ref = FirebaseDatabase.getInstance()
                                                        .getReference("sync/global/loginCodes")
                                                    val snapshot = ref.get().await()
                                                    var isValid = false
                                                    
                                                    if (snapshot.exists()) {
                                                        snapshot.children.forEach { child ->
                                                            val childCode = child.child("code").value?.toString()?.trim()?.lowercase()
                                                            val active = child.child("active").value as? Boolean ?: true
                                                            val expiresAtStr = child.child("expiresAt").value as? String
                                                            
                                                            if (childCode == code.trim().lowercase() && active) {
                                                                if (expiresAtStr != null) {
                                                                    try {
                                                                        val expiresAt = java.time.Instant.parse(expiresAtStr)
                                                                        if (java.time.Instant.now().isAfter(expiresAt)) {
                                                                            return@forEach
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        return@forEach
                                                                    }
                                                                }
                                                                isValid = true
                                                                return@forEach
                                                            }
                                                        }
                                                    }

                                                    if (isValid) {
                                                        onLoginSuccess(code)
                                                    } else {
                                                        errorMsg = "Invalid code"
                                                        loading = false
                                                    }
                                                } catch (e: Exception) {
                                                    errorMsg = "Network error"
                                                    loading = false
                                                }
                                            }
                                        }
                                        else -> if (code.length < 10) code += key
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.colors(
                                    containerColor = if (key == "OK") Color(0xFFEAB308) else Color.White.copy(alpha = 0.1f),
                                    contentColor = if (key == "OK") Color.Black else Color.White
                                )
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    color = if (key == "OK") Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
