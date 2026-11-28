package com.example.walletlookup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Incoming / outgoing colours (used only for money moving in and out). */
val InGreen = Color(0xFF2EBD85)
val OutRed = Color(0xFFF6465D)

/** Coin logos: the public-domain (CC0) "cryptocurrency-icons" set, plus a few networks' from Trust Wallet's assets. */
private fun coinRes(symbol: String): Int? = when (symbol.uppercase().replace("₮", "T")) {
    "BTC" -> R.drawable.coin_btc
    "ETH" -> R.drawable.coin_eth
    "BNB" -> R.drawable.coin_bnb
    "TRX" -> R.drawable.coin_trx
    "SOL" -> R.drawable.coin_sol
    "XRP" -> R.drawable.coin_xrp
    "LTC" -> R.drawable.coin_ltc
    "BCH" -> R.drawable.coin_bch
    "DOGE" -> R.drawable.coin_doge
    "ADA" -> R.drawable.coin_ada
    "DOT" -> R.drawable.coin_dot
    "ATOM" -> R.drawable.coin_atom
    "XLM" -> R.drawable.coin_xlm
    "ALGO" -> R.drawable.coin_algo
    "XTZ" -> R.drawable.coin_xtz
    "VET" -> R.drawable.coin_vet
    "VTHO" -> R.drawable.coin_vtho
    "ETC" -> R.drawable.coin_etc
    "AVAX" -> R.drawable.coin_avax
    "POL", "MATIC" -> R.drawable.coin_matic
    "EOS" -> R.drawable.coin_eos
    "USDT" -> R.drawable.coin_usdt
    "USDC" -> R.drawable.coin_usdc
    "DAI" -> R.drawable.coin_dai
    "LINK" -> R.drawable.coin_link
    "UNI" -> R.drawable.coin_uni
    "TON" -> R.drawable.coin_ton
    "APT" -> R.drawable.coin_apt
    "NEAR" -> R.drawable.coin_near
    "CELO" -> R.drawable.coin_celo
    "S" -> R.drawable.coin_sonic
    "ARB" -> R.drawable.coin_arbitrum
    "OP" -> R.drawable.coin_optimism
    "BASE" -> R.drawable.coin_base
    else -> null
}

/** Coins without a logo (an unknown token) get a grey circle with an initial. */
private fun fallback(symbol: String): Pair<Color, String> = Color(0xFF8A8A8A) to symbol.take(1).uppercase()

/** The coin that stands for a network (its logo is used as the network's logo). */
fun networkSymbol(network: String): String = when (network) {
    "Ethereum", "Ethereum & EVM networks" -> "ETH"
    "BSC" -> "BNB"
    "Polygon" -> "POL"
    "Arbitrum" -> "ARB"
    "Optimism" -> "OP"
    "Base" -> "BASE"
    "Avalanche" -> "AVAX"
    "Celo" -> "CELO"
    "Sonic" -> "S"
    "Ethereum Classic" -> "ETC"
    "VeChain" -> "VET"
    "Tron" -> "TRX"
    "Solana" -> "SOL"
    "TON" -> "TON"
    "XRP Ledger" -> "XRP"
    "Aptos" -> "APT"
    "Bitcoin" -> "BTC"
    "Bitcoin Cash" -> "BCH"
    "Litecoin" -> "LTC"
    "Dogecoin" -> "DOGE"
    "Cardano" -> "ADA"
    "Polkadot" -> "DOT"
    "Cosmos Hub" -> "ATOM"
    "Stellar" -> "XLM"
    "Algorand" -> "ALGO"
    "NEAR", "NEAR Protocol" -> "NEAR"
    "Tezos" -> "XTZ"
    "EOS" -> "EOS"
    else -> network.take(3)
}

@Composable
fun CoinIcon(symbol: String, size: Dp = 28.dp) {
    val res = coinRes(symbol)
    if (res != null) {
        Image(painterResource(res), contentDescription = symbol, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        val (color, letter) = fallback(symbol)
        Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(letter, color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
    }
}

@Composable
fun NetworkIcon(network: String, size: Dp = 28.dp) = CoinIcon(networkSymbol(network), size)

/** Draws a coin logo onto a bitmap canvas (used by the share card). */
fun drawCoinLogo(context: Context, c: Canvas, symbol: String, cx: Float, cy: Float, r: Float) {
    val res = coinRes(symbol)
    val rect = RectF(cx - r, cy - r, cx + r, cy + r)
    if (res != null) {
        // Works for both the PNG coins and the vector logos (TON, Aptos).
        androidx.core.content.ContextCompat.getDrawable(context, res)?.let { dr ->
            dr.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
            dr.draw(c)
            return
        }
    }
    val (color, letter) = fallback(symbol)
    c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.argb(255,
        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()) })
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE; textSize = r * 0.9f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    c.drawText(letter, cx, cy + r * 0.32f, text)
}
