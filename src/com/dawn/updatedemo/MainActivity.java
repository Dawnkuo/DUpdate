package com.dawn.updatedemo;

import com.dawn.update.UpdateUtils;
import com.dawn.updatedemo.R;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    UpdateUtils mUpdate = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUpdate = new UpdateUtils(this);
        mUpdate.checkVersion();
    }
    
    @Override
    protected void onDestroy() {
        mUpdate.cancel();
        super.onDestroy();
    }
}
