#include <Servo.h>
#include <EEPROM.h>
#include <SoftwareSerial.h>

SoftwareSerial bt(10, 11); // RX, TX

const byte FINGER_COUNT = 4;
Servo fingers[FINGER_COUNT];
const byte servoPins[FINGER_COUNT] = {9, 8, 7, 6};

const bool invertFingerServos = true;

Servo wrist;
const int wristPin = 3;
int currentWristAngle = 90;
const int moveStep = 2;

const int muscleSensorPin = A0;
const int joyXPin = A1;
const int batteryPin = A3;
const int buttonPin = 2;

int threshold = 700;
const int thresholdAddress = 20;

bool isPreviewMode = false;
bool isHandClosed = false;
bool isMuscleFlexed = false;

unsigned long lastBatterySendTime = 0;
const unsigned long batterySendInterval = 3000;

int customSlots[3][FINGER_COUNT];
int activeSlot = 0;
int currentFingerAngles[FINGER_COUNT] = {-1, -1, -1, -1};

String inputString = "";

void setup() {
  Serial.begin(9600);
  bt.begin(9600);

  for (byte i = 0; i < FINGER_COUNT; i++) {
    fingers[i].attach(servoPins[i]);
  }

  wrist.attach(wristPin);
  wrist.write(currentWristAngle);

  pinMode(muscleSensorPin, INPUT);
  pinMode(joyXPin, INPUT);
  pinMode(batteryPin, INPUT);
  pinMode(buttonPin, INPUT_PULLUP);

  loadFromEEPROM();

  openHand();

  bt.println("T:" + String(threshold));
}

void loop() {
  int muscleValue = analogRead(muscleSensorPin);
  int batteryValue = analogRead(batteryPin);

  Serial.println(muscleValue);

  if (millis() - lastBatterySendTime > batterySendInterval) {
    lastBatterySendTime = millis();

    int batteryPercent = map(batteryValue, 650, 860, 0, 100);
    batteryPercent = constrain(batteryPercent, 0, 100);

    bt.println("B:" + String(batteryPercent));
  }

  handleJoystick();
  handleButton();
  readBluetoothCommands();
  handleEmg(muscleValue);

  delay(20);
}

void handleJoystick() {
  int joyX = analogRead(joyXPin);

  if (joyX > 600) {
    currentWristAngle -= moveStep;
  } else if (joyX < 400) {
    currentWristAngle += moveStep;
  }

  currentWristAngle = constrain(currentWristAngle, 0, 180);
  wrist.write(currentWristAngle);
}

void handleButton() {
  if (digitalRead(buttonPin) == LOW && !isPreviewMode) {
    delay(200);

    activeSlot++;
    if (activeSlot > 3) activeSlot = 0;

    EEPROM.update(0, activeSlot);
    openHand();

    while (digitalRead(buttonPin) == LOW);
  }
}

void readBluetoothCommands() {
  while (bt.available()) {
    char c = bt.read();

    if (c == '*') {
      processCommand(inputString);
      inputString = "";
    } else {
      inputString += c;
    }
  }
}

void handleEmg(int muscleValue) {
  if (isPreviewMode) return;

  if (muscleValue > threshold && !isMuscleFlexed) {
    isMuscleFlexed = true;

    if (!isHandClosed) {
      closeToActiveSlot();
      isHandClosed = true;
    } else {
      openHand();
      isHandClosed = false;
    }
  } else if (muscleValue < (threshold - 50) && isMuscleFlexed) {
    isMuscleFlexed = false;
  }
}

void processCommand(String cmd) {
  cmd.trim();

  if (cmd == "E") {
    isPreviewMode = false;
    openHand();
    return;
  }

  if (cmd == "R") {
    bt.println("T:" + String(threshold));
    return;
  }

  if (cmd == "C") {
    calibrateEmgThreshold();
    return;
  }

  if (cmd.startsWith("T:")) {
    threshold = constrain(cmd.substring(2).toInt(), 0, 1023);
    EEPROM.put(thresholdAddress, threshold);
    bt.println("T:" + String(threshold));
    return;
  }

  if (cmd.startsWith("P:")) {
    isPreviewMode = true;
    String payload = cmd.substring(2);

    int secondColon = payload.indexOf(':');

    if (secondColon != -1 && payload.indexOf(',') == -1) {
      byte fingerIndex = payload.substring(0, secondColon).toInt();
      int angle = constrain(payload.substring(secondColon + 1).toInt(), 0, 180);

      writeOneFingerAngle(fingerIndex, angle);
      return;
    }

    int angles[FINGER_COUNT];
    parseAngles(payload, angles);
    writeFingerAngles(angles);
    return;
  }

  if (cmd.startsWith("A:")) {
    activeSlot = constrain(cmd.substring(2).toInt(), 0, 3);
    EEPROM.update(0, activeSlot);
    return;
  }

  if (cmd.startsWith("W:")) {
    int colonIndex = cmd.indexOf(':', 2);

    if (colonIndex == -1) {
      bt.println("ERR:W_FORMAT");
      return;
    }

    int slotId = cmd.substring(2, colonIndex).toInt();

    if (slotId < 1 || slotId > 3) {
      bt.println("ERR:W_SLOT");
      return;
    }

    int arrayIndex = slotId - 1;
    int angles[FINGER_COUNT];

    parseAngles(cmd.substring(colonIndex + 1), angles);

    for (byte i = 0; i < FINGER_COUNT; i++) {
      customSlots[arrayIndex][i] = angles[i];
      EEPROM.update((arrayIndex * FINGER_COUNT) + i + 1, angles[i]);
    }

    return;
  }
}

