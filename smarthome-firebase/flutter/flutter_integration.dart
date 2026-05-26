/// flutter_integration.dart
/// ─────────────────────────────────────────────────────────────
/// Flutter Smart Home App — Firebase Integration Reference
///
/// pubspec.yaml dependencies:
///   firebase_core: ^2.24.2
///   firebase_auth: ^4.16.0
///   firebase_database: ^10.4.0
///   firebase_messaging: ^14.7.10
///   http: ^1.2.0
///
/// Run:  flutter pub add firebase_core firebase_auth firebase_database
///                         firebase_messaging http
///       flutterfire configure   (links to your Firebase project)

import 'dart:convert';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_database/firebase_database.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:http/http.dart' as http;

// ─────────────────────────────────────────────────────────────
// CONFIG
// ─────────────────────────────────────────────────────────────

const String kApiBase =
    'https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/api';

// ─────────────────────────────────────────────────────────────
// 1. AUTHENTICATION
// ─────────────────────────────────────────────────────────────

class AuthService {
  final _auth = FirebaseAuth.instance;

  /// Register a new user (email + password).
  /// After creating the Firebase account, register the DB profile.
  Future<UserCredential> register(String email, String password,
      {String? displayName}) async {
    final credential = await _auth.createUserWithEmailAndPassword(
      email: email,
      password: password,
    );

    // Create backend profile via Cloud Function
    final idToken = await credential.user!.getIdToken();
    await http.post(
      Uri.parse('$kApiBase/auth/register'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $idToken',
      },
      body: jsonEncode({'displayName': displayName}),
    );

    // Register FCM token
    await registerFcmToken();
    return credential;
  }

  /// Sign in with email + password.
  Future<UserCredential> login(String email, String password) async {
    final credential = await _auth.signInWithEmailAndPassword(
      email: email,
      password: password,
    );
    await registerFcmToken(); // refresh token on every login
    return credential;
  }

  Future<void> logout() async {
    // Remove FCM token before signing out
    final token = await FirebaseMessaging.instance.getToken();
    if (token != null) {
      final idToken = await _auth.currentUser?.getIdToken();
      if (idToken != null) {
        await http.delete(
          Uri.parse('$kApiBase/auth/fcm-token'),
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer $idToken',
          },
          body: jsonEncode({'token': token}),
        );
      }
    }
    await _auth.signOut();
  }

  Future<String?> getIdToken() => _auth.currentUser?.getIdToken();

  Future<void> registerFcmToken() async {
    final fcmToken = await FirebaseMessaging.instance.getToken();
    final idToken  = await getIdToken();
    if (fcmToken == null || idToken == null) return;

    await http.post(
      Uri.parse('$kApiBase/auth/fcm-token'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $idToken',
      },
      body: jsonEncode({'token': fcmToken}),
    );
  }
}

// ─────────────────────────────────────────────────────────────
// 2. REALTIME DATABASE — LISTENING TO SENSOR DATA
// ─────────────────────────────────────────────────────────────

class SensorStream {
  final _db = FirebaseDatabase.instance;

  /// Returns a Stream of water level readings for a device.
  /// Emits a new Map every time ESP32 writes fresh data.
  ///
  /// Usage in a StreamBuilder:
  ///   StreamBuilder<Map<dynamic, dynamic>>(
  ///     stream: SensorStream().watchWaterLevel('device_abc'),
  ///     builder: (ctx, snap) => Text('${snap.data?['value']}%'),
  ///   )
  Stream<Map<dynamic, dynamic>> watchWaterLevel(String deviceId) {
    return _db
        .ref('sensors/$deviceId/water_level')
        .onValue
        .map((event) => (event.snapshot.value as Map?) ?? {});
  }

  Stream<Map<dynamic, dynamic>> watchRainStatus(String deviceId) {
    return _db
        .ref('sensors/$deviceId/rain_status')
        .onValue
        .map((event) => (event.snapshot.value as Map?) ?? {});
  }

  Stream<Map<dynamic, dynamic>> watchTemperature(String deviceId) {
    return _db
        .ref('sensors/$deviceId/temperature')
        .onValue
        .map((event) => (event.snapshot.value as Map?) ?? {});
  }

