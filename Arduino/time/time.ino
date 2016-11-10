
long int currentTime;

void setup()  {
  Serial.begin(9600);
  currentTime = millis();
}

void loop(){    
  Serial.println((millis()-currentTime)/1000);
}

