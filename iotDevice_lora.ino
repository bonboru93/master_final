//DATA
const uint8_t header[] = "bonboru93";
const uint8_t trailer[] = "39urobnob";
const uint8_t msgSize = 2; 
uint8_t nonce_message[4 + msgSize];

//EEPROM
#include <EEPROM.h>
void eeprom_read(uint8_t pos, uint8_t* data, unsigned len)
{
  for (unsigned i = 0; i < len; ++i)
    data[i] = EEPROM.read(pos + i);
}

//LORA
#include <SoftwareSerial.h>
SoftwareSerial mySerial(10, 11);
void open_lora()
{
  digitalWrite(A1, HIGH);
}
void close_lora()
{
  digitalWrite(A1, LOW);
}

//LOW POWER
#include <LowPower.h>

//RTC
#include <DS1302.h>
#include <TimeLib.h>
DS1302 rtc(8, 7, 6);
void open_rtc()
{
  digitalWrite(A2, HIGH);
}
void close_rtc()
{
  digitalWrite(A2, LOW);
}
void get_time()
{
  Time t = rtc.time();
  setTime(t.hr, t.min, t.sec, t.date, t.mon, t.yr);
  unsigned long utc = now();
  Serial.println(utc);
  nonce_message[3] = (uint8_t)utc;
  nonce_message[2] = (uint8_t)(utc >> 8);
  nonce_message[1] = (uint8_t)(utc >> 16);
  nonce_message[0] = (uint8_t)(utc >> 24);
}

//DHT
#include <dht.h>
dht DHT;
void open_dht()
{
  digitalWrite(A3, HIGH);
}
void close_dht()
{
  digitalWrite(A3, LOW);
}
void get_dht()
{
  while(DHT.read11(5) != DHTLIB_OK);
  nonce_message[4] = DHT.temperature;
  nonce_message[5] = DHT.humidity;
}

//Ethereum Account
#include <keccak256.h>
SHA3_CTX ctx;
uint8_t hash[32];
void get_hash(uint8_t *list, unsigned size, uint8_t *dest)
{
  keccak_init(&ctx);
  keccak_update(&ctx, list, size);
  keccak_final(&ctx, dest);
}
#include <uECC.h>
uint8_t contract[20];
uint8_t address[20];
uint8_t privkey[32];
uint8_t signature[64];
int RNG(uint8_t *dest, unsigned size) 
{
  randomSeed(analogRead(0));
  for (unsigned i = 0; i < size; ++i) dest[i] = random(0, 0xFF);
  return 1;
}

void setup()
{
  pinMode(A1, OUTPUT);
  pinMode(A2, OUTPUT);
  pinMode(A3, OUTPUT);
  pinMode(3, OUTPUT);
  digitalWrite(3, LOW);
  pinMode(4, OUTPUT);
  digitalWrite(4, LOW);
  
  uECC_set_rng(&RNG);

  eeprom_read(0, contract, 20);
  eeprom_read(20, address, 20);
  eeprom_read(40, privkey, 32);

  Serial.begin(9600);
  mySerial.begin(9600);
}

uint8_t sleep_round = 0;

void loop() 
{
  LowPower.powerDown(SLEEP_8S, ADC_OFF, BOD_OFF);
  sleep_round++;
  //if (sleep_round < 10) return;
  
  open_rtc();
  open_dht();
  open_lora();
  
  get_time();
  get_dht();
  get_hash(nonce_message, 4 + msgSize, hash);
  uECC_sign(privkey, hash, 32, signature, uECC_secp256k1());
  
  mySerial.write(header, 9);
  mySerial.write(contract, 20);
  mySerial.write(address, 20);
  mySerial.write(signature, 64);
  mySerial.write(nonce_message, 4 + msgSize);
  mySerial.write(trailer, 9);
  
  close_lora();
  close_rtc();
  close_dht();
  sleep_round = 0;
}
