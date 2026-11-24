package com.centiox.verdant

import kotlin.test.*

class PortableSecurityTest {
    @Test
    fun protectedBackupsAuthenticateTheirContentsAndUseFreshRandomness() {
        val json = encodeBackup(BudgetData(name = "Private owner", onboarded = true))
        val password = "a long unique backup password"
        val first = protectBackup(json, password)
        val second = protectBackup(json, password)
        assertTrue(isProtectedBackup(first))
        assertFalse(first.contains("Private owner"))
        assertNotEquals(first, second)
        assertEquals(json, unprotectBackup(first, password))
        assertEquals(json, unprotectBackup(second, password))
        assertFailsWith<IllegalArgumentException> { unprotectBackup(first, "incorrect password") }
        val damaged = first.replace("ciphertext\":\"", "ciphertext\":\"A")
        assertFailsWith<IllegalArgumentException> { unprotectBackup(damaged, password) }
        assertFailsWith<IllegalArgumentException> { protectBackup(json, "short") }
    }

    @Test
    fun calculatorUsesDecimalMathAndOperatorPrecedence() {
        assertEquals(30, calculateAmount("0.10 + 0.20"))
        assertEquals(17500, calculateAmount("(100 + 250) ÷ 2"))
        assertEquals(4000, calculateAmount("100 - 20 * 3"))
        assertEquals(3333, calculateAmount("100 / 3"))
        assertEquals(1, calculateAmount("0.005"))
        assertFailsWith<IllegalArgumentException> { calculateAmount("1/0") }
        assertFails { calculateAmount("1e30") }
        assertFails { calculateAmount("2-10") }
        assertFails { calculateAmount("9".repeat(40)) }
    }
}
