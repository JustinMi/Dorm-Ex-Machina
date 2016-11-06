#include <AddicoreRFID.h>
#include <Time.h> 
#include <SPI.h>

#define  uchar unsigned char
#define uint  unsigned int

uchar fifobytes;
uchar fifoValue;

AddicoreRFID myRFID; // create AddicoreRFID object to control the RFID module

const int chipSelectPin = 10;
const int NRSTPD = 5;

int wilson = 8;
int justin = 7;

unsigned long int currentTime1,currentTime2;

int p1,p2 = 0;

#define MAX_LEN 16

void setup() {                
   Serial.begin(9600);                        // RFID reader SOUT pin connected to Serial RX pin at 9600bps 
 
  // start the SPI library:
  SPI.begin();
  
  pinMode(chipSelectPin,OUTPUT);              // Set digital pin 10 as OUTPUT to connect it to the RFID /ENABLE pin 
  digitalWrite(chipSelectPin, LOW);         // Activate the RFID reader
  pinMode(NRSTPD,OUTPUT);                     // Set digital pin 10 , Not Reset and Power-down
  digitalWrite(NRSTPD, HIGH);

  pinMode(wilson,OUTPUT);
  pinMode(justin,OUTPUT);

  myRFID.AddicoreRFID_Init();  
}

void loop()
{
  uchar i, tmp, checksum1;
  uchar status;
  uchar str[MAX_LEN];
  uchar RC_size;
  uchar blockAddr;  //Selection operation block address 0 to 63
  String mynum = "";

  //Find tags, return tag type
  status = myRFID.AddicoreRFID_Request(PICC_REQIDL, str); 
  //Anti-collision, return tag serial number 4 bytes
  status = myRFID.AddicoreRFID_Anticoll(str);
  if (status == MI_OK)
  {
    Serial.print("The tag's number is:\t");
    Serial.print(str[0]);
    Serial.print(" , ");
    Serial.print(str[1]);
    Serial.print(" , ");
    Serial.print(str[2]);
    Serial.print(" , ");
    Serial.println(str[3]);
      // Should really check all pairs, but for now we'll just use the first
      if(str[0] == 196)                    
      {
        p1 = 1;
        currentTime1 = millis();
        digitalWrite(wilson,HIGH);
        Serial.println("\nHello Wilson!\n");
      } else if(str[0] == 12) {   
        p2 = 1;
        currentTime2 = millis();
        digitalWrite(justin,HIGH);         
        Serial.println("\nHello Justin!\n");
      }
      Serial.println();
  }

  if (p1) {
    if ((millis()-currentTime1)/1000 > 5) {
      p1 = 0;
      digitalWrite(wilson,LOW);
    }
  }

  if (p2) {
    if ((millis()-currentTime2)/1000 > 5) {
      p2 = 0;
      digitalWrite(justin,LOW);
    }
  }
    
  myRFID.AddicoreRFID_Halt();      //Command tag into hibernation              

}

