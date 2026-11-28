package com.example.walletlookup

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks for the 21.7 review round (no network: only the logic). */
class ReviewFixesTest {
    @Test fun invisibleCharactersAndLineBreaksAreRemovedFromAPastedAddress() {
        val tron = "TGSNahJvAdh86aR5pdcxiep1mBWSRK3HHL"
        assertEquals(tron, cleanAddress("$tron​"))
        assertEquals(tron, cleanAddress("﻿$tron"))
        assertEquals(tron, cleanAddress("${tron.take(17)}\r\n${tron.drop(17)}"))
        assertEquals(tron, cleanAddress("\t$tron\t"))
        assertEquals(AddrType.TRON, detect(cleanAddress("$tron​")))
    }

    @Test fun dogecoinMultisigAddressesArentTakenForSolana() {
        // Dogecoin P2SH addresses: 34 characters starting with A or 9.
        assertEquals(AddrType.DOGECOIN, detect("A8c3xNz2mqsDLFwv5KL5fpH12QEwDaoTXo"))
        assertEquals(AddrType.DOGECOIN, detect("9tp7THKDYTEb4MiT5sVSE2zkMRpzLDpGTd"))
        // Solana addresses (43–44 characters) still are.
        assertEquals(AddrType.SOLANA, detect("9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"))
        assertEquals(AddrType.SOLANA, detect("AC5RDfQFmDS1deWZos921JfqscXdByf8BKHs5ACWjtW2"))
    }

    @Test fun aCutOffBackupIsReportedAsDamagedNotAsAWrongPassword() {
        for (text in listOf("{\"encrypted\":1,\"salt\":\"abc", "{\"encrypted\":1}", "not a backup")) {
            val e = runCatching { BackupCrypto.decrypt(text, "pw") }.exceptionOrNull()
            assertTrue(text, e is BackupCrypto.DamagedException)
        }
    }

    @Test fun anEvmAddressInOtherLetterCaseIsTheSameWallet() {
        assertEquals(sameKey("0xAbC0000000000000000000000000000000000001@BSC"), sameKey("0xabc0000000000000000000000000000000000001@BSC"))
        // Case matters for other networks (Tron, Solana, …).
        assertTrue(sameKey("TGSNahJvAdh86aR5pdcxiep1mBWSRK3HHL") != sameKey("tgsnahjvadh86ar5pdcxiep1mbwsrk3hhl"))
    }

    @Test fun aChangeAfterALongGapIsSplitIntoTheRealPayments() {
        // The real case: the app last took the balances on 21 Nov; by 23 Nov nine transfers had happened, adding up
        // to +0.1235 USDT. They must come back one by one, not as one "payment" no transaction matches.
        val h = 3_600_000L
        val since = 1_000_000_000_000L
        val now = since + 45 * h
        fun t(inc: Boolean, amt: String, at: Long) = Transfer(inc, BigDecimal(amt), null, at, null, "0x$amt$at", symbol = "USDT")
        val list = listOf(
            t(true, "599.83", since + 14 * h), t(false, "500.01", since + 14 * h + 60_000), t(false, "100.42", since + 14 * h + 120_000),
            t(true, "184.99", since + 43 * h), t(true, "607.86", since + 43 * h + 180_000), t(false, "150", since + 43 * h + 240_000),
            t(false, "642.0065", since + 43 * h + 300_000), t(true, "14.87", since + 43 * h + 600_000), t(false, "15", since + 43 * h + 660_000),
        )
        val change = list.fold(BigDecimal.ZERO) { s, x -> s + if (x.incoming) x.amount else x.amount.negate() }
        assertEquals(9, pickTransfers(list, change, now, since).size)
        // Hidden dust (spam under a cent) is in the balance but not in the list: still the same nine payments.
        assertEquals(9, pickTransfers(list, change + BigDecimal("0.000016"), now, since).size)
        // Without knowing when the balances were last taken, the old 3-hour window can't explain it.
        assertEquals(0, pickTransfers(list, change, now).size)
        // A transfer from before the last check doesn't belong to this change.
        assertEquals(0, pickTransfers(list + t(true, "5", since - 3 * h), change + BigDecimal("5"), now, since).size)
    }

    @Test fun anExchangeIsRecognisedByItsWalletOrByYourLabel() {
        assertEquals("Binance", exchangeFor("0x8894E0a0c962CB723c1976a4421c95949bE2D4E3"))
        assertEquals("HTX", exchangeFor("0xab5C66752a9e8167967685F1450532fB96d5d24f"))
        val stranger = "0x1111111111111111111111111111111111111111"
        assertEquals("Bybit", exchangeFor(stranger, "My Bybit"))
        assertEquals("Binance", exchangeFor(stranger, "Binance · deposit"))
        assertEquals(null, exchangeFor(stranger, "Gateway shop"))
        assertEquals(null, exchangeFor(stranger, null))
    }
}
