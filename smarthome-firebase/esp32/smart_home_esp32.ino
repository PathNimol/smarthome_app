/**
 * smart_home_esp32.ino
 * ─────────────────────────────────────────────────────────────
 * ESP32 Firmware — Smart Home IoT Sensor Node
 *
 * Hardware:
 *   - ESP32 DevKit v1 (or any ESP32 board)
 *   - HC-SR04 ultrasonic sensor   → water level  (GPIO 5 TRIG, 18 ECHO)
 *   - Rain sensor module           → GPIO 34 (ADC)
 *   - DHT22 temperature/humidity  → GPIO 4
 *   - Relay module (4-ch)         → GPIO 26 (Fan), 27 (Light), 14 (Pump)
 *
 * Libraries required (install via Arduino Library Manager):
 *   - ArduinoJson      (Benoit Blanchon)
 *   - DHT sensor library (Adafruit)
 *   - HTTPClient       (built into ESP32 Arduino core)
 *   - FirebaseESP32    (Mobizt) — for direct Realtime DB reads
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <FirebaseESP32.h>
#include <Preferences.h>   // NVS for storing device token

// ── Configuration ─────────────────────────────────────────────

// WiFi
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Backend (Firebase Cloud Functions)
const char* API_BASE_URL  = "https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/api";
const char* DEVICE_ID     = "device_xxxxxxxxxxxxxxxx";   // from /devices POST
const char* DEVICE_TOKEN  = "";   // stored in NVS after first flash

// Firebase (direct Realtime DB for relay state listening)
#define FIREBASE_HOST     "YOUR_PROJECT_ID-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH     "YOUR_DATABASE_SECRET_OR_DEVICE_TOKEN"

// ── Pin Definitions ────────────────────────────────────────────

#define TRIG_PIN     5
#define ECHO_PIN     18
#define RAIN_PIN     34    // ADC1 channel 6
#define DHT_PIN      4
#define DHT_TYPE     DHT22

#define RELAY_FAN    26
#define RELAY_LIGHT  27
#define RELAY_PUMP   14

// ── Intervals ─────────────────────────────────────────────────

#define SENSOR_UPLOAD_INTERVAL_MS  10000   // 10 seconds
#define RELAY_POLL_INTERVAL_MS      1000   //  1 second (or use Firebase listener)

// ── Globals ───────────────────────────────────────────────────

DHT          dht(DHT_PIN, DHT_TYPE);
FirebaseData fbData;
FirebaseAuth fbAuth;
FirebaseConfig fbConfig;
Preferences  prefs;

unsigned long lastUpload = 0;
unsigned long lastRelayPoll = 0;

String deviceToken = "";

// ─────────────────────────────────────────────────────────────
// SETUP
// ─────────────────────────────────────────────────────────────

void setup() {
  Serial.begin(115200);

  // Init GPIO
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  pinMode(RELAY_FAN,   OUTPUT);
  pinMode(RELAY_LIGHT, OUTPUT);
  pinMode(RELAY_PUMP,  OUTPUT);

  // Relays are active-LOW; start OFF
  digitalWrite(RELAY_FAN,   HIGH);
  digitalWrite(RELAY_LIGHT, HIGH);
  digitalWrite(RELAY_PUMP,  HIGH);

  dht.begin();

  // Load device token from NVS
  prefs.begin("smarthome", true);
  deviceToken = prefs.getString("device_token", String(DEVICE_TOKEN));
  prefs.end();

  connectWiFi();
  initFirebase();

  Serial.println("Smart Home ESP32 ready");
}

// ─────────────────────────────────────────────────────────────
// LOOP
// ─────────────────────────────────────────────────────────────

void loop() {
  unsigned long now = millis();

  // ── Upload sensor data ─────────────────────────────────────
  if (now - lastUpload >= SENSOR_UPLOAD_INTERVAL_MS) {
    lastUpload = now;
    uploadSensors();
  }

  // ── Poll relay states from Firebase ───────────────────────
  if (now - lastRelayPoll >= RELAY_POLL_INTERVAL_MS) {
    lastRelayPoll = now;
    pollRelayStates();
  }

  // Reconnect WiFi if dropped
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi lost — reconnecting...");
    connectWiFi();
  }
}

// ─────────────────────────────────────────────────────────────
// SENSOR READING
// ─────────────────────────────────────────────────────────────

/**
 * Read ultrasonic sensor and convert to water level %.
 * Tank height is set to 30 cm; adjust to match your tank.
 */