void parseAngles(String data, int* outArray) {
  byte currentIndex = 0;
  int startPos = 0;

  for (int i = 0; i <= data.length(); i++) {
    if (i == data.length() || data[i] == ',') {
      if (currentIndex < FINGER_COUNT) {
        outArray[currentIndex] = constrain(data.substring(startPos, i).toInt(), 0, 180);
      }

      startPos = i + 1;
      currentIndex++;

      if (currentIndex >= FINGER_COUNT) break;
    }
  }

  while (currentIndex < FINGER_COUNT) {
    outArray[currentIndex] = 0;
    currentIndex++;
  }
}

void writeOneFingerAngle(byte fingerIndex, int angle) {
  if (fingerIndex >= FINGER_COUNT) return;

  angle = constrain(angle, 0, 180);

  if (currentFingerAngles[fingerIndex] == angle) {
    return;
  }

  int physicalAngle = angle;

  if (invertFingerServos) {
    physicalAngle = 180 - angle;
  }

  fingers[fingerIndex].write(physicalAngle);
  currentFingerAngles[fingerIndex] = angle;
}

void writeFingerAngles(int* angles) {
  for (byte i = 0; i < FINGER_COUNT; i++) {
    writeOneFingerAngle(i, angles[i]);
  }
}

void loadFromEEPROM() {
  activeSlot = EEPROM.read(0);

  if (activeSlot > 3) {
    activeSlot = 0;
  }

  for (byte slot = 0; slot < 3; slot++) {
    for (byte i = 0; i < FINGER_COUNT; i++) {
      int val = EEPROM.read((slot * FINGER_COUNT) + i + 1);
      customSlots[slot][i] = (val <= 180) ? val : 0;
    }
  }

  int savedThreshold = 0;
  EEPROM.get(thresholdAddress, savedThreshold);

  if (savedThreshold >= 0 && savedThreshold <= 1023) {
    threshold = savedThreshold;
  }
}

void closeToActiveSlot() {
  for (byte i = 0; i < FINGER_COUNT; i++) {
    if (activeSlot == 0) {
      writeOneFingerAngle(i, 180);
    } else {
      writeOneFingerAngle(i, customSlots[activeSlot - 1][i]);
    }

    delay(50);
  }
}

void openHand() {
  for (byte i = 0; i < FINGER_COUNT; i++) {
    writeOneFingerAngle(i, 0);
    delay(80);
  }
}

void calibrateEmgThreshold() {
  isPreviewMode = true;
  openHand();

  bt.println("CAL:Тримайте руку розслабленою");
  delay(700);

  long relaxedSum = 0;
  int relaxedMax = 0;
  const int relaxedSamples = 100;

  for (int i = 0; i < relaxedSamples; i++) {
    int value = analogRead(muscleSensorPin);
    relaxedSum += value;

    if (value > relaxedMax) {
      relaxedMax = value;
    }

    delay(15);
  }

  bt.println("CAL:Напружте м'яз");
  delay(700);

  long flexedSum = 0;
  int flexedMax = 0;
  const int flexedSamples = 100;

  for (int i = 0; i < flexedSamples; i++) {
    int value = analogRead(muscleSensorPin);
    flexedSum += value;

    if (value > flexedMax) {
      flexedMax = value;
    }

    delay(15);
  }

  int relaxedAvg = relaxedSum / relaxedSamples;
  int flexedAvg = flexedSum / flexedSamples;

  if (flexedAvg > relaxedAvg + 30) {
    threshold = (relaxedAvg + flexedAvg) / 2;
  } else {
    threshold = relaxedMax + 80;
  }

  threshold = constrain(threshold, 0, 1023);
  EEPROM.put(thresholdAddress, threshold);

  bt.println("T:" + String(threshold));
  bt.println("CAL:Калібрування завершено");

  isPreviewMode = false;
}