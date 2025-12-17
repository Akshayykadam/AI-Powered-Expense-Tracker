package com.expense.tracker.data.local.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModelDownloadManager"
private const val CHANNEL_ID = "model_download"
private const val NOTIFICATION_ID = 1001

/**
 * Download progress state
 */
data class DownloadProgress(
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val progress: Float = 0f,
    val error: String? = null,
    val isComplete: Boolean = false
) {
    val downloadedMB: String get() = "%.1f".format(downloadedBytes / (1024.0 * 1024.0))
    val totalMB: String get() = "%.0f".format(totalBytes / (1024.0 * 1024.0))
    val progressText: String get() = "$downloadedMB / $totalMB MB"
}

/**
 * Manages AI model download with background support and progress tracking
 */
@Singleton
class ModelDownloadManager @Inject constructor() {
    
    companion object {
        // Gemma 1B Instruction-Tuned model (MediaPipe .task format)
        const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"
        // Community User Provided Link (Verified Accessible)
        const val MODEL_URL = "https://huggingface.co/metsman/gemma-2b-it-cpu-int4-org/resolve/main/gemma-2b-it-cpu-int4.bin"
        const val EXPECTED_SIZE_MB = 1285L // ~1.28 GB
    }
    
    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()
    
    /**
     * Check if model is already downloaded
     */
    /**
     * Check if model is already downloaded
     */
    fun isModelDownloaded(context: Context): Boolean {
        val modelFile = getModelFile(context)
        // Check for > 1GB to ensure mostly complete (model is ~1.3GB)
        val exists = modelFile.exists() && modelFile.length() > 1_000_000_000L
        if (exists) {
            _downloadProgress.value = DownloadProgress(
                isComplete = true,
                downloadedBytes = modelFile.length(),
                totalBytes = modelFile.length(),
                progress = 1f
            )
        }
        return exists
    }
    
    /**
     * Get model file location
     */
    fun getModelFile(context: Context): File {
        return File(context.getExternalFilesDir(null), MODEL_FILENAME)
    }
    
    /**
     * Start background download with progress
     */
    suspend fun startDownload(context: Context) = withContext(Dispatchers.IO) {
        val modelFile = getModelFile(context)
        
        // If already downloaded, skip
        if (modelFile.exists() && modelFile.length() > 1_000_000_000L) {
            _downloadProgress.value = DownloadProgress(isComplete = true, progress = 1f)
            return@withContext
        }
        
        try {
            _downloadProgress.value = DownloadProgress(isDownloading = true)
            
            // Create notification channel
            createNotificationChannel(context)
            
            // Start download
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("User-Agent", "ExpenseTracker/1.0")
            
            // Handle redirects (GitHub releases redirect)
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                downloadFromUrl(context, newUrl, modelFile)
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                downloadWithProgress(context, connection, modelFile)
            } else {
                throw Exception("HTTP error: $responseCode")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadProgress.value = DownloadProgress(
                isDownloading = false,
                error = "Download failed: ${e.message}"
            )
        }
    }
    
    private suspend fun downloadFromUrl(context: Context, urlString: String, outputFile: File) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            downloadWithProgress(context, connection, outputFile)
        } else {
            throw Exception("HTTP error on redirect: ${connection.responseCode}")
        }
    }
    
    private suspend fun downloadWithProgress(
        context: Context,
        connection: HttpURLConnection,
        outputFile: File
    ) = withContext(Dispatchers.IO) {
        val totalBytes = connection.contentLengthLong.takeIf { it > 0 } 
            ?: (EXPECTED_SIZE_MB * 1024 * 1024)
        
        var downloadedBytes = 0L
        val buffer = ByteArray(8192)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                var bytesRead: Int
                var lastNotificationTime = 0L
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    
                    _downloadProgress.value = DownloadProgress(
                        isDownloading = true,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        progress = progress
                    )
                    
                    // Update notification every 500ms
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime > 500) {
                        lastNotificationTime = now
                        updateNotification(
                            context, 
                            notificationManager, 
                            downloadedBytes, 
                            totalBytes
                        )
                    }
                }
            }
        }
        
        connection.disconnect()
        
        // Complete
        _downloadProgress.value = DownloadProgress(
            isDownloading = false,
            isComplete = true,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progress = 1f
        )
        
        // Final notification
        showCompleteNotification(context, notificationManager)
        
        Log.d(TAG, "Download complete: ${outputFile.absolutePath}")
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows AI model download progress"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification(
        context: Context,
        manager: NotificationManager,
        downloaded: Long,
        total: Long
    ) {
        val downloadedMB = "%.1f".format(downloaded / (1024.0 * 1024.0))
        val totalMB = "%.0f".format(total / (1024.0 * 1024.0))
        val progress = ((downloaded.toFloat() / total) * 100).toInt()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading AI Model")
            .setContentText("$downloadedMB / $totalMB MB")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompleteNotification(context: Context, manager: NotificationManager) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AI Model Ready")
            .setContentText("Download complete. Smart categorization enabled.")
            .setAutoCancel(true)
            .build()
        
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        _downloadProgress.value = DownloadProgress(
            isDownloading = false,
            error = "Download cancelled"
        )
    }
    
    /**
     * Delete downloaded model
     */
    fun deleteModel(context: Context) {
        try {
            val modelFile = getModelFile(context)
            if (modelFile.exists()) {
                val deleted = modelFile.delete()
                Log.d(TAG, "Model deleted: $deleted")
            }
            
            // Reset state
            _downloadProgress.value = DownloadProgress()
            
            // Cancel any ongoing notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model", e)
        }
    }

    /**
     * Reset download state
     */
    fun resetState() {
        _downloadProgress.value = DownloadProgress()
    }
}
