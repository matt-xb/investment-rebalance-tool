package com.example.rebalance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rebalance.data.AssetRepository
import com.example.rebalance.domain.RebalanceCalculator
import com.example.rebalance.model.AllocationResult
import com.example.rebalance.model.AssetItem
import com.example.rebalance.model.NewFundRebalanceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RebalanceUiState(
    val assets: List<AssetItem> = RebalanceCalculator.defaultAssets,
    val analyzedAssets: List<AssetItem> = RebalanceCalculator.defaultAssets,
    val totalAmount: Double = 0.0,
    val investmentInput: String = "",
    val newFundInput: String = "",
    val allocationResults: List<AllocationResult> = emptyList(),
    val newFundResults: List<NewFundRebalanceResult> = emptyList(),
    val needsRebalance: Boolean = false
)

class RebalanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AssetRepository(application)
    private val investmentInput = MutableStateFlow("")
    private val newFundInput = MutableStateFlow("")

    val uiState: StateFlow<RebalanceUiState> = combine(
        repository.assets,
        investmentInput,
        newFundInput
    ) { storedAssets, investment, newFund ->
        val analyzed = RebalanceCalculator.calculatePortfolio(storedAssets)
        RebalanceUiState(
            assets = storedAssets,
            analyzedAssets = analyzed,
            totalAmount = RebalanceCalculator.totalAmount(storedAssets),
            investmentInput = investment,
            newFundInput = newFund,
            allocationResults = RebalanceCalculator.allocateInvestment(parseAmount(investment), storedAssets),
            newFundResults = RebalanceCalculator.rebalanceWithNewFunds(parseAmount(newFund), storedAssets),
            needsRebalance = RebalanceCalculator.needsRebalance(storedAssets)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RebalanceUiState()
    )

    fun updateInvestmentInput(value: String) {
        investmentInput.value = value
    }

    fun updateNewFundInput(value: String) {
        newFundInput.value = value
    }

    fun updateCurrentAmount(assetId: String, amountInput: String) {
        val nextAssets = uiState.value.assets.map { asset ->
            if (asset.id == assetId) asset.copy(currentAmount = parseAmount(amountInput)) else asset
        }
        viewModelScope.launch {
            repository.saveAssets(nextAssets)
        }
    }
}
