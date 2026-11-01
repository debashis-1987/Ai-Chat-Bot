package dev.androdev.aichat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.androdev.aichat.data.ChatDatabase
import dev.androdev.aichat.data.ChatRepository
import dev.androdev.aichat.data.Message
import dev.androdev.aichat.data.Role
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {

    private lateinit var database: ChatDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChatDatabase::class.java).build()
        repository = ChatRepository(database.chatDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun storesMessagesInPositionOrder() = runBlocking {
        repository.createConversation("c1", "Chat")
        repository.saveMessage("c1", Message(id = "m1", role = Role.USER, content = "hi", position = 1))
        repository.saveMessage(
            "c1",
            Message(
                id = "m2",
                role = Role.ASSISTANT,
                content = "hello",
                position = 2,
                reasoning = "plan",
                reasoningMs = 1250L,
            ),
        )

        val messages = repository.messages("c1")

        assertEquals(listOf("m1", "m2"), messages.map { it.id })
        assertEquals(listOf(Role.USER, Role.ASSISTANT), messages.map { it.role })
        assertEquals(listOf("hi", "hello"), messages.map { it.content })
        assertEquals("plan", messages[1].reasoning)
        assertEquals(1250L, messages[1].reasoningMs)
    }

    @Test
    fun deleteRemovesConversationAndSupportsRestore() = runBlocking {
        repository.createConversation("c1", "Keep me")
        repository.saveMessage("c1", Message(id = "m1", role = Role.USER, content = "hi", position = 1))

        val conversation = repository.conversation("c1")
        val messages = repository.messages("c1")
        repository.deleteConversation("c1")

        assertNull(repository.conversation("c1"))
        assertEquals(emptyList<Message>(), repository.messages("c1"))

        assertNotNull(conversation)
        repository.restoreConversation(conversation!!, messages)

        assertEquals("Keep me", repository.conversation("c1")?.title)
        assertEquals(listOf("m1"), repository.messages("c1").map { it.id })
    }

    @Test
    fun renameAndPinPersist() = runBlocking {
        repository.createConversation("c1", "Old title")
        repository.renameConversation("c1", "New title")
        repository.setPinned("c1", true)

        val conversation = repository.conversation("c1")

        assertEquals("New title", conversation?.title)
        assertEquals(true, conversation?.pinned)
    }
}
