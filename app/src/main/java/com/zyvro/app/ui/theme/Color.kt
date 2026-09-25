package com.zyvro.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO NOVA DESIGN SYSTEM v4.0 — Deep Space Aura Palette
// ═══════════════════════════════════════════════════════════════════════════

// ── Primary Accent: Electric Cyan-Teal ──────────────────────────────────
val NovaPrimary     = Color(0xFF00D9FF)   // Electric Cyan — primary brand
val NovaPrimaryDeep = Color(0xFF0099CC)   // Deep Cyan
val NovaPrimaryDark = Color(0xFF00D9FF)   // Dark mode primary (same)
val NovaPrimarySoft = Color(0xFF001F2E)   // Cyan tint background

// ── Secondary Accent: Vivid Violet ──────────────────────────────────────
val NovaViolet      = Color(0xFF8B5CF6)   // Vivid Violet
val NovaVioletDeep  = Color(0xFF6D28D9)   // Deep Purple
val NovaVioletSoft  = Color(0xFF1E1B4B)   // Violet tint bg

// ── Tertiary: Rose Pink ──────────────────────────────────────────────────
val NovaRose        = Color(0xFFFF2D78)   // Hot Rose
val NovaRoseSoft    = Color(0xFF2D0A1A)   // Rose tint bg

// ── Success / Warning / Error ─────────────────────────────────────────────
val AccentGreen     = Color(0xFF00E676)   // Neon Green
val AccentOrange    = Color(0xFFFF9100)   // Neon Orange
val AccentRed       = Color(0xFFFF1744)   // Neon Red

// ── Aliases for backwards compatibility ─────────────────────────────────
val NovaCyan        = NovaPrimary
val NovaCyanDeep    = NovaPrimaryDeep
val NovaAqua        = NovaPrimary
val NovaAquaDeep    = NovaPrimaryDeep
val NovaAquaSoft    = NovaPrimarySoft
val NovaAzure       = Color(0xFF0050FF)
val NovaSapphire    = Color(0xFF0026A3)
val NovaCyanBright  = Color(0xFF18FFFF)
val NovaPurpleLight = NovaViolet
val NovaSecondary   = NovaViolet
val NovaTertiary    = NovaRose
val NovaInk         = Color(0xFFE8F4F8)

// ── Backgrounds: Deep Space ──────────────────────────────────────────────
val SpaceBlack      = Color(0xFF060912)   // True deep space
val SpaceNavy       = Color(0xFF0B0F1E)   // Deep navy
val SpaceCard       = Color(0xFF0F1629)   // Card surface
val SpaceCardHigh   = Color(0xFF141C35)   // Elevated card
val SpaceGlass      = Color(0xFF1A2240)   // Glass surface
val SpaceBorder     = Color(0xFF2A3560)   // Glass border

// ── Light Mode ───────────────────────────────────────────────────────────
val LightBg         = Color(0xFFF0F4FF)   // Soft blue-white
val LightCard       = Color(0xFFFFFFFF)
val LightCardAlt    = Color(0xFFF5F8FF)
val LightBorder     = Color(0xFFD4DCFF)
val LightText       = Color(0xFF0A0D1A)
val LightTextSub    = Color(0xFF4A5490)

// ── Dark mode text ───────────────────────────────────────────────────────
val DarkText        = Color(0xFFE8F0FF)
val DarkTextSub     = Color(0xFF8090C0)
val DarkSeparator   = Color(0xFF1E2845)

// ── iOS compatibility aliases ─────────────────────────────────────────────
val IOSGroupedLight = LightBg
val IOSCardLight    = LightCard
val IOSGroupedDark  = SpaceBlack
val IOSCardDark     = SpaceCard
val IOSCardDarkElevated = SpaceCardHigh

val IOSBlue         = Color(0xFF0A84FF)
val IOSBlueLight    = Color(0xFF007AFF)
val IOSGreen        = AccentGreen
val IOSGreenLight   = Color(0xFF34C759)
val IOSRed          = AccentRed
val IOSRedLight     = Color(0xFFFF3B30)
val IOSOrange       = AccentOrange
val IOSOrangeLight  = Color(0xFFFF9500)
val IOSGray         = Color(0xFF5A6A9A)
val IOSSeparatorLight = LightBorder
val IOSSeparatorDark  = DarkSeparator

val IOSTitleLight   = LightText
val IOSCaptionLight = LightTextSub
val IOSTitleDark    = DarkText
val IOSCaptionDark  = DarkTextSub

// ── Backward compat light tokens ─────────────────────────────────────────
val BackgroundLight           = LightBg
val BackgroundLightSecondary  = LightCardAlt
val SurfaceLight              = LightCard
val SurfaceVariantLight       = LightCardAlt
val CardBorderLight           = LightBorder
val TextPrimaryLight          = LightText
val TextSecondaryLight        = LightTextSub

// ── Backward compat dark tokens ──────────────────────────────────────────
val BackgroundDark            = SpaceBlack
val BackgroundDarkSecondary   = SpaceNavy
val SurfaceDark               = SpaceCard
val SurfaceVariantDark        = SpaceCardHigh
val CardBorderDark            = SpaceBorder
val TextPrimaryDark           = DarkText
val TextSecondaryDark         = DarkTextSub

// ── Platform Brand Colors ────────────────────────────────────────────────
val FacebookBlue    = Color(0xFF1877F2)
val ThreadsDark     = Color(0xFF101010)
val PinterestRed    = Color(0xFFE60023)
val YouTubeRed      = Color(0xFFFF0000)
val InstagramPink   = Color(0xFFE1306C)
val TikTokCyan      = Color(0xFF00F2FE)
val TwitterBlue     = Color(0xFF1DA1F2)
val RedditOrange    = Color(0xFFFF4500)
val SoundCloudOrange = Color(0xFFFF5500)
val TwitchPurple    = Color(0xFF9146FF)
val BilibiliBlue    = Color(0xFF00A1D6)

// ── Gradient Presets ─────────────────────────────────────────────────────
val GradientCyan    = listOf(Color(0xFF00D9FF), Color(0xFF0099CC))
val GradientViolet  = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
val GradientRose    = listOf(Color(0xFFFF2D78), Color(0xFFFF6B35))
val GradientGold    = listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
val GradientGreen   = listOf(Color(0xFF00E676), Color(0xFF00BFA5))
val GradientAurora  = listOf(Color(0xFF00D9FF), Color(0xFF8B5CF6), Color(0xFFFF2D78))
