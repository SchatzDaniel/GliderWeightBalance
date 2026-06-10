package com.danielschatz.gliderweightbalance.updater

object VersionComparator {

    /**
     * Compares two version strings.
     * Returns a positive number if v1 > v2, negative if v1 < v2, and 0 if equal.
     * Handles versions like "v1.1.0-debug" and "1.1.0-beta3".
     */
    fun compare(v1: String, v2: String): Int {
        // Normalize: remove "v" prefix and "-debug" suffix
        val clean1 = v1.removePrefix("v").removeSuffix("-debug")
        val clean2 = v2.removePrefix("v").removeSuffix("-debug")

        val parts1 = clean1.split("-")
        val parts2 = clean2.split("-")

        val version1 = parts1[0].split(".").map { it.toIntOrNull() ?: 0 }
        val version2 = parts2[0].split(".").map { it.toIntOrNull() ?: 0 }

        // Compare version numbers (1.1.0)
        val length = maxOf(version1.size, version2.size)
        for (i in 0 until length) {
            val n1 = version1.getOrElse(i) { 0 }
            val n2 = version2.getOrElse(i) { 0 }
            if (n1 != n2) return n1.compareTo(n2)
        }

        // Versions are identical, compare suffixes (-beta3)
        val suffix1 = parts1.getOrNull(1)
        val suffix2 = parts2.getOrNull(1)

        return when {
            suffix1 == null && suffix2 == null -> 0
            suffix1 == null -> 1 // "1.1.0" > "1.1.0-beta"
            suffix2 == null -> -1 // "1.1.0-beta" < "1.1.0"
            else -> compareSuffixes(suffix1, suffix2)
        }
    }

    private fun compareSuffixes(s1: String, s2: String): Int {
        val r = Regex("(\\D+)(\\d*)")
        val match1 = r.matchEntire(s1)
        val match2 = r.matchEntire(s2)

        if (match1 != null && match2 != null) {
            val name1 = match1.groupValues[1]
            val num1 = match1.groupValues[2].toIntOrNull() ?: 0
            val name2 = match2.groupValues[1]
            val num2 = match2.groupValues[2].toIntOrNull() ?: 0

            if (name1 != name2) return name1.compareTo(name2)
            return num1.compareTo(num2)
        }

        return s1.compareTo(s2)
    }

    fun isNewer(latest: String, current: String): Boolean {
        return compare(latest, current) > 0
    }
}
