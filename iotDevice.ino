//DATA
const uint8_t len[16] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2};
const uint8_t msgSize = 2; 
uint8_t nonce_message[4 + msgSize];

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
  delay(1000);
}
void close_dht()
{
  digitalWrite(A3, LOW);
}
void get_dht()
{
  DHT.read11(5);
  nonce_message[4] = DHT.temperature;
  nonce_message[5] = DHT.humidity;
}

//RFID CARD 
#include <SPI.h>
#include <MFRC522.h>
MFRC522 mfrc522(10, 9);
MFRC522::MIFARE_Key key;
MFRC522::StatusCode status;
void open_card()
{
  digitalWrite(A1, HIGH);
  SPI.begin();
  mfrc522.PCD_Init();
  while (!mfrc522.PICC_IsNewCardPresent());
  while (!mfrc522.PICC_ReadCardSerial());
}
void close_card()
{
  SPI.end();
  pinMode(10, INPUT);
  digitalWrite(A1, LOW);
}
void read_card(uint8_t block, uint8_t* target)
{
  Serial.println(block);
  static const uint8_t bufSize = 18;
  mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_B, block, &key, &(mfrc522.uid));
  status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(block, target, &bufSize);
  while (status != MFRC522::STATUS_OK);
}
void write_card(uint8_t block, uint8_t* data, uint8_t len)
{
  while(len > 0)
  {
    Serial.println(block);
    mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_B, block, &key, &(mfrc522.uid));
    if (len < 16)
    {
      uint8_t buf[16] = {0};
      for (uint8_t i = 0; i < len; i++) buf[i] = data[i];
      status = mfrc522.MIFARE_Write(block, buf, 16);
      while (status != MFRC522::STATUS_OK);
      break;
    }
    status = mfrc522.MIFARE_Write(block, data, 16);
    while (status != MFRC522::STATUS_OK);
    data += 16;
    len -= 16;
    block++;
    block += ((block + 1) % 4 == 0);
  }
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
uint8_t privkey[34];
uint8_t signature[64];
int RNG(uint8_t *dest, unsigned size) 
{
  randomSeed(analogRead(0));
  for (unsigned i = 0; i < size; ++i) dest[i] = random(0, 0xFF);
  return 1;
}

void setup()
{
  Serial.begin(9600);

  key.keyByte[0] = 0x62; key.keyByte[1] = 0x62; key.keyByte[2] = 0x75;
  key.keyByte[3] = 0x62; key.keyByte[4] = 0x62; key.keyByte[5] = 0x75;

  pinMode(A1, OUTPUT);
  pinMode(A2, OUTPUT);
  pinMode(A3, OUTPUT);

  open_card();
  read_card(60, privkey);
  read_card(61, privkey+16);
  write_card(14, len, 16);
  close_card();
  
  uECC_set_rng(&RNG);
}

uint8_t sleep_round = 0;

void loop() 
{
  LowPower.powerDown(SLEEP_8S, ADC_OFF, BOD_OFF);
  sleep_round++;
  //if (sleep_round < 10) return;
  
  open_rtc();
  open_dht();
  open_card();
  
  get_time();
  get_dht();
  write_card(10, nonce_message, 4);
  write_card(16, nonce_message+4, msgSize);
  get_hash(nonce_message, 4 + msgSize, hash);
  uECC_sign(privkey, hash, 32, signature, uECC_secp256k1());
  write_card(8, signature, 32);
  write_card(12, signature+32, 32);

  close_rtc();
  close_dht();
  close_card();

  sleep_round = 0;
}
