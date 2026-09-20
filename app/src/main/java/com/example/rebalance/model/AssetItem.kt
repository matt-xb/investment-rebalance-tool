package com.example.rebalance.model

data class AssetItem(
    val id: String,
    val name: String,
    val targetPercent: Double,
    val currentAmount: Double = 0.0,
    val currentPercent: Double = 0.0,
    val targetAmount: Double = 0.0,
    val deviationAmount: Double = 0.0,
    val deviationPercent: Double = 0.0,
    val adjustmentAmount: Double = 0.0,
    val actionSuggestion: String = ActionSuggestion.None.label
)

enum class ActionSuggestion(val label: String) {
    Buy("买入"),
    Sell("卖出"),
    None("无需调整"),
    InputRequired("请输入当前持仓")
}

data class AllocationResult(
    val assetId: String,
    val assetName: String,
    val amount: Double
)

data class NewFundRebalanceResult(
    val assetId: String,
    val assetName: String,
    val buyAmount: Double,
    val feeAmount: Double = 0.0,
    val totalCost: Double = buyAmount + feeAmount,
    val projectedPercent: Double = 0.0,
    val projectedDeviationPercent: Double = 0.0
)
