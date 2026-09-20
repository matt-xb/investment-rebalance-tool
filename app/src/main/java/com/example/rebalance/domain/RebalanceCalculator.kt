package com.example.rebalance.domain

import com.example.rebalance.model.ActionSuggestion
import com.example.rebalance.model.AllocationResult
import com.example.rebalance.model.AssetItem
import com.example.rebalance.model.NewFundRebalanceResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object RebalanceCalculator {
    private const val ZERO_TOLERANCE = 0.005
    private const val SCORE_TOLERANCE = 0.000001
    private const val MIN_BUY_AMOUNT = 200.0
    private const val BUY_FEE = 5.0
    private const val MIN_TRADE_TOTAL_COST = MIN_BUY_AMOUNT + BUY_FEE

    val defaultAssets = listOf(
        AssetItem(id = "a500", name = "A500", targetPercent = 20.0),
        AssetItem(id = "dividend_low_vol_100", name = "红利低波100", targetPercent = 8.0),
        AssetItem(id = "hong_kong_stock", name = "港股", targetPercent = 8.0),
        AssetItem(id = "sp500", name = "标普500", targetPercent = 32.0),
        AssetItem(id = "nasdaq100", name = "纳斯达克100", targetPercent = 20.0),
        AssetItem(id = "gold", name = "黄金", targetPercent = 8.0),
        AssetItem(id = "cash", name = "现金/货币基金", targetPercent = 4.0)
    ).also { defaultAssets ->
        require(isTargetPercentValid(defaultAssets)) { "Default target allocation must total 100%." }
    }

    private val historicalDefaultAssets = listOf(
        listOf(
            AssetItem(id = "a500", name = "A500", targetPercent = 21.0),
            AssetItem(id = "dividend_low_vol_100", name = "红利低波100", targetPercent = 19.0),
            AssetItem(id = "hong_kong_stock", name = "港股", targetPercent = 6.0),
            AssetItem(id = "sp500", name = "标普500", targetPercent = 18.0),
            AssetItem(id = "nasdaq100", name = "纳斯达克100", targetPercent = 14.0),
            AssetItem(id = "gold", name = "黄金", targetPercent = 12.0),
            AssetItem(id = "cash", name = "现金/货币基金", targetPercent = 10.0)
        ),
        listOf(
            AssetItem(id = "a500", name = "A500", targetPercent = 22.0),
            AssetItem(id = "dividend_low_vol_100", name = "红利低波100", targetPercent = 12.0),
            AssetItem(id = "sp500", name = "标普500", targetPercent = 22.0),
            AssetItem(id = "nasdaq100", name = "纳斯达克100", targetPercent = 24.0),
            AssetItem(id = "gold", name = "黄金", targetPercent = 10.0),
            AssetItem(id = "southeast_asia_stock", name = "东南亚股票", targetPercent = 6.0),
            AssetItem(id = "cash", name = "现金", targetPercent = 4.0)
        )
    )

    fun totalAmount(assets: List<AssetItem>): Double =
        roundMoney(assets.sumOf { max(0.0, it.currentAmount) })

    fun calculatePortfolio(assets: List<AssetItem>): List<AssetItem> {
        val total = totalAmount(assets)
        return assets.map { asset ->
            val safeCurrentAmount = max(0.0, asset.currentAmount)
            val currentPercent = if (total > 0.0) safeCurrentAmount / total * 100.0 else 0.0
            val targetAmount = if (total > 0.0) total * asset.targetPercent / 100.0 else 0.0
            val deviationAmount = safeCurrentAmount - targetAmount
            val deviationPercent = currentPercent - asset.targetPercent
            val adjustmentAmount = targetAmount - safeCurrentAmount

            asset.copy(
                currentAmount = roundMoney(safeCurrentAmount),
                currentPercent = roundPercent(currentPercent),
                targetAmount = roundMoney(targetAmount),
                deviationAmount = roundMoney(deviationAmount),
                deviationPercent = roundPercent(deviationPercent),
                adjustmentAmount = roundMoney(adjustmentAmount),
                actionSuggestion = adjustmentSuggestion(adjustmentAmount, total)
            )
        }
    }

    fun allocateInvestment(totalInvestment: Double, assets: List<AssetItem>): List<AllocationResult> {
        val safeInvestment = max(0.0, totalInvestment)
        return assets.map {
            AllocationResult(
                assetId = it.id,
                assetName = it.name,
                amount = roundMoney(safeInvestment * it.targetPercent / 100.0)
            )
        }
    }

    fun rebalanceWithNewFunds(newInvestment: Double, assets: List<AssetItem>): List<NewFundRebalanceResult> {
        val safeNewInvestment = max(0.0, newInvestment)
        if (safeNewInvestment < MIN_TRADE_TOTAL_COST) {
            return rebalanceResults(assets, emptyMap())
        }

        val purchases = mutableMapOf<String, Double>()
        var currentScore = scorePurchases(assets, purchases)

        while (true) {
            val bestNext = assets
                .mapNotNull { asset ->
                    val candidatePurchases = purchases.toMutableMap()
                    candidatePurchases[asset.id] = (candidatePurchases[asset.id] ?: 0.0) + MIN_BUY_AMOUNT
                    val candidateCost = totalTradeCost(candidatePurchases)
                    if (candidateCost <= safeNewInvestment + ZERO_TOLERANCE) {
                        candidatePurchases to scorePurchases(assets, candidatePurchases)
                    } else {
                        null
                    }
                }
                .minWithOrNull(compareBy<Pair<Map<String, Double>, PurchaseScore>> { it.second.totalDeviation }
                    .thenBy { it.second.maxDeviation }
                    .thenByDescending { it.second.usedCost })
                ?: break

            if (!isBetterScore(bestNext.second, currentScore)) break
            purchases.clear()
            purchases.putAll(bestNext.first)
            currentScore = bestNext.second
        }

        return rebalanceResults(assets, purchases)
    }

    fun needsRebalance(assets: List<AssetItem>, thresholdPercent: Double = 5.0): Boolean =
        calculatePortfolio(assets).any { abs(it.deviationPercent) >= thresholdPercent }

    fun isTargetPercentValid(assets: List<AssetItem>): Boolean =
        abs(assets.sumOf { it.targetPercent } - 100.0) < 0.01 && assets.all { it.targetPercent >= 0.0 }

    fun isHistoricalDefaultAllocation(assets: List<AssetItem>): Boolean =
        historicalDefaultAssets.any { historical -> hasSameAllocation(assets, historical) }

    fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

    fun roundPercent(value: Double): Double = round(value * 100.0) / 100.0

    private fun rebalanceResults(
        assets: List<AssetItem>,
        purchases: Map<String, Double>
    ): List<NewFundRebalanceResult> {
        val projectedTotal = totalAmount(assets) + purchases.values.sum()
        return assets.map { asset ->
            val buyAmount = roundMoney(purchases[asset.id] ?: 0.0)
            val feeAmount = if (buyAmount > ZERO_TOLERANCE) BUY_FEE else 0.0
            val projectedAmount = max(0.0, asset.currentAmount) + buyAmount
            val projectedPercent = if (projectedTotal > ZERO_TOLERANCE) {
                projectedAmount / projectedTotal * 100.0
            } else {
                0.0
            }
            NewFundRebalanceResult(
                assetId = asset.id,
                assetName = asset.name,
                buyAmount = buyAmount,
                feeAmount = roundMoney(feeAmount),
                totalCost = roundMoney(buyAmount + feeAmount),
                projectedPercent = roundPercent(projectedPercent),
                projectedDeviationPercent = roundPercent(projectedPercent - asset.targetPercent)
            )
        }
    }

    private fun scorePurchases(assets: List<AssetItem>, purchases: Map<String, Double>): PurchaseScore {
        val projectedTotal = totalAmount(assets) + purchases.values.sum()
        val deviations = assets.map { asset ->
            val projectedAmount = max(0.0, asset.currentAmount) + (purchases[asset.id] ?: 0.0)
            val projectedPercent = if (projectedTotal > ZERO_TOLERANCE) {
                projectedAmount / projectedTotal * 100.0
            } else {
                0.0
            }
            abs(projectedPercent - asset.targetPercent)
        }
        return PurchaseScore(
            totalDeviation = deviations.sum(),
            maxDeviation = deviations.maxOrNull() ?: 0.0,
            usedCost = totalTradeCost(purchases)
        )
    }

    private fun totalTradeCost(purchases: Map<String, Double>): Double {
        val buyTotal = purchases.values.sum()
        val feeTotal = purchases.values.count { it > ZERO_TOLERANCE } * BUY_FEE
        return buyTotal + feeTotal
    }

    private fun isBetterScore(candidate: PurchaseScore, current: PurchaseScore): Boolean = when {
        candidate.totalDeviation < current.totalDeviation - SCORE_TOLERANCE -> true
        candidate.totalDeviation > current.totalDeviation + SCORE_TOLERANCE -> false
        candidate.maxDeviation < current.maxDeviation - SCORE_TOLERANCE -> true
        candidate.maxDeviation > current.maxDeviation + SCORE_TOLERANCE -> false
        else -> candidate.usedCost > current.usedCost + SCORE_TOLERANCE
    }

    private fun hasSameAllocation(assets: List<AssetItem>, expected: List<AssetItem>): Boolean =
        assets.size == expected.size && assets.zip(expected).all { (actual, default) ->
            actual.id == default.id &&
                actual.name == default.name &&
                abs(actual.targetPercent - default.targetPercent) < 0.01
        }

    private fun adjustmentSuggestion(adjustmentAmount: Double, totalAmount: Double): String = when {
        totalAmount <= ZERO_TOLERANCE -> ActionSuggestion.InputRequired.label
        adjustmentAmount > ZERO_TOLERANCE -> "建议买入 ¥${formatMoney(adjustmentAmount)}"
        adjustmentAmount < -ZERO_TOLERANCE -> "建议卖出 ¥${formatMoney(abs(adjustmentAmount))}"
        else -> ActionSuggestion.None.label
    }

    private fun formatMoney(value: Double): String =
        String.format(Locale.CHINA, "%.2f", roundMoney(value))

    private data class PurchaseScore(
        val totalDeviation: Double,
        val maxDeviation: Double,
        val usedCost: Double
    )
}
