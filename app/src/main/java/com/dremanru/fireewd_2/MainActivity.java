package com.dremanru.fireewd_2;

import static com.dremanru.fireewd_2.ui.cam.CamFragment.Set_Cam_All_State;
import static com.dremanru.fireewd_2.ui.temprh.TempRHFragment.retrieve_id_success;
import static com.dremanru.fireewd_2.ui.thermal.ThermalFragment.processThermalCam;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.dremanru.fireewd_2.ui.TransparentActivity;
import com.dremanru.fireewd_2.ui.temprh.TempRHFragment;
import com.dremanru.fireewd_2.ui.thermal.CanvasView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.dremanru.fireewd_2.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    int i = 1;
    public static String id_selected;
    public static int color_theme;

    public static String[] title_EmerxNotification = {
            "FLASH EMERGENCY!!",
            "CUBICLE TEMPERATURE EMERGENCY!!",
            "CUBICLE HUMIDITY EMERGENCY!!",
            "JUNCTION HOTSPOT EMERGENCY!!",
            "FLAME EMERGENCY!!"
    };
    public static String[] text_EmerxNotification = {
            "Bright Flash has Occured in the Cubicle",
            "The cubicle chamber has unusually high room Temperature",
            "The cubicle chamber has unusually high room Humidity",
            "Hot Spot on Bus Junction may has passed the Limit",
            "Flame has Occured in the Cubicle"
    };

    //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
    public static String[] list_variablesSTR = new String[8];

    public static void Set_Stakeholder_Changes(DatabaseReference fb, boolean value){
        String path = "/" + MainActivity.id_selected + "/stakeholder_changes";
        if(MainActivity.id_selected != null) {
            fb.child(path).setValue(value);
        }
    }

    public static void Set_Constraint_Value(DatabaseReference fb, int id_parameters, float value){
        if(MainActivity.id_selected != null) {
            //Getting all parameters contraint
            String path = "/" + MainActivity.id_selected + "/DataLog";
            fb.child(path).child("All_parameters_constraint").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String c = snapshot.getValue(String.class);
                    if (c != null) {
                        list_variablesSTR = c.split(",");
                        list_variablesSTR[id_parameters] = String.valueOf(value);

                        String all_parameters = "";
                        int parameters_valid = 0;
                        for (int i = 0; i < 8; i++) {
                            if (Float.parseFloat(list_variablesSTR[i]) > 0) {
                                parameters_valid += 1;
                            }
                        }

                        if(parameters_valid == 8) {
                            for (int i = 0; i < 8; i++) {
                                if(i < 7) {
                                    all_parameters = all_parameters + list_variablesSTR[i] + ",";
                                }else{
                                    all_parameters = all_parameters + list_variablesSTR[i];
                                }
                            }
                            fb.child(path).child("All_parameters_constraint").setValue(all_parameters);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    //  0 = TH, 1 = thermal, 2 = cam
    public static String[] list_variables_cont_STR = new String[4];
    public static void Set_Cont_State(DatabaseReference fb, int id_parameters, int value /*0 = false, 1 = true*/){
        if(MainActivity.id_selected != null) {
            //Getting all parameters cont_state
            String path = "/" + MainActivity.id_selected + "/DataLog";
            fb.child(path).child("All_cont_state").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String c = snapshot.getValue(String.class);
                    if (c != null) {
                        String all_parameters = "";

                        if(id_parameters == 5){
                            all_parameters = "0,0,0,0";
                        }else{
                            list_variables_cont_STR = c.split(",");
                            list_variables_cont_STR[id_parameters] = String.valueOf(value);
                            for (int i = 0; i < 4; i++) {
                                if (i < 3) {
                                    all_parameters = all_parameters + list_variables_cont_STR[i] + ",";
                                } else {
                                    all_parameters = all_parameters + list_variables_cont_STR[i];
                                }
                            }
                        }
                        fb.child(path).child("All_cont_state").setValue(all_parameters);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    public static void Set_Cont_State_BINtoINT(DatabaseReference fb, int id_parameters, int value /*0 = false, 1 = true*/, int arr_length){
        //id_parameters: (TH, thermal, cam, ldr-flame) = (0,0,0,0) (id=0,1,2,3)

        if(MainActivity.id_selected != null) {
            //Getting all parameters cont_state
            String path = "/" + MainActivity.id_selected + "/DataLog";
            fb.child(path).child("All_cont_state_INT_BIN").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer c = snapshot.getValue(Integer.class);
                    if (c != null) {
                        int all_parameters_INT = 0;
                        String all_parameters_BIN = "";

                        if(id_parameters == 5){
                            all_parameters_INT = 0;
                        }else{
                            list_variables_cont_STR = str_INTtoBIN(c, arr_length);
                            list_variables_cont_STR[id_parameters] = String.valueOf(value);
                            for (int i = 0; i < 4; i++) {
                                all_parameters_BIN = all_parameters_BIN + list_variables_cont_STR[i];
                            }
                            all_parameters_INT = int_BINtoINT(Integer.parseInt(all_parameters_BIN));
                        }
                        fb.child(path).child("All_cont_state_INT_BIN").setValue(all_parameters_INT);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    public static String[] str_INTtoBIN(Integer num, int arr_length /*length of con_state array*/){
        String[] val_arr = new String[arr_length];
        String bin_tmp = "";
        int[] binary = new int[35];
        int id = 0;

        if(num == 0){
            for(int i = 0; i < arr_length; i++){
                val_arr[i] = "0";
            }
        }
        while(num > 0){
            binary[id++] = num % 2;
            num = num/2;
        }

        for(int i = id - 1; i >= 0; i--){
            bin_tmp = bin_tmp + String.valueOf(binary[i]);
        }
        if(id > 0){
            String s = "";
            for(int i = 0; i < arr_length-id; i++){
                s = s + "0";
            }
            s = s + bin_tmp;
            val_arr = s.split("");
        }

        return val_arr;
    }

    public static int int_BINtoINT(Integer n){
        int num = n;
        int dec_value = 0;

        // Initializing base
        // value to 1, i.e 2^0
        int base = 1;

        int temp = num;
        while (temp > 0) {
            int last_digit = temp % 10;
            temp = temp / 10;

            dec_value += last_digit * base;

            base = base * 2;
        }

        return dec_value;
    }

    public static void emergencyNotification(Activity activity, Context context, int causes, float value) {
        //causes: 0=light, 1=temp, 2=humid, 3=thermal, 4=flame
        String chanelID = "CHANNEL_ID_NOTIFICATION";
        String chanelID_causes = chanelID + "_" + String.valueOf(causes);
        String[] symbol = {"ohm", "°C", "%", "°C", "Volt"};
        String text = "";
        if (causes != 3)
            text = text_EmerxNotification[causes] + " (" + String.valueOf(value) + " " + symbol[causes] + ")";
        else
            text = text_EmerxNotification[causes];
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (activity != null) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(activity.getApplicationContext(), chanelID_causes);
            builder.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContentTitle(title_EmerxNotification[causes])
                    .setContentText(text)
                    .setSound(alarmSound)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            Intent intent = new Intent(activity.getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(),
                    0, intent, PendingIntent.FLAG_MUTABLE);
            builder.setContentIntent(pendingIntent);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel notificationChannel =
                    notificationManager.getNotificationChannel(chanelID_causes);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(chanelID_causes, "none", importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            notificationManager.notify(0, builder.build());
        }
    }

    ArrayList<String> array_fewdIDs;
    public static String[] list_emerx_constraint = new String[8];

    Spinner IDspinner;
    ImageView imgRetrievalID;
    private DatabaseReference database;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_tempRH, R.id.navigation_cam, R.id.navigation_thermal)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        IDspinner = binding.spinnerIDfewd;
        imgRetrievalID = binding.imgRetrievingFEWDID;

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorButtonNormal, typedValue, true);
        color_theme = ContextCompat.getColor(this, typedValue.resourceId);

        imgRetrievalID.startAnimation(
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.rotate_indefinitely));

        database = FirebaseDatabase.getInstance().getReference();
        array_fewdIDs = new ArrayList<>();

        database.child("/ID").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    array_fewdIDs.add(ds.getValue(String.class));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, array_fewdIDs);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                IDspinner.setAdapter(adapter);

                imgRetrievalID.clearAnimation();
                imgRetrievalID.setVisibility(View.GONE);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        IDspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                id_selected = array_fewdIDs.get(i);

                String path = "/" + MainActivity.id_selected + "/DataLog/All_parameters_constraint";
                if(MainActivity.id_selected != null){
                    database.child(path).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String c = snapshot.getValue(String.class);
                            if(c != null){
                                list_emerx_constraint = c.split(",");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }

                if(!retrieve_id_success){
                    startActivity(new Intent( MainActivity.this, TransparentActivity.class));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Set_Stakeholder_Changes(database, false);
        Set_Cont_State_BINtoINT(database, 2, 0, 4);
        Set_Cam_All_State(database, 0, 0);

        if (MainActivity.id_selected != null) {
            //Listening to Emergency State
            String path = "/" + MainActivity.id_selected + "/DataLog/All_emerx_state";
            database.child(path).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean emerx_light = false, emerx_flame = false;
                    boolean emerx_temp = false, emerx_humid = false;
                    boolean emerx_thermal = false;

                    String c = snapshot.getValue(String.class);

                    if(c != null && c.length() == 9) {
                        char[] emerx_state_charArr = c.toCharArray();
                        if(emerx_state_charArr[0] == '1')
                            emerx_light = true;
                        if(emerx_state_charArr[2] == '1')
                            emerx_flame = true;
                        if(emerx_state_charArr[4] == '1')
                            emerx_temp = true;
                        if(emerx_state_charArr[6] == '1')
                            emerx_humid = true;
                        if(emerx_state_charArr[8] == '1')
                            emerx_thermal = true;
                    }

                    if (emerx_light) {
                        String path = "/" + MainActivity.id_selected + "/DataLog/Light/emergency";
                        database.child(path + "/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                emergencyNotification(MainActivity.this, getApplicationContext(), 0, c);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    if (emerx_flame) {
                        String path = "/" + MainActivity.id_selected + "/DataLog/Flame/emergency";
                        database.child(path + "/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                emergencyNotification(MainActivity.this, getApplicationContext(), 4, c);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    if (emerx_temp) {
                        String path = "/" + MainActivity.id_selected + "/DataLog/Temp/emergency";
                        database.child(path + "/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                emergencyNotification(MainActivity.this, getApplicationContext(), 1, c);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    if (emerx_humid) {
                        String path = "/" + MainActivity.id_selected + "/DataLog/Humid/emergency";
                        database.child(path + "/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                emergencyNotification(MainActivity.this, getApplicationContext(), 2, c);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    if (emerx_thermal) {
                        String path = "/" + MainActivity.id_selected + "/DataLog/Thermal/emergency";
                        database.child(path + "/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String c = snapshot.getValue(String.class);
                                processThermalCam(c);
                                emergencyNotification(MainActivity.this, getApplicationContext(), 3, CanvasView.highest_temp);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        String path = "/" + MainActivity.id_selected + "/DataLog/Cam/image_base64";
        database.child(path+"/part1").setValue("");
        database.child(path+"/part2").setValue("");
        database.child(path+"/part3").setValue("");
        database.child(path+"/finish_state").setValue(false);

    }
}