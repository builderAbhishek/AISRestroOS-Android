package com.ais.restroos.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val TAG = "ImageCompressor"

    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): Result<File> = withContext(Dispatchers.IO) {
        val rawTempFile = File(context.cacheDir, "raw_dish_${System.currentTimeMillis()}.tmp")
        try {
            // 1. Single stream read: Copy remote/content URI into local temporary cache file
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(rawTempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for URI: $imageUri"))

            if (!rawTempFile.exists() || rawTempFile.length() == 0L) {
                return@withContext Result.failure(Exception("Image file is empty or unreadable"))
            }

            // 2. Measure dimensions from local file
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(rawTempFile.absolutePath, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(Exception("Invalid image dimensions ($originalWidth x $originalHeight)"))
            }

            // 3. Calculate optimal inSampleSize (power of 2)
            var sampleSize = 1
            val maxOriginal = max(originalWidth, originalHeight)
            while (maxOriginal / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // 4. Decode bitmap from local file
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val decodedBitmap = BitmapFactory.decodeFile(rawTempFile.absolutePath, decodeOptions)
                ?: return@withContext Result.failure(Exception("Failed to decode image bitmap"))

            // 5. Apply EXIF Orientation safely from local file path
            val rotatedBitmap = try {
                applyExifOrientation(rawTempFile.absolutePath, decodedBitmap)
            } catch (e: Exception) {
                Log.w(TAG, "EXIF rotation failed, using decoded bitmap directly", e)
                decodedBitmap
            }

            // 6. Scale down to maxDimension if needed
            val finalBitmap = scaleBitmapToMaxDimension(rotatedBitmap, maxDimension)

            // 7. Write to compressed output JPEG file
            val compressedFile = File(context.cacheDir, "dish_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
                outStream.flush()
            }

            // Recycle intermediate bitmaps if newly allocated
            if (finalBitmap !== decodedBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            if (finalBitmap !== rotatedBitmap && !rotatedBitmap.isRecycled) {
                rotatedBitmap.recycle()
            }

            Log.d(TAG, "Successfully compressed image to ${compressedFile.length()} bytes: ${compressedFile.absolutePath}")
            Result.success(compressedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Image compression error: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (rawTempFile.exists()) {
                rawTempFile.delete()
            }
        }
    }

    private fun scaleBitmapToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = max(1, (width * scale).toInt())
        val targetHeight = max(1, (height * scale).toInt())

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun applyExifOrientation(filePath: String, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
