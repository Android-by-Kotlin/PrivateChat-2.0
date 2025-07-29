package max.ohm.privatechat.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration2to3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Clear profile pictures that are too large
        database.execSQL("UPDATE chats SET profilePicture = NULL WHERE LENGTH(profilePicture) > 500000")
        
        // Clear any messages that might have large content
        database.execSQL("DELETE FROM messages WHERE LENGTH(message) > 100000")
    }
}
