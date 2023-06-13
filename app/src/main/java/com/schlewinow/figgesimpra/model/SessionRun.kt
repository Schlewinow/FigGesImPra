package com.schlewinow.figgesimpra.model

/**
 * Session data container adapted to include temporary data used during the session only.
 */
class SessionRun : Session() {

    fun copyFrom(session: Session) {
        imageDirectory = session.imageDirectory
        includeSubdirs = session.includeSubdirs
        allowMirroringImages = session.allowMirroringImages
        imageCount = session.imageCount
        durationSeconds = session.durationSeconds
        autoBreakActive = session.autoBreakActive
        breakInterval = session.breakInterval
        breakDurationSeconds = session.breakDurationSeconds
    }

    /**
     * The images selected from the source folder to be used during this session.
     * Randomized in order.
     */
    val selectedImages: MutableList<SessionImage> = mutableListOf()

    /**
     * Index of the currently shown image in the session.
     * Max value is imageCount - 1.
     */
    var currentImageIndex: Int = 0

    /**
     * Once the last image of the session was shown, go to a final screen to show a session overview.
     * This flag is true only during that stage. Reset to false if any image is being reviewed.
     */
    var finished: Boolean = false

    /**
     * Passed time in milliseconds during the current session image.
     * Subtracted from durationSeconds to compute the remaining time for the current session image.
     */
    var timerPassedMillis: Long = 0

    /**
     * Pause state of the session image timer.
     */
    var timerPaused: Boolean = false

    /**
     * Session auto break state.
     */
    var breakActive: Boolean = false
}