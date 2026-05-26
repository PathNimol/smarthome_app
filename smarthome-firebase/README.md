# Smart Home IoT Backend — Firebase + Node.js + TypeScript
## Complete Deployment & Integration Guide

---

## 📁 Project Structure

```
smarthome-firebase/
├── firebase.json                  # Firebase project config
├── database.rules.json            # Realtime DB security rules
├── .env.example                   # Environment variable template
├── .gitignore
├── package.json
│
├── functions/
│   ├── package.json
│   ├── tsconfig.json
│   └── src/
│       ├── index.ts               ← Cloud Functions entry point
│       ├── app.ts                 ← Express app
│       ├── config/
│       │   ├── firebase.ts        ← Admin SDK init
│       │   └── constants.ts       ← Thresholds, DB paths, roles
│       ├── middleware/
│       │   ├── auth.middleware.ts  ← Firebase + Device JWT auth
│       │   └── error.middleware.ts ← Global error handler
│       ├── services/
│       │   ├── auth.service.ts    ← User & FCM token management
│       │   ├── device.service.ts  ← Device registration, relays
│       │   ├── sensor.service.ts  ← Sensor data ingestion
│       │   └── activity.service.ts← Audit log
│       ├── notifications/
│       │   └── fcm.service.ts     ← All FCM push logic
│       ├── triggers/
│       │   ├── sensor.triggers.ts ← DB triggers for sensor alerts
│       │   ├── device.triggers.ts ← DB triggers for device events
│       │   └── scheduled.triggers.ts ← Cron functions
│       └── api/
│           ├── auth.routes.ts
│           ├── device.routes.ts
│           ├── sensor.routes.ts
│           └── notification.routes.ts
│
├── esp32/
│   └── smart_home_esp32.ino      ← Arduino firmware
│
└── flutter/
    └── flutter_integration.dart  ← Flutter integration reference
```

---

## 🗄️ Realtime Database Structure

```json
{
  "users": {
    "<uid>": {
      "uid": "abc123",
      "email": "user@example.com",
      "displayName": "John",
      "role": "admin | user | viewer",
      "createdAt": 1700000000000,
      "lastSeen":  1700001000000,
      "fcmTokens": {
        "<token_suffix>": "<fcm_token_string>"
      }
    }
  },

  "devices": {
    "<deviceId>": {
      "id": "device_a1b2c3d4",
      "name": "Living Room Hub",
      "type": "esp32_v1",
      "ownerId": "<uid>",
      "location": "living_room",
      "status": "online | offline",
      "lastPing": 1700001000000,
      "createdAt": 1700000000000
    }
  },

  "sensors": {
    "<deviceId>": {
      "water_level": {
        "value": 45,
        "unit": "%",
        "timestamp": 1700001000000,
        "status": "normal | warning | critical"
      },
      "rain_status": {
        "isRaining": false,
        "intensity": 12,
        "rawValue": 3200,
        "timestamp": 1700001000000
      },
      "temperature": {
        "value": 28.5,
        "unit": "°C",
        "timestamp": 1700001000000
      },
      "humidity": {
        "value": 72,
        "unit": "%",
        "timestamp": 1700001000000
      }
    }
  },

  "relay_states": {
    "<deviceId>": {
      "relay_1": {
        "state": false,
        "label": "Fan",
        "updatedAt": 1700001000000,
        "updatedBy": "<uid>",
        "autoMode": false
      },
      "relay_2": { "state": true,  "label": "Light", ... },
      "relay_3": { "state": false, "label": "Water Pump", ... }
    }
  },

  "notifications": {
    "<uid>": {
      "<notifId>": {
        "id": "abc123",
        "type": "water_level_critical",
        "title": "🚨 Critical Water Level!",
        "body": "Water level is at 92%. Immediate action required.",
        "deviceId": "device_a1b2c3d4",
        "timestamp": 1700001000000,
        "read": false
      }
    }
  },

  "activity_logs": {
    "<logId>": {
      "action": "relay_toggled",
      "userId": "<uid>",
      "deviceId": "device_a1b2c3d4",
      "timestamp": 1700001000000,
      "details": { "relayId": "relay_1", "state": true }
    }
  },

  "sensor_history": {
    "<deviceId>": {
      "water_level": {
        "1700001000000": { "value": 45, "unit": "%", "timestamp": 1700001000000 },
        "1700001010000": { "value": 47, "unit": "%", "timestamp": 1700001010000 }
      }
    }
  }
}
```

