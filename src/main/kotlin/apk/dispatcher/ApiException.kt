package apk.dispatcher

class ApiException(
    val code: Int,
    override val message: String, exception: Exception? = null
) : RuntimeException("code:${code},msg:$message", exception)