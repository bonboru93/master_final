//EEPROM
#include <EEPROM.h>
void eeprom_write(unsigned pos, uint8_t* data, unsigned len)
{
  for (unsigned i = 0; i < len; ++i)
    EEPROM.write(pos + i, data[i]);
}
void eeprom_read(unsigned pos, uint8_t* data, unsigned len)
{
  for (unsigned i = 0; i < len; ++i)
    data[i] = EEPROM.read(pos + i);
}

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
uint8_t address[32];
uint8_t privkey[32];
uint8_t contract[20] = {0x82,0x55,0xCD,0xd3,0xcC,0x90,0xFC,0xa3,0x9B,0xD0,0x12,0x16,0xc3,0x20,0x11,0xd8,0xe8,0xc1,0x87,0x52};

void print_hex(uint8_t *data, unsigned len)
{
  Serial.print("0x");
  for (unsigned i = 0; i < len; ++i)
  {
    if (data[i] < 0x10) Serial.print('0');
    Serial.print(data[i],HEX);
  }
  Serial.println();
}

void setup() 
{
  uECC_set_rng(&RNG);
  uECC_make_key(pubkey, privkey, uECC_secp256k1());
  keccak_init(&ctx);
  keccak_update(&ctx, pubkey, 64);
  keccak_final(&ctx, address);
  
  eeprom_write(0, contract, 20);
  eeprom_write(20, address+12, 20);
  eeprom_write(40, privkey, 32);

  eeprom_read(0, contract, 20);
  eeprom_read(20, address+12, 20);
  eeprom_read(40, privkey, 32);

  Serial.begin(9600);
  Serial.print("contract=");
  print_hex(contract, 20);
  Serial.print("addr=");
  print_hex(address+12, 20);
  Serial.print("privkey=");
  print_hex(privkey, 32);
}

void loop() {}
