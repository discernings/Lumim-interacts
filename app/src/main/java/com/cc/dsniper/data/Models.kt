// com.cc.dsniper.data.Models — data layer, Kotlin 1.9, Android 11+
package com.cc.dsniper.data

data class ProxyEntry(
    val host: String,
    val port: Int,
    val user: String = "",
    val pass: String = "",
    var latencyMs: Long = -1L,  // -1 = untested, -2 = dead
    var working: Boolean? = null
) {
    // "host:port" or "host:port:user:pass"
    override fun toString(): String = if (user.isEmpty()) "$host:$port"
    else "$host:$port:$user:$pass"

    companion object {
        fun parse(raw: String): ProxyEntry? {
            val parts = raw.trim().split(":")
            return when (parts.size) {
                2    -> ProxyEntry(parts[0], parts[1].toIntOrNull() ?: return null)
                4    -> ProxyEntry(parts[0], parts[1].toIntOrNull() ?: return null, parts[2], parts[3])
                else -> null
            }
        }
    }
}

data class SniperConfig(
    val digits: Int = 4,          // username length
    val onlyNumbers: Boolean = false,
    val onlyLetters: Boolean = false,
    val alphanumeric: Boolean = true,
    val speedMs: Long = 500L,     // base delay between attempts
    val jitterMs: Long = 200L,    // random extra delay (evasion)
    val maxConcurrent: Int = 3,   // parallel coroutines
    val stopOnFirst: Boolean = true
)

sealed class SniperState {
    object Idle : SniperState()
    data class Running(val checked: Int, val found: List<String>) : SniperState()
    data class Found(val username: String) : SniperState()
    data class Error(val msg: String) : SniperState()
    object Stopped : SniperState()
}

data class LogEntry(val timestamp: Long = System.currentTimeMillis(), val msg: String)
