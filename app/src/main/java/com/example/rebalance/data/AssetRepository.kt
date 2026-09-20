package com.example.rebalance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.rebalance.domain.RebalanceCalculator
import com.example.rebalance.model.AssetItem
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.assetDataStore by preferencesDataStore(name = "asset_store")

class AssetRepository(private val context: Context) {
    private val assetsKey = stringPreferencesKey("assets")

    val assets: Flow<List<AssetItem>> = context.assetDataStore.data.map { preferences ->
        val decodedAssets = preferences[assetsKey]?.let(::decodeAssets).orEmpty()
        if (decodedAssets.isEmpty()) {
            RebalanceCalculator.defaultAssets
        } else {
            migrateHistoricalDefaults(decodedAssets)
        }
    }

    suspend fun saveAssets(assets: List<AssetItem>) {
        context.assetDataStore.edit { preferences ->
            preferences[assetsKey] = encodeAssets(assets)
        }
    }

    private fun migrateHistoricalDefaults(assets: List<AssetItem>): List<AssetItem> {
        if (!RebalanceCalculator.isHistoricalDefaultAllocation(assets)) return assets
        return RebalanceCalculator.defaultAssets.map { defaultAsset ->
            defaultAsset.copy(currentAmount = assets.firstOrNull { it.id == defaultAsset.id }?.currentAmount ?: 0.0)
        }
    }

    private fun encodeAssets(assets: List<AssetItem>): String =
        assets.joinToString("\n") { asset ->
            listOf(
                encode(asset.id),
                encode(asset.name),
                asset.targetPercent.toString(),
                asset.currentAmount.toString()
            ).joinToString("\t")
        }

    private fun decodeAssets(raw: String): List<AssetItem> =
        raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size != 4) return@mapNotNull null
                AssetItem(
                    id = decode(parts[0]),
                    name = decode(parts[1]),
                    targetPercent = parts[2].toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    currentAmount = parts[3].toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                )
            }
            .toList()

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
