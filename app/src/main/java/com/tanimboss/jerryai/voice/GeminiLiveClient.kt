// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import com.google.gson.JsonParser

/** Fixed provider destination. No logging interceptor, redirects, URLs containing keys, or tool calls. */
class GeminiLiveClient internal constructor(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build(),
    private val endpoint: String = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent",
) : LiveConnector {
    override fun connect(key: String, config: VoiceConfig, resume: String?, onEvent: (LiveEvent) -> Unit): LiveConnection {
        require(key.isNotBlank() && config.valid())
        val request = Request.Builder().url(endpoint).header("x-goog-api-key", key).build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!webSocket.send(LiveProtocol.setup(config, resume))) onEvent(LiveEvent.Failure(true, "Could not send Gemini setup."))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try { LiveProtocol.decode(text).forEach(onEvent) }
                catch (_: RuntimeException) { onEvent(LiveEvent.Failure(false, "Unsupported or malformed Gemini response. Audio stopped; check model compatibility.")) }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { onMessage(webSocket, bytes.utf8()) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onEvent(LiveEvent.Failure(code !in listOf(1008, 1003), "Gemini connection closed (code $code)."))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code
                val retry = code == null || code == 429 || code >= 500
                onEvent(LiveEvent.Failure(retry, when(code) {
                    400,404 -> "Gemini model, Live configuration or resume handle was rejected. Restart a fresh session or check the model ID and voice."
                    401,403 -> "Gemini authentication or access denied. Check your key, project and model permissions."
                    429 -> "Gemini quota/rate limit reached. Check your provider account; free usage is not guaranteed."
                    else -> "Gemini network connection failed. Check connectivity and provider availability."
                }))
            }
        })
        return object : LiveConnection {
            private fun send(text: String): Boolean = socket.queueSize() < 128_000 && socket.send(text)
            override fun audio(pcm: ShortArray) = send(LiveProtocol.audio(pcm))
            override fun activity(start: Boolean) = send(LiveProtocol.activity(start))
            override fun audioEnd() = send(LiveProtocol.audioEnd())
            override fun close() { socket.cancel() }
        }
    }
}

/** Model discovery is an explicit user action; no account or model availability is assumed. */
class GeminiModels(private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()) {
    fun list(key: String): List<String> {
        require(key.isNotBlank())
        val names = mutableListOf<String>()
        var token: String? = null
        repeat(5) {
            val url = HttpUrl.Builder().scheme("https").host("generativelanguage.googleapis.com").addPathSegments("v1beta/models")
                .addQueryParameter("pageSize", "1000").apply { token?.let { addQueryParameter("pageToken", it) } }.build()
            client.newCall(Request.Builder().url(url).header("x-goog-api-key", key).build()).execute().use { response ->
                check(response.isSuccessful) { "Model discovery was denied or unavailable. Check key, network, access and quota." }
                val body = response.body.string()
                require(body.length < 2_000_000)
                val json = JsonParser.parseString(body).asJsonObject
                json.getAsJsonArray("models")?.forEach { item ->
                    val model = item.asJsonObject
                    if (model.getAsJsonArray("supportedGenerationMethods")?.any { m -> m.asString.equals("bidiGenerateContent", true) } == true) {
                        model.get("name")?.asString?.takeIf { VoiceConfig(model=it).valid() }?.let(names::add)
                    }
                }
                token = json.get("nextPageToken")?.asString?.takeIf { it.isNotBlank() }
            }
            if (token == null) return names.distinct().sorted()
        }
        return names.distinct().sorted()
    }
}
