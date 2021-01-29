package com.example.bonboru.nfc2eth;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class AccountFragment extends Fragment {

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ((TextView) getView().findViewById(R.id.address)).setText(intent.getStringExtra("address"));
            }
        }, new IntentFilter("eth.address"));
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ((TextView)getView().findViewById(R.id.balance)).setText(Double.toString(intent.getDoubleExtra("balance", 0)));
            }
        }, new IntentFilter("eth.balance"));
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(getActivity(), "Wrong Password", Toast.LENGTH_SHORT).show();
                checkAccount();
            }
        }, new IntentFilter("eth.wrongPassword"));
    }

    @Override
    public void onStart() {
        super.onStart();

        checkAccount();
    }

    private void checkAccount() {
        if (EthService.account == null) {
            File file = new File(getActivity().getFilesDir() + "/account.json");
            if (!file.exists()) {
                final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_create_account, null);
                new AlertDialog.Builder(getActivity())
                        .setTitle("Create New Account")
                        .setView(dialogView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String password = ((EditText) dialogView.findViewById(R.id.create_password)).getText().toString();
                                String password_confirm = ((EditText) dialogView.findViewById(R.id.create_password_confirm)).getText().toString();
                                if (!password.equals(password_confirm)) {
                                    Toast.makeText(getActivity(), "Not Match", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Intent intent = new Intent(getActivity(), EthService.class);
                                intent.setAction("account.createAccount");
                                intent.putExtra("password", password);
                                getActivity().startService(intent);
                            }
                        })
                        .setCancelable(false)
                        .show();
            } else {
                final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_unlock_account, null);
                new AlertDialog.Builder(getActivity())
                        .setTitle("Unlock Account")
                        .setView(dialogView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String password = ((EditText) dialogView.findViewById(R.id.unlock_password)).getText().toString();
                                Intent intent = new Intent(getActivity(), EthService.class);
                                intent.setAction("account.unlockAccount");
                                intent.putExtra("password", password);
                                getActivity().startService(intent);
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

}
