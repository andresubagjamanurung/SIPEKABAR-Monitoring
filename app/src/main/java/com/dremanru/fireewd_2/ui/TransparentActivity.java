package com.dremanru.fireewd_2.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.dremanru.fireewd_2.MainActivity;
import com.dremanru.fireewd_2.R;
import com.dremanru.fireewd_2.ui.cam.CamFragment;
import com.dremanru.fireewd_2.ui.temprh.TempRHFragment;

public class TransparentActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle arg0){
        super.onCreate(arg0);
        setContentView(R.layout.transparent_layout);

        TempRHFragment.retrieve_id_success = true;

        startActivity(new Intent(this, MainActivity.class));
    }
}
