int led = 8;
char inbyte = 0;

void setup() {
  Serial.begin(9600);
  pinMode(led, OUTPUT);
  digitalWrite(led, LOW);
}

void loop() {  
  if (Serial.available() > 0)
  {
    inbyte = Serial.read();
    if (inbyte == '0')
    {
      digitalWrite(led,LOW);
    }
    if (inbyte == '1')
    {
      digitalWrite(led, HIGH);
    }
    if (inbyte == '2')
    {
      sendStatus();
    }
  }
  delay(2000);
}

void sendAndroidValues() {
  Serial.print('#');
  Serial.print('2');
  Serial.print('~');
  Serial.println();
  delay(10);
}

void sendStatus() {
  Serial.print('#');
  Serial.print('1');
  Serial.print('~');
  Serial.println();
  delay(10);
}

