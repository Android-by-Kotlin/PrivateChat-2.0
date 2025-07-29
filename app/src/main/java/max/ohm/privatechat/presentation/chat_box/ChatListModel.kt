package max.ohm.privatechat.presentation.chat_box

data class ChatListModel(
    val name: String? = null,
    val phoneNumber: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val profilePicture: String? = null,
    val isOnline: Boolean = false,
    val status: String? = null,
    val unreadCount: Int = 0,
    // Legacy fields for compatibility
    val image: Int? = null,
    val userId: String? = null,
    val time: String? = null,
    val message: String? = null,
    val profileImage: String? = null
) {
    constructor() : this(
        null, null, null, System.currentTimeMillis(), 
        null, false, null, 0, null, null, null, null, null
    )
}
