package com.example.presentation.screens

import java.util.Locale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GradientPurpleEnd
import com.example.ui.theme.GradientPurpleStart

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x11FFFFFF),
    borderColor: Color = Color(0x15FFFFFF),
    borderWidth: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AnimatedCountUpText(
    targetValue: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    ),
    color: Color = Color.White
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Text(
        text = String.format("%s %,.2f", currencySymbol, animatedValue.value),
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun CustomCalculatorNumpad(
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var formula by remember { mutableStateOf("") }

    val keys = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", "=", "+")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .padding(14.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = formula.ifEmpty { "0" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = ElectricPurple,
                textAlign = TextAlign.End
            )
        }

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key in listOf("/", "*", "-", "+", "=", "C")
                    val isPrimary = key == "="
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isPrimary -> Brush.horizontalGradient(listOf(GradientPurpleStart, GradientPurpleEnd))
                                    isAction -> Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                    else -> Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                                }
                            )
                            .clickable {
                                when (key) {
                                    "C" -> formula = ""
                                    "=" -> {
                                        if (formula.isNotEmpty()) {
                                            try {
                                                val result = evaluateSimpleExpression(formula)
                                                formula = String.format(Locale.US, "%.1f", result)
                                                onValueChange(formula)
                                            } catch (e: Exception) {
                                                formula = "Error"
                                            }
                                        }
                                    }
                                    else -> {
                                        if (formula == "Error") formula = ""
                                        formula += key
                                        // Auto update standard values if not action
                                        if (!isAction) {
                                            onValueChange(formula)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isAction && !isPrimary) ElectricPurple else Color.White
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

fun evaluateSimpleExpression(expr: String): Double {
    // Basic single operator calculator evaluation (e.g. "500+200")
    val regex = Regex("([0-9.]+)\\s*([+\\-*/])\\s*([0-9.]+)")
    val match = regex.find(expr) ?: return expr.toDoubleOrNull() ?: 0.0
    val val1 = match.groupValues[1].toDoubleOrNull() ?: 0.0
    val op = match.groupValues[2]
    val val2 = match.groupValues[3].toDoubleOrNull() ?: 0.0
    return when (op) {
        "+" -> val1 + val2
        "-" -> val1 - val2
        "*" -> val1 * val2
        "/" -> if (val2 != 0.0) val1 / val2 else 0.0
        else -> 0.0
    }
}
