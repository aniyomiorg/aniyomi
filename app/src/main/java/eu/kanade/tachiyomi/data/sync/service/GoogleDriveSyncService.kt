package eu.kanade.tachiyomi.data.sync.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GoogleDriveSyncService(context: Context, json: Json, syncPreferences: SyncPreferences) : SyncService(
    context,
    json,
    syncPreferences,
) {
    constructor(context: Context) : this(
        context,
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        },
        Injekt.get<SyncPreferences>(),
    )

    enum class DeleteSyncDataStatus {
        NOT_INITIALIZED,
        NO_FILES,
        SUCCESS,
        ERROR,
    }

    private val appName = context.stringResource(MR.strings.app_name)

    private val remoteFileName = "${appName}_sync.proto.gz"

    private val googleDriveService = GoogleDriveService(context)

    private val protoBuf: ProtoBuf = Injekt.get()

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    override suspend fun doSync(syncData: SyncData): Backup? {
        beforeSync()

        try {
            val remoteSData = pullSyncData()

            if (remoteSData != null) {
                // Get local unique device ID
                val localDeviceId = syncPreferences.uniqueDeviceID()
                val lastSyncDeviceId = remoteSData.deviceId

                // Log the device IDs
                logcat(LogPriority.DEBUG, "SyncService") {
                    "Local device ID: $localDeviceId, Last sync device ID: $lastSyncDeviceId"
                }

                // check if the last sync was done by the same device if so
                // overwrite the remote data with the local data
                return if (lastSyncDeviceId == localDeviceId) {
                    pushSyncData(syncData)
                    syncData.backup
                } else {
                    // Merge the local and remote sync data
                    val mergedSyncData = mergeSyncData(syncData, remoteSData)
                    pushSyncData(mergedSyncData)
                    mergedSyncData.backup
                }
            }

            pushSyncData(syncData)
            return syncData.backup
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, "SyncService") { "Error syncing: ${e.message}" }
            return null
        }
    }

    private suspend fun beforeSync() {
        googleDriveService.refreshToken()
    }

    @Suppress("TooGenericExceptionThrown", "TooGenericExceptionCaught")
    private fun pullSyncData(): SyncData? {
        val drive = googleDriveService.driveService
            ?: throw Exception(context.stringResource(MR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        if (fileList.isEmpty()) {
            logcat(LogPriority.INFO) { "No files found in app data" }
            return null
        }

        val gdriveFileId = fileList[0].id
        logcat(LogPriority.DEBUG) { "Google Drive File ID: $gdriveFileId" }

        try {
            drive.files().get(gdriveFileId).executeMediaAsInputStream().use { inputStream ->
                GZIPInputStream(inputStream).use { gzipInputStream ->
                    val byteArray = gzipInputStream.readBytes()
                    val backup = protoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
                    val deviceId = fileList[0].appProperties["deviceId"] ?: ""
                    return SyncData(deviceId = deviceId, backup = backup)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Error downloading file: ${e.message}" }
            throw Exception("Failed to download sync data: ${e.message}", e)
        }
    }

    @Suppress("TooGenericExceptionThrown")
    private suspend fun pushSyncData(syncData: SyncData) {
        val drive = googleDriveService.driveService
            ?: throw Exception(context.stringResource(MR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        val backup = syncData.backup ?: return

        val byteArray = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        if (byteArray.isEmpty()) {
            throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
        }

        PipedOutputStream().use { pos ->
            PipedInputStream(pos).use { pis ->
                withIOContext {
                    launch {
                        GZIPOutputStream(pos).use { gzipOutputStream ->
                            gzipOutputStream.write(byteArray)
                        }
                    }

                    val mediaContent = InputStreamContent("application/octet-stream", pis)

                    if (fileList.isNotEmpty()) {
                        val fileId = fileList[0].id
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = "application/octet-stream"
                            appProperties = mapOf("deviceId" to syncData.deviceId)
                        }
                        drive.files().update(fileId, fileMetadata, mediaContent).execute()
                        logcat(
                            LogPriority.DEBUG,
                        ) { "Updated existing sync data file in Google Drive with file ID: $fileId" }
                    } else {
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = "application/octet-stream"
                            parents = listOf("appDataFolder")
                            appProperties = mapOf("deviceId" to syncData.deviceId)
                        }
                        val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute()
                        logcat(
                            LogPriority.DEBUG,
                        ) { "Created new sync data file in Google Drive with file ID: ${uploadedFile.id}" }
                    }
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getAppDataFileList(drive: Drive): MutableList<File> {
        try {
            // Search for the existing file by name in the appData folder
            val query = "mimeType='application/x-gzip' and name = '$remoteFileName'"
            val fileList = drive.files()
                .list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name, createdTime, appProperties)")
                .execute()
                .files
            logcat { "AppData folder file list: $fileList" }

            return fileList
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Error no sync data found in appData folder" }
            return mutableListOf()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun deleteSyncDataFromGoogleDrive(): DeleteSyncDataStatus {
        val drive = googleDriveService.driveService

        if (drive == null) {
            logcat(LogPriority.ERROR) { "Google Drive service not initialized" }
            return DeleteSyncDataStatus.NOT_INITIALIZED
        }
        googleDriveService.refreshToken()

        return withIOContext {
            try {
                val appDataFileList = getAppDataFileList(drive)

                if (appDataFileList.isEmpty()) {
                    this@GoogleDriveSyncService
                        .logcat(LogPriority.DEBUG) { "No sync data file found in appData folder of Google Drive" }
                    DeleteSyncDataStatus.NO_FILES
                } else {
                    for (file in appDataFileList) {
                        drive.files().delete(file.id).execute()
                        this@GoogleDriveSyncService.logcat(
                            LogPriority.DEBUG,
                        ) { "Deleted sync data file in appData folder of Google Drive with file ID: ${file.id}" }
                    }
                    DeleteSyncDataStatus.SUCCESS
                }
            } catch (e: Exception) {
                this@GoogleDriveSyncService.logcat(LogPriority.ERROR, throwable = e) {
                    "Error occurred while interacting with Google Drive"
                }
                DeleteSyncDataStatus.ERROR
            }
        }
    }
}

class GoogleDriveService(private val context: Context) {
    var driveService: Drive? = null
    companion object {
        const val REDIRECT_URI = "eu.kanade.google.oauth:/oauth2redirect"
    }
    private val syncPreferences = Injekt.get<SyncPreferences>()

    init {
        initGoogleDriveService()
    }

    /**
     * Initializes the Google Drive service by obtaining the access token and refresh token from the SyncPreferences
     * and setting up the service using the obtained tokens.
     */
    private fun initGoogleDriveService() {
        val accessToken = syncPreferences.googleDriveAccessToken().get()
        val refreshToken = syncPreferences.googleDriveRefreshToken().get()

        if (accessToken == "" || refreshToken == "") {
            driveService = null
            return
        }

        setupGoogleDriveService(accessToken, refreshToken)
    }

    /**
     * Launches an Intent to open the user's default browser for Google Drive sign-in.
     * The Intent carries the authorization URL, which prompts the user to sign in
     * and grant the application permission to access their Google Drive account.
     * @return An Intent configured to launch a browser for Google Drive OAuth sign-in.
     */
    fun getSignInIntent(): Intent {
        val authorizationUrl = generateAuthorizationUrl()

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(authorizationUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Generates the authorization URL required for the user to grant the application
     * permission to access their Google Drive account.
     * Sets the approval prompt to "force" to ensure that the user is always prompted to grant access,
     * even if they have previously granted access.
     * @return The authorization URL.
     */
    @Suppress("TooGenericExceptionThrown")
    private fun generateAuthorizationUrl(): String {
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val secrets = GoogleClientSecrets.load(
            jsonFactory,
            context.assets.open("client_secrets.json").reader(),
        )

        val flow = GoogleAuthorizationCodeFlow.Builder(
            NetHttpTransport(),
            jsonFactory,
            secrets,
            listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA),
        ).setAccessType("offline").build()

        return flow.newAuthorizationUrl()
            .setRedirectUri(REDIRECT_URI)
            .setApprovalPrompt("force")
            .build()
    }

    @Suppress("TooGenericExceptionThrown")
    internal suspend fun refreshToken() = withIOContext {
        val refreshToken = syncPreferences.googleDriveRefreshToken().get()

        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val secrets = GoogleClientSecrets.load(
            jsonFactory,
            context.assets.open("client_secrets.json").reader(),
        )

        val credential = GoogleCredential.Builder()
            .setJsonFactory(jsonFactory)
            .setTransport(NetHttpTransport())
            .setClientSecrets(secrets)
            .build()

        if (refreshToken == "") {
            throw Exception(context.stringResource(MR.strings.google_drive_not_signed_in))
        }

        credential.refreshToken = refreshToken

        try {
            credential.refreshToken()
            val newAccessToken = credential.accessToken
            // Save the new access token
            syncPreferences.googleDriveAccessToken().set(newAccessToken)
            setupGoogleDriveService(newAccessToken, credential.refreshToken)
        } catch (e: TokenResponseException) {
            if (e.details.error == "invalid_grant") {
                // The refresh token is invalid, prompt the user to sign in again
                this@GoogleDriveService.logcat(LogPriority.ERROR, throwable = e) {
                    "Refresh token is invalid, prompt user to sign in again"
                }
                throw e.message?.let { Exception(it, e) } ?: Exception("Unknown error", e)
            } else {
                // Token refresh failed; handle this situation
                this@GoogleDriveService.logcat(LogPriority.ERROR) { "Failed to refresh access token ${e.message}" }
                this@GoogleDriveService.logcat(LogPriority.ERROR) { "Google Drive sync will be disabled" }
                throw e.message?.let { Exception(it, e) } ?: Exception("Unknown error", e)
            }
        } catch (e: IOException) {
            // Token refresh failed; handle this situation
            this@GoogleDriveService.logcat(LogPriority.ERROR, throwable = e) { "Failed to refresh access token" }
            this@GoogleDriveService.logcat(LogPriority.ERROR) { "Google Drive sync will be disabled" }
            throw e.message?.let { Exception(it, e) } ?: Exception("Unknown error", e)
        }
    }

    /**
     * Sets up the Google Drive service using the provided access token and refresh token.
     * @param accessToken The access token obtained from the SyncPreferences.
     * @param refreshToken The refresh token obtained from the SyncPreferences.
     */
    private fun setupGoogleDriveService(accessToken: String, refreshToken: String) {
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val secrets = GoogleClientSecrets.load(
            jsonFactory,
            context.assets.open("client_secrets.json").reader(),
        )

        val credential = GoogleCredential.Builder()
            .setJsonFactory(jsonFactory)
            .setTransport(NetHttpTransport())
            .setClientSecrets(secrets)
            .build()

        credential.accessToken = accessToken
        credential.refreshToken = refreshToken

        driveService = Drive.Builder(
            NetHttpTransport(),
            jsonFactory,
            credential,
        ).setApplicationName(context.stringResource(MR.strings.app_name))
            .build()
    }

    /**
     * Handles the authorization code returned after the user has granted the
     * application permission to access their Google Drive account.
     * It obtains the access token and refresh token using the authorization code,
     * saves the tokens to the SyncPreferences,
     * sets up the Google Drive service using the obtained tokens, and initializes the service.
     * @param authorizationCode The authorization code obtained from the OAuthCallbackServer.
     * @param activity The current activity.
     * @param onSuccess A callback function to be called on successful authorization.
     * @param onFailure A callback function to be called on authorization failure.
     */
    @Suppress("TooGenericExceptionCaught")
    fun handleAuthorizationCode(
        authorizationCode: String,
        activity: Activity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
        val secrets = GoogleClientSecrets.load(
            jsonFactory,
            context.assets.open("client_secrets.json").reader(),
        )

        val tokenResponse: GoogleTokenResponse = GoogleAuthorizationCodeTokenRequest(
            NetHttpTransport(),
            jsonFactory,
            secrets.installed.clientId,
            secrets.installed.clientSecret,
            authorizationCode,
            REDIRECT_URI,
        ).setGrantType("authorization_code").execute()

        try {
            // Save the access token and refresh token
            val accessToken = tokenResponse.accessToken
            val refreshToken = tokenResponse.refreshToken

            // Save the tokens to SyncPreferences
            syncPreferences.googleDriveAccessToken().set(accessToken)
            syncPreferences.googleDriveRefreshToken().set(refreshToken)

            setupGoogleDriveService(accessToken, refreshToken)
            initGoogleDriveService()

            activity.runOnUiThread {
                onSuccess()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e) { "Failed to handle authorization code" }
            activity.runOnUiThread {
                onFailure(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
