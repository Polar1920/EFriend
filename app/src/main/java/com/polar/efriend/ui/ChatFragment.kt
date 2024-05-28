package com.polar.efriend

import ChatViewModel
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.polar.efriend.data.Message
import com.polar.efriend.utils.Constants.SEND_ID
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var messagingAdapter: MessagingAdapter
    private lateinit var etMessage: EditText
    private lateinit var btnVoiceNote: ImageButton
    private lateinit var speechRecognizer: SpeechRecognizer
    private var recordingDialog: AlertDialog? = null
    private var voiceMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        messagingAdapter = MessagingAdapter()
        val rvMessages = view.findViewById<RecyclerView>(R.id.rv_messages)
        rvMessages.adapter = messagingAdapter
        rvMessages.layoutManager = LinearLayoutManager(activity)

        etMessage = view.findViewById(R.id.et_message)
        btnVoiceNote = view.findViewById(R.id.btn_voice_note)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                showRecordingDialog()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                hideRecordingDialog()
            }
            override fun onError(error: Int) {
                hideRecordingDialog()
            }
            override fun onResults(results: Bundle?) {
                hideRecordingDialog()
                results?.let {
                    val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let { resultList ->
                        if (resultList.isNotEmpty()) {
                            voiceMessage = resultList[0]
                        }
                    }
                }
                val message = Message("Audio enviado", SEND_ID, "00:00")
                messagingAdapter.insertMessage(message)
                voiceMessage?.let { messagingAdapter.sendToApiChat(it, requireContext()) }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        btnVoiceNote.setOnClickListener {
            startSpeechToText()
        }

        val btnSend = view.findViewById<Button>(R.id.btn_send)
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString()
            val message = Message(messageText, SEND_ID, messagingAdapter.getCurrentTime())
            messagingAdapter.insertMessage(message)
            etMessage.text.clear()
            messagingAdapter.sendToApiChat(messageText, requireContext())
        }

        requestPermissions()

        return view
    }

    private fun showRecordingDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage("Hable ahora...")
        builder.setCancelable(false)
        recordingDialog = builder.create()
        recordingDialog?.show()
    }

    private fun hideRecordingDialog() {
        recordingDialog?.dismiss()
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
        speechRecognizer.startListening(intent)
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
        messagingAdapter.mediaPlayer?.release()
        messagingAdapter.mediaPlayer = null
    }
}

