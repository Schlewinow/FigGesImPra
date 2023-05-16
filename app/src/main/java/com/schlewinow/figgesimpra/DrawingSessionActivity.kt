package com.schlewinow.figgesimpra

import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.schlewinow.figgesimpra.model.SessionImage
import com.schlewinow.figgesimpra.model.SessionRun
import java.util.*

class DrawingSessionActivity : AppCompatActivity() {

    private var runningSession: SessionRun = SessionRun()

    private var sessionImageView: ImageView? = null

    private var sessionImageNameText: TextView? = null

    private var sessionImageCounterText: TextView? = null

    private var sessionRemainingTimeText: TextView? = null

    private var sessionDrawingLayout: View? = null

    private var sessionFinishLayout: View? = null

    /**
     * The thread updating the timer.
     * As the thread references the current UI, it cannot be used with a new instance of the UI
     * (e.g. in the case of a screen orientation change).
     * Hence, the thread itself is managed by the activity itself.
     */
    private  var timerThread: Thread? = null

    /**
     * Flag can be used to quit the timer thread at any time.
     * Should be used to properly clean up the activity at the end or cancel of a session.
     */
    private var timerThreadRunning: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing_session)

        if (SessionDataManager.runningSession == null) {
            // Setup data.
            if(SessionDataManager.setupSession == null) {
                finish()
                return
            }

            runningSession.copyFrom(SessionDataManager.setupSession!!)
            if (runningSession.imageDirectory == null) {
                finish()
                return
            }

            // Get available image files and shuffle them.
            val imageFiles = ImageFileTools.getImageFiles(this, runningSession.imageDirectory!!, runningSession.includeSubdirs)
            runningSession.selectedImages.addAll(generateRandomImageList(imageFiles, runningSession.imageCount))

            SessionDataManager.runningSession = runningSession
        }
        else {
            // Restore data.
            runningSession = SessionDataManager.runningSession!!
        }

        // Back navigation stops the session, so cleanup is required.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backToSettings()
            }
        })

        // Setup timer thread.
        timerThread = Thread { runTimer() }
        timerThread?.start()

        setupSessionUI()
        setupSessionFinishUI()

        if(runningSession.finished) {
            showFinishLayout()
        }
        else {
            showDrawingLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Safety net. Required when screen orientation is changed.
        timerThreadRunning = false
    }

    private fun setupSessionUI() {
        sessionImageView = findViewById(R.id.drawingSessionImage)
        sessionImageNameText = findViewById(R.id.drawingSessionImageName)
        sessionImageCounterText = findViewById(R.id.drawingSessionImageCounterText)
        sessionRemainingTimeText = findViewById(R.id.drawingSessionRemainingTimeText)
        sessionDrawingLayout = findViewById(R.id.drawingSessionLayout)

        showImage(runningSession.selectedImages[runningSession.currentImageIndex])

        val pauseButton: ImageButton = findViewById(R.id.drawingSessionPauseButton)
        updatePauseButtonUI(pauseButton, runningSession.timerPaused)
        pauseButton.setOnClickListener {
            runningSession.timerPaused = !runningSession.timerPaused
            updatePauseButtonUI(pauseButton, runningSession.timerPaused)
        }

        val stopButton: ImageButton = findViewById(R.id.drawingSessionStopButton)
        stopButton.setOnClickListener { finishSession() }

        val previousButton: ImageButton = findViewById(R.id.drawingSessionPreviousButton)
        previousButton.setOnClickListener { showPreviousImage() }

        val nextButton: ImageButton = findViewById(R.id.drawingSessionNextButton)
        nextButton.setOnClickListener { showNextImage() }
    }

    private fun setupSessionFinishUI() {
        sessionFinishLayout = findViewById(R.id.drawingSessionFinishLayout)
        sessionFinishLayout?.visibility = View.GONE

        val previewImageRecycler: RecyclerView = findViewById(R.id.drawingSessionFinishImageRecycler)
        val orientation = resources.configuration.orientation
        val columns = if(orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 3
        previewImageRecycler.layoutManager = GridLayoutManager(this, columns)
        previewImageRecycler.adapter = ImageReviewRecyclerAdapter(runningSession.selectedImages)

        val finishButton: Button = findViewById(R.id.drawingSessionFinishButton)
        finishButton.setOnClickListener {
            backToSettings()
        }
    }

    /**
     * Show icon for pause or play depending on current state.
     * @param pauseButton Image button to have icon switched.
     * @param isPaused Current session pause state.
     */
    private fun updatePauseButtonUI(pauseButton: ImageButton, isPaused: Boolean) {
        if (isPaused) {
            pauseButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.button_play))
        }
        else {
            pauseButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.button_pause))
        }
    }

    /**
     * Update the UI to show the proper image and session progress.
     */
    private fun showImage(sessionImage: SessionImage) {
        runningSession.timerPassedMillis = 0

        ImageFileTools.loadImage(this, sessionImage.imageFile, sessionImageView!!)
        sessionImageView?.scaleX = if (sessionImage.mirrored) -1.0f else 1.0f

        val sessionImageFile = DocumentFile.fromSingleUri(this,  sessionImage.imageFile)
        sessionImageNameText?.text = sessionImageFile?.name
        sessionImageCounterText?.text = "${runningSession.currentImageIndex + 1}/${runningSession.imageCount}"
    }

    /**
     * Show the previous image of the session.
     * Does nothing if the first image is currently being shown.
     */
     private fun showPreviousImage() {
        if (runningSession.currentImageIndex == 0) {
            return
        }

        runningSession.currentImageIndex--
        showImage(runningSession.selectedImages[runningSession.currentImageIndex])
    }

    /**
     * Show the next image of the session.
     * Quit the session if the last image is currently being shown.
     * Synchronized because this may be called from the timer thread.
     */
    @Synchronized private fun showNextImage() {
        runningSession.currentImageIndex++

        if (runningSession.currentImageIndex >= runningSession.imageCount) {
            finishSession()
        }
        else {
            showImage(runningSession.selectedImages[runningSession.currentImageIndex])
        }
    }

    private fun showDrawingLayout() {
        sessionDrawingLayout?.visibility = View.VISIBLE
        sessionFinishLayout?.visibility = View.GONE
        runningSession.finished = false
    }

    private fun showFinishLayout() {
        sessionDrawingLayout?.visibility = View.GONE
        sessionFinishLayout?.visibility = View.VISIBLE
        runningSession.finished = true
    }

    /**
     * Show a special screen to summarize the session before ending it.
     * Allows user to re-enter specific images if they wish to do so.
     */
    private fun finishSession() {
        runningSession.timerPaused = true
        showFinishLayout()
    }

    /**
     * Cleanup and quit of the current activity.
     */
    private fun backToSettings() {
        timerThreadRunning = false
        SessionDataManager.runningSession = null
        finish()
    }

    /**
     * Generate a randomized image list. No doubles will be contained unless the number of samples is greater than the number of source images.
     * @param imageFiles The source to sample from.
     * @param numberImages The number of samples to take.
     * @return A list containing random samples from the input file list.
     */
    private fun generateRandomImageList(imageFiles: List<Uri>, numberImages: Int) : List<SessionImage> {
        val randomizedFiles: MutableList<SessionImage> = mutableListOf()
        val availableFiles: MutableList<Uri> = mutableListOf()
        availableFiles.addAll(imageFiles)

        while (randomizedFiles.size < numberImages) {
            // Pick a random image and remove it from the pool for further pulls.
            val randomIndex : Int = (Math.random() * availableFiles.size).toInt()
            var mirrored = false
            if (runningSession.allowMirroringImages) {
                mirrored = Math.random() >= 0.5
            }

            randomizedFiles.add(SessionImage(availableFiles.removeAt(randomIndex), mirrored))

            // Once running out of source images, reset the available file list.
            if (availableFiles.isEmpty()) {
               availableFiles.addAll(imageFiles)
            }
        }

        return randomizedFiles
    }

    /**
     * Image timer thread method.
     * Counts down a timer. Once it hits zero, goes to the next session image.
     * Can be paused via the currently running session or quit using the local timerThreadRunning.
     */
    private fun runTimer() {
        val mainHandler = Handler(Looper.getMainLooper())
        var lastTimeCheck: Date = Calendar.getInstance().time

        while(timerThreadRunning) {
            val currentTimeCheck: Date = Calendar.getInstance().time

            if (!runningSession.timerPaused) {
                // Only pause the passed time update. Keep the UI-updates active.
                runningSession.timerPassedMillis += currentTimeCheck.time - lastTimeCheck.time
            }

            val passedSeconds = runningSession.timerPassedMillis / 1000
            val remainingTime  = runningSession.durationSeconds - passedSeconds
            val remainingTimeString = "" + (remainingTime / 60) + ":" + (if(remainingTime % 60 < 10) "0" else "") + (remainingTime % 60)
            mainHandler.post { sessionRemainingTimeText?.text = remainingTimeString }

            if (passedSeconds >= runningSession.durationSeconds) {
                mainHandler.post { showNextImage() }
            }

            lastTimeCheck = currentTimeCheck
            Thread.sleep(100)
        }
    }

    inner class ImageReviewRecyclerAdapter(private val images: List<SessionImage>) : RecyclerView.Adapter<ImageReviewEntryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageReviewEntryHolder {
            val view =  LayoutInflater.from(this@DrawingSessionActivity).inflate(R.layout.element_session_drawing_review, parent, false)
            return ImageReviewEntryHolder(view)
        }

        override fun onBindViewHolder(holder: ImageReviewEntryHolder, position: Int) {
            holder.setup(images[position], position)
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }

    inner class ImageReviewEntryHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun setup(image: SessionImage, index: Int) {
            val previewImageView: ImageView = view.findViewById(R.id.drawingSessionElementReviewImage)
            ImageFileTools.loadThumbnail(this@DrawingSessionActivity, image.imageFile, previewImageView, SessionSetupActivity.PREVIEW_THUMBNAIL_SIZE)

            view.setOnClickListener {
                runningSession.currentImageIndex = index
                showImage(image)
                showDrawingLayout()
            }
        }
    }
}