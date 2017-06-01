/*
 * MIT License
 *
 * Copyright (c) 2017 Mega Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shollmann.android.megachess.wsclient

import com.google.gson.Gson
import com.shollmann.android.megachess.wsclient.model.GeneratedMove
import com.shollmann.android.megachess.wsclient.model.payload.*
import okhttp3.*
import okio.ByteString
import java.util.*
import java.util.concurrent.TimeUnit

class WebSocketClient : WebSocketListener() {
    val URL = "wss://mega-chess.herokuapp.com/service"
    val TOKEN = "2d6574e6-ec31-4ee2-a69f-e5ea33e866bb"
    val gson = Gson()
    var client = OkHttpClient()
    var webSocket: WebSocket? = null

    fun run() {
        client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .build()

        val request = Request.Builder()
                .url(URL)
                .build()

        client.newWebSocket(request, this)
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown()
    }

    override fun onOpen(aWebSocket: WebSocket?, response: Response?) {
        val connect = Connect(TOKEN)
        webSocket = aWebSocket
        webSocket!!.send(gson.toJson(connect))
    }

    override fun onMessage(webSocket: WebSocket?, rawPayload: String?) {
        println("MESSAGE: " + rawPayload!!)

        if (rawPayload.contains("connect_ok")) {
            println("yoohoo! connect ok!")
        } else if (rawPayload.contains("ask_challenge")) {
            val askChallenge = gson.fromJson(rawPayload, AskChallenge::class.java)
            println("yoohoo! challenge someone asking for a challenge!")
            webSocket?.send(gson.toJson(AcceptChallenge(askChallenge.data.boardId)))
        } else if (rawPayload.contains("your_turn")) {
            val yourTurn = gson.fromJson(rawPayload, YourTurn::class.java)
            val generatedMove = generateMove()
            webSocket?.send(gson.toJson(Move(
                    yourTurn.data.boardId,
                    yourTurn.data.turnToken,
                    generatedMove.fromRow, generatedMove.fromCol, generatedMove.toRow, generatedMove.toCol)
            ))
        }
    }

    private fun generateMove(): GeneratedMove {
        val random = Random()
        val fromRow = random.nextInt(16) + 1
        val fromCol = random.nextInt(16) + 1
        val toRow = random.nextInt(16) + 1
        val toCol = random.nextInt(16) + 1

        return GeneratedMove(fromCol, fromRow, toCol, toRow)
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        println("MESSAGE: " + bytes!!.hex())
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        webSocket!!.close(1000, null)
        println("CLOSE: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        t!!.printStackTrace()
    }

    companion object {
        @JvmStatic fun instanceRun() {
            WebSocketClient().run()
        }
    }
}