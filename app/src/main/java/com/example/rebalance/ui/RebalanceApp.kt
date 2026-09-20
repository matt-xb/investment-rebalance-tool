package com.example.rebalance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rebalance.model.AllocationResult
import com.example.rebalance.model.AssetItem
import com.example.rebalance.model.NewFundRebalanceResult
import kotlin.math.abs

private enum class Screen(val route: String, val title: String) {
    Home("home", "首页"),
    Allocation("allocation", "金额分配"),
    Holdings("holdings", "当前持仓"),
    Rebalance("rebalance", "再平衡"),
    Settings("settings", "配置设置")
}

@Composable
fun RebalanceApp(viewModel: RebalanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Home.route
    val currentScreen = Screen.entries.firstOrNull { it.route == currentRoute } ?: Screen.Home

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF2E7D5B),
            secondary = Color(0xFFB87333),
            surface = Color.White,
            background = Color(0xFFF6F7F4)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    title = currentScreen.title,
                    canNavigateBack = currentScreen != Screen.Home,
                    onBack = { navController.popBackStack() }
                )
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Home.route) {
                        PageContent {
                            HomePage(
                                totalAmount = uiState.totalAmount,
                                assets = uiState.analyzedAssets,
                                needsRebalance = uiState.needsRebalance,
                                navController = navController
                            )
                        }
                    }
                    composable(Screen.Allocation.route) {
                        PageContent {
                            AllocationPage(
                                investmentInput = uiState.investmentInput,
                                results = uiState.allocationResults,
                                onInputChange = viewModel::updateInvestmentInput
                            )
                        }
                    }
                    composable(Screen.Holdings.route) {
                        PageContent {
                            HoldingsPage(
                                totalAmount = uiState.totalAmount,
                                assets = uiState.analyzedAssets,
                                onAmountChange = viewModel::updateCurrentAmount,
                                onGoRebalance = { navController.navigate(Screen.Rebalance.route) }
                            )
                        }
                    }
                    composable(Screen.Rebalance.route) {
                        PageContent {
                            NewFundRebalancePage(
                                newFundInput = uiState.newFundInput,
                                results = uiState.newFundResults,
                                onInputChange = viewModel::updateNewFundInput
                            )
                        }
                    }
                    composable(Screen.Settings.route) {
                        PageContent {
                            SettingsPage(assets = uiState.assets)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun TopBar(title: String, canNavigateBack: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateBack) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomePage(
    totalAmount: Double,
    assets: List<AssetItem>,
    needsRebalance: Boolean,
    navController: NavController
) {
    CardBlock {
        Text("当前总资产", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text("¥${moneyText(totalAmount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (needsRebalance) "已明显偏离，建议再平衡" else "偏离不大，暂不需要调整",
            color = if (needsRebalance) Color(0xFFC0392B) else Color(0xFF5F6368)
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        HomeButton("金额分配", Modifier.weight(1f)) { navController.navigate(Screen.Allocation.route) }
        HomeButton("当前持仓", Modifier.weight(1f)) { navController.navigate(Screen.Holdings.route) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        HomeButton("再平衡", Modifier.weight(1f)) { navController.navigate(Screen.Rebalance.route) }
        HomeButton("配置设置", Modifier.weight(1f)) { navController.navigate(Screen.Settings.route) }
    }

    SectionTitle("目标比例概览")
    assets.forEach { asset ->
        AssetOverviewRow(asset = asset)
    }
}

@Composable
private fun AllocationPage(
    investmentInput: String,
    results: List<AllocationResult>,
    onInputChange: (String) -> Unit
) {
    AmountInput(
        label = "本次投资总金额",
        value = investmentInput,
        onValueChange = onInputChange
    )
    SectionTitle("应买入金额")
    results.forEach { result ->
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.assetName, fontWeight = FontWeight.SemiBold)
                Text("买入 ¥${moneyText(result.amount)}", color = Color(0xFF2E7D5B), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HoldingsPage(
    totalAmount: Double,
    assets: List<AssetItem>,
    onAmountChange: (String, String) -> Unit,
    onGoRebalance: () -> Unit
) {
    CardBlock {
        Text("按当前总资产测算", fontWeight = FontWeight.Bold)
        Text("当前总资产：¥${moneyText(totalAmount)}", color = Color.Gray)
    }

    assets.forEach { asset ->
        var input by rememberSaveable(asset.id) { mutableStateOf(moneyText(asset.currentAmount)) }
        LaunchedEffect(asset.currentAmount) {
            if (parseAmount(input) != asset.currentAmount) {
                input = moneyText(asset.currentAmount)
            }
        }
        CardBlock {
            Text(asset.name, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    onAmountChange(asset.id, it)
                },
                label = { Text("当前持仓金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("当前持仓", "¥${moneyText(asset.currentAmount)}")
            DetailRow("当前占比", percentText(asset.currentPercent))
            DetailRow("目标占比", percentText(asset.targetPercent))
            DetailRow("目标金额", "¥${moneyText(asset.targetAmount)}")
            DetailRow("偏离比例", signedPercentText(asset.deviationPercent), colorForSignedValue(asset.deviationPercent))
            DetailRow("偏离金额", signedMoneyText(asset.deviationAmount), colorForSignedValue(asset.deviationAmount))
            DetailRow("调整建议", asset.actionSuggestion, adjustmentColor(asset.adjustmentAmount))
        }
    }

    Button(
        onClick = onGoRebalance,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("去再平衡")
    }
}

@Composable
private fun NewFundRebalancePage(
    newFundInput: String,
    results: List<NewFundRebalanceResult>,
    onInputChange: (String) -> Unit
) {
    val investmentAmount = parseAmount(newFundInput)
    val isInsufficientForTrade = investmentAmount > 0.0 && investmentAmount < 205.0

    CardBlock {
        Text("模式：可执行买单再平衡", fontWeight = FontWeight.Bold)
        Text("按 200 元整数倍和每笔 5 元手续费生成建议，手续费从新增资金中扣除。", color = Color.Gray)
        if (isInsufficientForTrade) {
            Text("资金不足以完成一笔最低买入。", color = Color(0xFFC0392B), fontWeight = FontWeight.SemiBold)
        }
    }
    AmountInput(
        label = "新增投资金额",
        value = newFundInput,
        onValueChange = onInputChange
    )
    SectionTitle("本次建议买单")
    results.forEach { result ->
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.assetName, fontWeight = FontWeight.SemiBold)
                Text("买入 ¥${moneyText(result.buyAmount)}", color = Color(0xFF2E7D5B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow("手续费", "¥${moneyText(result.feeAmount)}")
            DetailRow("占用资金", "¥${moneyText(result.totalCost)}")
            DetailRow("买后预计占比", percentText(result.projectedPercent))
            DetailRow(
                "买后偏离",
                signedPercentText(result.projectedDeviationPercent),
                colorForSignedValue(result.projectedDeviationPercent)
            )
        }
    }
}

@Composable
private fun SettingsPage(assets: List<AssetItem>) {
    CardBlock {
        Text("MVP 阶段使用默认资产配置", fontWeight = FontWeight.Bold)
        Text("后续可在这里开放新增、删除、名称和目标比例修改，并保存前校验合计 100%。", color = Color.Gray)
    }
    assets.forEach { asset ->
        AssetOverviewRow(asset = asset)
    }
}

@Composable
private fun AmountInput(label: String, value: String, onValueChange: (String) -> Unit) {
    CardBlock {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AssetOverviewRow(asset: AssetItem) {
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(asset.name, fontWeight = FontWeight.SemiBold)
            Text(percentText(asset.targetPercent), color = Color(0xFF2E7D5B), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("当前 ¥${moneyText(asset.currentAmount)}", color = Color.Gray)
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color(0xFF202124)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

private fun signedPercentText(value: Double): String =
    "${if (value > 0.0) "+" else ""}${percentText(value)}"

private fun signedMoneyText(value: Double): String {
    val prefix = when {
        value > 0.0 -> "+¥"
        value < 0.0 -> "-¥"
        else -> "¥"
    }
    return "$prefix${moneyText(abs(value))}"
}

private fun colorForSignedValue(value: Double): Color = when {
    value > 0.0 -> Color(0xFFC0392B)
    value < 0.0 -> Color(0xFF2E7D5B)
    else -> Color(0xFF5F6368)
}

private fun adjustmentColor(adjustmentAmount: Double): Color = when {
    adjustmentAmount > 0.0 -> Color(0xFF2E7D5B)
    adjustmentAmount < 0.0 -> Color(0xFFC0392B)
    else -> Color(0xFF5F6368)
}

