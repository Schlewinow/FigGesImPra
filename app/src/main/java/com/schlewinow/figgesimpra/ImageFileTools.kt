package com.schlewinow.figgesimpra

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.Closeable

/**
 * Toolset for any operation including images.
 */
object ImageFileTools {
    /**
     * Last directory requested to be scanned for images.
     * Used as cache to avoid repeating slow file operations.
     */
    private var lastScannedDirectory: Uri? = null

    /**
     * Including/excluding subdirs changes scan request results, so it must be considered for cache.
     */
    private var lastScannedDirectorySubdirs: Boolean? = null

    /**
     * Cached results of the last directory scan.
     */
    private var lastScannedImageFiles: List<Uri> = listOf()

    /**
     * Load an image into the target view in full resolution.
     */
    fun loadImage(context: Context, imageFile: Uri, targetView: ImageView) {
        if (!checkIfImage(imageFile)) {
            return
        }

        Glide.with(context)
            .load(imageFile)
            .into(targetView)
    }

    /**
     * Generate a thumbnail of an image file an apply it to the target view.
     */
    fun loadThumbnail(context: Context, imageFile: Uri, targetView: ImageView, targetSize: Int) {
        if (!checkIfImage(imageFile)) {
            return
        }

        Glide.with(context)
            .load(imageFile)
            .override(targetSize)
            .into(targetView)
    }

    /**
     * Get the image files inside a folder. Including contents of child directories is optional.
     * Uses cache mechanism to avoid repeating expensive file access operations.
     * @param context App context required for file access.
     * @param imageDirectory Source directory to scan for image files.
     * @param includeSubDirs If false, only scan target directory. If true, scan subdirs as well.
     * @return A list containing the collected image files.
     */
    fun getImageFiles(context: Context, imageDirectory: Uri, includeSubDirs: Boolean): List<Uri> {
        if (lastScannedDirectory?.equals(imageDirectory) == true && lastScannedDirectorySubdirs == includeSubDirs) {
            return lastScannedImageFiles
        }
        else {
            lastScannedDirectory = imageDirectory
            lastScannedDirectorySubdirs = includeSubDirs
        }

        val result = getImageFilesForDirectory(context, imageDirectory, includeSubDirs)
        lastScannedImageFiles = result.toList()
        return result
    }

    /**
     * Get the image files inside a folder. Including contents of child directories is optional.
     * @param context App context required for file access.
     * @param imageDirectory Source directory to scan for image files.
     * @param includeSubDirs If false, only scan target directory. If true, scan subdirs as well.
     * @return A list containing the collected image files.
     */
    private fun getImageFilesForDirectory(context: Context, imageDirectory: Uri, includeSubDirs: Boolean): List<Uri> {
        val imageFiles: MutableList<Uri> = mutableListOf()
        var childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(imageDirectory, DocumentsContract.getTreeDocumentId(imageDirectory))

        val directoriesToScan : MutableList<Uri> = mutableListOf()
        directoriesToScan.add(childrenUri)

        while (directoriesToScan.isNotEmpty()) {
            childrenUri = directoriesToScan.removeFirst()

            val cursor: Cursor? = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )

            try {
                while (cursor?.moveToNext() == true) {
                    val docId = cursor.getString(0)
                    val name = cursor.getString(1)
                    val mime = cursor.getString(2)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (includeSubDirs) {
                            // ToDo: This seems to be the slow call down the line.
                            val childDirectory = DocumentsContract.buildChildDocumentsUriUsingTree(imageDirectory, docId)
                            directoriesToScan.add(childDirectory)
                        }
                    } else {
                        if (checkIfImage(name)) {
                            val childFile = DocumentsContract.buildDocumentUriUsingTree(imageDirectory, docId)
                            imageFiles.add(childFile)
                        }
                    }
                }
            } finally {
                closeQuietly(cursor)
            }
        }

        return imageFiles
    }

    private fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            }
            catch (re: RuntimeException) {
                throw re
            }
            catch (ignore: Exception) {
            }
        }
    }

    /**
     * Check if the file type is a supported image by checking the file Uri.
     * @param file Uri of the file to be checked.
     * @return true if the file is a supported image, false otherwise.
     */
    private fun checkIfImage(file: Uri): Boolean {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.toString())
        return checkIfImageByExtension(extension.lowercase())
    }

    /**
     * Check if the file type is a supported image by checking the file name.
     * @param fileName Name of the file. Must include extension for this method to work properly.
     * @return true if the file is a supported image, false otherwise.
     */
    private fun checkIfImage(fileName: String): Boolean {
        val extension = fileName.substring(fileName.lastIndexOf(".") + 1)
        return checkIfImageByExtension(extension.lowercase())
    }

    /**
     * Check if the file type is a supported image by checking the file extension.
     * @param extension The file extension as a string.
     * @return true if the file is a supported image, false otherwise.
     */
    private fun checkIfImageByExtension(extension: String): Boolean {
        if (extension == "jpg" || extension == "jpeg"
            || extension == "png"
            || extension == "webp") {
            return true
        }

        return false
    }
}