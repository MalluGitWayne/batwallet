package com.example.walletlookup

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
}
