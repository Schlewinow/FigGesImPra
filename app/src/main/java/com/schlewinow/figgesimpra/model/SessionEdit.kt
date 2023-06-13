package com.schlewinow.figgesimpra.model

import android.net.Uri

/**
 * Wrapper around session class that allows to edit values at runtime.
 * Used in the [SessionSetupActivity][com.schlewinow.figgesimpra.SessionSetupActivity] to modify the session data.
 * Once the drawing sessions starts, these data may not be modified anymore.
 */
class SessionEdit : Session() {

    fun editImageDirectory(imageDirectory: Uri?) {
        this.imageDirectory = imageDirectory
    }

    fun editIncludeSubdirs(includeSubdirs: Boolean) {
        this.includeSubdirs = includeSubdirs
    }

    fun editAllowMirroringImages(allowMirroringImages: Boolean) {
        this.allowMirroringImages = allowMirroringImages
    }

    fun editImageCount(imageCount : Int) {
        this.imageCount = imageCount
    }

    fun editDurationSeconds(durationSeconds: Int) {
        this.durationSeconds = durationSeconds
    }

    fun editAutoBreakActive(autoBreakActive: Boolean) {
        this.autoBreakActive = autoBreakActive
    }

    fun editBreakInterval(breakInterval: Int) {
        this.breakInterval = breakInterval
    }

    fun editBreakDuration(breakDuration: Int) {
        this.breakDurationSeconds = breakDuration
    }
}