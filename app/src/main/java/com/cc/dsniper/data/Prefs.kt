// com.cc.dsniper.data.Prefs — SharedPreferences wrapper
package com.cc.dsniper.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ds_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v).apply()

    var proxies: List<ProxyEntry>
        get() {
            val json = sp.getString("proxies", "[]") ?: "[]"
            val type = object : TypeToken<List<ProxyEntry>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
        set(v) = sp.edit().putString("proxies", gson.toJson(v)).apply()

    var sniperConfig: SniperConfig
        get() {
            val json = sp.getString("sniper_cfg", null)
            return if (json == null) SniperConfig()
            else gson.fromJson(json, SniperConfig::class.java)
        }
        set(v) = sp.edit().putString("sniper_cfg", gson.toJson(v)).apply()
}
