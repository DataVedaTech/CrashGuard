package com.dataveda.crashguard.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SafetyTeal = Color(0xFF0A6C74)
val SafetyTealDeep = Color(0xFF063B47)
val SafetyTealDark = Color(0xFF4DD0E1)
val NavySlate = Color(0xFF0F1E2E)
val EmergencyRed = Color(0xFFD92D20)
val EmergencyRedDeep = Color(0xFF7A1212)
val EmergencyRedDark = Color(0xFFF97066)
val SafeGreen = Color(0xFF16A34A)
val SafeGreenDeep = Color(0xFF0D5F2C)
val WarningAmber = Color(0xFFD97706)
val AccentIndigo = Color(0xFF3B5BDB)
val AccentPurple = Color(0xFF7048E8)
val CardBorderLight = Color(0xFFE2E8F0)
val CardBorderDark = Color(0xFF283442)

val HeroTealGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF084B5A), Color(0xFF0D7480), Color(0xFF0F3547))
)

val HeroArmedGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF074E3F), Color(0xFF0B6E79), Color(0xFF0A3948))
)

val HeroInactiveGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
)

val EmergencyGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE11D48), Color(0xFFB91C1C))
)

private val LightColors = lightColorScheme(
    primary = SafetyTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F4F7),
    onPrimaryContainer = Color(0xFF042F35),
    secondary = Color(0xFF1E4E5F),
    onSecondary = Color.White,
    error = EmergencyRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF601410),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = CardBorderLight,
)

private val DarkColors = darkColorScheme(
    primary = SafetyTealDark,
    onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF0B4F5A),
    onPrimaryContainer = Color(0xFFB3F3FF),
    secondary = Color(0xFF9ACBD3),
    onSecondary = Color(0xFF00363D),
    error = EmergencyRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7A1B1B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1118),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF151E28),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1F2B38),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CardBorderDark,
)

private val CrashGuardTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
)

@Composable
fun CrashGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CrashGuardTypography,
        content = content,
    )
}

/** Tinted rounded-square icon badge used across cards and headers. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    containerColor: Color = tint.copy(alpha = 0.14f),
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 14.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Compact pill badge with optional leading dot or icon. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    containerColor: Color = color.copy(alpha = 0.15f),
    icon: ImageVector? = null,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

/** Telemetry stat tile for the hero cockpit card. */
@Composable
fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF5EEAD4),
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
