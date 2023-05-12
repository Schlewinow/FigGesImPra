package com.schlewinow.figgesimpra

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
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
            setupSession.editImageCount(10)
            setupSession.editDurationSeconds(30)

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
        var countTextWatcher: TextWatcher? = null
        countTextWatcher = sessionImageCountText.doOnTextChanged { s, _, _, _ ->
            var newImageCount = setupSession.imageCount
            try {
                newImageCount = Integer.parseInt(s.toString())
            }
            catch (nfex: NumberFormatException) {
                // Happens if the text field is left empty.
            }
            updateSessionImageCount(newImageCount, sessionImageCountText, countTextWatcher)
        }
        updateSessionImageCount(setupSession.imageCount, sessionImageCountText, countTextWatcher)

        val sessionImageCountPlusButton: Button = findViewById(R.id.sessionSetupCounterPlusButton)
        sessionImageCountPlusButton.setOnClickListener {
            updateSessionImageCount(setupSession.imageCount + 1, sessionImageCountText, countTextWatcher) }

        val sessionImageCountMinusButton: Button = findViewById(R.id.sessionSetupCounterMinusButton)
        sessionImageCountMinusButton.setOnClickListener {
            updateSessionImageCount(setupSession.imageCount - 1, sessionImageCountText, countTextWatcher) }
    }

    private fun setupSessionDurationUI() {
        val sessionImageDurationText: EditText = findViewById(R.id.sessionSetupTimerEditText)
        var durationTextWatcher: TextWatcher? = null
        durationTextWatcher = sessionImageDurationText.doOnTextChanged { s, _, _, _ ->
            var newImageDuration = setupSession.durationSeconds
            try {
                newImageDuration = Integer.parseInt(s.toString())
            }
            catch (nfex: NumberFormatException) {
                // Happens if the text field is left empty.
            }
            updateSessionImageDuration(newImageDuration, sessionImageDurationText, durationTextWatcher)
        }
        updateSessionImageDuration(setupSession.durationSeconds, sessionImageDurationText, durationTextWatcher)

        val sessionDurationButton30: Button = findViewById(R.id.sessionSetupTimer30SecButton)
        sessionDurationButton30.setOnClickListener {
            updateSessionImageDuration(30, sessionImageDurationText, durationTextWatcher) }

        val sessionDurationButton45: Button = findViewById(R.id.sessionSetupTimer45SecButton)
        sessionDurationButton45.setOnClickListener {
            updateSessionImageDuration(45, sessionImageDurationText, durationTextWatcher) }

        val sessionDurationButton60: Button = findViewById(R.id.sessionSetupTimer1MinButton)
        sessionDurationButton60.setOnClickListener {
            updateSessionImageDuration(60, sessionImageDurationText, durationTextWatcher) }

        val sessionDurationButton120: Button = findViewById(R.id.sessionSetupTimer2MinButton)
        sessionDurationButton120.setOnClickListener {
            updateSessionImageDuration(120, sessionImageDurationText, durationTextWatcher) }

        val sessionDurationButton300: Button = findViewById(R.id.sessionSetupTimer5MinButton)
        sessionDurationButton300.setOnClickListener {
            updateSessionImageDuration(300, sessionImageDurationText, durationTextWatcher) }

        val sessionDurationButton600: Button = findViewById(R.id.sessionSetupTimer10MinButton)
        sessionDurationButton600.setOnClickListener {
            updateSessionImageDuration(600, sessionImageDurationText, durationTextWatcher) }
    }

    /**
     * Update the number of images after which the training session finishes.
     * @param imageCount The number of images after which the training session will stop.
     * @param imageCountText The EditText to update with the latest value. Set to null of no UI update is required.
     * @param countTextWatcher Listener on UI text changes. Deactivated when the UI is updated to avoid endless recursion.
     */
    private fun updateSessionImageCount(imageCount: Int, imageCountText: EditText?, countTextWatcher: TextWatcher?) {
        if (imageCount < 1) {
            setupSession.editImageCount(1)
        }
        else {
            setupSession.editImageCount(imageCount)
        }

        imageCountText?.removeTextChangedListener(countTextWatcher)
        imageCountText?.setText("${setupSession.imageCount}")
        imageCountText?.addTextChangedListener(countTextWatcher)

        if (imageCountText?.hasFocus() == true) {
            imageCountText.setSelection(imageCountText.length())
        }
    }

    /**
     * Update the duration a single image is shown to the user during an active session.
     * @param durationInSecs Duration in seconds a single image is shown to the user during a session.
     * @param imageDurationText The EditText to update with the latest value. Set to null of no UI update is required.
     * @param durationTextWatcher Listener on UI text changes. Deactivated when the UI is updated to avoid endless recursion.
     */
    private fun updateSessionImageDuration(durationInSecs: Int, imageDurationText: EditText?, durationTextWatcher: TextWatcher?)
    {
        if (durationInSecs < 1) {
            setupSession.editDurationSeconds(1)
        }
        else {
            setupSession.editDurationSeconds(durationInSecs)
        }

        imageDurationText?.removeTextChangedListener(durationTextWatcher)
        imageDurationText?.setText("${setupSession.durationSeconds}")
        imageDurationText?.addTextChangedListener(durationTextWatcher)

        if (imageDurationText?.hasFocus() == true) {
            imageDurationText.setSelection(imageDurationText.length())
        }
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