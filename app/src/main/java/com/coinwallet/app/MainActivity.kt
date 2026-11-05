package com.coinwallet.app
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import android.graphics.BitmapFactory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class Coin(
    val id: String,
    val name: String,
    val symbol: String,
    val image: String,
    val price: Double,
    val change: Double,
    val marketCap: Double,
    val volume: Double,
    val high: Double,
    val low: Double
)

data class Holding(
    val id: String,
    val name: String,
    val symbol: String,
    val quantity: Double,
    val buyPrice: Double
)

object MarketApi {

    private fun request(urlText: String): String {
        val connection = URL(urlText).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")

        return try {
            if (connection.responseCode !in 200..299) {
                throw Exception("API error: HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun getMarkets(): List<Coin> {
        val url = "https://api.coingecko.com/api/v3/coins/markets" +
                "?vs_currency=usd&order=market_cap_desc" +
                "&per_page=100&page=1&sparkline=false" +
                "&price_change_percentage=24h"

        val json = JSONArray(request(url))
        val result = mutableListOf<Coin>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            result.add(
                Coin(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    symbol = item.optString("symbol").uppercase(),
                    image = item.optString("image"),
                    price = item.optDouble("current_price"),
                    change = item.optDouble("price_change_percentage_24h"),
                    marketCap = item.optDouble("market_cap"),
                    volume = item.optDouble("total_volume"),
                    high = item.optDouble("high_24h"),
                    low = item.optDouble("low_24h")
                )
            )
        }

        return result
    }

    fun getChart(coinId: String, days: String): List<Double> {
        val url = "https://api.coingecko.com/api/v3/coins/$coinId/market_chart" +
                "?vs_currency=usd&days=$days"

        val json = JSONObject(request(url))
        val prices = json.getJSONArray("prices")
        val result = mutableListOf<Double>()

        for (i in 0 until prices.length()) {
            result.add(prices.getJSONArray(i).getDouble(1))
        }

        return result
    }
}

class MainActivity : ComponentActivity() {

    private val coins = mutableStateListOf<Coin>()
    private val holdings = mutableStateListOf<Holding>()

    private var loading by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadHoldings()

        setContent {
            MaterialTheme {
                CoinWalletScreen(
                    coins = coins,
                    holdings = holdings,
                    loading = loading,
                    errorMessage = errorMessage,
                    onRefresh = { loadMarkets() },
                    onAddHolding = { holding -> addHolding(holding) },
                    onRemoveHolding = { id -> removeHolding(id) }
                )
            }
        }

        loadMarkets()
    }

    private fun loadMarkets() {
        if (loading) return

        loading = true
        errorMessage = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = MarketApi.getMarkets()

                withContext(Dispatchers.Main) {
                    coins.clear()
                    coins.addAll(result)
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Unable to load market data"
                    loading = false
                }
            }
        }
    }

    private fun loadHoldings() {
        val prefs = getSharedPreferences("coinwallet", MODE_PRIVATE)
        val saved = prefs.getString("holdings", "[]") ?: "[]"

        try {
            val array = JSONArray(saved)
            holdings.clear()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                holdings.add(
                    Holding(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        symbol = item.optString("symbol"),
                        quantity = item.optDouble("quantity"),
                        buyPrice = item.optDouble("buyPrice")
                    )
                )
            }
        } catch (_: Exception) {
            holdings.clear()
        }
    }

    private fun saveHoldings() {
        val array = JSONArray()

        holdings.forEach { holding ->
            val item = JSONObject()
            item.put("id", holding.id)
            item.put("name", holding.name)
            item.put("symbol", holding.symbol)
            item.put("quantity", holding.quantity)
            item.put("buyPrice", holding.buyPrice)
            array.put(item)
        }

        getSharedPreferences("coinwallet", MODE_PRIVATE)
            .edit()
            .putString("holdings", array.toString())
            .apply()
    }

    private fun addHolding(holding: Holding) {
        holdings.add(holding)
        saveHoldings()
    }

    private fun removeHolding(id: String) {
        holdings.removeAll { it.id == id }
        saveHoldings()
    }
}

// COINWALLET MAIN DASHBOARD

private val walletBackground = Color(0xFF0B1424)
private val walletCard = Color(0xFF132139)
private val walletSurface2 = Color(0xFF1A2942)
private val walletStroke = Color(0xFF293B56)
private val walletCyan = Color(0xFF35C5F4)
private val walletGreen = Color(0xFF42D6A4)
private val walletRed = Color(0xFFFF718B)
private val walletText = Color(0xFFF3F6FF)
private val walletMuted = Color(0xFF9BAAC0)

