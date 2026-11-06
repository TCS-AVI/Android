TCS Home - BlackBerry QNX style launcher for Android
Designed by The Computer Services

No internet permission and no web links: it only changes how your phone looks and feels.

BUILD WITHOUT ANDROID STUDIO (free GitHub cloud build)
1. Make a free account at github.com and tap New repository (any name, Public or Private).
2. Choose "uploading an existing file". Drag in everything INSIDE this folder
   (app, gradle, .github, build.gradle.kts, settings.gradle.kts, gradle.properties, .gitignore),
   not the folder itself. Press Commit changes.
3. Open the Actions tab. "Build TCS Home APK" starts by itself (or press Run workflow).
   Wait about 5 minutes for the green tick.
4. Open the finished run, scroll to Artifacts, download TCSHome-APK, unzip it to get app-debug.apk.
5. Copy app-debug.apk to the phone and tap it. Allow "Install unknown apps" for your file manager
   or browser if Android asks. If Play Protect warns, choose Install anyway (it is a debug build).
6. Press Home, choose "TCS Home", then Always.

If the run shows a red cross, open it and send the error text to Claude to fix.

BUILD WITH ANDROID STUDIO (optional)
File > Open > this folder, wait for Gradle sync, then Build > Build APK(s).

Inside the launcher: swipe up on the lock screen, swipe left and right between pages,
pull down the top handle for quick settings, long-press an app for App info or Uninstall,
More > Style for colour, wallpaper and icons.
To go back to your old launcher: Settings > Apps > Default apps > Home app.
