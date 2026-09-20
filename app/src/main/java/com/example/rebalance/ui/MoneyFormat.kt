package com.example.rebalance.ui

import java.util.Locale

fun moneyText(value: Double): String = String.format(Locale.CHINA, "%.2f", value)

fun percentText(value: Double): String = String.format(Locale.CHINA, "%.2f%%", value)

fun parseAmount(input: String): Double =
    input.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
