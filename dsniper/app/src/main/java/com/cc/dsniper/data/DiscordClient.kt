// com.cc.dsniper.data.DiscordClient
// Target: Discord API v10 — username check + claim
// Evasion: rotated proxies, real browser UA, human-like jitter,
//          randomised super-properties header (base64 encoded JSON Discord uses)
package com.cc.dsniper.data

import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val JSON_MT = "application/json; charset=utf-8".toMediaType()

// Rotates through the proxy list; wraps around.
class ProxyRotator(proxies: List<ProxyEntry>) {
    private val list = proxies.toMutableList()
    private var idx = 0

    fun next(): ProxyEntry? {
        val working = list.filter { it.working != false }
        if (working.isEmpty()) return null
        val entry = working[idx % working.size]
        idx++
        return entry
    }

    fun markDead(p: ProxyEntry) {
        val i = list.indexOfFirst { it.host == p.host && it.port == p.port }
        if (i >= 0) list[i] = list[i].copy(working = false, latencyMs = -2)
    }
}

object DiscordClient {

    // Fake super-properties Discord client sends as encoded JSON.
    // Varies per call to avoid fingerprinting.
    private fun superProperties(): String {
        val builds = listOf(281809, 281819, 281826, 281831, 281840)
        val sysLocales = listOf("en-US", "en-GB", "pt-BR", "fr-FR", "de-DE")
        val obj = JSONObject().apply {
            put("os", "Windows")
            put("browser", "Chrome")
            put("device", "")
            put("system_locale", sysLocales.random())
            put("browser_user_agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/${Random.nextInt(119,124)}.0.0.0 Safari/537.36")
            put("browser_version", "${Random.nextInt(119,124)}.0.0.0")
            put("os_version", "10")
            put("referrer", "")
            put("referring_domain", "")
            put("referrer_current", "https://discord.com/")
            put("referring_domain_current", "discord.com")
            put("release_channel", "stable")
            put("client_build_number", builds.random())
            put("client_event_source", JSONObject.NULL)
        }
        return Base64.encodeToString(obj.toString().toByteArray(), Base64.NO_WRAP)
    }

    private fun buildClient(proxy: ProxyEntry?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false)

        if (proxy != null) {
            val javaProxy = Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress(proxy.host, proxy.port)
            )
            builder.proxy(javaProxy)
            if (proxy.user.isNotEmpty()) {
                builder.proxyAuthenticator { _, response ->
                    val cred = Credentials.basic(proxy.user, proxy.pass)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", cred)
                        .build()
                }
            }
        }
        return builder.build()
    }

    // Check if username is available.
    // Returns: true = available, false = taken, null = rate-limited / error
    suspend fun checkUsername(
        username: String,
        token: String,
        proxy: ProxyEntry?
    ): Boolean? {
        val client = buildClient(proxy)
        val sp = superProperties()
        val request = Request.Builder()
            .url("https://discord.com/api/v10/unique-username/username-attempt-unauthed")
            .post(
                JSONObject().apply {
                    put("username", username)
                }.toString().toRequestBody(JSON_MT)
            )
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("X-Super-Properties", sp)
            .header("X-Discord-Locale", "en-US")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://discord.com")
            .header("Referer", "https://discord.com/channels/@me")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .build()

        return try {
            val resp = client.newCall(request).execute()
            when (resp.code) {
                200 -> {
                    val body = resp.body?.string() ?: return null
                    val json = JSONObject(body)
                    // taken = false means available
                    !json.optBoolean("taken", true)
                }
                429 -> null   // rate limit — caller backs off
                401 -> null   // bad token
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Claim (PATCH /users/@me) — only works if username is available.
    // Returns: true = claimed, false = failed
    suspend fun claimUsername(
        username: String,
        token: String,
        proxy: ProxyEntry?
    ): Boolean {
        val client = buildClient(proxy)
        val sp = superProperties()
        val request = Request.Builder()
            .url("https://discord.com/api/v10/users/@me")
            .patch(
                JSONObject().apply {
                    put("username", username)
                }.toString().toRequestBody(JSON_MT)
            )
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("X-Super-Properties", sp)
            .header("X-Discord-Locale", "en-US")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://discord.com")
            .header("Referer", "https://discord.com/channels/@me")
            .build()

        return try {
            val resp = client.newCall(request).execute()
            resp.code == 200
        } catch (e: Exception) {
            false
        }
    }

    // Test token validity — GET /users/@me
    suspend fun testToken(token: String, proxy: ProxyEntry?): Pair<Boolean, String> {
        val client = buildClient(proxy)
        val request = Request.Builder()
            .url("https://discord.com/api/v10/users/@me")
            .get()
            .header("Authorization", token)
            .header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("X-Super-Properties", superProperties())
            .build()

        return try {
            val resp = client.newCall(request).execute()
            if (resp.code == 200) {
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val user = json.optString("username", "?")
                val disc = json.optString("discriminator", "0")
                true to "$user#$disc"
            } else {
                false to "HTTP ${resp.code}"
            }
        } catch (e: Exception) {
            false to e.message.orEmpty()
        }
    }

    // Test proxy latency — hits Discord's CDN
    suspend fun testProxy(proxy: ProxyEntry): ProxyEntry {
        val client = buildClient(proxy)
        val t0 = System.currentTimeMillis()
        return try {
            val resp = client.newCall(
                Request.Builder()
                    .url("https://discord.com/api/v10/gateway")
                    .get()
                    .build()
            ).execute()
            val latency = System.currentTimeMillis() - t0
            proxy.copy(
                working = resp.code == 200,
                latencyMs = if (resp.code == 200) latency else -2
            )
        } catch (e: Exception) {
            proxy.copy(working = false, latencyMs = -2)
        }
    }
}
