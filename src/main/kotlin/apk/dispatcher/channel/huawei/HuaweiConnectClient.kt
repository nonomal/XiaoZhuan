package apk.dispatcher.channel.huawei

import apk.dispatcher.util.ProgressChange
import apk.dispatcher.util.defaultLogger
import apk.dispatcher.util.getApkInfo
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HuaweiConnectClient {

    private val connectApi = HuaweiConnectApi()

    /**
     * @param file apk文件
     * @param clientId 接口参数
     * @param clientSecret 接口参数
     * @param updateDesc 更新描述
     * @param progressChange 上传进度回调
     */
    @Throws
    suspend fun uploadApk(
        file: File,
        clientId: String,
        clientSecret: String,
        updateDesc: String,
        progressChange: ProgressChange
    ) {
        val apkInfo = getApkInfo(file)
        val rawToken = getToken(clientId, clientSecret)
        val token = "Bearer $rawToken"
        val appId = getAppId(clientId, token, apkInfo.applicationId)
        val uploadUrl = getUploadUrl(clientId, token, appId, file)
        uploadFile(file, uploadUrl, progressChange)
        val bindResult = bindApk(clientId, token, appId, file, uploadUrl)
        waitApkReady(clientId, token, appId, bindResult)
        modifyUpdateDesc(clientId, token, appId, updateDesc)
        submit(clientId, token, appId)
    }


    /**
     * 获取token
     */
    private suspend fun getToken(clientId: String, clientSecret: String): String {
        defaultLogger.info("获取token")
        val result = connectApi.getToken(HWTokenParams(clientId, clientSecret))
        result.result?.throwOnFail()
        return checkNotNull(result.token)
    }

    /**
     * 获取AppId
     */
    private suspend fun getAppId(clientId: String, token: String, applicationId: String): String {
        defaultLogger.info("获取AppId")
        val result = connectApi.getAppId(clientId, token, applicationId)
        result.result.throwOnFail()
        val appIds = result.list ?: emptyList()
        check(appIds.isNotEmpty())
        return appIds.first().id
    }

    /**
     * 获取Apk上传地址
     */
    private suspend fun getUploadUrl(
        clientId: String,
        token: String,
        appId: String,
        file: File,
    ): HWUploadUrlResp.UploadUrl {
        defaultLogger.info("获取Apk上传地址")
        val result = connectApi.getUploadUrl(clientId, token, appId, file.name, file.length())
        result.result.throwOnFail()
        return checkNotNull(result.url)
    }

    /**
     * 上传文件
     */
    private suspend fun uploadFile(
        file: File,
        url: HWUploadUrlResp.UploadUrl,
        progressChange: ProgressChange
    ) {
        defaultLogger.info("上传Apk文件")
        connectApi.uploadFile(file, url, progressChange)
    }

    /**
     * 刷新Apk文件
     */
    private suspend fun bindApk(
        clientId: String,
        token: String,
        appId: String,
        file: File,
        url: HWUploadUrlResp.UploadUrl,
    ): HWBindFileResp {
        defaultLogger.info("绑定Apk文件")
        val fileInfo = HWRefreshApk.FileInfo(file.name, url.objectId)
        val params = HWRefreshApk(files = listOf(fileInfo))
        val result = connectApi.bindApkFile(clientId, token, appId, params)
        result.result.throwOnFail()
        return result
    }

    /**
     * 等待Apk编译完成
     */
    private suspend fun waitApkReady(
        clientId: String,
        token: String,
        appId: String,
        bindFileResult: HWBindFileResp,
    ) {
        defaultLogger.info("等待Apk编译完成")
        val startTime = System.currentTimeMillis()
        while (true) {
            delay(10.seconds)
            if (System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(3)) {
                throw TimeoutException("检测apk状态超时")
            }
            val result = connectApi.getApkCompileState(clientId, token, appId, bindFileResult.pkgId)
            result.result.throwOnFail()
            if (result.pkgStateList.first().isSuccess()) {
                break
            }
        }
        defaultLogger.info("Apk编译完成")
    }

    /**
     * 修改新版本更新描述
     */
    private suspend fun modifyUpdateDesc(
        clientId: String,
        token: String,
        appId: String,
        updateDesc: String
    ) {
        defaultLogger.info("修改新版本更新描述")
        val desc = HWVersionDesc(updateDesc)
        val result = connectApi.updateVersionDesc(clientId, token, appId, desc)
        result.result.throwOnFail()
    }

    /**
     * 提交审核
     */
    private suspend fun submit(
        clientId: String,
        token: String,
        appId: String,
    ) {
        defaultLogger.info("提交审核")
        val result = connectApi.submit(clientId, token, appId)
        result.result.throwOnFail()
    }

}