@Composable
fun CoinWalletScreen(
    coins: List<Coin>,
    holdings: List<Holding>,
    loading: Boolean,
    errorMessage: String,
    onRefresh: () -> Unit,
    onAddHolding: (Holding) -> Unit,
    onRemoveHolding: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Market") }
    var search by remember { mutableStateOf("") }
    var selectedCoin by remember { mutableStateOf<Coin?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCoins = coins.filter {
        it.name.contains(search, true) ||
        it.symbol.contains(search, true)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = walletBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "COINWALLET",
                        color = walletGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your crypto dashboard",
                        color = walletText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onRefresh) {
                    Text("↻ Refresh", color = walletCyan
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(walletCard, RoundedCornerShape(16.dp))
                    .padding(5.dp)
            ) {
                listOf("Market", "Portfolio").forEach { tab ->
                    val active = selectedTab == tab

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (active) Color(0xFF263A50)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedTab = tab
                                selectedCoin = null
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            color = if (active) walletGreen else walletMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (selectedCoin != null) {
                CoinDetailScreen(
                    coin = selectedCoin!!,
                    onBack = { selectedCoin = null }
                )
            } else if (selectedTab == "Market") {

                MarketDashboard(
                    coins = filteredCoins,
                    search = search,
                    onSearchChange = { search = it },
                    loading = loading,
                    errorMessage = errorMessage,
                    onSelectCoin = { selectedCoin = it },
                    onRefresh = onRefresh
                )

            } else {

                PortfolioDashboard(
                    holdings = holdings,
                    coins = coins,
                    onAdd = { showAddDialog = true },
                    onRemove = onRemoveHolding
                )
            }
        }
    }

    if (showAddDialog) {
        AddHoldingDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddHolding(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MarketDashboard(
    coins: List<Coin>,
    search: String,
    onSearchChange: (String) -> Unit,
    loading: Boolean,
    errorMessage: String,
    onSelectCoin: (Coin) -> Unit,
    onRefresh: () -> Unit
) {
    Text(
        "Cryptocurrency Market",
        color = walletText,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search Bitcoin, Ethereum...") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = walletText,
            unfocusedTextColor = walletText,
            focusedBorderColor = walletGreen,
            unfocusedBorderColor = walletMuted,
            focusedLabelColor = walletGreen,
            cursorColor = walletGreen
        )
    )

    Spacer(Modifier.height(14.dp))

    if (loading && coins.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().padding(35.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = walletGreen)
        }
    }

    if (errorMessage.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(walletCard, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                "Market data unavailable",
                color = walletRed,
                fontWeight = FontWeight.Bold
            )
            Text(
                errorMessage,
                color = walletMuted,
                fontSize = 12.sp
            )
            TextButton(onClick = onRefresh) {
                Text("Try again", color = walletGreen)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Coin", color = walletMuted, fontSize = 12.sp)
        Text("Price / 24h", color = walletMuted, fontSize = 12.sp)
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(coins, key = { it.id }) { coin ->
            CoinMarketRow(
                coin = coin,
                onClick = { onSelectCoin(coin) }
            )
        }

        if (!loading && coins.isEmpty() && errorMessage.isBlank()) {
            item {
                Text(
                    "No coins found.",
                    color = walletMuted,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}


@Composable
private fun CoinAvatar(imageUrl: String, symbol: String) {
    var bitmap by remember(imageUrl) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(imageUrl) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                java.net.URL(imageUrl).openStream().use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(walletSurface2),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "$symbol logo",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                symbol.take(3),
                color = walletCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CoinMarketRow(
    coin: Coin,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(walletCard, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinAvatar(coin.image, coin.symbol)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                coin.name,
                color = walletText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                coin.symbol,
                color = walletMuted,
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatUsd(coin.price),
                color = walletText,
                fontWeight = FontWeight.Bold
            )

            Text(
                String.format(Locale.US, "%.2f%%", coin.change),
                color = if (coin.change >= 0) walletGreen else walletRed,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PortfolioDashboard(
    holdings: List<Holding>,
    coins: List<Coin>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    val totalValue = holdings.sumOf { holding ->
        val current = coins.find {
            it.symbol.equals(holding.symbol, true)
        }?.price ?: holding.buyPrice

        holding.quantity * current
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "My Portfolio",
            color = walletText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(walletCard, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Text("Estimated portfolio value", color = walletMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                formatUsd(totalValue),
                color = walletGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Based on your manually entered holdings",
                color = walletMuted,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = walletGreen,
                contentColor = Color.Black
            )
        ) {
            Text("+ Add asset", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        if (holdings.isEmpty()) {
            Text(
                "No assets added yet. Add your first asset.",
                color = walletMuted,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(holdings, key = { it.id }) { holding ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(walletCard, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    holding.name,
                                    color = walletText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    holding.symbol,
                                    color = walletMuted,
                                    fontSize = 12.sp
                                )
                            }

                            TextButton(onClick = { onRemove(holding.id) }) {
                                Text("Remove", color = walletRed)
                            }
                        }

                        Text(
                            "Quantity: ${holding.quantity}",
                            color = walletMuted
                        )
                    }
                }
            }
        }
    }
}

private fun formatUsd(value: Double): String {
    return if (value >= 1.0) {
        String.format(Locale.US, "$%,.2f", value)
    } else {
        String.format(Locale.US, "$%.8f", value)
    }
}

// INDIVIDUAL COIN DETAILS AND PRICE CHART

@Composable
private fun CoinDetailScreen(
    coin: Coin,
    onBack: () -> Unit
) {
    var selectedDays by remember { mutableStateOf("7") }
    var prices by remember(coin.id) { mutableStateOf<List<Double>>(emptyList()) }
    var loadingChart by remember(coin.id) { mutableStateOf(false) }
    var chartError by remember(coin.id) { mutableStateOf("") }

    LaunchedEffect(coin.id, selectedDays) {
        loadingChart = true
        chartError = ""

        try {
            val result = withContext(Dispatchers.IO) {
                MarketApi.getChart(coin.id, selectedDays)
            }
            prices = result
        } catch (e: Exception) {
            chartError = e.message ?: "Chart unavailable"
        } finally {
            loadingChart = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Text("← Back to market", color = walletGreen)
        }

        Text(
            coin.name,
            color = walletText,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(coin.symbol, color = walletMuted)

        Spacer(Modifier.height(16.dp))

        Text(
            formatUsd(coin.price),
            color = walletGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            String.format(Locale.US, "%.2f%% (24h)", coin.change),
            color = if (coin.change >= 0) walletGreen else walletRed
        )

        Spacer(Modifier.height(22.dp))

        Text(
            "Price chart",
            color = walletText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                "1" to "1D",
                "7" to "7D",
                "30" to "1M",
                "90" to "3M",
                "365" to "1Y"
            ).forEach { (days, label) ->
                FilterChip(
                    selected = selectedDays == days,
                    onClick = { selectedDays = days },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = walletGreen,
                        selectedLabelColor = Color.Black,
                        labelColor = walletText
                    )
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        if (loadingChart) {
            Box(
                Modifier.fillMaxWidth().height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = walletGreen)
            }
        } else if (prices.size > 1) {
            PriceChart(
                prices = prices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chartError.ifBlank { "No chart data available" },
                    color = walletMuted
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Market information",
            color = walletText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        DetailInfoRow("Market capitalization", formatUsd(coin.marketCap))
        DetailInfoRow("24-hour volume", formatUsd(coin.volume))
        DetailInfoRow("24-hour high", formatUsd(coin.high))
        DetailInfoRow("24-hour low", formatUsd(coin.low))
        DetailInfoRow("Current price", formatUsd(coin.price))
        DetailInfoRow(
            "24-hour change",
            String.format(Locale.US, "%.2f%%", coin.change)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Market prices are supplied by CoinGecko. Values may be delayed.",
            color = walletMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = walletMuted)
        Text(value, color = walletText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriceChart(
    prices: List<Double>,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (prices.size < 2) return@Canvas

        val minPrice = prices.minOrNull() ?: return@Canvas
        val maxPrice = prices.maxOrNull() ?: return@Canvas
        val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

        val stepX = size.width / (prices.size - 1).toFloat()

        val path = androidx.compose.ui.graphics.Path()

        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val y = size.height -
                    ((price - minPrice) / range).toFloat() * size.height

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = walletGreen,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx()
            )
        )
    }
}

// ADD CUSTOM PORTFOLIO ASSET

@Composable
private fun AddHoldingDialog(
    onDismiss: () -> Unit,
    onSave: (Holding) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }

    val valid = name.isNotBlank() &&
            symbol.isNotBlank() &&
            (quantity.toDoubleOrNull() ?: 0.0) > 0.0 &&
            (buyPrice.toDoubleOrNull() ?: 0.0) >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = walletCard,
        title = {
            Text("Add portfolio asset", color = walletText)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Symbol") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("Purchase price (USD)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Holding(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            symbol = symbol.trim(),
                            quantity = quantity.toDouble(),
                            buyPrice = buyPrice.toDouble()
                        )
                    )
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = walletGreen,
                    contentColor = Color.Black
                )
            ) {
                Text("Save asset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = walletMuted)
            }
        }
    )
}
