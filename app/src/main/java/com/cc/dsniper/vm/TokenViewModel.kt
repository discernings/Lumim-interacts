// com.cc.dsniper.vm.TokenViewModel
package com.cc.dsniper.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cc.dsniper.data.DiscordClient
import com.cc.dsniper.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TokenState {
    object Idle : TokenState()
    object Testing : TokenState()
    data class Valid(val username: String) : TokenState()
    data class Invalid(val reason: String) : TokenState()
}

class TokenViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _token = MutableStateFlow(prefs.token)
    val token = _token.asStateFlow()

    private val _state = MutableStateFlow<TokenState>(TokenState.Idle)
    val state = _state.asStateFlow()

    fun saveToken(t: String) {
        prefs.token = t
        _token.value = t
        _state.value = TokenState.Idle
    }

    fun testToken() {
        val t = _token.value
        if (t.isEmpty()) {
            _state.value = TokenState.Invalid("Token vazio")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = TokenState.Testing
            val proxies = prefs.proxies.filter { it.working == true }
            val proxy = proxies.randomOrNull()
            val (ok, info) = DiscordClient.testToken(t, proxy)
            _state.value = if (ok) TokenState.Valid(info) else TokenState.Invalid(info)
        }
    }
}
