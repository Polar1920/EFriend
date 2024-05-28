import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.polar.efriend.data.Message

class ChatViewModel : ViewModel() {
    val messages: MutableLiveData<List<Message>> = MutableLiveData()

    init {
        messages.value = arrayListOf()
    }

    fun addMessage(message: Message) {
        val updatedMessages = messages.value?.toMutableList() ?: mutableListOf()
        updatedMessages.add(message)
        messages.value = updatedMessages
    }
}