---

## 🚀 Deployment Guide

### Step 1 — Prerequisites

```bash
# Install Node.js 18+
node --version  # should be v18+

# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Select your project
firebase use --add
# (select your Firebase project from the list)
```

### Step 2 — Set Environment Variables

```bash
# Set secrets via Firebase Functions config
firebase functions:config:set \
  firebase.database_url="https://YOUR_PROJECT-default-rtdb.firebaseio.com" \
  firebase.service_account_base64="$(base64 -i serviceAccountKey.json | tr -d '\n')"

# Set device secret (used to sign ESP32 JWT tokens)
firebase functions:config:set \
  device.secret="$(openssl rand -hex 32)"

# Verify config
firebase functions:config:get
```

### Step 3 — Install Dependencies

```bash
cd functions
npm install
npm run build   # TypeScript compile check
```

### Step 4 — Deploy Everything

```bash
# Deploy database rules + all functions
firebase deploy

# Or deploy individually
firebase deploy --only database         # rules only
firebase deploy --only functions        # all functions
firebase deploy --only functions:api    # Express API only
```

### Step 5 — Enable Billing (required for outbound HTTP + Scheduled Functions)

Cloud Functions that make outbound HTTP calls or use Pub/Sub scheduling
require the **Blaze (pay-as-you-go)** plan. Enable it in the Firebase Console.

---

## 💻 Local Development with Emulator

```bash
# Start all emulators
firebase emulators:start

# Emulator UI:  http://localhost:4000
# Auth:         http://localhost:9099
# Functions:    http://localhost:5001/YOUR_PROJECT/YOUR_REGION/api
# Database:     http://localhost:9000

# In .env.local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
FIREBASE_DATABASE_EMULATOR_HOST=localhost:9000
```

---

## 📡 API Reference

### Base URL
`https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/api`

### Authentication

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/auth/register` | POST | Firebase ID Token | Create DB profile |
| `/auth/profile` | GET | Firebase ID Token | Get own profile |
| `/auth/fcm-token` | POST | Firebase ID Token | Register FCM token |
| `/auth/fcm-token` | DELETE | Firebase ID Token | Remove FCM token |
| `/auth/role` | PUT | Admin only | Change user role |

### Devices

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/devices` | POST | Admin | Register new device → returns JWT |
| `/devices` | GET | User | List all devices |
| `/devices/:id/relays` | GET | User | Get relay states |
| `/devices/:id/relays/:relayId` | PUT | User | Toggle relay |

### Sensors (ESP32)

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/sensors/:deviceId/batch` | POST | Device JWT | Upload all sensors at once |
| `/sensors/:deviceId/water` | POST | Device JWT | Water level only |
| `/sensors/:deviceId/rain` | POST | Device JWT | Rain status only |
| `/sensors/:deviceId/temp` | POST | Device JWT | Temperature + humidity |
| `/sensors/:deviceId/history/:type` | GET | Firebase Token | Read sensor history |

### Notifications

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/notifications` | GET | Firebase Token | Get inbox |
| `/notifications/:id/read` | PUT | Firebase Token | Mark as read |

---

## 📦 Example Sensor Payloads

### 1. Full Batch Upload (recommended)

```http
POST /sensors/device_a1b2c3d4/batch
X-Device-Token: eyJhbGci...
Content-Type: application/json

{
  "waterLevel": {
    "value": 67,
    "unit": "%"
  },
  "rain": {
    "rawValue": 2800,
    "isRaining": false
  },
  "tempHumidity": {
    "temperature": 29.3,
    "humidity": 78
  }
}
```

