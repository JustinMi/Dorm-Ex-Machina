#include <MFRC522.h>
#include <Time.h>
#include <SPI.h>

#define SS_PIN 10
#define RST_PIN 5
#define uchar unsigned char
#define uint unsigned int

uchar fifobytes;
uchar fifoValue;

MFRC522 mfrc522(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key key;

byte b[10];
byte s = sizeof(b);

int led = 8;
int buzzer = 7;

int doorSensor = 2;
int motionSensor = 4;

int motionSensorState = LOW;
int motionVal = 0;
int doorVal = 0;
int rfidVal = 1;
int buzzerSounded = 0;
int doorOpen = 0;

unsigned long timerRFID;
unsigned long timerBT;

byte wilson[4] = {196,235,178,235};
#define MAX_LEN 16

int isWalkingCloser = 0;

void setup() {
  Serial.begin(9600);
//  SPI.begin();
//  mfrc522.PCD_Init();

  pinMode(doorSensor, INPUT);
  digitalWrite(doorSensor, HIGH);
  pinMode(motionSensor, INPUT);

  pinMode(buzzer, OUTPUT);
  digitalWrite(buzzer,HIGH);

  pinMode(led, OUTPUT);
  digitalWrite(led, LOW);

//  timerRFID = millis();
  timerBT = millis();
}
int tmpRfidVal;
void loop() {
  motionVal = digitalRead(motionSensor);
  doorVal = digitalRead(doorSensor);
  
//  tmpRfidVal = readRFID();
//  if(tmpRfidVal == 0 || tmpRfidVal == 1) {
//    rfidVal = tmpRfidVal;
//  }
  readBT();
  
  if (!doorVal) {
    buzzerSounded = 0;
  }

  if (doorVal && rfidVal && !buzzerSounded) {
    soundBuzzer();
    buzzerSounded = 1;
  }
}

int readRFID() {
  if((millis() - timerRFID) > 1000) {
    if(!mfrc522.PICC_IsNewCardPresent()) {
      return 0;
    }
    if(!mfrc522.PICC_ReadCardSerial()) {
      return 0;
    }
    timerRFID = millis();
    return 1;
  }
  return -1;
}

int authenticate(byte *uid) {
  for (byte i=0; i<4; i++) {
    if (uid[i] != wilson[i]) {
      return 0;
    }
  }
  return 1;
}

void dump_byte_array(byte *buffer, byte bufferSize) {
  for (byte i=0; i< bufferSize; i++) {
    Serial.print(buffer[i] < 0x10 ? " 0" : " ");
    Serial.print(buffer[i], HEX);
  }
}

void soundBuzzer() {
  for (byte j=0; j<200; j++) {
    digitalWrite(buzzer, HIGH);
    delay(1);
    digitalWrite(buzzer, LOW);
    delay(1);
  }
  digitalWrite(buzzer,HIGH);
}

char inbyte;

void readBT() {
  if ((millis() - timerBT) > 2000 && Serial.available() > 0) {
    inbyte = Serial.read();
    if (inbyte == '2') {
      Serial.print('#');
      Serial.print(rfidVal);
      Serial.print('~');
      Serial.println();
      delay(10);

      isWalkingCloser = 0;
      digitalWrite(led, LOW);
    } else if (inbyte == '3') {
      Serial.print('#');
      Serial.print(rfidVal);
      Serial.print('~');
      Serial.println();
      delay(10);

      isWalkingCloser = 1;
      digitalWrite(led, HIGH);
    }
    timerBT = millis();
  }
}

