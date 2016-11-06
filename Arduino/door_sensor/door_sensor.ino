const int switchPin = 2;
const int ledPin = 4;
void setup() {
  Serial.begin(9600);
  pinMode(switchPin, INPUT); 
  pinMode(ledPin, OUTPUT); 
  digitalWrite(switchPin, HIGH);
}

void loop() {
  Serial.println(digitalRead(switchPin));
  if(digitalRead(switchPin) == LOW){
    digitalWrite(ledPin, LOW);
  } else {
    digitalWrite(ledPin, HIGH);
  }
}


