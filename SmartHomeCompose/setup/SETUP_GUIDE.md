# 🏠 SmartHome — Complete Beginner Setup Guide

## What You Need (Install in this order)

---

## STEP 1 — Install Java (JDK 17)

Android Studio needs Java to compile your app.

1. Go to: https://adoptium.net
2. Download **Temurin 17 (LTS)**
3. Run the installer → click Next until done
4. Verify: open Terminal / Command Prompt, type:
   ```
   java -version
   ```
   You should see: `openjdk version "17..."`

---

## STEP 2 — Install Android Studio Ladybug

1. Go to: https://developer.android.com/studio
2. Click **Download Android Studio**
3. Run the installer
4. During setup wizard:
   - Choose **Standard** installation
   - Accept all license agreements
   - Wait for SDK download (~2–4 GB, be patient)

> ⚠️ First launch takes 5–10 minutes. This is normal.

---

## STEP 3 — Create a New Project

1. Open Android Studio
2. Click **New Project**
3. Select **Empty Activity** (this is the Compose template)
4. Fill in:
   ```
   Name:           SmartHome
   Package name:   com.smarthome
   Save location:  anywhere you like (e.g. C:\Projects\SmartHome)
   Language:       Kotlin
   Minimum SDK:    API 26 (Android 8.0)
   ```
5. Click **Finish**
6. Wait for Gradle sync (bottom progress bar — ~2 min first time)

---

## STEP 4 — Set Up an Emulator (Virtual Phone)

If you don't have an Android phone:

1. In Android Studio → top menu → **Tools → Device Manager**
2. Click **+** → **Create Virtual Device**
3. Choose **Pixel 8** → Next
4. Download **API 35 (Android 15)** → Next → Finish
5. Click ▶ (Play) button next to your new device to start it

If you have a real Android phone:
1. On your phone: **Settings → About Phone** → tap **Build Number 7 times**
2. Go back → **Developer Options → USB Debugging → Enable**
3. Plug phone into computer via USB
4. Click **Allow** on your phone when prompted
5. Your phone will appear in Android Studio's device selector

---

## STEP 5 — Set Up Firebase

### 5.1 Create Firebase Account & Project
1. Go to: https://console.firebase.google.com
2. Sign in with Google account
3. Click **Add project**
   - Name: `SmartHome`
   - Disable Google Analytics (not needed)
   - Click **Create project**

### 5.2 Enable Authentication
1. Left sidebar → **Authentication** → **Get started**
2. **Sign-in method** tab → **Email/Password** → Enable → **Save**

### 5.3 Enable Realtime Database
1. Left sidebar → **Realtime Database** → **Create database**
2. Choose location nearest to you
3. Select **Start in test mode** → **Enable**
4. After creation, go to **Rules** tab, paste this:
```json
{
  "rules": {
    "smarthome": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```
5. Click **Publish**

### 5.4 Enable Firestore
1. Left sidebar → **Firestore Database** → **Create database**
2. Select **Start in test mode** → **Next** → **Enable**

### 5.5 Enable Cloud Messaging (FCM)
1. Left sidebar → **Project Settings** (gear icon)
2. Go to **Cloud Messaging** tab — it's already enabled. Note your **Server key** for later.

### 5.6 Connect Firebase to Your Android App
1. In Firebase Console → **Project Settings** (gear icon) → **Your apps**
2. Click **Android** icon
3. Fill in:
   ```
   Android package name: com.smarthome
   App nickname: SmartHome
   ```
4. Click **Register app**
5. Click **Download google-services.json**
6. In Android Studio → find the `app` folder in the left panel
7. Right-click `app` → **Show in Explorer/Finder**
8. Copy `google-services.json` into the `app/` folder (same level as `build.gradle`)
9. Back in Firebase → click **Next** → **Next** → **Continue to console**

### 5.7 Create a Test User
1. Firebase Console → **Authentication** → **Users** tab
2. Click **Add user**
   ```
   Email:    test@smarthome.com
   Password: Test@1234
   ```
3. Click **Add user**

### 5.8 Seed the Database
1. Firebase Console → **Realtime Database**
2. Click the **⋮** (three dots) → **Import JSON**
3. Upload the `firebase-seed.json` file from this package
4. Your database now has initial device and sensor data

---

## STEP 6 — Add Code to Your Project

### 6.1 Replace Gradle Files

In Android Studio, open these files and replace their contents:

| File to open | Replace with |
|---|---|
| `build.gradle.kts` (project level) | contents of `project-build.gradle.kts` |
| `app/build.gradle.kts` | contents of `app-build.gradle.kts` |

After replacing → click **Sync Now** (yellow bar at top)

> ⚠️ Gradle sync downloads libraries — takes 2–5 min on first run

### 6.2 Add Source Files

Copy these files into your project (matching the folder paths shown):

```
app/src/main/java/com/smarthome/
├── SmartHomeApp.kt
├── data/model/Models.kt
├── data/repository/SmartHomeRepository.kt
├── viewmodel/ViewModels.kt
├── ui/theme/Theme.kt
├── ui/theme/Color.kt
├── ui/components/Components.kt
├── ui/login/LoginScreen.kt
├── ui/dashboard/DashboardScreen.kt
├── ui/devices/DeviceControlScreen.kt
├── ui/notifications/NotificationScreen.kt
├── util/SmartHomeFcmService.kt
└── util/di/AppModule.kt
```

### 6.3 Replace MainActivity.kt

Open `app/src/main/java/com/smarthome/MainActivity.kt` and replace with the provided `MainActivity.kt`

### 6.4 Update AndroidManifest.xml

Open `app/src/main/AndroidManifest.xml` and replace with the provided `AndroidManifest.xml`

---

## STEP 7 — Run the App

1. Make sure your emulator is running (or phone is connected)
2. Click the green **▶ Run** button in Android Studio toolbar
3. Wait for build (~1–2 min first time)
4. App launches on emulator/phone
5. Log in with: `test@smarthome.com` / `Test@1234`
6. You should see the Dashboard!

---

## STEP 8 — Test Real-time Updates

While the app is running:
1. Go to Firebase Console → **Realtime Database**
2. Navigate to `smarthome → devices → fan → state`
3. Click the pencil icon → change `"OFF"` to `"ON"`
4. Watch your app update **instantly** — no refresh!

---

## Common Problems & Fixes

| Problem | Fix |
|---|---|
| "Gradle sync failed" | Check internet. Try: File → Invalidate Caches → Restart |
| "google-services.json not found" | Make sure it's in the `app/` folder, not the project root |
| "No devices found" | Start your emulator first, or check USB debugging on phone |
| App crashes on launch | Check Logcat (bottom panel) for red error. Most common: wrong package name in Firebase |
| "Build failed: unresolved reference" | Make sure all .kt files are copied to correct folders |

---

## Folder Structure Visual

```
SmartHome/                    ← Your Android Studio project
├── app/
│   ├── google-services.json  ← Downloaded from Firebase (REQUIRED)
│   ├── build.gradle.kts      ← App dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/smarthome/
│           ├── SmartHomeApp.kt
│           ├── MainActivity.kt
│           ├── data/...
│           ├── viewmodel/...
│           └── ui/...
├── build.gradle.kts          ← Project-level gradle
└── functions/                ← Firebase Cloud Functions (optional, deploy separately)
    └── index.js
```
