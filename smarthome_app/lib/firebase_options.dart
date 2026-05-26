// File generated based on your Firebase project configuration.
// Project: smarthome-app-5c0f3
// ⚠️ Replace the android appId once you register your Android app
//    in Firebase Console → Project Settings → Add Android app

import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      case TargetPlatform.macOS:
        return macos;
      case TargetPlatform.windows:
        return windows;
      case TargetPlatform.linux:
        throw UnsupportedError(
          'DefaultFirebaseOptions have not been configured for linux.',
        );
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  // ── Web ──────────────────────────────────────────────────────
  static const FirebaseOptions web = FirebaseOptions(
    apiKey:            'AIzaSyApxeigZgnzbeMM4s7Akvn2Lf2sEP2hfrE',
    authDomain:        'smarthome-app-5c0f3.firebaseapp.com',
    databaseURL:       'https://smarthome-app-5c0f3-default-rtdb.firebaseio.com',
    projectId:         'smarthome-app-5c0f3',
    storageBucket:     'smarthome-app-5c0f3.firebasestorage.app',
    messagingSenderId: '729119248953',
    appId:             '1:729119248953:web:adf42bcaee16403fe4b3e8',
    measurementId:     'G-TH6W2F09GB',
  );

  // ── Android ──────────────────────────────────────────────────
  // ⚠️ Replace appId with your real Android app ID from Firebase Console
  // Firebase Console → Project Settings → Your apps → Android app → App ID
  static const FirebaseOptions android = FirebaseOptions(
    apiKey:            'AIzaSyApxeigZgnzbeMM4s7Akvn2Lf2sEP2hfrE',
    databaseURL:       'https://smarthome-app-5c0f3-default-rtdb.firebaseio.com',
    projectId:         'smarthome-app-5c0f3',
    storageBucket:     'smarthome-app-5c0f3.firebasestorage.app',
    messagingSenderId: '729119248953',
    appId:             '1:729119248953:android:REPLACE_WITH_ANDROID_APP_ID',
  );

  // ── iOS ──────────────────────────────────────────────────────
  // ⚠️ Requires Mac + Xcode. Replace with values from GoogleService-Info.plist
  static const FirebaseOptions ios = FirebaseOptions(
    apiKey:            'AIzaSyApxeigZgnzbeMM4s7Akvn2Lf2sEP2hfrE',
    databaseURL:       'https://smarthome-app-5c0f3-default-rtdb.firebaseio.com',
    projectId:         'smarthome-app-5c0f3',
    storageBucket:     'smarthome-app-5c0f3.firebasestorage.app',
    messagingSenderId: '729119248953',
    appId:             '1:729119248953:ios:REPLACE_WITH_IOS_APP_ID',
    iosBundleId:       'com.example.smarthomeApp',
  );

  // ── macOS ─────────────────────────────────────────────────────
  static const FirebaseOptions macos = FirebaseOptions(
    apiKey:            'AIzaSyApxeigZgnzbeMM4s7Akvn2Lf2sEP2hfrE',
    databaseURL:       'https://smarthome-app-5c0f3-default-rtdb.firebaseio.com',
    projectId:         'smarthome-app-5c0f3',
    storageBucket:     'smarthome-app-5c0f3.firebasestorage.app',
    messagingSenderId: '729119248953',
    appId:             '1:729119248953:ios:REPLACE_WITH_MACOS_APP_ID',
    iosBundleId:       'com.example.smarthomeApp',
  );

  // ── Windows ───────────────────────────────────────────────────
  static const FirebaseOptions windows = FirebaseOptions(
    apiKey:            'AIzaSyApxeigZgnzbeMM4s7Akvn2Lf2sEP2hfrE',
    authDomain:        'smarthome-app-5c0f3.firebaseapp.com',
    databaseURL:       'https://smarthome-app-5c0f3-default-rtdb.firebaseio.com',
    projectId:         'smarthome-app-5c0f3',
    storageBucket:     'smarthome-app-5c0f3.firebasestorage.app',
    messagingSenderId: '729119248953',
    appId:             '1:729119248953:web:adf42bcaee16403fe4b3e8',
    measurementId:     'G-TH6W2F09GB',
  );
}