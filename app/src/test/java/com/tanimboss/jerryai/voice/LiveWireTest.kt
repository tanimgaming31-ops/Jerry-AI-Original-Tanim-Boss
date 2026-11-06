// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.voice

import mockwebserver3.MockWebServer
import mockwebserver3.MockResponse
import okhttp3.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class LiveWireTest {
    @Test fun actualWebSocketHandshakeSetupPcmAndInterruptionRoundTrip() {
        MockWebServer().use { server ->
            val received=LinkedBlockingQueue<String>();val events=LinkedBlockingQueue<LiveEvent>()
            lateinit var peer:WebSocket
            server.enqueue(MockResponse.Builder().webSocketUpgrade(object:WebSocketListener(){
                override fun onOpen(webSocket:WebSocket,response:Response){peer=webSocket}
                override fun onMessage(webSocket:WebSocket,text:String){received.offer(text);if(text.contains("\"setup\""))webSocket.send("{\"setupComplete\":{}}")}
            }).build())
            server.start()
            val client=OkHttpClient.Builder().readTimeout(0,TimeUnit.SECONDS).build()
            val connection=GeminiLiveClient(client,server.url("/live").toString()).connect("test-only-key",VoiceConfig("test-model"),null){events.offer(it)}
            try {
                assertTrue(received.poll(5,TimeUnit.SECONDS).contains("models/test-model"))
                assertEquals(LiveEvent.Ready,events.poll(5,TimeUnit.SECONDS))
                val request=server.takeRequest()
                assertEquals("test-only-key",request.headers["x-goog-api-key"]);assertFalse(request.url.toString().contains("test-only-key"))
                assertTrue(connection.activity(true));assertTrue(connection.audio(shortArrayOf(1,2)))
                assertTrue(received.poll(5,TimeUnit.SECONDS).contains("activityStart"))
                val frame=com.google.gson.JsonParser.parseString(received.poll(5,TimeUnit.SECONDS)).asJsonObject.getAsJsonObject("realtimeInput").getAsJsonObject("audio")
                assertEquals("audio/pcm;rate=16000",frame.get("mimeType").asString)
                assertArrayEquals(byteArrayOf(1,0,2,0),java.util.Base64.getDecoder().decode(frame.get("data").asString))
                peer.send("{\"serverContent\":{\"interrupted\":true}}")
                assertEquals(LiveEvent.Interrupted,events.poll(5,TimeUnit.SECONDS))
            }finally{connection.close();client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
        }
    }
    @Test fun deniedHandshakeIsSanitizedAndNotRetryable() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().code(403).body("private-provider-response").build());server.start()
            val events=LinkedBlockingQueue<LiveEvent>()
            val client=OkHttpClient()
            val connection=GeminiLiveClient(client,server.url("/live").toString()).connect("test-only-key",VoiceConfig("test-model"),null){events.offer(it)}
            try {
                val failure=events.poll(5,TimeUnit.SECONDS) as LiveEvent.Failure
                assertFalse(failure.retryable);assertFalse(failure.detail.contains("private-provider-response"));assertFalse(failure.detail.contains("test-only-key"))
            }finally{connection.close();client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
        }
    }
}
