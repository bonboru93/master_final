package com.example.bonboru.nfc2eth;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.ethereum.geth.*;


public class EthService extends IntentService {

    public static Account account = null;

    public static class TransactionRecord {
        public byte[] contratAddr;
        public byte[] deviceAddr;
        public byte[] nonce;
        public byte[] signature;
        public byte[] length;
        public byte[] value;
        public String contractAddrStr;
        public String deviceAddrStr;
        public String time;
        public String state;

        public TransactionRecord(byte[] contratAddr, byte[] deviceAddr, byte[] nonce, byte[] signature, byte[] length, byte[] value, String contractAddrStr, String deviceAddrStr, String time, boolean isConfirm) {
            this.contratAddr = contratAddr;
            this.deviceAddr = deviceAddr;
            this.nonce = nonce;
            this.signature = signature;
            this.length = length;
            this.value = value;
            this.contractAddrStr = contractAddrStr;
            this.deviceAddrStr = deviceAddrStr;
            this.time = time;
            this.state = "Pending";
        }
    }

    public static ArrayList<TransactionRecord> transactionRecords = new ArrayList<>();
    public static ArrayList<TransactionRecord> pendingTransactionRecords = new ArrayList<>();

    private static Context ethContext;
    private static Node node;
    private static EthereumClient ethereumClient;
    private static String savedPassword;

    private static byte[] hex2bytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    private static byte[] standardContract = hex2bytes("60606040526004361061007f5763ffffffff60e060020a6000350416631d5e3fe58114610081578063435cfadc146100a6578063469e9067146101195780638aba4659146101bc5780638da5cb5b146101d2578063bf1fe42014610201578063c7fb412c14610217578063dc55509014610236578063fe173b9714610249575b005b341561008c57600080fd5b61009461025c565b60405190815260200160405180910390f35b34156100b157600080fd5b61007f60048035600160a060020a0316906024803563ffffffff169160443591606435919060a49060843590810190830135806020601f8201819004810201604051908101604052818152929190602084018383808284375094965061026295505050505050565b341561012457600080fd5b610138600160a060020a0360043516610548565b60405163ffffffff8316815260406020820181815290820183818151815260200191508051906020019080838360005b83811015610180578082015183820152602001610168565b50505050905090810190601f1680156101ad5780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b34156101c757600080fd5b61007f600435610614565b34156101dd57600080fd5b6101e5610649565b604051600160a060020a03909116815260200160405180910390f35b341561020c57600080fd5b61007f600435610658565b341561022257600080fd5b61007f600160a060020a036004351661068d565b341561024157600080fd5b6100946106cf565b341561025457600080fd5b6100946106d5565b60005481565b6001543a111561027157600080fd5b600160a060020a03851660009081526003602052604081205463ffffffff161161029a57600080fd5b600160a060020a03851660009081526003602052604090205463ffffffff908116908516116102c857600080fd5b84600160a060020a03166001858360405160e060020a63ffffffff84160281526004810182805190602001908083835b602083106103175780518252601f1990920191602091820191016102f8565b6001836020036101000a0380198251168184511617909252505050919091019350604092505050518091039020601b86866040516000815260200160405260405193845260ff9092166020808501919091526040808501929092526060840192909252608090920191516020810390808403906000865af1151561039a57600080fd5b505060206040510351600160a060020a03161480610499575084600160a060020a03166001858360405160e060020a63ffffffff84160281526004810182805190602001908083835b602083106104025780518252601f1990920191602091820191016103e3565b6001836020036101000a0380198251168184511617909252505050919091019350604092505050518091039020601c86866040516000815260200160405260405193845260ff9092166020808501919091526040808501929092526060840192909252608090920191516020810390808403906000865af1151561048557600080fd5b505060206040510351600160a060020a0316145b15156104a457600080fd5b60408051908101604090815263ffffffff861682526020808301849052600160a060020a03881660009081526003909152208151815463ffffffff191663ffffffff919091161781556020820151816001019080516105079291602001906106db565b5050600054600160a060020a03331691503a0280156108fc0290604051600060405180830381858888f19350505050151561054157600080fd5b5050505050565b60036020528060005260406000206000915090508060000160009054906101000a900463ffffffff1690806001018054600181600116156101000203166002900480601f01602080910402602001604051908101604052809291908181526020018280546001816001161561010002031660029004801561060a5780601f106105df5761010080835404028352916020019161060a565b820191906000526020600020905b8154815290600101906020018083116105ed57829003601f168201915b5050505050905082565b60045433600160a060020a0390811691161461062f57600080fd5b600254603c01421161064057600080fd5b60005542600255565b600454600160a060020a031681565b60045433600160a060020a0390811691161461067357600080fd5b600254603c01421161068457600080fd5b60015542600255565b60045433600160a060020a039081169116146106a857600080fd5b600160a060020a03166000908152600360205260409020805463ffffffff19166001179055565b60025481565b60015481565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061071c57805160ff1916838001178555610749565b82800160010185558215610749579182015b8281111561074957825182559160200191906001019061072e565b50610755929150610759565b5090565b61077391905b80821115610755576000815560010161075f565b905600a165627a7a72305820a45d3d27d24a66addd5b942e77c6259318b69ee563f97ed101eaeefe5c9e1a690029");

