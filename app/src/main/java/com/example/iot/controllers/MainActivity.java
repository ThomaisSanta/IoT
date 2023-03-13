package com.example.iot.controllers;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iot.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.os.Bundle;
import android.os.Handler;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TabLayoutMediator.TabConfigurationStrategy, MqttCallback,  LocationListener {
    ViewPager2 viewPager2 ;
    TabLayout tabLayout;
    private String coordinates_number;
    private Double latitude ;
    private Double longitude ;
    private boolean gps ;
    ViewPager2Adapter viewPager2Adapter = new ViewPager2Adapter(this);
    ArrayList<String> FragmentTitles = new ArrayList<String>();
    ArrayList<Fragment> fragmentList = new ArrayList<>();
    private static final String KEY_PRIVATE = "private";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener;
    public static final String deviceID = "DeviceIdKey";
    public static final String serverIP = "ServerIpKey";
    public static final String serverPort = "ServerPortKey";
    private boolean privateMode;
    private TextView preferenceTextView;
    private LocationRequest locationRequest;
    private LocationManager locationManager;
    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    SmokeFragment smokeFragment ;
    GasFragment gasFragment;
    TemperatureFragment temperatureFragment;
    UVFragment uvFragment;
    ArrayList<IoT> IoTArrayList = new ArrayList<>();
    private int index=1;
    private int temperature_index;
    private int uv_index;
    Handler handler = new Handler();
    Runnable runnable;
    private int delay=1000;
    private boolean connect_status=false;
    MqttAsyncClient client;
    String clientId = "";
    String iotTopic = "iot";
    int qos = 2;
    String broker = "";
    int timeout = 5000;
    String ServerIP = "";
    String ServerPort = "";
    String DeviceConnectID = "";
    LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager2 = findViewById(R.id.viewPager2);
        tabLayout = findViewById(R.id.tabLayout);
        createTab("Smoke sensor");
        createTab("Gas sensor");
        new TabLayoutMediator(tabLayout, viewPager2, this).attach();
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gps = sharedPreferences.getBoolean("auto_mode",false) ;
        Boolean switchPref = sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_EXAMPLE_SWITCH, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        sharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                privateMode = sharedPreferences.getBoolean(KEY_PRIVATE, false);
                if( key.equals("clientSessionId")) {
                    editor.putString(deviceID,key) ;
                    editor.commit();
                }
                else if (key.equals("server_port")){
                    editor.putString(serverPort,key);
                    editor.commit();
                }
                else if (key.equals("server_ip")){
                    editor.putString(serverIP,key);
                    editor.commit();
                }
                else if(key.equals("auto_mode")) {
                    gps = sharedPreferences.getBoolean("auto_mode",false) ;
                    if(gps) {
                        getGPSLocation();
                    }else{
                        coordinates_number = sharedPreferences.getString("manual_mode","");
                        find_manual_coordinates(coordinates_number);
                    }
                }
                else if(key.equals("manual_mode")) {
                    coordinates_number = sharedPreferences.getString("manual_mode","");
                    find_manual_coordinates(coordinates_number);
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener);
        privateMode = sharedPreferences.getBoolean(KEY_PRIVATE, false);
    }

    //connection lost
    @Override
    public void connectionLost(Throwable cause) {
        try{
            connect_status=false;
            Log.d("mqtt","Connection to broker lost! Cause: " + cause.getMessage());
            cause.printStackTrace();
            client.reconnect();

        } catch (MqttException e) {
            Log.d("mqtt",e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d("mqtt", "Message from topic : " + message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d("mqtt",String.format("Message %d was delivered.", token.getMessageId()));
    }

    public void find_manual_coordinates(String coord_num) {
        if(coord_num.equals("1")){
            latitude =  37.96809452684323 ;
            longitude = 23.76630586399502 ;
        }else if(coord_num.equals("2")) {
            latitude = 37.96799937191987 ;
            longitude = 23.766603589104385 ;
        }else if(coord_num.equals("3")) {
            latitude = 37.967779456380754 ;
            longitude = 23.767174897611685 ;
        }else if(coord_num.equals("4")) {
            latitude = 37.96790421900921 ;
            longitude = 23.76626294807113 ;
        }
        Toast.makeText(getApplicationContext(),"Coordinates are:  "+latitude+"  "+longitude,Toast.LENGTH_LONG).show();
    }


    @Override
    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
        tab.setText(FragmentTitles.get(position));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu_main) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu_main);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.new_sensor:
                Intent intent_sensor = new Intent(MainActivity.this, NewSensorActivity.class);
                someActivityResultLauncher.launch(intent_sensor);
                return true;
            case R.id.session:
                //connect to server
                ServerIP = sharedPreferences.getString("server_ip","");
                ServerPort = sharedPreferences.getString("server_port","");
                broker = "tcp://"+ServerIP+":"+ServerPort;
                try{
                    client = new MqttAsyncClient(broker, clientId, new MemoryPersistence());
                    client.setCallback(this);
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setAutomaticReconnect(true);
                    Log.d("mqtt", "Connecting to broker: "+broker);
                    IMqttToken connectToken = client.connect(connOpts);
                    connectToken.waitForCompletion(timeout);
                    Log.d("mqtt", "Connected");
                    connect_status = true;
                } catch (MqttException e) {
                    Log.d("mqtt", e.getMessage());
                }
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.reset_settings:
                // check which session should be restored
                // call function which is yet to be implemented to reset settings
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
                Toast.makeText(getApplicationContext(), "Settings have been reset", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.exit_app:
                // call function to ask user if he wants to exit
                new AlertDialog.Builder(this)
                        .setTitle("Exit")
                        .setMessage("Do you want to exit the app?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.super.onBackPressed();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void createTab(String TabName) {
        viewPager2.setOffscreenPageLimit(4);
        if(TabName.equals("Smoke sensor")) {
            FragmentTitles.add("Smoke") ;
            fragmentList.add(new SmokeFragment());
            IoTArrayList.add(new IoT("Smoke")) ;
            FragmentManager fragmentManagerSlider  = getSupportFragmentManager();
            FragmentTransaction fragmentTransactionSlider = fragmentManagerSlider.beginTransaction();
            smokeFragment = new SmokeFragment() ;
            fragmentTransactionSlider.add(smokeFragment,"keys") ;
            fragmentTransactionSlider.commit();
        }else if(TabName.equals("Gas sensor")){
            FragmentTitles.add("Gas") ;
            fragmentList.add(new GasFragment());
            IoTArrayList.add(new IoT("Gas")) ;
            FragmentManager fragmentManagerSlider  = getSupportFragmentManager();
            FragmentTransaction fragmentTransactionSlider = fragmentManagerSlider.beginTransaction();
            gasFragment = new GasFragment() ;
            fragmentTransactionSlider.add(gasFragment,"keys") ;
            fragmentTransactionSlider.commit();
        }else if(TabName.equals("Temperature sensor")) {
            FragmentTitles.add("Temperature") ;
            fragmentList.add(new TemperatureFragment());
            temperature_index = ++index ;
            IoTArrayList.add(temperature_index,new IoT("Temperature")) ;
            FragmentManager fragmentManagerSlider  = getSupportFragmentManager();
            FragmentTransaction fragmentTransactionSlider = fragmentManagerSlider.beginTransaction();
            temperatureFragment = new TemperatureFragment() ;
            fragmentTransactionSlider.add(temperatureFragment,"keys") ;
            fragmentTransactionSlider.commit();
        }else if(TabName.equals("UV sensor")) {
            FragmentTitles.add("UV") ;
            fragmentList.add(new UVFragment());
            uv_index=++index;
            IoTArrayList.add(uv_index,new IoT("UV")) ;
            FragmentManager fragmentManagerSlider  = getSupportFragmentManager();
            FragmentTransaction fragmentTransactionSlider = fragmentManagerSlider.beginTransaction();
            uvFragment = new UVFragment() ;
            fragmentTransactionSlider.add(uvFragment,"keys") ;
            fragmentTransactionSlider.commit();
        }
        viewPager2Adapter.setFragments(fragmentList);
        viewPager2.setAdapter(viewPager2Adapter);
        new TabLayoutMediator(tabLayout, viewPager2, this).attach();
    }

    public void getSmokeSlider(float fl) {
        Log.d("SLIDER MESSAGE", String.valueOf(fl));
        IoTArrayList.get(0).setSliderValue(fl);
    }

    public void getSmokeSendData(boolean send_data) {
        Log.d("SEND DATA",String.valueOf(send_data));
        IoTArrayList.get(0).setSendData(send_data);
    }

    public void getGasSlider(float fl) {
        Log.d("SLIDER MESSAGE", String.valueOf(fl));
        IoTArrayList.get(1).setSliderValue(fl);
    }

    public void getGasSendData(boolean send_data) {
        Log.d("SEND DATA",String.valueOf(send_data));
        IoTArrayList.get(1).setSendData(send_data);
    }

    public void getTemperatureSlider(float fl) {
        Log.d("SLIDER MESSAGE", String.valueOf(fl));
        IoTArrayList.get(temperature_index).setSliderValue(fl);
    }

    public void getTemperatureSendData(boolean send_data) {
        Log.d("SEND DATA",String.valueOf(send_data));
        IoTArrayList.get(temperature_index).setSendData(send_data);
    }

    public void getUVSlider(float fl) {
        Log.d("SLIDER MESSAGE", String.valueOf(fl));
        IoTArrayList.get(uv_index).setSliderValue(fl);
    }

    public void getUVSendData(boolean send_data) {
        Log.d("SEND DATA",String.valueOf(send_data));
        IoTArrayList.get(uv_index).setSendData(send_data);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode()== 78) {
                        Intent intent = result.getData();
                        String data = intent.getStringExtra("result");
                        createTab(data);
                    }
                }
            });

    @Override
    public void onLocationChanged(Location location) {
        try {
            Log.i("location", location.getLongitude() + "," + location.getLatitude());
            this.longitude = location.getLongitude();
            this.latitude = location.getLatitude();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getGPSLocation(){
        try {
            Log.i("location", "START");
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 10, MainActivity.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        handler.postDelayed(runnable=new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(runnable,delay);
                if(connect_status) {
                    try {
                        Log.d("PUBLISH", "publish");
                        publish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        },delay);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    public void publish() throws JSONException {
        JSONArray IoTArray = new JSONArray();
        JSONObject publishObject = new JSONObject();
        publishObject.put("Latitude",latitude);
        publishObject.put("Longitude",longitude);
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int battery_percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            publishObject.put("Battery",battery_percentage);
        }
        //publish unique id
        for(int i=0;i<IoTArrayList.size();i++) {
            JSONObject IoTObject = new JSONObject();
            if(IoTArrayList.get(i).getSendData()){
                IoTObject.put("Sensor Type",IoTArrayList.get(i).getType());
                IoTObject.put("Slider Value",IoTArrayList.get(i).getSliderValue());
                IoTArray.put(IoTObject);
            }
        }
        publishObject.put("IoT",IoTArray);
        MqttMessage message = new MqttMessage(String.valueOf(publishObject).getBytes());
        try{
            DeviceConnectID = sharedPreferences.getString("clientSessionId","");
            client.publish("iot/"+DeviceConnectID, message);
        }catch(MqttException ex){
            Log.d("exception\n", ex.getMessage());
        }
        Log.d("PUBLISH DATA\n", String.valueOf(publishObject));
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Exit");
        builder.setMessage("Do you want to exit the app?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user pressed "yes", then he is allowed to exit from application
                finish();
            }
        });
        builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user select "No", just cancel this dialog and continue with app
                dialog.cancel();
            }
        });
        AlertDialog alert=builder.create();
        alert.show();
    }
}