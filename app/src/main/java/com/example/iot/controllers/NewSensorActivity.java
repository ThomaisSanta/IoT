package com.example.iot.controllers;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;
import com.example.iot.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class NewSensorActivity extends AppCompatActivity {
    private RadioButton smokebtn;
    private RadioButton gasbtn;
    private RadioButton temperaturebtn;
    private RadioButton uvbtn;
    private AppCompatButton newIoTbtn ;
    String newSensorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sensor);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        smokebtn=(RadioButton)findViewById(R.id.radio_smoke);
        gasbtn=(RadioButton)findViewById(R.id.radio_gas);
        temperaturebtn=(RadioButton)findViewById(R.id.radio_temperature);
        uvbtn=(RadioButton)findViewById(R.id.radio_uv);
        newIoTbtn = (AppCompatButton) findViewById(R.id.createButton) ;
        radioButtonClicked();

    }

    public void radioButtonClicked() {
        newIoTbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(smokebtn.isChecked()){
                    newSensorType = smokebtn.getText().toString();
                }else if (gasbtn.isChecked()) {
                    newSensorType = gasbtn.getText().toString();
                }else if(temperaturebtn.isChecked()) {
                    newSensorType = temperaturebtn.getText().toString();
                }else if(uvbtn.isChecked()) {
                    newSensorType = uvbtn.getText().toString();
                }
                Intent intent = new Intent();
                intent.putExtra("result",newSensorType);
                setResult(78,intent);
                NewSensorActivity.super.onBackPressed();
            }
        });
    }

}