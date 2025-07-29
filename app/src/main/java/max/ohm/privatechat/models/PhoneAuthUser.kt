package max.ohm.privatechat.models

class PhoneAuthUser(
    val userId: String= "",
    val phoneNumber:String= "",
    val name:String= "",
    val status : String= "",
    val profileImage: String? = null,  // it can be string or null
    val about: String = "Hey there! I am using WhatsApp."
)