    public EthService() {
        super("EthService");
    }

    private void switchNode(Boolean state) throws Exception {
        if (state) {
            ethContext = new Context();

            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.setEthereumNetworkID(0x626275);
            nodeConfig.setEthereumGenesis("{\n" +
                    "  \"config\": {\n" +
                    "        \"chainId\": 1,\n" +
                    "        \"homesteadBlock\": 0,\n" +
                    "        \"eip155Block\": 0,\n" +
                    "        \"eip158Block\": 0\n" +
                    "    },\n" +
                    "  \"alloc\"      : {},\n" +
                    "  \"difficulty\" : \"0x50000\",\n" +
                    "  \"gasLimit\"   : \"0x2fefd8\"\n" +
                    "}");

            File file = new File(getFilesDir() + "/.ethereum/GethDroid/static-nodes.json");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                String bootstrapNodes = "[";
                bootstrapNodes += "\"enode://67d4663459950f7da4326f648405be80a93236e68920fb04574bde8c979dc29fe80056b78189016982598e945c0e7f29e26866d2e24fae469431a9e7a6ff64b2@192.168.0.9:30303\"";
                bootstrapNodes += "]";
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bootstrapNodes.getBytes());
                fos.close();
            }

            node = Geth.newNode(getFilesDir() + "/.ethereum", nodeConfig);
            node.start();
            ethereumClient = node.getEthereumClient();
            ethereumClient.subscribeNewHead(ethContext, newHeadHandler, 16);

            NodeInfo nodeInfo = node.getNodeInfo();

