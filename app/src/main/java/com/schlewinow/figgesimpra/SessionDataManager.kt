package com.schlewinow.figgesimpra

import android.content.Context
import android.net.Uri
import com.schlewinow.figgesimpra.model.Session
import com.schlewinow.figgesimpra.model.SessionEdit
import com.schlewinow.figgesimpra.model.SessionRun

/**
 * Data container to move session data around between activities
 * as well as store runtime data to recover in case of screen orientation changes.
 */
object SessionDataManager {
    /**
     * Stores the data of the session that is currently being set up.
     * Only to be used used in [SessionSetupActivity],
     * with the exception of creating the [SessionRun] at the beginning of a session.
     */
    var setupSession: SessionEdit? = null

    /**
     * Stores data of the currently active drawing session.
     * Only to be used in [DrawingSessionActivity].
     */
    var runningSession: SessionRun? = null

    /**
     * Store the given session settings to restore them at the next app start.
     * @param context Local app context, usually the current activity.
     * @param session The session data to be stored locally on the device.
     */
    fun storeSession(context: Context, session: Session) {
        val sharedPrefsEdit = context.getSharedPreferences("SessionStorage", Context.MODE_PRIVATE).edit()

        sharedPrefsEdit.putString("sessionImageDirectory", session.imageDirectory?.toString())
        sharedPrefsEdit.putBoolean("sessionIncludeSubdirs", session.includeSubdirs)
        sharedPrefsEdit.putBoolean("sessionAllowMirroringImages", session.allowMirroringImages)
        sharedPrefsEdit.putInt("sessionImageCount", session.imageCount)
        sharedPrefsEdit.putInt("sessionDurationSeconds", session.durationSeconds)
        sharedPrefsEdit.putBoolean("sessionStored", true)

        sharedPrefsEdit.apply()
    }

    /**
     * Allows to restore previously saved session settings for user convenience.
     * @param context Local app context, usually the current activity.
     * @return The session settings stored previously on the device. Null if none are present.
     */
    fun restoreSession(context: Context) : SessionEdit? {
        val sharedPrefs = context.getSharedPreferences("SessionStorage", Context.MODE_PRIVATE)
        val storedSessionAvailable = sharedPrefs.getBoolean("sessionStored", false)
        if (!storedSessionAvailable) {
            return null
        }

        val restoredSession = SessionEdit()
        restoredSession.editIncludeSubdirs(sharedPrefs.getBoolean("sessionIncludeSubdirs", false))
        restoredSession.editAllowMirroringImages(sharedPrefs.getBoolean("sessionAllowMirroringImages", false))
        restoredSession.editImageCount(sharedPrefs.getInt("sessionImageCount", 0))
        restoredSession.editDurationSeconds(sharedPrefs.getInt("sessionDurationSeconds", 0))

        val uriString = sharedPrefs.getString("sessionImageDirectory", "")
        if (uriString?.isNotEmpty() == true) {
            restoredSession.editImageDirectory(Uri.parse(uriString))
        }

        return restoredSession
    }
}