package com.schlewinow.figgesimpra

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.schlewinow.figgesimpra.model.SessionEdit


class SessionSetupActivity : AppCompatActivity() {

    companion object {
        /**
         * Width and height to be used for image folder previews.
         */
        const val PREVIEW_THUMBNAIL_SIZE: Int = 200
    }

    private var setupSession: SessionEdit = SessionEdit()

    private var loadingLayout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_setup)

        // Check for previous user settings.
        if (SessionDataManager.setupSession == null) {
            // Default values.
            setupSession.editImageDirectory(null)
            setupSession.editIncludeSubdirs(false)
            setupSession.editAllowMirroringImages(false)
            setupSession.editImageCount(10)
            setupSession.editDurationSeconds(30)
            setupSession.editAutoBreakActive(false)
            setupSession.editBreakInterval(5)
            setupSession.editBreakDuration(15)

            // Restore previous session setup if possible.
            val restoredSession = SessionDataManager.restoreSession(this)
            if (restoredSession != null) {
                setupSession = restoredSession
            }

            SessionDataManager.setupSession = setupSession
        }
        else {
            // Restore previous user settings if existing.
            setupSession = SessionDataManager.setupSession!!
        }

        loadingLayout = findViewById(R.id.sessionSetupLoadingLayout)
        loadingLayout?.visibility = View.GONE

        setupImageFolderUI()
        setupSessionImageCountUI()
        setupSessionDurationUI()
        setupSessionBreakUI()

        val startSessionButton: Button = findViewById(R.id.sessionSetupStartButton)
        startSessionButton.setOnClickListener { startSession() }
    }

    private fun setupImageFolderUI() {
        val chooseDirectoryButton: ImageButton = findViewById(R.id.sessionSetupDirectoryButton)
        chooseDirectoryButton.setOnClickListener { openDirectoryPicker() }

        val includeSubdirsCheckbox: CheckBox = findViewById(R.id.sessionSetupSubdirCheckbox)
        includeSubdirsCheckbox.isChecked = setupSession.includeSubdirs
        includeSubdirsCheckbox.setOnCheckedChangeListener { _, value ->
            setupSession.editIncludeSubdirs(value)
            if (setupSession.imageDirectory != null) {
                updateUiWithSelectedFolder(setupSession.imageDirectory!!)
            }
        }

        val mirrorImagesCheckbox: CheckBox = findViewById(R.id.sessionSetupMirrorCheckbox)
        mirrorImagesCheckbox.isChecked = setupSession.allowMirroringImages
        mirrorImagesCheckbox.setOnCheckedChangeListener { _, value -> setupSession.editAllowMirroringImages(value) }

        if (setupSession.imageDirectory != null) {
            updateUiWithSelectedFolder(setupSession.imageDirectory!!)
        }
    }

    private fun setupSessionImageCountUI() {
        val sessionImageCountText: EditText = findViewById(R.id.sessionSetupCounterEditText)
        sessionImageCountText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setupSession.editImageCount(
                    updateSessionEditText(
                        parseEditText(sessionImageCountText, setupSession.imageCount),
                        sessionImageCountText))
            }
        }
        setupSession.editImageCount(updateSessionEditText(setupSession.imageCount, sessionImageCountText))

        val sessionImageCountPlusButton: Button = findViewById(R.id.sessionSetupCounterPlusButton)
        sessionImageCountPlusButton.setOnClickListener {
            setupSession.editImageCount(updateSessionEditText(setupSession.imageCount + 1, sessionImageCountText)) }

        val sessionImageCountMinusButton: Button = findViewById(R.id.sessionSetupCounterMinusButton)
        sessionImageCountMinusButton.setOnClickListener {
            setupSession.editImageCount(updateSessionEditText(setupSession.imageCount - 1, sessionImageCountText)) }
    }

    private fun setupSessionDurationUI() {
        val sessionImageDurationText: EditText = findViewById(R.id.sessionSetupTimerEditText)
        sessionImageDurationText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setupSession.editDurationSeconds(
                    updateSessionEditText(
                        parseEditText(sessionImageDurationText, setupSession.durationSeconds),
                        sessionImageDurationText))
            }
        }
        setupSession.editDurationSeconds(updateSessionEditText(setupSession.durationSeconds, sessionImageDurationText))

        val sessionDurationButton30: Button = findViewById(R.id.sessionSetupTimer30SecButton)
        sessionDurationButton30.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(30, sessionImageDurationText)) }

        val sessionDurationButton45: Button = findViewById(R.id.sessionSetupTimer45SecButton)
        sessionDurationButton45.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(45, sessionImageDurationText)) }

        val sessionDurationButton60: Button = findViewById(R.id.sessionSetupTimer1MinButton)
        sessionDurationButton60.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(60, sessionImageDurationText)) }

        val sessionDurationButton120: Button = findViewById(R.id.sessionSetupTimer2MinButton)
        sessionDurationButton120.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(120, sessionImageDurationText)) }

        val sessionDurationButton300: Button = findViewById(R.id.sessionSetupTimer5MinButton)
        sessionDurationButton300.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(300, sessionImageDurationText)) }

        val sessionDurationButton600: Button = findViewById(R.id.sessionSetupTimer10MinButton)
        sessionDurationButton600.setOnClickListener {
            setupSession.editDurationSeconds(updateSessionEditText(600, sessionImageDurationText)) }
    }

    private fun setupSessionBreakUI() {
        val sessionBreakActivatedCheckbox: CheckBox = findViewById(R.id.sessionSetupBreakActivatedCheckbox)
        sessionBreakActivatedCheckbox.isChecked = setupSession.autoBreakActive
        sessionBreakActivatedCheckbox.setOnCheckedChangeListener { _, value -> setupSession.editAutoBreakActive(value) }

        val sessionBreakIntervalText: EditText = findViewById(R.id.sessionSetupBreakImagesEditText)
        sessionBreakIntervalText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setupSession.editBreakInterval(
                    updateSessionEditText(
                        parseEditText(sessionBreakIntervalText, setupSession.breakInterval),
                        sessionBreakIntervalText))
            }
        }
        setupSession.editBreakInterval(updateSessionEditText(setupSession.breakInterval, sessionBreakIntervalText))

        val sessionBreakDurationText: EditText = findViewById(R.id.sessionSetupBreakDurationEditText)
        sessionBreakDurationText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                setupSession.editBreakDuration(
                    updateSessionEditText(
                        parseEditText(sessionBreakDurationText, setupSession.breakDurationSeconds),
                        sessionBreakDurationText))
            }
        }
        setupSession.editBreakDuration(updateSessionEditText(setupSession.breakDurationSeconds, sessionBreakDurationText))
    }

    /**
     * Update the UI of a value and apply boundaries.
     * @param value The number to be shown in the UI
     * @param valueText The EditText to update with the latest value. Set to null of no UI update is required.
     * @return The value inside the boundaries that is now visualized in the UI.
     */
    private fun updateSessionEditText(value: Int, valueText: EditText?) : Int {
        var modifiedValue: Int = value

        if (value < 1) {
            modifiedValue = 1
        }

        valueText?.setText("$modifiedValue")
        return modifiedValue
    }

    /**
     * Parse the text inside an EditText into an integer.
     * @param editText Input text field to be parsed.
     * @param defaultValue Returned if any error occurs while parsing the text.
     * @return The parsed value. If the text could not be parsed, return the passed default value instead.
     */
    private fun parseEditText(editText: EditText, defaultValue: Int): Int {
        var newValue: Int = defaultValue
        try {
            newValue = Integer.parseInt(editText.text.toString())
        }
        catch (nfex: NumberFormatException) {
            // Happens if the text field is left empty.
        }
        return newValue
    }

    /**
     * If all required data is available, move to the drawing session screen.
     * Show a helping message otherwise.
     */
    private fun startSession() {
        if (setupSession.imageDirectory == null) {
            Toast.makeText(this, R.string.error_message_missing_image_folder, Toast.LENGTH_LONG).show()
            return
        }

        val imageFiles = ImageFileTools.getImageFiles(this, setupSession.imageDirectory!!, setupSession.includeSubdirs)
        if (imageFiles.isEmpty()) {
            Toast.makeText(this, R.string.error_message_empty_image_folder, Toast.LENGTH_LONG).show()
            return
        }

        // Clearing the focus will force the EditText with the latest input to update the data model.
        // The value will otherwise not be updated for the newly started session.
        currentFocus?.clearFocus()

        // If session setup is valid, store this setup for the next app start.
        SessionDataManager.storeSession(this, setupSession)

        // No data must be passed via intent as the setup data are stored in SessionDataManager.
        val navigationIntent = Intent(this, DrawingSessionActivity::class.java)
        startActivity(navigationIntent)
    }

    private val folderSelectionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returnedIntent: Intent? = result.data
            if (returnedIntent != null && returnedIntent.data != null) {
                val directoryUri = returnedIntent.data!!
                contentResolver.takePersistableUriPermission(directoryUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val newRootFolder = DocumentFile.fromTreeUri(this, directoryUri)
                if (newRootFolder?.isDirectory == true) {
                    updateUiWithSelectedFolder(directoryUri)
                }
            }
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        folderSelectionResultLauncher.launch(intent)
    }

    private fun updateUiWithSelectedFolder(imageFolder: Uri) {
        // Add a transitional animation to avoid flickering in the UI caused by quicker load times.
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 500
        loadingLayout?.visibility = View.VISIBLE
        loadingLayout?.startAnimation(alphaAnimation)

        val folderLoadingThread = HandlerThread("ImageFolderLoadingThread")
        folderLoadingThread.start()

        val folderLoadHandler = Handler(folderLoadingThread.looper)
        folderLoadHandler.post {
            // This might take a while.
            val imageFiles = ImageFileTools.getImageFiles(this, imageFolder, setupSession.includeSubdirs)

            // UI must always be updated on main thread.
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                val folderNameText: EditText = findViewById(R.id.sessionSetupDirectoryName)
                val imageFolderFile = DocumentFile.fromTreeUri(this, imageFolder)
                folderNameText.setText(imageFolderFile?.name)

                val previewImageRecycler: RecyclerView = findViewById(R.id.sessionSetupImagePreviewRecycler)
                previewImageRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                previewImageRecycler.adapter = ImagePreviewRecyclerAdapter(imageFiles)

                loadingLayout?.clearAnimation()
                loadingLayout?.visibility = View.GONE

                this@SessionSetupActivity.setupSession.editImageDirectory(imageFolder)
                if (imageFiles.isEmpty()) {
                    Toast.makeText(this, R.string.error_message_empty_image_folder, Toast.LENGTH_LONG).show()
                }
            }
            folderLoadingThread.quitSafely()
        }
    }

    inner class ImagePreviewRecyclerAdapter(private val images: List<Uri>) : RecyclerView.Adapter<ImagePreviewEntryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePreviewEntryHolder {
            val view =  LayoutInflater.from(this@SessionSetupActivity).inflate(R.layout.element_session_image_preview, parent, false)
            return ImagePreviewEntryHolder(view)
        }

        override fun onBindViewHolder(holder: ImagePreviewEntryHolder, position: Int) {
            holder.setup(images[position])
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }

    inner class ImagePreviewEntryHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun setup(file: Uri) {
            val previewImageView: ImageView = view.findViewById(R.id.sessionSetupElementPreviewImage)
            ImageFileTools.loadThumbnail(this@SessionSetupActivity, file, previewImageView, PREVIEW_THUMBNAIL_SIZE)
        }
    }
}