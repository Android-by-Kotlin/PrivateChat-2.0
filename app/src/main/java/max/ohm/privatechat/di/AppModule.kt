package max.ohm.privatechat.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import max.ohm.privatechat.data.dao.ChatDao
import max.ohm.privatechat.data.dao.MessageDao
import max.ohm.privatechat.data.database.PrivateChatDatabase
import max.ohm.privatechat.data.database.Migration2to3
import max.ohm.privatechat.data.database.Migration3to4
import max.ohm.privatechat.data.repository.ChatRepository
import javax.inject.Singleton


// these two funnction provide firebase instance

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    // funnction for Authentication
    fun provideFirebaseAuth(): FirebaseAuth{

        return FirebaseAuth.getInstance()
    }

    // funnction for database

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase{
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun providePrivateChatDatabase(
        @ApplicationContext context: Context
    ): PrivateChatDatabase {
        return Room.databaseBuilder(
            context,
            PrivateChatDatabase::class.java,
            "privatechat_database"
        )
        .addMigrations(Migration2to3(), Migration3to4())
        .fallbackToDestructiveMigration() // Fallback for other migrations
        .build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: PrivateChatDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: PrivateChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        chatDao: ChatDao,
        messageDao: MessageDao
    ): ChatRepository {
        return ChatRepository(chatDao, messageDao)
    }
}
