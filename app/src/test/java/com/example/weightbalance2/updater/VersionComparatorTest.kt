package com.example.weightbalance2.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testVersionComparison() {
        // Higher main version
        assertTrue(VersionComparator.compare("1.2.0", "1.1.0") > 0)
        assertTrue(VersionComparator.compare("2.0.0", "1.9.9") > 0)

        // Stable > Beta of same version
        assertTrue(VersionComparator.compare("1.1.0", "1.1.0-beta1") > 0)
        assertTrue(VersionComparator.compare("1.1.0", "1.1.0-alpha") > 0)

        // Beta comparisons
        assertTrue(VersionComparator.compare("1.1.0-beta2", "1.1.0-beta1") > 0)
        assertTrue(VersionComparator.compare("1.1.0-beta10", "1.1.0-beta2") > 0)
        assertTrue(VersionComparator.compare("1.1.0-beta2", "1.1.0-alpha5") > 0)
        
        // Identity
        assertEquals(0, VersionComparator.compare("1.1.0", "1.1.0"))
        assertEquals(0, VersionComparator.compare("1.1.0-beta1", "1.1.0-beta1"))
    }

    @Test
    fun testIsNewer() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.1.0-beta3"))
        assertTrue(!VersionComparator.isNewer("1.1.0-beta2", "1.1.0-beta3"))
        assertTrue(VersionComparator.isNewer("1.1.1", "1.1.0"))
    }
}
