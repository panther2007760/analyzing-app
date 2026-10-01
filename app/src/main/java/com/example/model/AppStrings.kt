package com.example.model

enum class AppLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    SINHALA("සිංහල", "si")
}

object AppStrings {

    fun get(key: String, language: AppLanguage): String {
        return if (language == AppLanguage.SINHALA) {
            sinhalaMap[key] ?: englishMap[key] ?: key
        } else {
            englishMap[key] ?: key
        }
    }

    private val englishMap = mapOf(
        "app_title" to "Aivora Chart AI",
        "tab_markets" to "Markets",
        "tab_signals" to "Signals",
        "tab_screen" to "AI Screen",
        "tab_chat" to "AI Chat",
        "tab_tools" to "Tools",
        "status_connected" to "CONNECTED",
        "status_offline" to "OFFLINE",
        "status_connecting" to "CONNECTING...",
        "mode_live" to "LIVE",
        "mode_demo" to "DEMO",
        "retry" to "Retry",
        "current_price" to "Current Price",
        "24h_change" to "24h Change",
        "24h_volume" to "24h Volume",
        "technical_indicators" to "Technical Indicators",
        "rsi_14" to "RSI (14)",
        "ema_9" to "EMA (9)",
        "ema_21" to "EMA (21)",
        "macd" to "MACD",
        "support" to "Support",
        "resistance" to "Resistance",
        "signal_bias" to "Signal Bias",
        "long_bias" to "LONG BIAS",
        "short_bias" to "SHORT BIAS",
        "wait" to "WAIT / MIXED",
        "risk_reward" to "Risk / Reward",
        "entry_zone" to "Entry Zone",
        "stop_loss" to "Stop Loss",
        "take_profit_1" to "Take Profit 1",
        "take_profit_2" to "Take Profit 2",
        "save_signal" to "Save Signal",
        "saved" to "Saved",
        "risk_calculator" to "Risk Calculator",
        "account_balance" to "Account Balance ($)",
        "risk_percent" to "Risk Percentage (%)",
        "position_size" to "Position Size",
        "dollar_risk" to "Dollar Risk",
        "price_alerts" to "Price Alerts",
        "create_alert" to "Create Alert",
        "target_price" to "Target Price",
        "forex_rates" to "Forex Rates",
        "ai_assistant" to "AI Technical Assistant",
        "ask_question" to "Ask about chart structure...",
        "history" to "Signal History",
        "clear_all" to "Clear All",
        "settings" to "Settings & Diagnostics",
        "forex_not_configured" to "Tick Streaming: NOT CONFIGURED (ECB Reference Active)",
        "invalidation" to "Invalidation Condition"
    )

    private val sinhalaMap = mapOf(
        "app_title" to "Aivora ප්‍රස්තාර AI",
        "tab_markets" to "වෙළඳපල",
        "tab_signals" to "සංඥා",
        "tab_screen" to "AI තිරය",
        "tab_chat" to "AI කතාබහ",
        "tab_tools" to "මෙවලම්",
        "status_connected" to "සම්බන්ධයි",
        "status_offline" to "නොබැඳි",
        "status_connecting" to "සම්බන්ධ වෙමින්...",
        "mode_live" to "සජීවී",
        "mode_demo" to "ආදර්ශන",
        "retry" to "නැවත උත්සාහ කරන්න",
        "current_price" to "වත්මන් මිල",
        "24h_change" to "පැය 24 වෙනස",
        "24h_volume" to "පැය 24 පරිමාව",
        "technical_indicators" to "තාක්ෂණික දර්ශක",
        "rsi_14" to "RSI (14)",
        "ema_9" to "EMA (9)",
        "ema_21" to "EMA (21)",
        "macd" to "MACD",
        "support" to "ආධාරක මට්ටම",
        "resistance" to "ප්‍රතිරෝධක මට්ටම",
        "signal_bias" to "සංඥා නැඹුරුව",
        "long_bias" to "මිලදී ගැනීමේ නැඹුරුව (LONG)",
        "short_bias" to "විකිණීමේ නැඹුරුව (SHORT)",
        "wait" to "රැඳී සිටින්න (WAIT)",
        "risk_reward" to "අවදානම / ඵලය",
        "entry_zone" to "ඇතුල්වීමේ කලාපය",
        "stop_loss" to "අලාභ සීමාව (Stop Loss)",
        "take_profit_1" to "ලාභ සීමාව 1 (TP1)",
        "take_profit_2" to "ලාභ සීමාව 2 (TP2)",
        "save_signal" to "සංඥාව සුරකින්න",
        "saved" to "සුරකින ලදි",
        "risk_calculator" to "අවදානම් ගණකය",
        "account_balance" to "ගිණුමේ ශේෂය ($)",
        "risk_percent" to "අවදානම් ප්‍රතිශතය (%)",
        "position_size" to "ප්‍රමාණයේ තරම",
        "dollar_risk" to "ඩොලර් අවදානම",
        "price_alerts" to "මිල අනතුරු ඇඟවීම්",
        "create_alert" to "නව ඇඟවීමක් එක් කරන්න",
        "target_price" to "ඉලක්කගත මිල",
        "forex_rates" to "විදේශ විනිමය අනුපාත",
        "ai_assistant" to "AI තාක්ෂණික සහකරු",
        "ask_question" to "ප්‍රස්තාරය පිළිබඳව අසන්න...",
        "history" to "සංඥා ඉතිහාසය",
        "clear_all" to "සියල්ල මකන්න",
        "settings" to "සැකසුම් සහ රෝග විනිශ්චය",
        "forex_not_configured" to "සජීවී විදේශ විනිමය: වින්‍යාස කර නොමැත",
        "invalidation" to "අවලංගු වීමේ කොන්දේසිය"
    )
}
