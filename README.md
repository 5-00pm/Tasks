# Tasks – a minimal offline to-do app

Kotlin + Jetpack Compose. No internet permission, no accounts; everything is stored in one JSON file on the device.

## How it works
- **Categories** sit in the top bar. Tap `+` to add one, long-press a category to rename or delete it. The **Focus** tab shows every task you've marked as focus, across categories.
- **Subtasks** are indented. In the bottom bar the pill (`Task` / `Sub` / `Sub 2`) sets the level of the next task you add. Tap a task to edit it, or to indent/outdent it and move it to another category.
- **Swipe a task left** to reveal: star, date, focus, delete.
- Completing a parent completes its subtasks. Deletes can be undone for a few seconds. Menu (top right) → *Clear completed*.

## Rename the app
Edit `app_name` in `app/src/main/res/values/strings.xml`.

## Get the APK
**Option A – GitHub Actions (no setup):** push this folder to a GitHub repo. The workflow in `.github/workflows/build-apk.yml` builds it; download the `tasks-apk` artifact from the run's page (Actions tab) and install the `.apk` on your phone.

**Option B – Android Studio:** open this folder, let it sync, then *Build → Build APK(s)*. The file ends up in `app/build/outputs/apk/debug/`.

**Option C – command line** (JDK 17 + Android SDK installed): `./gradlew assembleDebug`.

The debug APK installs directly; allow "install unknown apps" for your file manager or browser when asked.
