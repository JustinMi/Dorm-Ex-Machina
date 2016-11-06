#include <SPI.h>
#include <MFRC522.h>

#define SS_PIN 10
#define RST_PIN 5
MFRC522 mfrc522(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key key;

unsigned long currentTime = 0;

byte nuidPICC[3];

byte b[10];
byte s = sizeof(b);
byte result;
unsigned long timerRFID;
int rfidVal = 0;
void setup() {
  Serial.begin(9600);
  SPI.begin();
  mfrc522.PCD_Init();

  currentTime = millis();
  timerRFID = millis();
}

void loop() {
   Serial.println(readRFID());
  
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
  return 1;
}

void dump_byte_array(byte *buffer, byte bufferSize) {
  for (byte i=0; i< bufferSize; i++) {
    Serial.print(buffer[i] < 0x10 ? " 0" : " ");
    Serial.print(buffer[i], HEX);
  }
}

