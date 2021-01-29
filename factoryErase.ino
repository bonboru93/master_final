//Ethereum Account
#include <keccak256.h>
SHA3_CTX ctx;
#include <uECC.h>
int RNG(uint8_t *dest, unsigned size) 
{
  randomSeed(analogRead(0));
  for (unsigned i = 0; i < size; ++i) dest[i] = random(0, 0xFF);
  return 1;
}
uint8_t pubkey[64];
uint8_t address[44] = {0};
uint8_t privkey[32];
uint8_t contract[32] = {0x82,0x55,0xCD,0xd3,0xcC,0x90,0xFC,0xa3,0x9B,0xD0,0x12,0x16,0xc3,0x20,0x11,0xd8,0xe8,0xc1,0x87,0x52};

//RFID CARD 
#include <SPI.h>
#include <MFRC522.h>
#define RST_PIN 9
#define SS_PIN 10
MFRC522 mfrc522(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key oldKey;
MFRC522::MIFARE_Key factoryKey;
MFRC522::MIFARE_Key newKey;
uint8_t magic[] =
{
  0x62,0x6F,0x6E,0x62,0x6F,0x72,0x75, //bonboru
  0x62,0x6F,0x6E,0x62,0x6F,0x72,0x75, //bonboru
  0x39,0x33 //93
};
uint8_t dataTrailer[] = 
{
  0xFF,0xFF,0xFF,0xFF,0xFF,0xFF, //keyA
  0x78,0x77,0x88,0x00,           //access bit for data
  0x62,0x62,0x75,0x62,0x62,0x75  //keyB
};
uint8_t privkeyTrailer[] = 
{
  0xFF,0xFF,0xFF,0xFF,0xFF,0xFF, //keyA
  0x0F,0x00,0xFF,0x00,           //access bit for privkey
  0x62,0x62,0x75,0x62,0x62,0x75  //keyB
};
void write_card(uint8_t block, void* key, uint8_t *data)
{
  Serial.println(block);
  mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_B, block, (MFRC522::MIFARE_Key*)key, &(mfrc522.uid));
  MFRC522::StatusCode status = mfrc522.MIFARE_Write(block, data, 16);
  while (status != MFRC522::STATUS_OK);
}

void setup() 
{
  Serial.begin(9600);
  
  uECC_set_rng(&RNG);

  oldKey.keyByte[0] = 0x62; oldKey.keyByte[1] = 0x62; oldKey.keyByte[2] = 0x75;
  oldKey.keyByte[3] = 0x62; oldKey.keyByte[4] = 0x62; oldKey.keyByte[5] = 0x75;
  for (int i = 0; i < 6; i++) factoryKey.keyByte[i] = 0xFF;
  
  newKey.keyByte[0] = 0x62; newKey.keyByte[1] = 0x62; newKey.keyByte[2] = 0x75;
  newKey.keyByte[3] = 0x62; newKey.keyByte[4] = 0x62; newKey.keyByte[5] = 0x75;

  pinMode(A1, OUTPUT);
  digitalWrite(A1, HIGH);
  SPI.begin();
  mfrc522.PCD_Init();
  Serial.println("Waiting...");
}

void print_hex(uint8_t *list, unsigned size)
{
  Serial.print("0x");
  for (uint8_t *i = list; i < list + size; ++i)
  {
    if (*i < 0x10) Serial.print('0');
    Serial.print(*i,HEX);
  }
  Serial.println();
}



void loop() 
{
  if (!mfrc522.PICC_IsNewCardPresent()) return;
  if (!mfrc522.PICC_ReadCardSerial()) return;

  Serial.println("Generating...");
  
  //Generate Account
  uECC_make_key(pubkey, privkey, uECC_secp256k1());
  keccak_init(&ctx);
  keccak_update(&ctx, pubkey, 64);
  keccak_final(&ctx, address);
  Serial.print("addr=");
  print_hex(address+12, 20);
  Serial.print("privkey=");
  print_hex(privkey, 32);

  //Write Trailer
  for (uint8_t i = 3; i < 63; i += 4) write_card(i, &oldKey, dataTrailer);
  write_card(63, &oldKey, privkeyTrailer);

  //Write Contract Address
  write_card(1, &newKey, contract);
  write_card(2, &newKey, contract+16);

  //Write Magic Header
  write_card(4, &newKey, magic);

  //Write Address
  write_card(5, &newKey, address+12);
  write_card(6, &newKey, address+12+16);

  //Write Privkey
  write_card(60, &newKey, privkey);
  write_card(61, &newKey, privkey+16);

  mfrc522.PICC_HaltA();
  mfrc522.PCD_StopCrypto1();
  Serial.println("Waiting...");
}
