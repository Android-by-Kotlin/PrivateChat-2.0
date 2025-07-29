package max.ohm.privatechat.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration3to4 : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Clear ALL profile pictures to fix the blob too big issue
        database.execSQL("UPDATE chats SET profilePicture = NULL")
        
        // Clear any messages that might have large content
        database.execSQL("DELETE FROM messages WHERE LENGTH(message) > 50000")
        
        // Update any large lastMessage fields
        database.execSQL("UPDATE chats SET lastMessage = SUBSTR(lastMessage, 1, 1000) WHERE LENGTH(lastMessage) > 1000")
    }
}
