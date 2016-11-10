int buzzer = 7;//the pin of the active buzzer
void setup()
{
pinMode(buzzer,OUTPUT);//initialize the buzzer pin as an output

}
void loop()
{

  soundBuzzer();
  delay(3000);
  
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
