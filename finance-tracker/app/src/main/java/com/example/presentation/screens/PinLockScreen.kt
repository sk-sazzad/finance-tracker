package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import com.example.R
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    correctPin: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var wrongAttempts by remember { mutableStateOf(0) }
    var isLockedOut by remember { mutableStateOf(false) }
    var lockoutSecondsLeft by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    var canUseBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(fragmentActivity) {
        if (fragmentActivity != null) {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> canUseBiometric = true
                else -> canUseBiometric = false
            }
        }
    }

    val showBiometricPrompt = {
        if (fragmentActivity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Finance Tracker")
                .setSubtitle("Use your biometric credential to continue")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            val biometricPrompt = BiometricPrompt(fragmentActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            })
            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Auto-prompt biometric on first load
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            showBiometricPrompt()
        }
    }

    // Lockout countdown timer
    LaunchedEffect(isLockedOut) {
        if (isLockedOut) {
            lockoutSecondsLeft = 30
            while (lockoutSecondsLeft > 0) {
                delay(1000)
                lockoutSecondsLeft--
            }
            isLockedOut = false
            wrongAttempts = 0
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            if (enteredPin == correctPin) {
                onUnlocked()
            } else {
                wrongAttempts++
                error = true
                if (wrongAttempts >= 5) {
                    isLockedOut = true
                }
                delay(500)
                enteredPin = ""
                error = false
            }
        }
    }

    BackHandler {
        // Do nothing — prevent back button from bypassing PIN
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // App icon
            Image(painter = painterResource(R.mipmap.ic_launcher), contentDescription = null, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))

            Text("Finance Tracker", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Enter your PIN to continue", color = TextSecondaryDark, fontSize = 14.sp)

            if (isLockedOut) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔒", fontSize = 48.sp)
                    Text("Too many attempts", color = CoralRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Try again in ${lockoutSecondsLeft}s", color = TextSecondaryDark, fontSize = 14.sp)
                }
            } else {
                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape).background(
                                if (i < enteredPin.length) if (error) CoralRed else ElectricPurple
                                else Color(0x33FFFFFF)
                            )
                        )
                    }
                }
    
                if (error) {
                    Text("Incorrect PIN", color = CoralRed, fontSize = 13.sp)
                }
                if (wrongAttempts >= 3 && !isLockedOut) {
                    Text(
                        "⚠️ ${5 - wrongAttempts} attempts remaining",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp
                    )
                }
    
                // Number pad
                val keys = listOf("1","2","3","4","5","6","7","8","9","BIO","0","⌫")
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(keys) { key ->
                        if (key == "BIO") {
                            if (canUseBiometric) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF1E293B)).clickable { showBiometricPrompt() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric", tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                Box(modifier = Modifier.size(72.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        if (key == "⌫") {
                                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        } else if (enteredPin.length < 4) {
                                            enteredPin += key
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(key, color = Color.White, fontSize = if (key == "⌫") 20.sp else 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
