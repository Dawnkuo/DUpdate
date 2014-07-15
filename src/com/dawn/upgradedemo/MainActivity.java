package com.dawn.upgradedemo;

import com.dawn.upgrade.UpgradeUtils;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    UpgradeUtils upgrade = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        upgrade = new UpgradeUtils(this);
        upgrade.checkVersion();
    }
    
    @Override
    protected void onDestroy() {
        upgrade.cancel();
        super.onDestroy();
    }

 

}
