Archive builder: `ravenos-launcher/archive-build.sh`.

Required CI hook:
1. checkout with submodules
2. Java 17 + Gradle
3. run `bash ravenos-launcher/archive-build.sh`
4. upload the generated APK, SHA-256, badging, manifest, and device canary
5. publish those files as a prerelease tagged `ravenos-launcher-archive-<run-id>-<attempt>`

The manifest contains the exact durable APK download URL and private archive folder name.
