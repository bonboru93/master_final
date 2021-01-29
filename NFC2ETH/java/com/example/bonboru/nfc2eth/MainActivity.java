package com.example.bonboru.nfc2eth;

import android.app.Fragment;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Fragment accountFragment = new AccountFragment();
        final Fragment transactionFragment = new TransactionFragment();
        final Fragment nodeFragment = new NodeFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.frame, accountFragment)
                .add(R.id.frame, transactionFragment).hide(transactionFragment)
                .add(R.id.frame, nodeFragment).hide(nodeFragment)
                .commit();
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_account);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            Fragment currentFragment = accountFragment;

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                getFragmentManager().beginTransaction().hide(currentFragment).commit();
                switch (item.getItemId()) {
                    case R.id.navigation_account:
                        currentFragment = accountFragment;
                        break;
                    case R.id.navigation_transaction:
                        currentFragment = transactionFragment;
                        break;
                    case R.id.navigation_node:
                        currentFragment = nodeFragment;
                        break;
                }
                getFragmentManager().beginTransaction().show(currentFragment).commit();
                return true;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if ((NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) && (EthService.account != null)) {
            intent.setClass(this, NfcService.class);
            startService(intent);
        }
    }
}
