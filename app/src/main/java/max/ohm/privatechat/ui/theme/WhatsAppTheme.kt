package max.ohm.privatechat.ui.theme

import androidx.compose.ui.graphics.Color

object WhatsAppColors {
    // Light Theme Colors
    val LightGreen = Color(0xFF25D366)
    val DarkGreen = Color(0xFF075E54)
    val Teal = Color(0xFF128C7E)
    val TeaGreen = Color(0xFFDCF8C6)
    val LightGray = Color(0xFFE5DDD5)
    
    // Dark Theme Colors
    val DarkBackground = Color(0xFF0B141A)
    val DarkSurface = Color(0xFF1F2C33)
    val DarkCard = Color(0xFF2A3942)
    val DarkDivider = Color(0xFF2F3B43)
    val DarkTextPrimary = Color(0xFFE9EDEF)
    val DarkTextSecondary = Color(0xFF8696A0)
    val DarkGreenMessage = Color(0xFF005C4B)
    val DarkGrayMessage = Color(0xFF1F2C33)
    
    // Common Colors
    val Blue = Color(0xFF34B7F1)
    val ErrorRed = Color(0xFFEF5350)
}

data class WhatsAppColorScheme(
    val background: Color,
    val surface: Color,
    val card: Color,
    val divider: Color,
    val primary: Color,
    val primaryVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val messageOutgoing: Color,
    val messageIncoming: Color,
    val chatBackground: Color
)

val DarkWhatsAppColorScheme = WhatsAppColorScheme(
    background = WhatsAppColors.DarkBackground,
    surface = WhatsAppColors.DarkSurface,
    card = WhatsAppColors.DarkCard,
    divider = WhatsAppColors.DarkDivider,
    primary = WhatsAppColors.LightGreen,
    primaryVariant = WhatsAppColors.DarkGreen,
    textPrimary = WhatsAppColors.DarkTextPrimary,
    textSecondary = WhatsAppColors.DarkTextSecondary,
    messageOutgoing = WhatsAppColors.DarkGreenMessage,
    messageIncoming = WhatsAppColors.DarkGrayMessage,
    chatBackground = WhatsAppColors.DarkBackground
)

val LightWhatsAppColorScheme = WhatsAppColorScheme(
    background = Color.White,
    surface = Color.White,
    card = Color.White,
    divider = Color(0xFFE0E0E0),
    primary = WhatsAppColors.LightGreen,
    primaryVariant = WhatsAppColors.DarkGreen,
    textPrimary = Color.Black,
    textSecondary = Color.Gray,
    messageOutgoing = WhatsAppColors.TeaGreen,
    messageIncoming = Color.White,
    chatBackground = WhatsAppColors.LightGray
)
