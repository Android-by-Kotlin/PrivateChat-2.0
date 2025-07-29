package max.ohm.privatechat.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import max.ohm.privatechat.data.dao.ChatDao
import max.ohm.privatechat.data.dao.MessageDao
import max.ohm.privatechat.data.entities.ChatEntity
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.models.MessageStatusConverter

@Database(
    entities = [ChatEntity::class, Message::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(MessageStatusConverter::class)
abstract class PrivateChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
