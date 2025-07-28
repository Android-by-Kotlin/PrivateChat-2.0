package max.ohm.privatechat.presentation.chat_box

data class ChatListModel(

    val name : String? = null,
    val phoneNumber: String? = null,
    val image: Int? = null,
    val userId: String ? = null,
    val time : String ? = null,
    val message : String ? = null,
    val profileImage: String? = null


)
{

    constructor() : this(null, null, null, null, null, null, null)
}
