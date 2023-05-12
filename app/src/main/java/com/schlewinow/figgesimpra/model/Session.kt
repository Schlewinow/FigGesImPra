package com.schlewinow.figgesimpra.model

import android.net.Uri

/**
 * Data container for all data relevant to a drawing session.
 * Data are read only as they cannot be modified during an active session.
 */
open class Session {
    /**
     * Local directory from which to select the images used in the session.
     */
    var imageDirectory: Uri? = null
        protected set

    /**
     * Flag to check if image files from the sub directories of the image directory as included as well or not.
     */
    var includeSubdirs: Boolean = false
        protected set

    /**
     * Flag to check whether randomly vertically mirroring images is enabled during the session or not.
     */
    var allowMirroringImages: Boolean = false

    /**
     * The amount of images after which to finish the drawing session.
     */
    var imageCount: Int = 0
        protected set

    /**
     * How long a single image is shown during a drawing session before going to the next image.
     */
    var durationSeconds: Int = 0
        protected set
}