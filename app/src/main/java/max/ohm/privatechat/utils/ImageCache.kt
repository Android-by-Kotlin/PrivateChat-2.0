package max.ohm.privatechat.utils

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ImageCache {
    // Memory cache for bitmaps
    private val memoryCache: LruCache<String, Bitmap> = LruCache(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt() // 1/8 of available memory
    )
    
    // Get bitmap from cache
    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    
    // Add bitmap to cache
    fun addBitmapToCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }
    
    // Save bitmap to disk cache
    suspend fun saveBitmapToDisk(context: android.content.Context, key: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "profile_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "${key.md5()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageCache", "Error saving bitmap to disk: ${e.message}")
        }
    }
    
    // Load bitmap from disk cache
    suspend fun loadBitmapFromDisk(context: android.content.Context, key: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "profile_images")
            val file = File(cacheDir, "${key.md5()}.jpg")
            
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.also { bitmap ->
                    // Add to memory cache
                    addBitmapToCache(key, bitmap)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageCache", "Error loading bitmap from disk: ${e.message}")
            null
        }
    }
    
    // Clear cache
    fun clearCache() {
        memoryCache.evictAll()
    }
    
    // Extension function to generate MD5 hash
    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
