package chat.rocket.android.chatrooms.presentation

import chat.rocket.android.core.behaviours.LoadingView
import chat.rocket.android.core.behaviours.MessageView
import chat.rocket.core.model.MessageType

interface ChatRoomsView : LoadingView, MessageView {
    fun showLoadingRoom(name: CharSequence)
    fun navToConference(chatRoomId: String, chatRoomType: String, messageType: MessageType)
    fun hideLoadingRoom()
}