            Intent intent = new Intent("eth.start");
            intent.putExtra("client", nodeInfo.getName())
                    .putExtra("enode", nodeInfo.getEnode());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else
            node.stop();
    }

    private void createAccount(String password) throws Exception {
        KeyStore keyStore = new KeyStore(getApplicationContext().getFilesDir() + "/keystone", Geth.LightScryptN, Geth.LightScryptP);
        account = keyStore.newAccount(password);
        FileOutputStream fos = new FileOutputStream(getFilesDir() + "/account.json");
        fos.write(keyStore.exportKey(account, password, password));
        fos.close();
        savedPassword = password;

        Intent intent = new Intent("eth.address");
        intent.putExtra("address", account.getAddress().getHex());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        System.out.println(account.getAddress().getHex());
    }

    private void unlockAccount(String password) throws Exception {
        KeyStore keyStore = new KeyStore(getApplicationContext().getFilesDir() + "/keystone", Geth.LightScryptN, Geth.LightScryptP);
        FileInputStream fis = new FileInputStream(getFilesDir() + "/account.json");
        byte[] content = new byte[fis.available()];
        fis.read(content);
        fis.close();
        try {
            account = keyStore.importKey(content, password, password);
            savedPassword = password;
            Intent intent = new Intent("eth.address");
            intent.putExtra("address", account.getAddress().getHex());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("eth.wrongPassword"));
        }
        System.out.println(account.getAddress().getHex());
    }


    private void sendTransaction(TransactionRecord transactionRecord) throws Exception {
        Address contractAddr = new Address(transactionRecord.contratAddr);
        if (!Arrays.equals(ethereumClient.getCodeAt(ethContext, contractAddr, -1), standardContract)) throw new Exception();
        CallMsg callMsg = new CallMsg();
        callMsg.setFrom(account.getAddress());
        callMsg.setTo(contractAddr);
        callMsg.setData(new byte[]{(byte) 0x1d, (byte) 0x5e, (byte) 0x3f, (byte) 0xe5});
        callMsg.setGas(new BigInteger(ethereumClient.callContract(ethContext, callMsg, -1)).intValue());
        callMsg.setData(new byte[]{(byte) 0xfe, (byte) 0x17, (byte) 0x3b, (byte) 0x97});
        callMsg.setGasPrice(new BigInt(new BigInteger(ethereumClient.callContract(ethContext, callMsg, -1)).longValue()));
        byte[] data = new byte[4 + 192 + (transactionRecord.value.length / 32 + (transactionRecord.value.length % 32 > 0 ? 1 : 0)) * 32];
        data[0] = (byte) 0x43;
        data[1] = (byte) 0x5c;
        data[2] = (byte) 0xfa;
        data[3] = (byte) 0xdc;
        System.arraycopy(transactionRecord.deviceAddr, 0, data, 16, 20);
        System.arraycopy(transactionRecord.nonce, 0, data, 64, 4);
        System.arraycopy(transactionRecord.signature, 0, data, 68, 64);
        data[163] = (byte) 0xa0;
        System.arraycopy(transactionRecord.length, 0, data, 180, 16);
        System.arraycopy(transactionRecord.value, 0, data, 196, transactionRecord.value.length);
        callMsg.setData(data);
        long estimatedGas = ethereumClient.estimateGas(ethContext, callMsg);
        System.out.println(estimatedGas);
        if (estimatedGas > callMsg.getGas())
            throw new Exception();
        Transaction transaction = new Transaction(
                ethereumClient.getPendingNonceAt(ethContext, account.getAddress()),
                contractAddr,
                new BigInt(0),
                callMsg.getGas(),
                callMsg.getGasPrice(),
                data);
        KeyStore keyStore = new KeyStore(getApplicationContext().getFilesDir() + "/keystone", Geth.LightScryptN, Geth.LightScryptP);
        transaction = keyStore.signTxPassphrase(account, savedPassword, transaction, new BigInt(1));
        ethereumClient.sendTransaction(ethContext, transaction);
    }

    private void saveTransaction(byte[] contractAddr, byte[] deviceAddr, byte[] nonce, byte[] signature, byte[] length, byte[] value, String contractAddrStr, String deviceAddrStr) {
        TransactionRecord transactionRecord = new TransactionRecord(contractAddr, deviceAddr, nonce, signature, length, value, contractAddrStr, deviceAddrStr, Calendar.getInstance().getTime().toString(), false);
        transactionRecords.add(transactionRecord);
        pendingTransactionRecords.add(transactionRecord);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("eth.transactionStateChanged"));
    }

    private void getBalance() throws Exception {
        Intent intent = new Intent("eth.balance");
        intent.putExtra("balance", (new BigInteger(ethereumClient.getBalanceAt(ethContext, account.getAddress(), -1).getString(10))).doubleValue() / 1000000000000000000L);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private NewHeadHandler newHeadHandler = new NewHeadHandler() {
        @Override
        public void onError(String s) {
        }

        @Override
        public void onNewHead(Header header) {

            Intent intent = new Intent("eth.newBlock");
            intent.putExtra("number", header.getNumber())
                    .putExtra("time", header.getTime())
                    .putExtra("hash", header.getHash().getHex());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

            if (Calendar.getInstance().getTimeInMillis() > header.getTime() * 1000 + 30000)
                return;

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("eth.updated"));

            try {
                getBalance();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = pendingTransactionRecords.size() - 1; i >= 0; i--) {
                try {
                    sendTransaction(pendingTransactionRecords.get(i));
                } catch (Exception e) {
                    pendingTransactionRecords.get(i).state = "Invalid";
                    continue;
                }
                pendingTransactionRecords.get(i).state = "Sent";
            }
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("eth.transactionStateChanged"));
            pendingTransactionRecords.clear();
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            switch (intent.getAction()) {
                case "node.switch":
                    switchNode(intent.getBooleanExtra("state", false));
                    break;
                case "account.createAccount":
                    createAccount(intent.getStringExtra("password"));
                    break;
                case "account.unlockAccount":
                    unlockAccount(intent.getStringExtra("password"));
                    break;
                case "transaction.saveTransaction":
                    saveTransaction(
                            intent.getByteArrayExtra("contractAddr"),
                            intent.getByteArrayExtra("deviceAddr"),
                            intent.getByteArrayExtra("nonce"),
                            intent.getByteArrayExtra("signature"),
                            intent.getByteArrayExtra("length"),
                            intent.getByteArrayExtra("value"),
                            intent.getStringExtra("contratAddrStr"),
                            intent.getStringExtra("deviceAddrStr"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
