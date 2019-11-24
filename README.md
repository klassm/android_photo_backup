Photo Copy
============

Android App copying photos from one external drive to another. This is intended
as photo backup (i.e. on vacation) to make sure you don't loose any photos when
a SD-Card breaks.

The app works scans all externally attached drives (like SD-Cards or external disks)
for a "DCIM" folder. If such a folder is found on an external drive, it is considered to be
a "source" drive, so a drive to backup files from.
All other drives are considered to be target drives.

By default the app will copy all files into a target folder "photo_copy_images". You can
change the name of the folder within the preferences.
Also by default the app will verify the MD5 hash of all copied files, thus verifying the
integrity of all copied files.

The app requires the latest Android version 10 to work / install.

## Building the App

Issue `./gradlew assemble` on the command line and install the resulting apk file 
(`app/build/outputs/apk/release/app-release-unsigned.apk`) on your Android device.
