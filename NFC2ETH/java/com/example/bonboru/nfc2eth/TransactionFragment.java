package com.example.bonboru.nfc2eth;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class TransactionFragment extends Fragment {

    public TransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TextView textView = getView().findViewById(R.id.node_status);
                textView.setText("●  UPDATED");
                textView.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        }, new IntentFilter("eth.updated"));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = getView().findViewById(R.id.node_status);
                        textView.setText("●  OUT OF DATE");
                        textView.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                    }
                });
            }
        }, 0, 30000);
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {

            private final char[] hexArray = "0123456789ABCDEF".toCharArray();

            private String bytes2hex(byte[] bytes) {
                char[] hexChars = new char[bytes.length * 2];
                for (int j = 0; j < bytes.length; j++) {
                    int v = bytes[j] & 0xFF;
                    hexChars[j * 2] = hexArray[v >>> 4];
                    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                }
                return "0x" + new String(hexChars);
            }

            @Override
            public void onReceive(Context context, final Intent intent) {
                final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_transaction, null);
                final String contractAddrStr = bytes2hex(intent.getByteArrayExtra("contractAddr"));
                ((TextView) dialogView.findViewById(R.id.contractAddr)).setText(contractAddrStr);
                final String deviceAddrStr = bytes2hex(intent.getByteArrayExtra("deviceAddr"));
                ((TextView) dialogView.findViewById(R.id.deviceAddr)).setText(deviceAddrStr);
                ((TextView) dialogView.findViewById(R.id.nonce)).setText(bytes2hex(intent.getByteArrayExtra("nonce")));
                System.out.println(intent.getByteArrayExtra("value").length);
                ((TextView) dialogView.findViewById(R.id.length)).setText(Integer.toString(intent.getByteArrayExtra("value").length));
                ((TextView) dialogView.findViewById(R.id.signature)).setText(bytes2hex(intent.getByteArrayExtra("signature")));
                new AlertDialog.Builder(getActivity())
                        .setTitle("Sign New Transaction")
                        .setView(dialogView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                intent.putExtra("contratAddrStr", contractAddrStr)
                                        .putExtra("deviceAddrStr", deviceAddrStr)
                                        .setClass(getActivity(), EthService.class)
                                        .setAction("transaction.saveTransaction");
                                getActivity().startService(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                System.out.println("contractAddr=" + contractAddrStr);
                System.out.println("deviceAddr=" + deviceAddrStr);
                System.out.println("nonce=" + bytes2hex(intent.getByteArrayExtra("nonce")));
                System.out.println("value=" + bytes2hex(intent.getByteArrayExtra("value")));
                System.out.println("signature=" + bytes2hex(intent.getByteArrayExtra("signature")));
            }
        }, new IntentFilter("nfc.newTransaction"));
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ((BaseAdapter) ((ListView) getView().findViewById(R.id.list)).getAdapter()).notifyDataSetChanged();
            }
        }, new IntentFilter("eth.transactionStateChanged"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);
        ((ListView) view.findViewById(R.id.list)).setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return EthService.transactionRecords.size();
            }

            @Override
            public EthService.TransactionRecord getItem(int position) {
                return EthService.transactionRecords.get(EthService.transactionRecords.size() - position - 1);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.list_transaction, parent, false);
                EthService.TransactionRecord transactionRecord = getItem(position);
                ((TextView) convertView.findViewById(R.id.contractAddr)).setText(transactionRecord.contractAddrStr);
                ((TextView) convertView.findViewById(R.id.deviceAddr)).setText(transactionRecord.deviceAddrStr);
                ((TextView) convertView.findViewById(R.id.time)).setText(transactionRecord.time);
                TextView textView = convertView.findViewById(R.id.state);
                textView.setText(transactionRecord.state);
                switch (transactionRecord.state) {
                    case "Pending":
                        textView.setTextColor(getResources().getColor(R.color.colorPending));
                        break;
                    case "Invalid":
                        textView.setTextColor(getResources().getColor(R.color.colorInvalid));
                        break;
                    case "Sent":
                        textView.setTextColor(getResources().getColor(R.color.colorSent));
                }
                return convertView;
            }
        });
        return view;
    }

}