### 2. Critical Water Level (triggers alert)

```json
{
  "waterLevel": { "value": 92, "unit": "%" }
}
```
→ Cloud Function fires → FCM push to all users → Pump auto-starts

### 3. Rain Detected

```json
{
  "rain": { "rawValue": 800, "isRaining": true }
}
```
→ Cloud Function fires → "🌧️ Rain Detected" push notification

### 4. Toggle Fan ON (from Flutter app)

```http
PUT /devices/device_a1b2c3d4/relays/relay_1
Authorization: Bearer <firebase-id-token>

{ "state": true }
```
→ relay_states DB updated → ESP32 reads it within ~1 sec → Physical relay toggles

---

## 🔌 How ESP32 Connects

1. **Get a device token** — An admin calls `POST /devices` from the Flutter app.
   The response includes a JWT token. **Flash this token onto the ESP32's NVS flash.**

2. **Each upload cycle** (every 10 seconds):
   - Read sensors (ultrasonic, rain ADC, DHT22)
   - `POST /sensors/<deviceId>/batch` with `X-Device-Token: <jwt>`
   - Backend validates token, writes to DB, triggers Cloud Functions

3. **Relay polling** — ESP32 reads `relay_states/<deviceId>/<relayId>/state`
   from Firebase Realtime DB every 1 second using the FirebaseESP32 library.
   When the value changes (Flutter user toggled), the physical relay switches.

---

## 📱 How Flutter Listens to Realtime Updates

```dart
// 1. Subscribe to a stream (no polling needed!)
FirebaseDatabase.instance
  .ref('sensors/device_a1b2c3d4/water_level')
  .onValue   // ← Firebase pushes updates in real time
  .listen((event) {
    final level = event.snapshot.child('value').value;
    print('Water level: $level%');
  });

// 2. Use in StreamBuilder widget for automatic UI updates
StreamBuilder<DatabaseEvent>(
  stream: FirebaseDatabase.instance
      .ref('relay_states/device_a1b2c3d4/relay_1/state')
      .onValue,
  builder: (context, snapshot) {
    final isOn = snapshot.data?.snapshot.value as bool? ?? false;
    return Switch(value: isOn, onChanged: (v) => RelayService().setRelay(...));
  },
)
```

---

## 🔔 How FCM Notifications Work

```
ESP32 uploads sensors
        │
        ▼
Cloud Function (onWaterLevelWrite)
        │  threshold exceeded?
        ▼
FCM Service.broadcastToAllUsers()
        │
        ├─► Sends push to every user's FCM token (multicast)
        │
        └─► Saves notification to DB (notifications/<uid>/<id>)
                    │
                    ▼
            Flutter app receives in:
            - Foreground: FirebaseMessaging.onMessage
            - Background: system notification tray
            - Killed:     getInitialMessage() on next open
```

FCM notification types:
| Type | Trigger | Auto Action |
|---|---|---|
| `water_level_warning` | level ≥ 70% | none |
| `water_level_critical` | level ≥ 90% | pump auto-ON |
| `rain_detected` | intensity ≥ 30% | none |
| `device_offline` | no ping for 5min | none |
| `device_online` | ping resumes | none |
| `relay_changed` | any relay toggle (non-auto) | none |
| `temperature_high` | temp ≥ 40°C | fan auto-ON |

---

## 🔑 Security Summary

- **Mobile users** authenticate with Firebase Auth (email/password, Google, etc.)
  and send `Authorization: Bearer <id-token>` with every request.
- **ESP32 devices** use a long-lived JWT signed with `DEVICE_SECRET`,
  sent in the `X-Device-Token` header.
- **Database rules** enforce that only the device owner (or admin) can write
  device config, only the authenticated user can read their notifications,
  and sensor history is write-only to authenticated requestors.
- **Role-based access**: `admin` can register devices and change roles;
  `user` can control relays; `viewer` is read-only.
