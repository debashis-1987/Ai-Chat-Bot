package dev.androdev.aichat.data

object ServiceLocator {
    @Volatile
    var chatService: ChatService = ChatClient()
}
