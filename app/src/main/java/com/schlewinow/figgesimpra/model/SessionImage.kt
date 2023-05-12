package com.schlewinow.figgesimpra.model

import android.net.Uri

/**
 * Image data used during runtime session.
 * @param imageFile Image to be shown in the drawing session.
 * @param mirrored Flag to check if the image is horizontally mirrored or not.
 */
class SessionImage (val imageFile: Uri, val mirrored: Boolean) {
}