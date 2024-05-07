package apk.dispatcher.huawei

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = false)
data class AppIdResult(
    @Json(name = "ret")
    val result: Result,
    @Suppress("SpellCheckingInspection")
    @Json(name = "appids")
    val list: List<AppId>?
) {
    @JsonClass(generateAdapter = false)
    data class AppId(
        @Json(name = "key")
        val name: String,
        @Json(name = "value")
        val id: String,
    )
}