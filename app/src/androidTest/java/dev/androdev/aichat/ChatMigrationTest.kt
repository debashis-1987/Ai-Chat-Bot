package dev.androdev.aichat

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.androdev.aichat.data.ChatDatabase
import dev.androdev.aichat.data.MIGRATION_1_2
import dev.androdev.aichat.data.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ChatDatabase::class.java,
    )

    @Test
    fun migrationAddsPinnedColumnAndKeepsRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE conversations (" +
                                    "id TEXT NOT NULL PRIMARY KEY, " +
                                    "title TEXT NOT NULL, " +
                                    "createdAt INTEGER NOT NULL, " +
                                    "updatedAt INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE messages (" +
                                    "id TEXT NOT NULL PRIMARY KEY, " +
                                    "conversationId TEXT NOT NULL, " +
                                    "role TEXT NOT NULL, " +
                                    "content TEXT NOT NULL, " +
                                    "position INTEGER NOT NULL, " +
                                    "timestamp INTEGER NOT NULL)",
                            )
                            db.execSQL("CREATE INDEX index_messages_conversationId ON messages(conversationId)")
                            db.execSQL(
                                "INSERT INTO conversations (id, title, createdAt, updatedAt) " +
                                    "VALUES ('c1', 'Existing chat', 1, 1)",
                            )
                            db.execSQL(
                                "INSERT INTO messages (id, conversationId, role, content, position, timestamp) " +
                                    "VALUES ('m1', 'c1', 'user', 'hello', 1, 1)",
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )

        val db = helper.writableDatabase
        MIGRATION_1_2.migrate(db)

        db.query("SELECT title, pinned FROM conversations WHERE id = 'c1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing chat", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM messages").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        helper.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun secondMigrationAddsReasoningColumnsAndKeepsRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME_V2)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME_V2)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE conversations (" +
                                    "id TEXT NOT NULL PRIMARY KEY, " +
                                    "title TEXT NOT NULL, " +
                                    "pinned INTEGER NOT NULL DEFAULT 0, " +
                                    "createdAt INTEGER NOT NULL, " +
                                    "updatedAt INTEGER NOT NULL)",
                            )
                            db.execSQL(
                                "CREATE TABLE messages (" +
                                    "id TEXT NOT NULL PRIMARY KEY, " +
                                    "conversationId TEXT NOT NULL, " +
                                    "role TEXT NOT NULL, " +
                                    "content TEXT NOT NULL, " +
                                    "position INTEGER NOT NULL, " +
                                    "timestamp INTEGER NOT NULL)",
                            )
                            db.execSQL("CREATE INDEX index_messages_conversationId ON messages(conversationId)")
                            db.execSQL(
                                "INSERT INTO conversations (id, title, pinned, createdAt, updatedAt) " +
                                    "VALUES ('c1', 'Existing chat', 1, 1, 1)",
                            )
                            db.execSQL(
                                "INSERT INTO messages (id, conversationId, role, content, position, timestamp) " +
                                    "VALUES ('m1', 'c1', 'assistant', 'hello', 1, 1)",
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )

        val db = helper.writableDatabase
        MIGRATION_2_3.migrate(db)

        db.query("SELECT reasoning, reasoningMs FROM messages WHERE id = 'm1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM conversations WHERE pinned = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        helper.close()
        context.deleteDatabase(DB_NAME_V2)
    }

    @Test
    fun roomValidatesMigrationFromV2() {
        helper.createDatabase(ROOM_DB, 2).apply {
            execSQL(
                "INSERT INTO conversations (id, title, pinned, createdAt, updatedAt) " +
                    "VALUES ('c1', 'Existing chat', 1, 1, 1)",
            )
            execSQL(
                "INSERT INTO messages (id, conversationId, role, content, position, timestamp) " +
                    "VALUES ('m1', 'c1', 'assistant', 'hello', 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(ROOM_DB, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT reasoning, reasoningMs FROM messages WHERE id = 'm1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
            db.query("SELECT pinned FROM conversations WHERE id = 'c1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val DB_NAME_V2 = "migration-test-v2.db"
        const val ROOM_DB = "room-migration-test.db"
    }
}
