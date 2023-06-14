# FigGesImPra
Figure/gesture image practice app for Android.
Small app to to support drawing practice, with figure and gesture drawings in mind.
Select a folder on your phone, pick the amount of images and duration per image and start a drawing session.
At the end of each session, you will get a chance to take another look at any image used during the session.

## How to use
![Selecting image directory](/docs/images/screen_settings_directory.jpg?raw=true "Image directory selection")

First thing to do is select a directory on the device containing the images for future drawing sessions.
If the option is enabled, sub-directories of the selected directory will be included as well.
During sessions, images may be randomly mirrored to effectively double the variation of available contents.

![Setting up session details](/docs/images/screen_settings_session_params.jpg?raw=true "Drawing session parameters")

The session can be modified using few settings.
The major settings are the amount of images the session will contain, as well as how long a single image will be displayed before moving on to the next.
The timer can be paused at any moment during an active drawing session though.

![Setting up session breaks](/docs/images/screen_settings_break.jpg?raw=true "Automatic session break parameters")

Depending on the personal workflow, a break may be required in between some images (e.g. changing to a new sheet of paper).
To avoid having to manually pause the app every time, automatic breaks can be triggered.
These will happen every time the selected image interval has passed.

![Drawing session](/docs/images/screen_session_draw.jpg?raw=true "Drawing session screen")

During a session, the timer can be (un)paused, the previous or next image shown or the session quit.

![Session finish](/docs/images/screen_session_finish.jpg?raw=true "Session finished screen")

After finishing a session (or quitting manually), a final screen is shown with an overview of the session images.
Tapping an image will re-open that image within the session.
Otherwise going back to the settings ends the session.