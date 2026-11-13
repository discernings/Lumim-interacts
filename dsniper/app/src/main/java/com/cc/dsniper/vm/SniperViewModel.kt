// com.cc.dsniper.vm.SniperViewModel
// Coroutine-based brute-force loop, proxy rotation, jitter
package com.cc.dsniper.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cc.dsniper.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class SniperViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _state = MutableStateFlow<SniperState>(SniperState.Idle)
    val state = _state.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log = _log.asStateFlow()

    private var sniperJob: Job? = null

    fun start() {
        if (sniperJob?.isActive == true) return
        val token = prefs.token
        if (token.isEmpty()) {
            _state.value = SniperState.Error("Token não configurado")
            return
        }
        val config = prefs.sniperConfig
        val proxies = prefs.proxies.filter { it.working != false }
        val rotator = ProxyRotator(proxies)

        sniperJob = viewModelScope.launch(Dispatchers.IO) {
            _state.value = SniperState.Running(0, emptyList())
            addLog("▶ Iniciando — ${config.digits} chars, speed ${config.speedMs}ms, " +
                   "jitter ${config.jitterMs}ms, concurrent ${config.maxConcurrent}")

            val found = mutableListOf<String>()
            val gen = UsernameGen.generate(config)
            var checked = 0
            val sem = kotlinx.coroutines.sync.Semaphore(config.maxConcurrent)

            val jobs = mutableListOf<Job>()

            for (username in gen) {
                if (!isActive) break

                val proxy = rotator.next()
                val u = username
                val j = launch {
                    sem.acquire()
                    try {
                        val available = DiscordClient.checkUsername(u, token, proxy)
                        when (available) {
                            true -> {
                                addLog("✓ DISPONÍVEL: $u — tentando claim...")
                                val claimed = DiscordClient.claimUsername(u, token, proxy)
                                if (claimed) {
                                    addLog("🎯 CLAIMED: $u")
                                    found.add(u)
                                    _state.value = SniperState.Found(u)
                                    if (config.stopOnFirst) {
                                        this@launch.coroutineContext.job.parent?.cancel()
                                        return@launch
                                    }
                                } else {
                                    addLog("✗ Claim falhou: $u")
                                }
                            }
                            false -> { /* taken — normal, silent */ }
                            null  -> {
                                // rate-limit or error — back off
                                addLog("⚠ Rate-limit/erro em $u — aguardando...")
                                delay(2000 + Random.nextLong(1000))
                            }
                        }
                        // Human-like delay
                        delay(config.speedMs + Random.nextLong(config.jitterMs))
                    } finally {
                        sem.release()
                    }
                }
                jobs.add(j)

                checked++
                if (checked % 50 == 0) {
                    _state.value = SniperState.Running(checked, found.toList())
                    addLog("... $checked verificados")
                }
            }

            jobs.joinAll()

            if (found.isEmpty()) {
                _state.value = SniperState.Stopped
                addLog("■ Concluído — nenhum encontrado")
            }
        }
    }

    fun stop() {
        sniperJob?.cancel()
        _state.value = SniperState.Stopped
        addLog("■ Parado pelo usuário")
    }

    fun clearLog() {
        _log.value = emptyList()
    }

    private fun addLog(msg: String) {
        val current = _log.value.takeLast(499).toMutableList()
        current.add(LogEntry(msg = msg))
        _log.value = current
    }
}
