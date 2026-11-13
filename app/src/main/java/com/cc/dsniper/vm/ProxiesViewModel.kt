// com.cc.dsniper.vm.ProxiesViewModel
package com.cc.dsniper.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cc.dsniper.data.DiscordClient
import com.cc.dsniper.data.Prefs
import com.cc.dsniper.data.ProxyEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProxiesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _proxies = MutableStateFlow(prefs.proxies)
    val proxies = _proxies.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing = _testing.asStateFlow()

    fun addProxiesFromText(raw: String) {
        val newList = raw.lines()
            .mapNotNull { ProxyEntry.parse(it) }
            .filter { np -> _proxies.value.none { it.host == np.host && it.port == np.port } }
        val updated = (_proxies.value + newList)
        _proxies.value = updated
        prefs.proxies = updated
    }

    fun removeProxy(proxy: ProxyEntry) {
        val updated = _proxies.value.filter { it.host != proxy.host || it.port != proxy.port }
        _proxies.value = updated
        prefs.proxies = updated
    }

    fun clearAll() {
        _proxies.value = emptyList()
        prefs.proxies = emptyList()
    }

    fun testAll() {
        if (_testing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _testing.value = true
            val current = _proxies.value.toList()
            val results = current.map { proxy ->
                async { DiscordClient.testProxy(proxy) }
            }.awaitAll()
            _proxies.value = results
            prefs.proxies = results
            _testing.value = false
        }
    }

    fun testOne(proxy: ProxyEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val tested = DiscordClient.testProxy(proxy)
            val updated = _proxies.value.map {
                if (it.host == proxy.host && it.port == proxy.port) tested else it
            }
            _proxies.value = updated
            prefs.proxies = updated
        }
    }
}
