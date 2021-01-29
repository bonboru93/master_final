package com.example.bonboru.nfc2eth;

import android.app.IntentService;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

public class NfcService extends IntentService {

    public NfcService() {
        super("NfcService");
    }

    private void loadCard(MifareClassic card) throws IOException {
        byte[] blockData;

        card.connect();

        card.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);

        byte[] contractAddr = new byte[20];
        blockData = card.readBlock(1);
        System.arraycopy(blockData, 0, contractAddr, 0, 16);
        blockData = card.readBlock(2);
        System.arraycopy(blockData, 0, contractAddr, 16, 4);

        card.authenticateSectorWithKeyA(1, MifareClassic.KEY_DEFAULT);

        byte[] deviceAddr = new byte[20];
        blockData = card.readBlock(5);
        System.arraycopy(blockData, 0, deviceAddr, 0, 16);
        blockData = card.readBlock(6);
        System.arraycopy(blockData, 0, deviceAddr, 16, 4);

        card.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);

        byte[] signature = new byte[64];
        blockData = card.readBlock(8);
        System.arraycopy(blockData, 0, signature, 0, 16);
        blockData = card.readBlock(9);
        System.arraycopy(blockData, 0, signature, 16, 16);

        byte[] nonce = new byte[4];
        blockData = card.readBlock(10);
        System.arraycopy(blockData, 0, nonce, 0, 4);

        card.authenticateSectorWithKeyA(3, MifareClassic.KEY_DEFAULT);

        blockData = card.readBlock(12);
        System.arraycopy(blockData, 0, signature, 32, 16);
        blockData = card.readBlock(13);
        System.arraycopy(blockData, 0, signature, 48, 16);

        byte[] lengthBlock = card.readBlock(14);
        int length = (lengthBlock[14] * 16) + lengthBlock[15], currentBlock = 16, tail = 0;
        byte[] value = new byte[length];
        card.authenticateSectorWithKeyA(currentBlock / 4, MifareClassic.KEY_DEFAULT);
        while (length > 0) {
            if ((currentBlock + 1) % 4 == 0) {
                currentBlock++;
                card.authenticateSectorWithKeyA(currentBlock / 4, MifareClassic.KEY_DEFAULT);
            }
            blockData = card.readBlock(currentBlock);
            System.arraycopy(blockData, 0, value, tail, (length > 16) ? 16 : length);
            length -= 16;
            currentBlock++;
            tail += 16;
        }

        Intent intent = new Intent("nfc.newTransaction");
        intent.putExtra("contractAddr", contractAddr)
                .putExtra("deviceAddr", deviceAddr)
                .putExtra("nonce", nonce)
                .putExtra("signature", signature)
                .putExtra("length", lengthBlock)
                .putExtra("value", value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            switch (intent.getAction()) {
                case "android.nfc.action.TECH_DISCOVERED":
                    loadCard(MifareClassic.get((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)));
                    break;
            }

        } catch (Exception e) {
        }
    }
}