  /// Streams ALL sensor data for a device in one subscription.
  Stream<Map<dynamic, dynamic>> watchAllSensors(String deviceId) {
    return _db
        .ref('sensors/$deviceId')
        .onValue
        .map((event) => (event.snapshot.value as Map?) ?? {});
  }

  /// Streams device status (online / offline).
  Stream<String> watchDeviceStatus(String deviceId) {
    return _db
        .ref('devices/$deviceId/status')
        .onValue
        .map((e) => (e.snapshot.value as String?) ?? 'unknown');
  }
}

// ─────────────────────────────────────────────────────────────
// 3. RELAY CONTROL
// ─────────────────────────────────────────────────────────────

class RelayService {
  final _authService = AuthService();

  /// Toggles a relay via the Cloud Function REST API.
  /// The backend writes to relay_states/<deviceId>/<relayId>/state
  /// and the ESP32 reads it within ~1 second.
  Future<void> setRelay(String deviceId, String relayId, bool state) async {
    final idToken = await _authService.getIdToken();
    if (idToken == null) throw Exception('Not authenticated');

    final resp = await http.put(
      Uri.parse('$kApiBase/devices/$deviceId/relays/$relayId'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $idToken',
      },
      body: jsonEncode({'state': state}),
    );

    if (resp.statusCode != 200) {
      throw Exception('Relay control failed: ${resp.body}');
    }
  }

  /// Returns a stream of relay state changes.
  Stream<bool> watchRelayState(String deviceId, String relayId) {
    return FirebaseDatabase.instance
        .ref('relay_states/$deviceId/$relayId/state')
        .onValue
        .map((e) => (e.snapshot.value as bool?) ?? false);
  }
}

// ─────────────────────────────────────────────────────────────
// 4. PUSH NOTIFICATIONS (FCM)
// ─────────────────────────────────────────────────────────────

class NotificationService {
  static Future<void> init() async {
    final messaging = FirebaseMessaging.instance;

    // Request permission (iOS requires explicit prompt)
    await messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    // ── Foreground messages ──
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('📩 Foreground notification: ${message.notification?.title}');
      // Show in-app snackbar / dialog here
      _handleMessage(message);
    });

    // ── Background / terminated tap ──
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      print('🔔 Notification tapped: ${message.data}');
      _handleNotificationTap(message);
    });

    // ── Check if app was opened from a terminated state ──
    final initial = await messaging.getInitialMessage();
    if (initial != null) _handleNotificationTap(initial);
  }

  static void _handleMessage(RemoteMessage msg) {
    final type = msg.data['type'] ?? '';
    switch (type) {
      case 'water_level_critical':
        // Navigate to water level screen, show alert dialog, etc.
        break;
      case 'rain_detected':
        // Show rain alert banner
        break;
      case 'device_offline':
        // Update device status indicator
        break;
    }
  }

  static void _handleNotificationTap(RemoteMessage msg) {
    // Navigate to the relevant screen based on notification type
    final deviceId = msg.data['deviceId'];
    print('Navigate to device: $deviceId');
  }
}

// ─────────────────────────────────────────────────────────────
// 5. EXAMPLE WIDGET — WATER LEVEL CARD
// ─────────────────────────────────────────────────────────────

/*
class WaterLevelCard extends StatelessWidget {
  final String deviceId;
  const WaterLevelCard({required this.deviceId});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Map<dynamic, dynamic>>(
      stream: SensorStream().watchWaterLevel(deviceId),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const CircularProgressIndicator();
        }
        final data   = snapshot.data!;
        final level  = (data['value']  as num?)?.toInt()  ?? 0;
        final status = (data['status'] as String?) ?? 'normal';

        return Card(
          color: status == 'critical'
              ? Colors.red.shade100
              : status == 'warning'
                  ? Colors.orange.shade100
                  : Colors.green.shade100,
          child: ListTile(
            leading: const Icon(Icons.water),
            title: Text('Water Level: $level%'),
            subtitle: Text('Status: $status'),
          ),
        );
      },
    );
  }
}
*/
