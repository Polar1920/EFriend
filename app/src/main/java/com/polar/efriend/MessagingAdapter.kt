package com.polar.efriend

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.polar.efriend.data.Message
import com.polar.efriend.utils.Constants.RECEIVE_ID
import com.polar.efriend.utils.Constants.SEND_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class MessagingAdapter : RecyclerView.Adapter<MessagingAdapter.MessageViewHolder>() {

    var messagesList = mutableListOf<Message>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                messagesList.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    fun insertMessage(message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            messagesList.add(message)
            notifyItemInserted(messagesList.size)
        }
    }

    fun simulateAutoReply() {
        val autoReplyMessage = Message("Respuesta automática", RECEIVE_ID, getCurrentTime())
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(2000)
            insertMessage(autoReplyMessage)
        }
    }

    fun sendToApiChat(message: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val randomNumber = (0..4).random() // Generar un número aleatorio entre 0 y 4
                val url = when (randomNumber) {
                    0 -> "https://efriend-api.onrender.com/predict"
                    1 -> "https://efriend-api-chatv2.onrender.com/predict" // Endpoint de audio
                    2 -> "https://efriend-api-chatv2.onrender.com/predict" // Endpoint de audio
                    3 -> "https://efriend-api-chatv2.onrender.com/predict" // Endpoint de audio
                    else -> "https://efriend-api.onrender.com/predict" // Valor por defecto
                }

                val formBody = FormBody.Builder()
                    .add("message", message)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException?) {
                        call.cancel()
                        handleNetworkError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseType = if (url.contains("efriend-api.onrender.com")) {
                            "chat"
                        } else {
                            "audio"
                        }
                        handleResponse(response, responseType, context)
                    }
                })
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleNetworkError(e)
                }
            }
        }
    }

    private fun handleResponse(response: Response, responseType: String, context: Context) {
        if (response.isSuccessful) {
            when (responseType) {
                "chat" -> handleChatResponse(response)
                "audio" -> handleAudioResponse(response, context)
            }
        } else {
            handleNetworkError(IOException("Error fetching response"))
        }
    }

    private fun handleChatResponse(response: Response) {
        val myResponse: String = response.body()?.string() ?: "error"
        val jsonObject = JSONObject(myResponse)
        var result = jsonObject.getString("result")
        result = result.replace("</s>", "").trim()

        val botMessage = Message(result, RECEIVE_ID, getCurrentTime())
        insertMessage(botMessage)
    }

    private fun handleAudioResponse(response: Response, context: Context) {
        if (response.isSuccessful) {
            val inputStream = response.body()?.byteStream()
            inputStream?.let { stream ->
                try {
                    // Crear un archivo temporal para almacenar el audio
                    val audioFile = File.createTempFile("audio", ".mp3", context.cacheDir)
                    audioFile.deleteOnExit()

                    // Escribir el audio en el archivo
                    FileOutputStream(audioFile).use { fileOutputStream ->
                        stream.copyTo(fileOutputStream)
                    }

                    // Reproducir el audio desde el archivo
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setDataSource(audioFile.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()

                    // Mantener una referencia al MediaPlayer para poder controlarlo más adelante
                    this.mediaPlayer = mediaPlayer
                } catch (e: IOException) {
                    handleNetworkError(e)
                }
            } ?: handleNetworkError(IOException("Error fetching audio"))
        } else {
            handleNetworkError(IOException("Error fetching audio"))
        }
    }

    var mediaPlayer: MediaPlayer? = null

    private fun handleNetworkError(e: Exception?) {
        val errorMessage = Message("Error de red: ${e?.message}", RECEIVE_ID, getCurrentTime())
        insertMessage(errorMessage)
    }

    fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messagesList[position]

        when (currentMessage.id) {
            SEND_ID -> {
                holder.itemView.findViewById<TextView>(R.id.tv_message).apply {
                    text = currentMessage.message
                    visibility = View.VISIBLE
                }
                holder.itemView.findViewById<TextView>(R.id.tv_bot_message).visibility = View.GONE
            }

            RECEIVE_ID -> {
                holder.itemView.findViewById<TextView>(R.id.tv_bot_message).apply {
                    text = currentMessage.message
                    visibility = View.VISIBLE
                }
                holder.itemView.findViewById<TextView>(R.id.tv_message).visibility = View.GONE
            }
        }
    }
}
