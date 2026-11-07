package com.example.walletlookup

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest

/*
 * Solana token balances without the "indexed" getTokenAccountsByOwner call, which the free publicnode node now refuses
 * (it wants a paid token) and Solana's own public node often rate-limits, so balances got stuck on old values.
 * A wallet's USDT / USDC sit in its associated token account, whose address is worked out from the wallet and the
 * mint, and read with getMultipleAccounts, which every free node answers.
 */

private const val B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
private const val ATA_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

internal fun b58decode(s: String): ByteArray {
    var n = BigInteger.ZERO
    for (c in s) {
        val d = B58.indexOf(c)
        require(d >= 0) { "Not Base58: $s" }
        n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(d.toLong()))
    }
    val digits = if (n.signum() == 0) ByteArray(0) else n.toByteArray().let { if (it[0].toInt() == 0) it.copyOfRange(1, it.size) else it }
    return ByteArray(s.takeWhile { it == '1' }.length) + digits
}

internal fun b58encode(bytes: ByteArray): String {
    var n = BigInteger(1, bytes)
    val sb = StringBuilder()
    val base = BigInteger.valueOf(58)
    while (n.signum() > 0) { val (q, r) = n.divideAndRemainder(base); sb.append(B58[r.toInt()]); n = q }
    for (b in bytes) { if (b.toInt() == 0) sb.append('1') else break }
    return sb.reverse().toString()
}

private val P: BigInteger = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19))
private val D: BigInteger = BigInteger.valueOf(-121665).mod(P).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P)

/** Whether 32 bytes decode to a point on the ed25519 curve (a program-derived address must not). */
internal fun onCurve(key: ByteArray): Boolean {
    val le = key.reversedArray()
    le[0] = (le[0].toInt() and 0x7f).toByte() // drop the x sign bit
    val y = BigInteger(1, le).mod(P)
    val y2 = y.multiply(y).mod(P)
    val u = y2.subtract(BigInteger.ONE).mod(P)
    val v = D.multiply(y2).add(BigInteger.ONE).mod(P)
    val x2 = u.multiply(v.modInverse(P)).mod(P)
    if (x2.signum() == 0) return true
    return x2.modPow(P.subtract(BigInteger.ONE).shiftRight(1), P) == BigInteger.ONE
}

/** The program-derived address for [seeds] under [program] (Solana's findProgramAddress). */
internal fun findPda(seeds: List<ByteArray>, program: ByteArray): ByteArray {
    for (bump in 255 downTo 0) {
        val sha = MessageDigest.getInstance("SHA-256")
        seeds.forEach(sha::update)
        sha.update(byteArrayOf(bump.toByte()))
        sha.update(program)
        sha.update("ProgramDerivedAddress".toByteArray())
        val h = sha.digest()
        if (!onCurve(h)) return h
    }
    error("No program address")
}

/** The associated token account of [owner] for [mint]. */
internal fun associatedTokenAccount(owner: String, mint: String): String =
    b58encode(findPda(listOf(b58decode(owner), b58decode(TOKEN_PROGRAM), b58decode(mint)), b58decode(ATA_PROGRAM)))

/**
 * [owner]'s token accounts for [mints]: mint → (account address, balance), only the accounts that exist. One request.
 * Throws when the node can't be read (so a failed read never shows as 0).
 */
internal fun solanaTokenAccounts(owner: String, mints: Collection<String>): Map<String, Pair<String, BigDecimal>> {
    val accounts = mints.associateWith { associatedTokenAccount(owner, it) }
    val list = JSONArray().apply { accounts.values.forEach { put(it) } }
    val value = solanaCall("getMultipleAccounts", JSONArray().put(list).put(JSONObject().put("encoding", "jsonParsed")))
        .getJSONObject("result").getJSONArray("value")
    val out = mutableMapOf<String, Pair<String, BigDecimal>>()
    accounts.entries.forEachIndexed { i, (mint, address) ->
        val acc = value.optJSONObject(i) ?: return@forEachIndexed
        val amount = acc.optJSONObject("data")?.optJSONObject("parsed")?.optJSONObject("info")
            ?.optJSONObject("tokenAmount")?.optString("uiAmountString")?.takeIf { it.isNotBlank() } ?: "0"
        out[mint] = address to BigDecimal(amount)
    }
    return out
}
