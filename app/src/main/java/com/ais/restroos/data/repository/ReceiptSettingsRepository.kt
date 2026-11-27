package com.ais.restroos.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ais.restroos.core.utils.ImageCompressor
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.EscPosPrinter
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.ReceiptSettingsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.FileOutputStream
import java.net.URL

object ReceiptSettingsRepository {

    suspend fun loadAndSyncFromServer(context: Context): ReceiptSettingsDto = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(context)
        var local = prefs.getReceiptSettings()
        var qrBmp = loadCachedQrBitmap(prefs)
        EscPosPrinter.updateActiveReceiptConfig(local, qrBmp)

        try {
            val resp = RetrofitClient.apiService.getReceiptSettings()
            val remote = resp.body()?.data
            if (resp.isSuccessful && resp.body()?.status == "success" && remote != null) {
                prefs.saveReceiptSettings(remote)
                if (!remote.qr_image_url.isNullOrBlank()) {
                    qrBmp = downloadAndCacheQr(prefs, remote.qr_image_url) ?: qrBmp
                } else {
                    prefs.getQrCacheFile().delete()
                    qrBmp = null
                }
                local = remote
                EscPosPrinter.updateActiveReceiptConfig(local, qrBmp)
            }
        } catch (_: Exception) {
        }
        local
    }

    suspend fun saveSettingsToServer(
        context: Context,
        dto: ReceiptSettingsDto,
        qrBitmap: Bitmap?
    ): Result<ReceiptSettingsDto> = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(context)
        prefs.saveReceiptSettings(dto)
        EscPosPrinter.updateActiveReceiptConfig(dto, qrBitmap)
        try {
            val resp = RetrofitClient.apiService.saveReceiptSettings(dto)
            val saved = resp.body()?.data
            if (resp.isSuccessful && resp.body()?.status == "success" && saved != null) {
                prefs.saveReceiptSettings(saved)
                EscPosPrinter.updateActiveReceiptConfig(saved, qrBitmap)
                Result.success(saved)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "Failed to save settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadQrImage(context: Context, imageUri: Uri): Result<Pair<String, Bitmap?>> = withContext(Dispatchers.IO) {
        try {
            val compressResult = ImageCompressor.compressImage(context, imageUri)
            if (compressResult.isFailure) {
                return@withContext Result.failure(compressResult.exceptionOrNull() ?: Exception("Compression failed"))
            }
            val file = compressResult.getOrThrow()
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            val prefs = PreferencesManager(context)
            if (bmp != null) {
                FileOutputStream(prefs.getQrCacheFile()).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            }

            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("image", file.name, reqFile)
            val resp = RetrofitClient.apiService.uploadReceiptQr(part)
            val url = resp.body()?.data?.get("url")
            if (resp.isSuccessful && resp.body()?.status == "success" && !url.isNullOrBlank()) {
                Result.success(url to bmp)
            } else {
                Result.failure(Exception(resp.body()?.message ?: "QR upload failed (${resp.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadCachedQrBitmap(prefs: PreferencesManager): Bitmap? {
        val f = prefs.getQrCacheFile()
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadAndCacheQr(prefs: PreferencesManager, urlStr: String): Bitmap? {
        return try {
            val fullUrl = MenuRepository.normalizeImageUrl(urlStr) ?: urlStr
            URL(fullUrl).openStream().use { input ->
                val bmp = BitmapFactory.decodeStream(input) ?: return null
                FileOutputStream(prefs.getQrCacheFile()).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }
}