float readWaterLevelPercent() {
  const float TANK_HEIGHT_CM = 30.0;

  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  long duration = pulseIn(ECHO_PIN, HIGH, 30000);  // 30ms timeout
  if (duration == 0) return -1;  // sensor error

  float distanceCm = duration * 0.034 / 2.0;
  float waterHeight = TANK_HEIGHT_CM - distanceCm;
  float pct = (waterHeight / TANK_HEIGHT_CM) * 100.0;

  return constrain(pct, 0.0, 100.0);
}

/**
 * Read rain sensor ADC (0–4095).
 * Lower value = more rain (resistive sensor).
 */
int readRainRaw() {
  return analogRead(RAIN_PIN);
}

bool isRaining(int raw) {
  return raw < 2000;   // threshold: adjust for your sensor
}

// ─────────────────────────────────────────────────────────────
// SENSOR UPLOAD
// ─────────────────────────────────────────────────────────────

void uploadSensors() {
  if (WiFi.status() != WL_CONNECTED) return;

  float waterPct = readWaterLevelPercent();
  int   rainRaw  = readRainRaw();
  float temp     = dht.readTemperature();
  float hum      = dht.readHumidity();

  if (isnan(temp) || isnan(hum)) {
    Serial.println("DHT read failed");
    temp = 0; hum = 0;
  }

  // Build JSON payload
  DynamicJsonDocument doc(512);

  if (waterPct >= 0) {
    JsonObject wl = doc.createNestedObject("waterLevel");
    wl["value"] = (int)waterPct;
    wl["unit"]  = "%";
  }

  JsonObject rain = doc.createNestedObject("rain");
  rain["rawValue"]  = rainRaw;
  rain["isRaining"] = isRaining(rainRaw);

  JsonObject th = doc.createNestedObject("tempHumidity");
  th["temperature"] = temp;
  th["humidity"]    = hum;

  String body;
  serializeJson(doc, body);

  // POST to Cloud Functions
  String url = String(API_BASE_URL) + "/sensors/" + DEVICE_ID + "/batch";

  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Token", deviceToken);

  int code = http.POST(body);
  if (code == 200) {
    Serial.printf("Sensors uploaded (water: %.0f%%, rain: %d, temp: %.1f°C)\n",
                  waterPct, rainRaw, temp);
  } else {
    Serial.printf("Upload failed: HTTP %d\n", code);
    if (code > 0) {
      Serial.println(http.getString());
    }
  }

  http.end();
}

// ─────────────────────────────────────────────────────────────
// RELAY CONTROL (poll Firebase Realtime DB)
// ─────────────────────────────────────────────────────────────

/**
 * Reads relay states from Firebase Realtime DB.
 * The Flutter app writes to:  relay_states/<deviceId>/<relayId>/state
 * ESP32 reads those values and physically toggles the relays.
 *
 * For production, use the Firebase streaming/SSE SDK
 * so changes are pushed instantly without polling.
 */
void pollRelayStates() {
  String basePath = "/relay_states/" + String(DEVICE_ID);

  applyRelay(basePath + "/relay_1/state", RELAY_FAN,   "Fan");
  applyRelay(basePath + "/relay_2/state", RELAY_LIGHT, "Light");
  applyRelay(basePath + "/relay_3/state", RELAY_PUMP,  "Pump");
}

void applyRelay(String path, int pin, const char* label) {
  if (Firebase.getBool(fbData, path)) {
    bool desiredState = fbData.boolData();
    // Active-LOW relay: HIGH = OFF, LOW = ON
    int pinLevel = desiredState ? LOW : HIGH;
    digitalWrite(pin, pinLevel);
    // Serial.printf("  %s → %s\n", label, desiredState ? "ON" : "OFF");
  }
}

// ─────────────────────────────────────────────────────────────
// WIFI
// ─────────────────────────────────────────────────────────────

void connectWiFi() {
  Serial.printf("Connecting to WiFi: %s", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 30) {
    delay(500);
    Serial.print(".");
    retries++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nWiFi connected: %s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("\nWiFi connection failed — will retry next loop");
  }
}

// ─────────────────────────────────────────────────────────────
// FIREBASE
// ─────────────────────────────────────────────────────────────

void initFirebase() {
  fbConfig.host = FIREBASE_HOST;
  fbConfig.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&fbConfig, &fbAuth);
  Firebase.reconnectWiFi(true);
  Serial.println("Firebase initialized");
}
