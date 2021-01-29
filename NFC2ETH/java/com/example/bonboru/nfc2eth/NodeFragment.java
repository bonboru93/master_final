package com.example.bonboru.nfc2eth;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class NodeFragment extends Fragment {

    public NodeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ((TextView)getView().findViewById(R.id.client)).setText(intent.getStringExtra("client"));
                ((TextView)getView().findViewById(R.id.enode)).setText(intent.getStringExtra("enode"));
                ((TextView)getView().findViewById(R.id.latestBlock)).setText("Syncing");
            }
        }, new IntentFilter("eth.start"));
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String info = "#" + intent.getLongExtra("number", 0);
                info += "\n\n@" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(intent.getLongExtra("time", 0) * 1000);
                info += "\n\n" + intent.getStringExtra("hash");
                ((TextView)getView().findViewById(R.id.latestBlock)).setText(info);
            }
        }, new IntentFilter("eth.newBlock"));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_node, container, false);

        ((Switch)view.findViewById(R.id.onoff)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(getActivity(), EthService.class);
                intent.setAction("node.switch");
                intent.putExtra("state", isChecked);
                getActivity().startService(intent);
            }
        });
        return view;
    }

}
