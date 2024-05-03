package com.dremanru.fireewd_2.ui.thermal;

import static com.dremanru.fireewd_2.MainActivity.Set_Constraint_Value;
import static com.dremanru.fireewd_2.MainActivity.Set_Cont_State;
import static com.dremanru.fireewd_2.MainActivity.Set_Cont_State_BINtoINT;
import static com.dremanru.fireewd_2.MainActivity.Set_Stakeholder_Changes;
import static com.dremanru.fireewd_2.MainActivity.emergencyNotification;
import static com.dremanru.fireewd_2.MainActivity.list_emerx_constraint;
import static com.dremanru.fireewd_2.ui.cam.CamFragment.Set_Cam_All_State;
import static com.dremanru.fireewd_2.ui.cam.CamFragment.Set_Cam_All_State_BINtoINT;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dremanru.fireewd_2.MainActivity;
import com.dremanru.fireewd_2.R;
import com.dremanru.fireewd_2.databinding.FragmentThermalBinding;
import com.dremanru.fireewd_2.ui.TransparentActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class ThermalFragment extends Fragment {

    private DatabaseReference database;
    StorageReference storageReference;
    ValueEventListener imageStateListener;

    ValueEventListener pixeListener;
    Button bLiveThermal, bReqHotspot, bReqEmerx, bOverlay;
    Button bPHot1, bPHot2, bCold;
    Button bHotspotSet, bMaxReadingSet, bMinReadingSet, bThermalEmerxSet;
    EditText eThermalEmerxSetting, eTempHotspot, eMinimumTemp, eMaximumTemp;
    TextView tDateHotspot, tTempEmerx, tEmerx, tEmergencyType;
    Spinner sHotspotDates, sEmergencyDates;
    public static CanvasView canvasView;
    static ImageView imgCamView, imgRetrievalCam;


    public static byte[] list_pixelBYT = new byte[68];
    public static int[] list_pixelInt = new int[68];

    int color_streamingStart, color_defaultButton;
    boolean retrieveState = true;
    boolean view_streamingThermal = false;
    boolean view_hotspot = false, view_emerx = false;
    boolean view_overlay = false;
    boolean imageState = false;
    boolean array_address_changed = false;
    String date_chosen = "00-00-00 00:00";      //for getting the emergency/hotspot cosen to be showed
    static boolean bHighOne_clicked = false, bHighTwo_clicked = false, bCold_clicked = false;
    int recordedID_selected = 0, emerxID_selected = 0;
    int array_address_selected_thermal, array_address_selected_cam;

    int minimumTemp = 15, maximumTemp = 30, hotspotTemp = 40;
    float tempEmerxLimit = 60;

    ArrayList<String> array_hotspotDatesThermal, array_hotspotPixels, array_hotspotDatesCam, array_hotspotFilesCam, array_emerxCauseCam;
    ArrayList<Integer> array_hotspotMax, array_hotspotMin;
    ArrayList<String> array_emerxDatesThermal, array_emerxPixels, array_emerxCauseThermal, array_emerxDatesCam, array_emerxFilesCam;
    ArrayList<Integer> array_emerxMax, array_emerxMin;

    private FragmentThermalBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ThermalViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ThermalViewModel.class);

        binding = FragmentThermalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        database = FirebaseDatabase.getInstance().getReference();
        bLiveThermal = binding.buttonLivethermal;
        canvasView = binding.canvasLivethermal;
        tDateHotspot = binding.textDateHotspotViewed;
        sHotspotDates = binding.spinnerHotspotDates;
        sEmergencyDates = binding.spinnerEmergencyDates;
        bReqHotspot = binding.buttonReqHotspot;
        bReqEmerx = binding.buttonReqEmergency;
        imgCamView = binding.imgviewCam;
        imgRetrievalCam = binding.imgRetrieval;
        bOverlay = binding.buttonOverlay;

        eThermalEmerxSetting = binding.editThermalEmerxLimitSetting;
        tTempEmerx = binding.textTempEmerx;
        tEmerx = binding.textEmerx;
        tEmergencyType = binding.textEmergencyType;
        bThermalEmerxSet = binding.buttonThermalEmerxLimitSet;

        eTempHotspot = binding.editTempHotspot;
        eMinimumTemp = binding.editMinimumTemp;
        eMaximumTemp = binding.editMaximumTemp;
        bHotspotSet = binding.buttonHotspotTempSet;
        bMaxReadingSet = binding.buttonMaxReadingTempSet;
        bMinReadingSet = binding.buttonMinReadingTempSet;

        bPHot1 = binding.buttonHighOne;
        bPHot2 = binding.buttonHighTwo;
        bCold = binding.buttonCold;

        //Set the cam cont state to OFF at first fragment interaction
        if(MainActivity.id_selected != null) {
            Set_Stakeholder_Changes(database, false);
            Set_Cont_State_BINtoINT(database, 5, 0, 4);
            Set_Cam_All_State_BINtoINT(database, 0, 0, 3);
        }

        color_streamingStart = ContextCompat.getColor(getContext(), R.color.streaming);
        color_defaultButton = ContextCompat.getColor(getContext(), R.color.purple_500);

        imgRetrievalCam.setVisibility(View.GONE);

        //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
        //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
        if(MainActivity.id_selected != null) {
            String path = "";
            //Filing the thermal camera emergency properties
            eThermalEmerxSetting.setText(list_emerx_constraint[6]);

            bThermalEmerxSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eThermalEmerxSetting.getText()).equals("")){
                        float c = Float.parseFloat(String.valueOf(eThermalEmerxSetting.getText()));
                        if(c > 0){
                            String path = "/" + MainActivity.id_selected + "/DataLog/Thermal/emergency/value_constraint";
                            //database.child(path).setValue(c);

                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 6, c);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Emergency Temperature Limit of the Junction need to be higher than 0", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up the emergency temperature limit", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //Listening to Emergency State of Thermal-Data Log
            path = "/" + MainActivity.id_selected + "/DataLog/All_emerx_state";
            database.child(path).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean emerx_thermal = false;
                    String c = snapshot.getValue(String.class);

                    if(c != null && c.length() == 9) {
                        char[] emerx_state_charArr = c.toCharArray();
                        if(emerx_state_charArr[8] == '1')
                            emerx_thermal = true;
                    }

                    if(emerx_thermal){
                        String path = "/" + MainActivity.id_selected + "/DataLog/Thermal/emergency";
                        database.child(path+"/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String c = snapshot.getValue(String.class);
                                processThermalCam(c);
                                emergencyNotification(getActivity(), getContext(), 3, CanvasView.highest_temp);

                                tTempEmerx.setText(String.valueOf(CanvasView.highest_temp)+ " °C");
                                tEmerx.setVisibility(View.VISIBLE);
                                tTempEmerx.setVisibility(View.VISIBLE);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }else{
                        tEmerx.setVisibility(View.GONE);
                        tTempEmerx.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
        //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
        //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY


        //HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT
        //HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT
        if(MainActivity.id_selected != null) {
            //Filing the thermal camera properties and hotspot  setting
            eTempHotspot.setText(list_emerx_constraint[7]);
            eMaximumTemp.setText(list_emerx_constraint[5]);
            eMinimumTemp.setText(list_emerx_constraint[4]);

            bHotspotSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eTempHotspot.getText()).equals("") && !String.valueOf(eMaximumTemp.getText()).equals("") && !String.valueOf(eMinimumTemp.getText()).equals("")){
                        float h = Float.parseFloat(String.valueOf(eTempHotspot.getText()));
                        float max = Float.parseFloat(String.valueOf(eMaximumTemp.getText()));
                        float min = Float.parseFloat(String.valueOf(eMinimumTemp.getText()));

                        if(max > 0 && min > 0 && h > 0 && max > min && h > min && max > h){
                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 7, h);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Suggestion: Max. Reading > Hot Spot > Min. Reading Temperature", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up hotspot point, maximum reading, and minimum reading temperature", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            bMaxReadingSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eTempHotspot.getText()).equals("") && !String.valueOf(eMaximumTemp.getText()).equals("") && !String.valueOf(eMinimumTemp.getText()).equals("")){
                        float h = Float.parseFloat(String.valueOf(eTempHotspot.getText()));
                        float max = Float.parseFloat(String.valueOf(eMaximumTemp.getText()));
                        float min = Float.parseFloat(String.valueOf(eMinimumTemp.getText()));

                        if(max > 0 && min > 0 && h > 0 && max > min && h > min && max > h){
                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 5, max);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Suggestion: Max. Reading > Hot Spot > Min. Reading Temperature", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up hotspot point, maximum reading, and minimum reading temperature", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            bMinReadingSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eTempHotspot.getText()).equals("") && !String.valueOf(eMaximumTemp.getText()).equals("") && !String.valueOf(eMinimumTemp.getText()).equals("")){
                        float h = Float.parseFloat(String.valueOf(eTempHotspot.getText()));
                        float max = Float.parseFloat(String.valueOf(eMaximumTemp.getText()));
                        float min = Float.parseFloat(String.valueOf(eMinimumTemp.getText()));

                        if(max > 0 && min > 0 && h > 0 && max > min && h > min && max > h){
                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 4, min);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Suggestion: Max. Reading > Hot Spot > Min. Reading Temperature", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up hotspot point, maximum reading, and minimum reading temperature", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        //HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT
        //HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT   HOTSPOT

        bOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = "/" + MainActivity.id_selected + "/DataLog/Cam";

                if(!view_overlay && view_streamingThermal && !view_hotspot && !view_emerx && MainActivity.id_selected != null) {
                    imgRetrievalCam.setVisibility(View.VISIBLE);
                    imgRetrievalCam.startAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                    view_overlay = true;
                    bOverlay.setText("Overlaying...");
                    bOverlay.setBackgroundColor(color_streamingStart);
                    canvasView.setAlpha(0.5F);
                    imgCamView.setAlpha(1F);


                    //Give order to camera to take picture
                    imageStateListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Boolean c = snapshot.getValue(Boolean.class);
                            imageState = c;
                            //IF FALSE THEN RETRIEVE THE "CAM_STATE0", ELSE "CAM_STATE1", this is to update image to simulate streaming camera view
                            if (imageState && MainActivity.id_selected != null)
                                processImageCam("cam_state1", "liveCam");
                            else if (!imageState && MainActivity.id_selected != null)
                                processImageCam("cam_state0", "liveCam");
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    };
                    database.child(path+"/Cam_image_state").addValueEventListener(imageStateListener);


                    /*
                    //Process the base64 image
                    path = "/" + MainActivity.id_selected + "/DataLog/Cam/image_base64";
                    database.child(path).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Boolean finishState = snapshot.child("finish_state").getValue(Boolean.class);
                            String part1 = snapshot.child("part1").getValue(String.class);
                            String part2 = snapshot.child("part2").getValue(String.class);
                            String part3 = snapshot.child("part3").getValue(String.class);

                            if(finishState) {
                                processBase64Cam(part1, part2, part3);

                                String path = "/" + MainActivity.id_selected + "/DataLog/Cam/image_base64";
                                database.child(path + "/finish_state").setValue(false);

                                retrieveState = false;
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                     */

                }else if(!view_overlay && !view_streamingThermal && view_hotspot && !view_emerx /*&& !array_address_changed*/ && MainActivity.id_selected != null){
                    Set_Cont_State_BINtoINT(database, 1, 0, 4);

                    imgRetrievalCam.setVisibility(View.VISIBLE);
                    imgRetrievalCam.startAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                    view_overlay = true;
                    bOverlay.setText("Overlaying...");
                    bOverlay.setBackgroundColor(color_streamingStart);
                    canvasView.setAlpha(0.5F);
                    imgCamView.setAlpha(1F);

                    processImageCam(array_hotspotFilesCam.get(array_address_selected_cam), "hotspot/thermal");

                }else if(!view_overlay && !view_streamingThermal && !view_hotspot && view_emerx /*&& !array_address_changed*/ && MainActivity.id_selected != null){
                    Set_Cont_State_BINtoINT(database, 1, 0, 4);

                    imgRetrievalCam.setVisibility(View.VISIBLE);
                    imgRetrievalCam.startAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                    view_overlay = true;
                    bOverlay.setText("Overlaying...");
                    bOverlay.setBackgroundColor(color_streamingStart);
                    canvasView.setAlpha(0.5F);
                    imgCamView.setAlpha(1F);

                    processImageCam(array_emerxFilesCam.get(array_address_selected_cam), "emergency/"+array_emerxCauseCam.get(array_address_selected_cam));

                }else if(view_overlay){
                    Set_Cont_State_BINtoINT(database, 1, 0, 4);

                    String path_image64 = "/" + MainActivity.id_selected + "/DataLog/Cam/image_base64";
                    database.child(path_image64+"/part1").setValue("");
                    database.child(path_image64+"/part2").setValue("");
                    database.child(path_image64+"/part3").setValue("");
                    database.child(path_image64+"/finish_state").setValue(false);

                    retrieveState = true;

                    view_overlay = false;
                    bOverlay.setText("Overlay");
                    bOverlay.setBackgroundColor(color_defaultButton);
                    canvasView.setAlpha(1F);
                    imgCamView.setAlpha(0F);
                }
            }
        });

        bLiveThermal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgRetrievalCam.setVisibility(View.VISIBLE);
                imgRetrievalCam.startAnimation(
                        AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                String path = "/" + MainActivity.id_selected + "/DataLog/Thermal";
                if(retrieveState && MainActivity.id_selected != null){
                    Set_Stakeholder_Changes(database, true);
                    Set_Cont_State_BINtoINT(database, 1, 1, 4);

                    pixeListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String c = snapshot.getValue(String.class);
                            if(c != null){
                                processThermalCam(c);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    };
                    database.child(path+"/cont").addValueEventListener(pixeListener);

                    retrieveState = false;
                    view_streamingThermal = true;
                    view_hotspot = false;
                    view_emerx = false;
                    imgCamView.setAlpha(0F);
                    canvasView.setAlpha(1F);
                    bReqHotspot.setBackgroundColor(color_defaultButton);
                    bReqHotspot.setText("Req");
                    bReqEmerx.setBackgroundColor(color_defaultButton);
                    bReqEmerx.setText("Req");
                    bOverlay.setBackgroundColor(color_defaultButton);
                    bOverlay.setText("Overlay");
                    canvasView.setVisibility(View.VISIBLE);
                    bLiveThermal.setText("Streaming...");
                    bLiveThermal.setBackgroundColor(color_streamingStart);

                }else if(!retrieveState && MainActivity.id_selected != null){
                    Set_Stakeholder_Changes(database, false);
                    Set_Cont_State_BINtoINT(database, 1, 0, 4);

                    retrieveState = true;
                    view_streamingThermal = false;
                    bLiveThermal.setText("Live Camera");
                    bLiveThermal.setBackgroundColor(color_defaultButton);
                    startActivity(new Intent(getActivity(), TransparentActivity.class));
                }
            }
        });

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        bHighOne_clicked = false;
        bHighTwo_clicked = false;
        bCold_clicked = false;

        //Set_Cont_State(database, 5, 0); //all OFF cont state
        Set_Cont_State_BINtoINT(database, 5, 0, 4);
    }

    @Override
    public void onStart() {
        super.onStart();

        retrieveState = true;
        //To reset the thermal camera value whether it's from live or recorded
        for(int i = 0; i<68; i++) {
            list_pixelBYT[i] = 0;
            list_pixelInt[i] = 0;
        }

        array_hotspotDatesThermal = new ArrayList<>();
        array_hotspotMax = new ArrayList<>();
        array_hotspotMin = new ArrayList<>();
        array_hotspotPixels = new ArrayList<>();

        array_hotspotDatesCam = new ArrayList<>();
        array_hotspotFilesCam = new ArrayList<>();

        array_emerxDatesThermal = new ArrayList<>();
        array_emerxMax = new ArrayList<>();
        array_emerxMin = new ArrayList<>();
        array_emerxPixels = new ArrayList<>();
        array_emerxCauseThermal = new ArrayList<>();

        array_emerxDatesCam = new ArrayList<>();
        array_emerxFilesCam = new ArrayList<>();
        array_emerxCauseCam = new ArrayList<>();

        if(MainActivity.id_selected != null) {
            /*----------------------------------------------------------------------------------*/
            /*         THIS IS FOR INITIALIZE RECORDED HOTSPOT IN THERMAL DIRECTORY         */
            String pathHotspotThermal = "/" + MainActivity.id_selected + "/DataLog/Thermal/hotspot";
            database.child(pathHotspotThermal).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if(!Objects.equals(ds.getKey(), "00-00-00 00:00")) {
                            array_hotspotDatesThermal.add(ds.getKey());
                            array_hotspotMax.add(ds.child("max_set").getValue(Integer.class));
                            array_hotspotMin.add(ds.child("min_set").getValue(Integer.class));
                            array_hotspotPixels.add(ds.child("pixels").getValue(String.class));
                        }
                    }
                    if(getActivity() != null) {
                        Collections.reverse(array_hotspotDatesThermal);
                        Collections.reverse(array_hotspotMax);
                        Collections.reverse(array_hotspotMin);
                        Collections.reverse(array_hotspotPixels);

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, array_hotspotDatesThermal);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sHotspotDates.setAdapter(adapter);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            /*----------------------------------------------------------------------------------*/
            /*       THIS IS FOR INITIALIZE RECORDED HOTSPOT IN VISIBLE CAMERA DIRECTORY        */
            String pathHotspotCam = "/" + MainActivity.id_selected + "/DataLog/Cam/hotspot";
            database.child(pathHotspotCam).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if(!Objects.equals(ds.getKey(), "00-00-00 00:00")) {
                            array_hotspotDatesCam.add(ds.getKey());
                            array_hotspotFilesCam.add(ds.child("captured_file").getValue(String.class));
                        }
                    }
                    Collections.reverse(array_hotspotDatesCam);
                    Collections.reverse(array_hotspotFilesCam);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            /*----------------------------------------------------------------------------------*/


            /*----------------------------------------------------------------------------------*/
            /*         THIS IS FOR INITIALIZE RECORDED EMERGENCY DATES IN THERMAL DIRECTORY         */
            String pathEmerxThermal = "/" + MainActivity.id_selected + "/DataLog/Thermal/emergency";
            database.child(pathEmerxThermal).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if(!Objects.equals(ds.getKey(), "00-00-00 00:00")) {
                            array_emerxDatesThermal.add(ds.getKey());
                            array_emerxMax.add(ds.child("max_set").getValue(Integer.class));
                            array_emerxMin.add(ds.child("min_set").getValue(Integer.class));
                            array_emerxPixels.add(ds.child("pixels").getValue(String.class));
                            array_emerxCauseThermal.add(ds.child("captured_cause").getValue(String.class));
                        }
                    }
                    if(getActivity() != null) {
                        Collections.reverse(array_emerxDatesThermal);
                        Collections.reverse(array_emerxMax);
                        Collections.reverse(array_emerxMin);
                        Collections.reverse(array_emerxPixels);
                        Collections.reverse(array_emerxCauseThermal);

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, array_emerxDatesThermal);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sEmergencyDates.setAdapter(adapter);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            /*----------------------------------------------------------------------------------*/
            /*       THIS IS FOR INITIALIZE RECORDED EMERGENCY IN VISIBLE CAMERA DIRECTORY        */
            String pathEmerxCam = "/" + MainActivity.id_selected + "/DataLog/Cam/emergency";
            database.child(pathEmerxCam).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if(!Objects.equals(ds.getKey(), "00-00-00 00:00")) {
                            array_emerxDatesCam.add(ds.getKey());
                            array_emerxFilesCam.add(ds.child("captured_file").getValue(String.class));
                            array_emerxCauseCam.add(ds.child("captured_cause").getValue(String.class));
                        }
                    }
                    Collections.reverse(array_emerxDatesCam);
                    Collections.reverse(array_emerxFilesCam);
                    Collections.reverse(array_emerxCauseCam);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            /*----------------------------------------------------------------------------------*/
        }

        sHotspotDates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(MainActivity.id_selected != null) {
                    view_hotspot = true;
                    view_emerx = false;
                    view_overlay = false;

                    bReqHotspot.setBackgroundColor(color_defaultButton);
                    bReqHotspot.setText("Req");
                    bOverlay.setBackgroundColor(color_defaultButton);
                    bOverlay.setText("Overlay");

                    imgCamView.setAlpha(0F);
                    canvasView.setAlpha(1F);
                    for(int n = 0; n<68; n++)
                        list_pixelBYT[n] = 0;
                    canvasView.postInvalidate();

                    if(view_streamingThermal) {
                        view_streamingThermal = false;
                        startActivity(new Intent(getActivity(), TransparentActivity.class));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        sEmergencyDates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(MainActivity.id_selected != null) {
                    view_hotspot = false;
                    view_emerx = true;
                    view_overlay = false;

                    String emerxType = "Type: " + array_emerxCauseThermal.get(array_address_selected_thermal);
                    tEmergencyType.setText(emerxType);

                    bReqEmerx.setBackgroundColor(color_defaultButton);
                    bReqEmerx.setText("Req");
                    bOverlay.setBackgroundColor(color_defaultButton);
                    bOverlay.setText("Overlay");

                    imgCamView.setAlpha(0F);
                    canvasView.setAlpha(1F);
                    for(int n = 0; n<68; n++)
                        list_pixelBYT[n] = 0;
                    canvasView.postInvalidate();

                    if(view_streamingThermal) {
                        view_streamingThermal = false;
                        startActivity(new Intent(getActivity(), TransparentActivity.class));
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        bReqHotspot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgRetrievalCam.setVisibility(View.VISIBLE);
                imgRetrievalCam.startAnimation(
                        AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                if(MainActivity.id_selected != null && array_hotspotDatesThermal.size() != 0) {
                    view_streamingThermal = false;
                    view_hotspot = true;
                    view_emerx = false;

                    for(int i = 0; i < array_hotspotDatesThermal.size(); i++){
                        if(Objects.equals(array_hotspotDatesThermal.get(i), sHotspotDates.getSelectedItem())){
                            array_address_selected_thermal = i;
                        }
                    }
                    for(int i = 0; i < array_hotspotDatesCam.size(); i++){
                        if(Objects.equals(array_hotspotDatesCam.get(i), sHotspotDates.getSelectedItem())){
                            array_address_selected_cam = i;
                        }
                    }
                    processThermalCam(array_hotspotPixels.get(array_address_selected_thermal));

                    String date_viewed = "(Date: " + array_hotspotDatesThermal.get(array_address_selected_thermal) + ")" + " (Tmax = " + array_hotspotMax.get(array_address_selected_thermal) + "°C, Tmin = " + array_hotspotMin.get(array_address_selected_thermal) + "°C)";
                    bReqHotspot.setBackgroundColor(color_streamingStart);
                    bReqHotspot.setText("Loaded");
                    bReqEmerx.setBackgroundColor(color_defaultButton);
                    bReqEmerx.setText("Req");
                    tDateHotspot.setText(date_viewed);
                    tDateHotspot.setVisibility(View.VISIBLE);
                }
            }
        });

        bReqEmerx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgRetrievalCam.setVisibility(View.VISIBLE);
                imgRetrievalCam.startAnimation(
                        AnimationUtils.loadAnimation(getContext(), R.anim.rotate_indefinitely));

                if(MainActivity.id_selected != null && array_emerxDatesThermal.size() != 0) {
                    view_streamingThermal = false;
                    view_hotspot = false;
                    view_emerx = true;

                    for(int i = 0; i < array_emerxDatesThermal.size(); i++){
                        if(Objects.equals(array_emerxDatesThermal.get(i), sEmergencyDates.getSelectedItem())){
                            array_address_selected_thermal = i;
                        }
                    }
                    for(int i = 0; i < array_emerxDatesCam.size(); i++){
                        if(Objects.equals(array_emerxDatesCam.get(i), sEmergencyDates.getSelectedItem())){
                            array_address_selected_cam = i;
                        }
                    }
                    processThermalCam(array_emerxPixels.get(array_address_selected_thermal));

                    String date_viewed = "(Date: " + array_emerxDatesThermal.get(array_address_selected_thermal) + ")" + " (Tmax = " + array_emerxMax.get(array_address_selected_thermal) + "°C, Tmin = " + array_emerxMin.get(array_address_selected_thermal) + "°C)";
                    bReqEmerx.setBackgroundColor(color_streamingStart);
                    bReqEmerx.setText("Loaded");
                    bReqHotspot.setBackgroundColor(color_defaultButton);
                    bReqHotspot.setText("Req");
                    tDateHotspot.setText(date_viewed);
                    tDateHotspot.setVisibility(View.VISIBLE);
                }
            }
        });

        bPHot1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bHighOne_clicked){
                    bHighOne_clicked = true;
                    bPHot1.setText("Hot 1 ✔");
                    bPHot1.setTextColor(Color.parseColor("#000000"));
                    bPHot1.setBackgroundColor(Color.parseColor("#787878"));
                    canvasView.invalidate();
                }else{
                    bHighOne_clicked = false;
                    bPHot1.setText("P:Hot 1");
                    bPHot1.setTextColor(Color.parseColor("#FFFFFF"));
                    bPHot1.setBackgroundColor(Color.parseColor("#373737"));
                    canvasView.invalidate();
                }
            }
        });

        bPHot2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bHighTwo_clicked){
                    bHighTwo_clicked = true;
                    bPHot2.setText("Hot 2 ✔");
                    bPHot2.setTextColor(Color.parseColor("#000000"));
                    bPHot2.setBackgroundColor(Color.parseColor("#787878"));
                    canvasView.invalidate();
                }else{
                    bHighTwo_clicked = false;
                    bPHot2.setText("P:Hot 2");
                    bPHot2.setTextColor(Color.parseColor("#FFFFFF"));
                    bPHot2.setBackgroundColor(Color.parseColor("#373737"));
                    canvasView.invalidate();
                }
            }
        });

        bCold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bCold_clicked){
                    bCold_clicked = true;
                    bCold.setText("Cold ✔");
                    bCold.setTextColor(Color.parseColor("#000000"));
                    bCold.setBackgroundColor(Color.parseColor("#787878"));
                    canvasView.invalidate();
                }else{
                    bCold_clicked = false;
                    bCold.setText("P:Cold");
                    bCold.setTextColor(Color.parseColor("#FFFFFF"));
                    bCold.setBackgroundColor(Color.parseColor("#373737"));
                    canvasView.invalidate();
                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void  processBase64Cam(String base64_1, String base64_2, String base64_3){
        //String datas = "%2F9j%2F4AAQSkZJRgABAQEAAAAAAAD%2F2wBDAAoHCAkIBgoJCAkLCwoMDxkQDw4ODx8WFxIZJCAmJiQgIyIoLToxKCs2KyIjMkQzNjs9QEFAJzBHTEY%2FSzo%2FQD7%2F2wBDAQsLCw8NDx0QEB0%2BKSMpPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj4%2BPj7%2FxAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJCgv%2FxAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygpKjQ1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4eLj5OXm5%2Bjp6vHy8%2FT19vf4%2Bfr%2FxAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJCgv%2FxAC1EQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYnKCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2dri4%2BTl5ufo6ery8%2FT19vf4%2Bfr%2FwAARCALQBQADASEAAhEBAxEB%2F9oADAMBAAIRAxEAPwD0PFLiuw5wooGFIaQwo70DYUUxhRikCEpDQMSlpoTFK1EwqhEZUGqU9l3SqBmbLAUPIquwINa3MxmTUTVpFmbRC1RN0p8xNhI4md8IKuw6ax%2B9WMpm8YmjBaInarIGKyuaWHUhrMtDaY7BBk1IzMupzIOOlZVw3y1QENsdhL96aS0rZPU0eYI2LCDy4%2BRzV%2BskUOopiEpDTGNNJSYDcUUAJTaQXENNxQUJTelMkSoXGaVgGeSDVZ7UdqktSKkkZHWoWX1qGjQieOoXT5aVyiHFR96dgDb3qJhS0QrWGUw0B1GU38KBdRMUzFS9BMaVpMU2RqNxTSKfMMSihITFxSVZIA1LG5DAjtWtOViHc6TTb7dGA55rW3d69RO%2Bpz2sJ1qJhmmIrSLgVUYGtegEB4NQv1oWpViE9xURpSFcY1MYVk0MjpDzWY%2FIYVx1ppqChvem44rMVxDSVmxjTQahjGGjtUPsKwnWkpIfQKUVNihKXtQISjtS8imBzSVWzJ2FpKQS1HUv86OoWEzRQMXrS0DCil1AdUsabmGKUhXNi3j8uIetWBnP4YpLfU0Xw3Fz2xQPyp2LAtzS549qmS0GNpO1UP1A5zSEjb9amxDTG5o%2FCjRIPIB703Ixn0pxfUL9BNxxgn86DS6j3Glj64o6jrRoGwDrSVKfNoJh0pO2eRVbEbidHBIzzUa7lQA9cdaW%2Bguo9ztk9ietISR9zqaHEfmHc5Pem80rdA5Rfm6nrTVyeeeaI2EnZiEZxxwe1OXgkf5FRJ22Dm0PonbTcc16BzhiiqQCGipYxDSUhi0YoGFJQtRiUGnYQAU%2FGKpCY1qjNMEMIptNMY2SJXFUZbEHpTEU3sHzwKgayl9KpMmw3%2Bz3NH9mfLyaOcLDrEC3nMUnANahjGMrUNlIYRSYqGyxaTFBRG7qnWs5meZufu1mBUuXA%2BUVlzfM1aIBg9K0dPs%2BQ78%2Fh3rOTGa%2BMUooSAdSUgDFFMY002gBKSkAlFIBppKQxhooAYVpu3imAY4phFCAikjGKqmEF6T1KvYZJbHbwapvbuBhlxWVi0yt5dRmLk8ZoNLjNlMKUvIW5DtphWnawDNoppWgQ1hUZWgjqLtxzTMUMYm2mbe9QSxuKSrELSEVQhMY6U4UxeZYtpjE4xXS2l15yA5HzV6WGleNjCpvcuUZ4roMmQyJVWVPlrSLAqMvWoCK0BsjZeaidaiWo7ERHFN7VmwI6Z9axvYGJTKl6mlxppuKzAaaTtU9RjaQ1nNXAZ7UGosK4nSkxUNBcO9HalyloPpSU7AFKaj0AKSnZALRUktC0VQ1tYKUHil6lC0dqLdRcwq0dKFYSY7HNaVjBzv9vSk2i4xNFe1SVLRYmMduKKfUsQ9etICMVfKAZ%2Bak7VMo6juHQ00fcqXclDDS5%2BWrH1EBBOOuOKToecVHLyjYbu3ApOh70PYyGnGaOozSRXQXO3qKZnC%2BtTFdhXD60q%2FhV%2Bol2DB%2FOmZ3H2qL%2B8XIDTe34U%2BTqZh9KbuOOR0qrahqO43GkB44rP7ZPUQ4xz360j4OBjoK1t0ZabZ9HU2uo5RaQ0yhKKQCUUgEopjCkNMBtOAoAd0phNUIZSVLZQlNxSGFJVoQxhTCopgMK1HtqQKV5Bldw6ipLC43r5LdQOKBonZabSGNqCScJn1qWMqHdM25ulQ3MuwbEqUFzMneqn3qtgXrK13kMa2I0CrgVn1GPpcUDsFFAhKSmMQ02gYlFSSJSUhjaSmA00lAxKYaLiCkYccUyhpHFQOtSwHAfLTJFHcUxFdoFaoXseMg1JaZUktWHrVdouKnoap3IClQslJR7is2MKU3bxRYkZtpu2gQ0rTaTiA3bTTmgkb0FNoZQmKNvFF%2BpDExSd6qLJ6Dqu2F00L%2FAOz%2FACrrw7XMTNaWOkhl3LmpeteizlGOecVCy8VSEV3SqrLz%2BFapjIWHNRUtgbImHpUZFRJlEbVHWT1QCUw9ayGMPWm1DLQhpprJjExSGonuJjTSGpZQGkrH0Cw2lpjEpKExAe1LUjCkAp6gFOxxSJb1CirKFxSVPXUYtLVXEKKd1rH7Q7Fmzg3yDPStiIAcY7VRpFEx596KSaKa0DvTTwOKOo0xD1pVptg9w%2FCmtip%2BIewhPzE%2FypoY07GYrdfm7032FSUhCd3Ldabz2BqtRsKOTzzUWIYhxSemKTV4lp9BM7uaDgYojzdDKVmxvendqNebUtMXd%2BdR9Bk8VdhSaH%2B9Rj%2FV%2BtRci%2BofN26Zpn1q9ih46%2B9Ju4HvQlchr3hCRtOPWgL71M1bQ1t3Po41HXecY4UlTYYmKXFNgGKSkMSkosAUUihtPHSrExpppoGJTakYU3FPQYlNNIQ2mtT6AMprUDInFZl1G0beYnrU3BGjazfaYMn7w60pHNAytdziFPc1SUNN870gGTzADatZsslNbBYqNzVuzszJyw4pXEbCoF6VJipGGKKZQlLSQgpKdxjaaaQDTSUAFJQAlJSGMooAaaSmFxKD6VIXG1DKmRxVDGpkdaH5oEGKMUgEC54qCW1DUWC9io9mKheyIFI0vcrPasP4T%2BVVmT5qz2LGGM1EyUyWN24pm2le4CbKYUpCG7aj21TFcNtBHFKwMZ2oxV7C2FoX1ppibNKxudpw34VtRvxXr03eJyz0kP603HNaEXImFVpFq0xLcrstQN0qgZEwqM9Kz8x3IiKYwrLYoZTSKzGRmkx3qWC0G45pp9qyYxDTcVE0hiUhrNjQhNJnIqeQobQOlIAo7UC13CkOKz6ibFNAqrjjYKKkTFpaY7hRUy1ZQtLVWsFxafHljimG7Nq3hEadADmrIFJmyHdDxSNUa7juHPWm9s0bhsJ1pcYotZ2FKwHnpTcZBpLcTY08semM0i9KLJsoTgfKBSg4GaBXGn16Un1obHzCHJFHv6U%2Fsku4u71pM8c0W0AZ%2FvYpD93pyKQrAf5UgpiVhdwzTCEbcPXg0rMTWo5wGcMeopD0zU35RtDW4zjrSKeORQEbjs9%2BlN4PU8HvQ%2FclYht3E%2BgFJz%2BGKN%2FiHu7n0l1ppFd3U5goplBSYqQFoxSGNpKsYmKdiiwxD1pKQkNPWkpFCUhpAJSUxiUygYmKaaQhhpuKLjGMKrypvXFAFCNms7of3atTXSlflNTcZUERkbc%2FSmXE2BtSqEZkslVjyaLgW7Wz8zDEcVrom0VAx%2BKMUwQlFIYlFACikpgJSVIDDTTTASikAlJQAykNABSGkMTFFMQw0hqhjCKTbUCHYo21WwxgFDdOKYEaJxzTNn8NA7imMFagaBe4qbAV3sUP3aifTe6nNHQrmKklm4zxVd7dh1FTYq5C0WKZsqAIytN2UyRpTimYpBYaRSbadhMMUnSq6kjkyDkGteyud33vzrtws9bGFRXNKMhhRXeZDWqJumKaEVXXmoCPlNaAQNUbCpkVYjPWomrJxAZimmoY2yNulMP3vbFZtAlqJR2qGX5jT0plYS1QDTSVOw%2FMSkqXIa1FptRYYlJQMWj%2BGjoQHahaSeg4xClpFaBSigEFKKu1hBS4qGLcdjJrSsoM5YipukXBXZpAetOxUnRa4tJ0qyLDRnFJuOKgbG07NG7FuJQOlXyisIevFN6VD3BA1J%2FDj%2BRoUStEHBppp8hLGn2pakTEP0pOi0m2wWg09eaXt161SbYhORwBSA54Cge9Gu5DEYc5yKP4MDoKiTuaXT0FPakxyee1PoLoJx%2BNNJI456cikvIBW6%2FQ0pGeTQT5iY9PWmetV6gfSdFdhzCYoplCUUWGLRigQ2koKEpaBjDTaYBRUDEppplDaKACm0ANpppAMxTaGMY1RGoAzr7a31qnERvAamhFme442rWbM9WIqt81XLOz3fM1Sxo1ETaMCn0gFFFACYpKQxKKACkoGJRSAjprUwGU6kMSm0CExTKYC0UgG0UxiU2gAptABS5zQAAUxlpgJjimbaVhi4prJTEJtpoFIbEdA3aqzRjdgrQIhe0jP8Aqq1oOaXKO5BJZGoWtXA6UmWmiNrdh1WoTCR2qbdRjPLpNlMgYUpuyncYBafGdjZFbQlyyMpGpb3GV96uA8c16qaeqOV6BjioyKZBDItQFeKvoMgYVCab2GR4qJulYyK3GFefpUb1NlcfkMNJWNhjabWbDUYaSoZQw0hrNjCm1N7jCkPWokCEozUgFJRYBRzSA03tYoM0tTsIXNGaN2A6igewtOxzRclFm1iLt%2BNa8a7VCgVEjeJLS96NlYr0DpSUXQW1uANMxzT8yWL3yuCKZ15pFC0GncYhOKb17UaXJuNpfxqeo5BSUMlahn0A%2BtJjvS0Q2tA%2FEAU1qd11JY3gcdzRng0r%2B6AHpjNA6ZFNfCQI3BpNuVHy7qJ6aABPOOMZppzj6VKsL0D%2BLPFIcn6VXKlqU30HfTj8KABgjIYetZvXQhXGUDuB6VexTlofSdJXUc4tFWhigUlABSUDENNqthiGioYCGm0yrCYpKkApDRcBKSgoaaSmAlMNIBlMosMYap3E2OFpMCAQfxyVQuNvmfJQhMryPVc5c1XQDQtLL%2BJq0guBxUghaKQwpDQAlJTGJRUgJSU2AlBqQIyaZmmAzNLSEFNzQUJSUAFJQISkpjCkoAKYTSGJmhaYrjqQUwCkxQFxuKKYABTcUhDajYc0gI6YRz0qhi44pGiHWpYCqNwwajktIieVoApy6evYVBLZY6ZpDuVntJFGWU1E1uw%2FhoKIvLPTFNMdPYTJEO01oxSblHNelh5XVjmqx1JqbiukxI3HNRMuKdySuRxzULimyiLtURFSUmMPJqNhWNgW4w9aZU6oY003vWRVxpHNNrNjG001mAhptIoKSpJsFJWRdwoPShSEFGaY2haKjcQd6KZWyHCgUXBPQcKeg3Hihdw6mxaRBE96tjg0m9DaIClPSoC2oe55pvU0eZbYH2FJnHNUSNA2%2FKAoHbFITz%2F8AXqbAGaTpk0W90dxfWm0XEN%2BlJ0z7Uc1yeovqDTPucAfKBRJ9C4jj1NN4HNQtrEyYZoHXknGPWr8hXEPpmmnkU3okNdw2rSk5HHaou9iJP3hg5bofwpXAYd%2FbNPfcNhMc49DSHDcGlcOgv8qMjHy9alXb1Fcb0IOT7ik%2Fi4qOo1qx%2BMHng%2FSmBu4q1sDdtj6TNArvOUdRQMKTFMYmKMUAJSUAMooKQUhqRiUlIY2imMSkpgNpKkbG01qAGGmHpQIo3E3O1KbHFgb360DKl3c54XpWZK9PYRVdjmtPTLZZot9IaNXZilxQAlJQwEptSMKSgBKbTQBSUAJTHOKmwGXcXuG%2BWqxvzTsNIT7c1KNQoEO%2B3Gm%2F2hSAX7fR%2FaAoK5Rft9H9oLQKwn29aPt602gD7choN6lFgE%2B2D1pDdLQIPti%2BtN%2B2KKBi%2FbBU0U%2B%2BnYRYoouMWkpgNNJ1pCExTSKQDcCmkUAGKcFpiEKU3bmmMQLSGMGjYGRvFxxUP2fj2pjG%2FY4SfmjBqC4sExlKQitLp7YylQpHJHjIrehOzFJc6LSnI96XNekcfWww9aY9UhdSu49KgcZFNgRkVEaTLWgzFRtyPesBkZqPbjOBU6jWwnamVMtUOwlMNc7RQlNqbIXQbSVDGNoPNZjEpKT7DCjtU2HuH4UdqAFoo3JDvRSSKHCiiyAeKvWUOW3YpDgawXAp445pdToAjmmk0ubmATNJ3pPQTD2pv8qb0EGaQ0raljelL2xR8TCwlGTtxRyq5EkRH8qXP8qLFimkLYFSlqQIG4zSdgB0oUbPUlLmA84xhce1H8XtQ%2FIt6AcZpM1fQQn8VIfpUrezIDmkOCnPSltKyEA9eaUVUpKwIaaT%2BHpSvZBEUc8lR8p65qNeB0H3aE0K47%2BL8fzob%2FJqXLUa8z6VPWk216Bzi0lK4x1FAgptBQlNoATFJSGFBoGhKQ0hjKKLgNpKBpiGmZoAbSUhkZ4qjNOS2xKQhEj8tdzVUurjIwOlUgMuZ6rjL9qJAaMFjlfmpIWayucfwnr71IGyMOm5elMoGLTTRYY2kpgIaSpuIbULyqv3jimBH9oj%2FvCjz19aVhjftC1Bczjy%2FlPNAjEkzmoyDTLGHPpSUragJzRzTQhOcdKB0qeUq405oJpghm6m7%2Fm600UKXpN9SAm6jec02xjt9GaRIq5zjNXrRuasmXkaqHIqSpEJSVSEFNpAJSUAMxSYoAMUVQhaKQxMUmKBCYp1O2gxmOaaV%2BWgSE21G0QNCYFOWA9hUFepSleJzVFqIetMb3rUzInqFqqwEOKjIpWGRHrUZrKRSGGozUbjGU2s2ihDTDWbAZikYc1k9Chppnei4habWO0ixDSUvMaCjNQIWijqKwcUVHUoWgVY1oOFLUgTQpvcCtyBNkSjHahFRRMBxQeeKi%2BprygeaaaNihKTpVdCWNpP4eakSEzSZ7CixTQUmfSjyEGeaO3FKN7lDTTT0x%2BdNEgXyeaSkSKOmKTp0prUWwnO4%2BmaQnHOafLYAP603noaQtgzlMUv8qXLoITsc0nTvRbULh1cdaAeKjyGBG7n5sq3TsaZnvtOPc1W%2BgtR3ag8g8HPvUiaADkYpM5%2BlDae443TPpc0nSvQOYKSgewdKUdKQwpKAEptMoKSkAUtBQlR0hjaSgAplADTTaBjTTWOBSAoTzGRtiUscIjGT1pAVLuf%2BEVlTSU4oRXALmtSzsto3N1qZFWRoBagurcSL70IRBY3BhJhl%2B771okUANptADTRQAw000wK1xIUjJrm7qfzn3VcRFzTLfzPnNaf2cUS3KGeRTTbioEMNqvpTfsi%2BgpajGfZF9BSfZF7qPyoGNNkn90Un2JP7ooENNknpSfYU%2FuimMPsK1E9ih6igL2M%2FUY0t4Ce%2FasT7QaqwXBbo7uatqQVz61lsylIWlzVdSh1LU8xI9as2x%2BarTA2Ifu1OKZAhooAKbSAWmkUxMSm0h9BDQKoQtJSQxKSmIKWgAxSEUgExUdUgGsMiqN1BjLLxXRRlysia0Kn1pDmvRRysjbOaY1MEQNUTUAREVE3WoepVxnSmEVMkV0GEU2sWMaaZWbQIQimVmykNNFQxjabWdhhTajzADQaQwpBU2ELRmkAtOpFIO1OFV0Ga9hb7RlqvYqDeOw%2Bg0aFN3E6UypYnLUTvSUWC%2Bo3NJ2o8xjRzRQnqR1HCkxS6jG07tTBMifnmjPy4PpRpYXUSj%2BDA7VIwo5zmhaRJsxp4bnnmk3fN%2BFXbTUXQUf7RxTf4fm%2FlQnoMMZNIoGc0mJWDgjIwRR%2FDj19KjVbgFJgdz%2BVVsQ7ik4J44prDrTaHrsL04NJ%2BJqbajSEYbH%2FAEpQPSlZN%2B8WrH0zTcV6BwhS0iwxSUwENJUlBSUgEpKYwpaQxhppoAbSVJSCmGnYQ002gBjMFHNZ8sjSttTpSGSRxLGuaqXdx2WgDKmeqvLmncRqWNmBhyOa0wuBSKFxSEUgM69t%2FwCNOvepLG63rskzup9ALpqI0gG0bapAJtpGFMRlazN5dvtHVuK5wLuarSEdTYQCO3Aq1trMsaUpuylYQ3bTStMA2U0pSEN20YoATbRtoKGlajbgUCOP1248y92rnalZBNaPYm5Fmr9s%2BUrHdjLFLTRtceKdQIeKs2%2F36CTXgPFWKBC0lUAtNpAFN70CFptAITFJQIKKYwpKYhKKBCUopjEam9qBDaay5quozOurco%2BV5FVC3HFelRnzI5ZrUaeajIrchIjbpUDCgfkMaoyKUthkbZqPrWNxDDTOorOxrYaRTTUtANpvasWMb1pp61D7DG%2BtJWbQxDSVnYApKgeoUdKqwwpagT3ClFPcY6rVnD5s3PQUho3VHy076VnY3TD60lFigPBpoGKtiGvSdqhh1GdKbnNJgKaZTSFYeM96M%2BlO2gxKZzzWaQtxM%2FPmlFa2QtkJ70nvS6C6icntRnrUbO4CdaM%2FhVOw7ic0EnaMZqbEO7EPXikPXOKrYu6AktJ9aTse1J2sRZx1F70nr%2FSktuUAamfrVBcU4wvUbT%2FSk5pbvUAyc4x%2BJNLnnvz2qNA5j6aNGK9E5hcUlIYtNoYBim0DQhFJSGFNoGHalpDGUlMYlJSaAaaaakY2oZnEa5oEZ7SGd8L0qdYggoGVrufb8orHmerQiryzYrRs7Pjc1TIZqovFPxSAdimsKRViJlyKyLmFoZt6dDTA07aXzYhn73en4oEO20mKoBCKgl45oEzm9Zk33O0dFqLTIt90Pbmr%2ByT1OnVafisyhuKbiqGNIpuKVhBikIpAMIppFACYooKGkVR1GXybSRh120COEnOXLdzVd%2BlU2QQdqtWZ5xWSKL%2BDThVNFD6eKXkUPXrU8X3hQBsW%2FSrNESLhRVDCkxQIUUlABSUANpMUCCkqgCipASkp3GFJTAWm0CEpKLjI2GaybqDyCNq%2FJXRQlZmVSOhBTTXpHKQt1ph9qdxkTCmN0pW0KInqPHWs5AMNMrJou40imVDAaaaayYxtNrPYtMQ03FSxDTRUMPQTFBrOxaEoqbdRi0UyWwpwFT1Gh6jJrbsoAseTR0LiXKFoiro1QGm4xQO4lIalvoHQb1pvbipaFsJSVPQBM80U%2BW3vB5i9ab60%2BYdxcd80hPPFHMTYaR6Ug6GnzXCwnApKm7uGwuABTc803exLExSDpzUprcABI6%2BtHbiqdr6gJ26%2FWj6UR94VuonB%2FwDr0fjzTaUQuwHSkHvUaAxxK7qj4P8AnpSuJvUVvbtRz%2BGKah1Y%2Bod8uKbk5%2BXk4pOPUcj6eor0DlFptIaFopDEpKAEptMYU2kMKMUihtNamA2kNSMbTDTERSSKi8ms12e4fA6VIy1FCsS1Bcz7BigDHlkxVJjuNNiL9jaZO5q1wuBUjHinUxiU2kA01FLEJFxTASC2EVT0wG0hpiEqvckLCTQBx9w2%2BUn1rX0FPkdvwqpbCibYWipKCm4oAaRSEUCG000MYykxSEFNoAaa5nxPcfKsA7nmqgtQOZaoyPlNTMRUPHFTWzbZM1GwGwj5qcYParbLQ%2FYtL5SmgCWO0ZulTizmX%2BEn6UDuTIZY%2FvIamF5j7y0WEPF0tP8AtCetAhwlU96fuFAhaKBiUUDGmigQ2in0ASilcAptUAUlIQUlACUlAhKikTcuDVJ2Ax54GhfaenrUNevTlzK5ySVmRnrTO9WQRP1qNutGxd9CJqYw%2BWoGM7UlZyHsNYVFWcldXKENMIrHoMbim4qBoSkNSMaetJUMBKSswCkqRhS96gEOp460Isu6fDvfJB4raH0pM0gONNzg0RuX5idaTnP4VF9SxM4pjHmhoQUnQUNsNBlFDZO42jvT5iRCdvSjdx60hu%2B4p5I%2FlTM96LIGxB1pSTQ1qISk%2BtLlsN6i5poGAAetO5GwhzSU9i9LXE4AyaTPyjPNHLoS3YU%2FWk9u1KLC9w%2FiwOKT8xSeoxR%2BlA9s0W7Et6jWNFIfUP15pM8DIzVRJeugZ3sTj9aTvx0xUddyfhPp%2Biu8wA0YpMoSigYtNNMBDTaBhTaXQYh4palDG01qYxlIaQEZNQzShFpgUPmuH9quJEsa8VIyO4k2IaxLiTJzTSEzPkbJqzZWhkbJ6UNgbqpjpTqlDFFLTGFNIoATbS7aaADSVSENxxTKAENZWsS7LUj1oW4mc1jLV1em24itExVTCJcxSVBQUmKYxtIRSEMIpjUCGUVIxKaaYiKXhDXC6nP5927%2BtXBCkUCOaRlqZsClKPnoj61ncZsQnK5q0tUzVEgqRaZBqWS1pqoxSEL5YpvkL6UARGyi7IPwqJrBT04piuRtYHsxpv2aZOjU7jG%2F6Qp6Uee69RQIVbsVILhD3piHeYp704GkMKSgApKQwptMVhaSgBDR2phcSkoAQ000AQXEayrg1jzR7JCP8mu7DT%2ByY1F1ITUbcV2owsNYVCRmn0GMamMOKnoD0GEVHWUir6DTTD0rOwDaaRUvYsQ8UzFYgNNFRsMbSGoY7DaQ9Kya1KCj6VOzDoJjmlAwBUiF%2BlSqp%2FhpJFI37e38pdtWcUM3SEzzQai4%2Bo00zNLfcbQ0jFMIpSuAv1pKFsITim5NV6iGk80vHal9oOg3vS44p2uAh6ijipcAEPWij0AbTaa2BB7UCl00FpfUT6Gm%2FjTjLox8ovajtmhCvdAKBj%2BE5ApWD7IU0e5o9RW1Fo%2FzxQ%2FeJkuohHzc%2BtN%2Fh6ZPan0YhSFB65poXHC44qNwHe4UDntSfpTsuoJn0%2BRRXc9TEKOlAxMUYpIBKDTGJRUgJTaZY00lIBaYaAI6Q0AVp5Qi1TVWuG%2BbOKQWLiRiMYFDnatIoxrubc59Kyp3NUT1FtLYzvXQwwhBxUlEuKKAEpaQxDSYyaYh1FUA01G7hetAB2plAxrVzuty5kVKcdyJGfZR%2BbcKK6%2BJNsar6U2OOxLim1AxtFMY3im0CGmmHrSAZikoGNppoEZ2sXHkWTn14riG5FaxXukyGKhPShkbHSsWxpFKZfmqMcGo6iNSx5Sry1ZoiWpUpgatiOlaa0EjqWmISjFAhMUmKYxNtN2A1IiJrdD1AqBrFO2RVDImsmH3WqIw3CHjmj1Bsb5sq%2FeFOW69RRYdiUTKaduBqBC0VQwpKQgpMVQBSUhDDTTTAjJqrcKHHStIOzFIzZAVOCKhNerB8yOZkdMNWyRhqM81HQroNao%2B1TLQLaEfemVk0VYaabUsLDetNNYPctCU00uo7DaQ5rK2oriUhqSrgaKztqO4d6KkVhwFaenw5%2BYjipNYGqOKfQzoSA9aQ%2B1ZksZ0pho6jENM5zQ2iRT1pKSHoNamcinJEiMc0dqLaWG0hO%2FApTSWgraCk%2BtMNNDewmaT86G%2BwCg03cSOlFguFGOKBDc%2FMaKGriv0DvSn7pojoAh%2B9%2FSgk5pPUbG98Hj8aV%2Fm6il1JBmz9c800k44p8ohc8%2B1NwT9aa2KbsHOz7vA7Uh4570nF3uSx38WT1zSRn5f3nUCi7J5Lo%2BoqSu0wCikMSkpjEopIoSm0xhS0DGmmGkA2kpANNVriUIPepsMpJG00m5ulXUTYuKYIKoX0vG2gbMW4fmqqq0smFpMR0FpbLCnTmrmKLjFphFDAbThQAYpcU0IDSVYDTUbIGbmgYuPSmkUgIJflFcnqEnmXLntTRMixokWbrd6V1CdKnqNaDqSkMbTJFypx1piKLRXIPEtQyzXMXUKavRiJreZ5fvLipzUWKG02kA2mmgZzHia4zKsA7da5xulbLYglThKDWTL8hjIrfeUUiWsLHhcfSl0Cxbt7RYunf1qbbjpSHsKOalj61SEzWsq0xQSOpaACkpjCigQlJSEJRTATFNK0gGGMVC9pG3VaYyvJYcfKarNbzp0FMBgmkTrUgu%2FWjlGTLOjd6eDmkKwUvakFhKbmgCMmmGmIiJphpgQTwh1%2FwBqsxkwcGvQw1T7JjOPUYajb2rrMhmPWoyKbGkNNMIrNghpWmEVnJFMjpprNjuNxTKgYhptZcoxpoqbAxMU2sgQUm2pvYuwGlrNoOpNCm5gK6CCMIgUDgUvI2pkuKWs2bXA9KQnFSSMpPWmMQnioz1oS1Exv0pGPX6UnuMYWoHSmZuaE60lJj3F460h560dRifWkoYhKTNHO%2FhCwUmaLdx2sJ24o60nKxLHcUlK73HbQOTSdqEIO9GKqTdrksTFJ3qUK4d6B90e1V5FCdDzRxiheQOwh4am0cwcyFxz14zR2zQuaTsheh9RGgV22OYKWgYlJSGJSUDEpcUXKQ2ikMbTaAGHFJU3ArzzbR61USJpTuk6UMC4FAHFFLUor3D7ErDuZetUSZrEs1bOm2uxd5%2B9UMaNMLTsUygpKBDDRSGOWg1YhpopiGGkNIYlNJpAVL%2BTZau3tXHucmtIkm%2FoMWIWcjrW2BUMdgptAwprHFAFSWfJwnNRrb7jmTmqET7cUhqShtNoEIahlO1CTRYRwuozefeSPnIzVEfNJittiNSzSVivM0ExUsQ%2Bai5XqW1phFTaxQLUqChEtGvZdK01qiR1GKBBSUAFJQAlFAgpKYwpKQhKSqAKaVoAgkgVuoqjNp%2BPmRvwNO4yg6tGeaVLh1NSBcjulbg8Gpd1TsMM009aNwIzTDQhDO9HSmBG1VLiPcM10Up2kTPYzyOaZXqp6HMMOc000MlERFIahjGmo2HNRuX1GUzpUSGNxTSMCsb2DYSmmodxobik7VmUJSYrNoYlJUj6C0oHaolsBrabBgFj3rVUVkzopITvQanQtiZ4ppoAbTaY%2BgGo81NwENM7UbkjetL1FPYmwh%2B9xTc80tymFN79e1K6WhLuHtmj6j9aEIDTe9UXcWkpNibuIMBOKCaloyEpOOmPxoeuhaY7mjmqsiZPqJwvWkosh7oPTrmlOccdaDN7id6b%2BhoLsLzu701sjpzQkSLSZwM1L3AXv81J7UbaxHDWXKfUeKSu4wDFLQIKSgsbiigApKQCmkpFoaaYaYERqvPNs4HWkIghhLnc9W8UihMUxqBmRez5OB0FY075NMks6bab28xhxW8i46VAySimUFNoATFJtzQA%2Bm1oISkoEMxTaBiUyoEZGuS7bfZ3audC5bFadBHXWEey2SrlZlC0lUMbVG9jmcfusfiaBFSMXEPWMNUv2tv44j%2BFOxJb6imGpKG0lFwG1naxL5Vg5zz0prcRw0lJAOc1cnoSiZqTNZmlxTUsVLcZaHSmt0oGIOKmQUxbbmvZjitEcCkSOpaYgpKYgpKBiUUhCUlMAopjEooEJSUABppFAFaeBXUgisa7tmgJPVaBoqb8VNFcbTVMbNBZA68UtZCsMJpmaAuNNNNXcCI0w80xPcqTwZ5XrVPHNenQqc0bGMlZkZFN7VsZDaYetISG0w1LKGf5xTKzKQhpuPWs2ikMNIaiwxtIayaHcbQahoQ2kqAFFWLeIySAYyKytc0RvQJtSps4rJnQMzmn1EtirjDTaEG4wmijQQh6VHQMbTWpi3Q30opPVAGeelNNK2gmJ60gqWgsJ3paG9RW7CGk7c1dwsJyWb60DrmjyHbUQ9aKVrmTWoZoz%2FKnaxY1dvQU7Io5RoTuDS5GaWxO40nn0zQT8tFuoxaTjA4oJ3Exzu70NnFOzDQO2OM5pFJGenPrQ46iD3J6n0pV561FXf3Qiz6jpK7znEpaoYlFSULSGpGhppKBi0lACGo2pjKdxNj5V61HDAWO96TAt4pDUjGNVK7k2J9aYMwrqTrVWCJp5QBQSdLBCI02ip8UigpaChKKYDDTlpgFJTEJTaAEppFAiM8UxulIZzety5nC%2BlUbFd92g96rZEdTsY1wtPqUaBRTAaTiqMtwxOIhmgVyu088XzSRfLVyN0mQMnQ02IdUZqBjaSmIaa5zxNN8ixevJpw3A5d%2BetSRDC1U2KI5vWmVmWKOalTOanlGy0nIpG4NVoAvfip4hzVAbFoMVf7VJAYpaLADdKSmAtNpgFNoAWk6UgCimISkxTASikAlJQA01VuIw8ZBGc1QI5%2B8j8mYjt2qruqblli3utnBrSDZpAxTSUiRhqNqYDKSnsIZVOeDJ3Rj8K6KErMmpEpU0jivURy7DajNADaawqJPuMbTDWbKTGUhFZlDTTfrUBcaaQVEgEpvesvIsKSpYCgVr6dDtXce9YzNYrU01FJWNzpQmKDQxiUxvrUk2sNoJ44o8gI%2B2aaaAG9aTPSlYlCGkoHcT8%2FSk%2BlD7EyA89aTFK2gDR15oHelyCvZ2EJOaP5Ux9QzTOcUwHA%2B9J070rmbG%2FhS9Vpy0K1FOWOWpMe1O9xgPTpSmlIVhOD9aXNTewDc8%2FQ00E1VxSVkSdaaPmFNsLMaeSPXPWkznnGPapgAh9qd3xk9KepJ9SUV1vQxE6UtPcoSikAUlOwxtIaTRTClpANzVO5n6qvWqAigg53vVmp3GFNosMjfisG7m3OTQiWZUh3tW1pdt5ceT1NDBGnjFA5qShaKBiGigBKdTSAYaKoQlNNADaShiGmoJeAaAORvW3zsataHDuut3oKchHTgcUtSUFFMZUvWwAg6tTo4QgpMkV1BFUbJfLklj9DxTGXTUTUhjKbSARuBXE6zcGe9PovFXCNyGZOMyCrm35aqbFEaV4phXFZ6GggFTR9azHcnWlNVYBelWoeWoEbFqOKt0EjhSd6YBRTATvRTAbS0CGnrS0hiYpKBBSUwFpDQA2koAYaiY0wMzU4PMh%2F2h0rnCaTegxA1atjJlME1Iy7SGmSJTDSAjpDQO4002tY7BYpXMGPmQfhVKvToz5onPIQ00itjHyIzTTUO5Q2mGpsxobTayGNpuKhlCYpKi1xiU3HFZ2GGKWsykTWsXmPW8i4HFYT%2BI2gPoB%2BXis5G97DM%2BlJUANam4oT6C3DpTSaW7DoMzSZ%2FKi1ibjcUhFNPUbE6imUiWJQOlFwQetIaph1EpAamwhfWm5pJDeonaigBKCfanHuSL%2FOmtmgpqwvejOQD7VPL2IEHTNL1b2qmCG%2FxDIpe3NQ1YLgCA%2FPrSKOPX3NVug3dgzzSUo6Fb6AQc0DrTnIl2Epw%2B9%2BHpTtZXIWp9RUtdhlsIaSgYtJQO4UlAxaZii4wopDKVxP%2FAAJ1psNv3frRcCzRSAaaYaQzO1CbA21g3L0%2BgmO0%2B386bJHAro0XAqWNIfSYpFBSCmAppKYCgUNVIQ2imAmKaalDI6bVCENUtQk8u1dvakSzk5OWre0GHbBv9aqYom1iipLCimMzdQOy6gc%2FdzirmeKTJIpn2oarWqksz%2BtIC2ahakMZSUxlS%2Bl8m1Zz2rgpHLktnOTmtKZDEhG56skVnMcdhhpCKlFXGYqWOkwLFIetCHYeOat24%2Bai4jYgHy1Z7VQmLSUxC9qaKBBSUwFptIAooASimAlFADacKYEbU2kAw1GaYEE4ypFcrfp5VzxS8gK4NWbSXZKKz1KNoGg07iQzNIaYxtNNFxCU3vVoBpqhdQ7cuo%2BtdOGfKzOa0Kp56U016RzWGkVGamQDDTTUsY3mmkVBS2G%2FWkrMoSkrJooQ0mKhqxQmKO9Z3QzasoPLSrtcdR%2B9c6Y7CUlTzdyhmabSdrAJ%2BOKPxo2WghOtR4qUNyGUnSq02IuL2ppOaVuoxtFDfQBlJ3olEnnAdaCaBvuJSUr9AG%2BtApoLageBSdqbGLxjrSfSlcnVB0o7UbjQLxx703pgUIXoKtLz3zQ2T1E70pPFAMQ96T%2BGp6gxSMZ9RTAxAxzSj8QkOz7%2FAIUnOKf2hfCw5zx60gHzHPpVbjlY%2Bod9O3V1GAlFMYuaSgoWlpiEpKkoaapXE%2B75EqgHW9vj5j1qxU2GJTTSGJUMzbUOaAOfvJdzk1m8ySYHerYHRWFuIoBxzVysxhiimMSkpAFFADlFNNWA2imISm0AMptSA01ka3JiDZ600SznlUs4FdbYR%2BXbqtEgii5SYpFC0h6UAVpVS5iK5qj5d1D8qYZaoQ4RSyHMpq4qbVwKljA1E1ADKbSYGB4mn22nl9zXJsflxW0fhJZNCuFp%2BcGoeoISm4qCwNOWgon7UoosIlWrtoPmqRGtCOKmFUK4tFMQUUANNB6UxCUUgENFMBKKYCUmaTASkzQANytRZoQxpqM0CZG1c%2FraYcNikwMgU9Thqg0N2FspmpM0JECU01QhtIadwE%2BtJSQCUzH41omBn3EHk8oPl%2FlUBGa9ajO8TnkiNqYRWkmrGYymViCGnpTall2EphFSMSjpWbHYSkNJjQdKtWcO%2BX2rmn5GsEbYAApDXGdWwlJRJ6DGnpTc4yKi2griUlC2AaetIaT3JI6M02hjSabQiXsN60vej7RS2A03NFiWkJ3oPrSfkA3OKWlzANbGaCab2FYTNNpoHoOpCPU0hAaOgp9Aa6i49aOcUugbob0pPWjqDXUXn0pp68jrQhARzSnp1o5rMPshnnmkwOTRdolDR0p3sM02luMTnPWkB96nRgnys96h1KWPvmtCHVVb73FdXUysaEVwj9DU4arEGadQAUUFBSUFFKacyNsjqW2ttoyfvUAixTKQxKSgBlZmoTcbRQIwbh8mrOl229vMPahgjeQcU6kUFFIoTFNoEGKKBjkpGqyWNpKYCU2kMZ3pppCGmuZ1mTdcYHaqRLZX0yLzL2P0zXWLUsaH0UDCorltkLH2qkDMq3ufJTDo1Ti%2Bib1H1FDRJKkqSfdYVLSKENMNICI0w9KAOQ8Ry7r4p%2Fd4rDbnitVsZloDAFDVkaCbaXFD1GFO21Ax44p4FJi3JAKv2g6VQjWj6U%2BgQ6kqgCimIRqSgApvekAtNpgFIaAEpKQDc0jUwAdKioAKjNADGrG1wf6Nn071LA54U4VmNGzZNmEVarTUTG0poBDaO1NDGGiqYdRtHagLDWTcvNZs8XlEeldWGnrYynEgao2ruOe%2BpHTD1pDuJimVD0GJTTRbQYlNPWspIdw60Gs2UmOVcsK2bSHYOea56h00iz0pK5G2bsTtTfaouGw2mmgBpNN60hBTDR1FYbTaJANpD0o6CtoHWk7UCsJSY4pjsGO9NouIDS9%2Fak7XENppoXmFxBTutHXQBoOOKOc0PcAoz7UyXJh70mc9aUdVcB%2F8PWmdxtqYjkxTnp70h460LRCuH6UDpVXdgsho64NL3oeogI9qKz59R8ome2ME0mOvt71aV2TLc9ueIrTOa7GjIkjmZPumr0OqyD75yKRRqQajFJ3q4JA1IQ7dTs1QEiLnmqV1MWl2W54oKJbe3CDnrVigY002gY2mGkBDcShEzXPXcuXNO3URRjjM02FrpbaHy4wKQ0TgUppFBSYpANxRRYYU2gVx6daH61aJGYpDTGhpptAxDTGpAQyHCk1yF3J5kzN61USGamgRg73Pat8VJSFooGLiq9ym%2BPbRcQwRrsxiont4W%2B8i0CGpaRq4Zas0hjaYaBkRqC4O2M0hM4G5l86Z3Pc1XQZkrZ6GZc7Uw1ka2E5p2KkB22gUgHCn0DJU5rTs1pkmktPoJCjFUMSg0wEopCE70lMApKQDaKAG0maAGUmaYCrURpABpppgRGsvWf8AjxlpAcxS5rN7jRs2P%2Bpq31p36jFpDTEJQTxVMGNNN70CuLTSaaAKa%2FzLirhowZlzxGM%2F7JqA16lPXU5ZIjNMNU1fYSGmmVPKUJTTzUMEB4ptQyhaTGTUNNlF2zh5ya1EGBXm1tzopLQKSszYbTcmoQCbvamjk9KAExmk6U0IZmkFDHfUa1N7VIr6iUlIYLTM5p3J1YUdqpolsSk4P41DjYNbBx%2BNN6rR1uLdCdKQVcihe1IfekJsb60opsXQKOKzkmWnoHWir8iEBHY0nToaCdNgpDSeoNXFxx1puaXmU%2Bwh29KUGqVybi%2BlHft0rONhuaG0Z2txVi5mz6LlsGHTmqD23PSu%2Fc57FZrYg1CYypqbFDASKsRXksePmNR1GaEGq%2F3617C5juScN0plC3V5ul8i3PHRjToIQi%2Bpp9Bos0maAQlJSGMNRM20ZoBGPfT7j7CseVixpiZr6ZaeWu89TWpipGgpaBhSUDG0hpDG0UCYq9akkqwIqbQIQ02gBKYRSApalJ5doxrkD096tbEnVaPCEtAf73NaVSWJRQAVQuWLXAjp6CFlmEUdUd90%2FwAyjihWEy3ZzedFk9RwasVNhiUw0gIzWVrkvlWD46niqjuJnEP1p9qv3ia2mStyxSNXOWNxT6Bj8fLTe1K42KtPpMCWIZati1XimSy7inU0SFJQMKKYhtJQAUlABTaAEprUAJTTQA3NMbrxQA5aZTAKYaQETVj60%2BLRxQBzVPXrUWHc3rRNsAqx2piExRQMSkNAhKXFUMaaTFPoKwUY4p3GQSKHGKy5ozG2D0rtw9ToZVYkLUzrXZcwGmmmpZWwykqBiUVDATHNSpHvNZydjRGvGgQYFPrzJyuzritBc8U2sUymNPPWmdqG7ISEpM4pDsHOaaxqkNjKbU21MxueaKTAaaTtVaMY2m%2FWqVkF%2BotJmpcuxPUAeaPrQyhtGeKXLoKw3nrSDpTsAue1JQIOlLnilZ3ExM8%2BlLTWjAQDJozxStcObQM5pKPUTDvSU7IkO1NxiMUaIocQd3NAyO7fWo0voFugHk89KMd6exLQ0%2FlS9%2FoKb1VhWsfUu2opIFk6jNdxkUpbAfw1RlsyOq0XAqPaCqz2xHSmNEBjIpu5wazKuXrK98g881uW%2BpQSAfPg%2BhpXKLgfNOzQmCFpDTGRuazLy47KaaEY1w%2BTxU%2BnWfnPvYcCnLQEdAibRRUFCGikMKSmAlNoAbSVIwqUcpVIGMNMqyRppKAEplSMxtefEKr71gRLvmUepq%2FsmfU7O2j8qFV9BU1QixKSqAWs69t5DMJoWGemKQiCO2dn3TdammZYko6gMsY8IT6mrlK%2BoxpphpARmud8USAQpH6047ikco1WIP8AVg%2Btaz2JW4802sbmiFqRaVxjzTDSAKetMC1bplq2IFwKCSzRQIWkqhBSUwEpKQxtBoEIaaKAG0hoAaaTPFIBuKDTAB92mUAIajNAEbVg68%2F7rbUgYAqeEZcVA0dBGu1celPoQhKDVjENJ1oASlphsIabQDFppqxCGoZYxKhU1UHYGZslu6H1FQsuK9CnLm1OeWhEabWxGoyjtWLLEo7UvMYorQtIcDdXJiHobUkW%2B1FeczruJjimUiZB%2FFSUAJTfwFFgDNNoKuMprCm9iBKZmp8wGmihitqH86aRTBic55pMc0AxaaOKE%2B4mL1pPwpdRpjW55o7VVxCAUv0FTbqOwhpDT51cTQppAc03a1wFX71Io%2F8ArUrh0EakUcUr6EW7C9DQKfUeoUMfl9abBDjz7Uwn%2Fd%2FGoiN66imko63ZAv1pNvcHNN%2BRV7H1Saaa7TnExTGSmMie1R%2F4ahfTMoSG%2FOi9twMhrfI6VXe09Kp2GVntmFR4dfWs7FXLMF9cRY%2BY7fQ1pQ6sMfvBipGXEvom6OKlN1Hj74pgUri43fd6Vlzt6VQhtratO%2Be1b8MQjXAqXuNE1IaLFjaKQBTaBhTDQIZ3opDFpyNimIUio6oQykpMYGozQI5vW23T7fSqemx7r5Ku9okrc7NRQeKzRQzrRTGBqJjTEV3kVByaqyTo5GF3GgW5dj%2B56U6pGJUZoERmuR8TSZvVX0FOO4pHPt96rafdq5MIoaaZWTKW45amWgoU02pEPFPApjNCzTmtZFwtMkfS0CCkpgBpKoAptSISm0wE7UykAlMJxQgA9KbTAKa33qAHN92o6AEao6AI5OlcvrkmZdtAGYKu2KEzCshm32paoQUhosA2kpggopgxMUlACUUxhijbVCYbeKzrqDB3V04aVpWImtCgw%2BaozXczBDe9NqfUdxD1oFTJaFE8EW9%2Ba0lXAArzcQ%2Bh00ojzwKTNcljo6Bmo6kbCm5poVxO9GaUhDDTe3emAwijt3NSnf3Q6hUZp%2BQmxtJ2qXoIKaTmjVAHTrSUCvcSlFPoDA8cU0%2B1CEw7cUlOK7ghop2fSiQ%2BbsJ2pDSRLYoPrQeadtATE%2BbvQPUVNmC7ik0lVsV1uJ1NHODRa%2BohehpKCFETnNGOM0vUWop6dKSgFvdCdKdns3NGsR9T6pNJXcYIKUikUJwg3McVUmke5bZF%2Fq%2B59aVtdQJkt0UdKils427UwsU5bA%2Fw81Sktdp5SqArtbCovs1IocIKmUBVpgIdz8KKfDpzSH5%2BBUMZpxwiNcCpKkYUUDG0lAwpKAENMpgJSVICUlAEobctMarQiM0hoAKjk6UhHJ6m2byT61NoIzek%2B1XLYSWp1S0p5qChuKKBjH6VhXlxOku1vlFUrEMjni2w79%2B41qWsYWJcDtSYE%2BKKRQ01GaQDG%2B7XEa027VZqqCJZlY%2FfCrNEtxxGEUlQygWpqdygpKkkeKljGWpga9mmKvjpQSFOFMBKSmIKQ0hiUhpiG0xutIBM02kA1vvUjc0wENKOlAiMUtMY1zTM0CGmmGkMhlbC1x1%2FJ5t1nrxSewEa1rabHxurNFdDRpK0EFNakAlFMAxRTAQ0tIBMYpcVYC0lFgQZ9KgkwauOjAy7lNr%2B1VWr04vS5yyI6MUMWw3FKoycVm2WjUhjwtTY4ry6kjthog60zvWBqgPSmdqkQU2joHUbRQQNprUPsMTPNNzU2sMaaQc07i3G0UC6DajxVCFooYhKbn%2B9io0sAvT2oHrT0J8wPAprZxQr31NLDqQdae4ttBDR2o0J2EwM0vNHLoMM0n8OKEtA0Fx39KSp9RBS49aqy3BdhDSdhUDSENJyE4HTtVKN2Jjj972oI2nlaJPoSmJS%2BueCB2707IVz6rIpDXYZCUsjrGuTQgKJ3XT9Ttq3FEI0AFBRIaSi4xMU3GOlICF7aNuoqA2MRqubQLEf2BPej7Cg6ipuMmW3RRwKkxQwGkUykMKKQxtFDQxtNqRgabT2ASigBtNNMQm7FSbtwoQDGFNqhDajk%2B7SEcleHdcyt%2FtGtLw4vzSH2q5bCR0IpazKEpKYDTVWeFJkw4zQFjHubV4ehLRfyrWtpFkjG01TJRNSVJQw0w0gIpeENcHeHzLuQ%2B9XAmTKXSapC%2FtSkC2I3mxUP2uPPWpsUWUljYcOKlB9KlFXClpiHrVu2TLUgNmFcCp6okWigQUlACGkpjG0h6UhDKSkA1ulMpgHWkPWgBDzSdKADtTQeKAIzTTQA0mmUdAKGpT%2BVbtzXJdTmkMmiGWArdtl2RioQE%2BeaSmwE70maYBSGgW4UU0D0E707FMEFFMB2RiomOKYETNTCeKYyGaPzBiqEkZTiuzD1OhlJEBFNxXR1sZCVbtIc%2FMa56ztE0pr3jQHFNryup39AptJ2Abxj3pMVIPYRhSUXJQym0XAb9KTqaBaiGkoaB7iGo%2BlLoVcSm0riDvRxVXC1hlA%2B7S1JFptNoegppB92hKxMhKKGrgHSlFAxKKLai31A%2B9JR5FaBSUtyUBoHAoe1g6gOuc5pfrU9QG8ilyvT2ob6EdRNvNJRZ7laCt1wKQcLxVbvUi%2FMGD2pTT5exSPqs02u05yOaQRDJqqoe5bLcCkMupGEHFS0dChKKRQU00AMooBCYpKAExRikMYRTKAEptIoQ0lAIYaKBCU2gYhpKAG0hpAJTaBjg9Ox6VSERmq8%2FEZpks5CU5c%2FWtrw7yshpyFE3hS1JY2imIaaiNAxhXNVxa%2BXOHj%2BUd1pCLVJSBjTTDQBWujiBj7VwLHO4%2B5rSmRIih%2F1pqYqKJbjWxVu3EURrCduamwEkRLnFbcJ%2BUVm%2Bxa2JqcKTAlXrWlZR0xM1lHFOqhBS0AJTaQCdKSmA2kNAhlNakA2kPSgAU03vTATvSN0pgN6Co91IQwuKjaXFAyvLc46LVV7yXHEWfxpgZOo3Es2FKYxWbjFKQx6Eo2atpfP3xWQkTrqPPIqwt5E38WKdw1JhIG6EU6qATNGRQCE6UA0xhTwRigQZFNLVXUojLioi4FFw6DN9JuFBIoNRyBXHNXHTUeljPki2Hnmo%2B1elCV43OZqzHRJlq0olCrXFiZ9Doooceaaa4GzqYmaTNFw6CGm9BUNi8hrUdRRYBrHimdKPIbG9aKCRuaSgQ00h570r2AGpjUwG0mabiS2B6UmefwqSgFNFD7k2FNJ2pp6AtwFJ2p%2BbE9wx6UueKSKkFFFyWBxng0makQgzn2705hik2XZDaKp2JE7dKTIxT5gbHMO9Jx0pJ3Bq%2BoOec03rQri5e4uOMjsaTtjvSvbcWkRB69ead%2BNVd9Ckz6rqOWZYl967Wc1iqiNcvvbpV9ECLgUih1FGxQUUXEJSUFDaM0AJSUgEptAxpplIoSjFIBMVGeKAG0lACUlJDG0lACU2kIKbQBGaAxFAx3metRTYMZx6VZLOMl%2B%2B31rd8Of6mQ1UiYm6KWpKEpKEA2o2oGJS0hCGkNADDUZoGUdSbFlKf9muHHK5rSGhDGW%2FVqmas5asFsY2py7pNg7Vm96QmW7Rea1ENSzToTBqkWgZPCMtW5aJhaZLLlLVCCkpCCkpgNptADaQ0gGU00AIaTNIApuaYDGPNMLUwIy1MNADKjpAQuOajbGaYCfSo3t436rmlYCpJp8XbIqlLZKn3JPzFLlAqtEwNN%2BYdjUDsSpI69CamF1LjrVXAX7XJSi5encaDz5KTznouAvnP60faX9adybCG4fvUZupPU0OQ%2BhA9y2etM%2B1PnrU8wB9pel%2B0v60XBJB9qb1pPtLGqU7IEJ55NSAFkrpo1ehMo31LltDtXJqwawqyvI3paIKYRzmuc0aAim9ql3uV0EPNNNDIEpBigkYaaTQUJTDQxB9aCfSiwcoymmjQQ3JobihiuJTaQJhSUgYc9qTFVogEpaSJEpc02OwlAoGBpKFITEpc9qewnqIKKW4wpaOZJEiewpOMcgH8KnzAVj25pvReetCYA2fTNJVeSKH9Bimhe57ClchoUndQv1o5lsI%2Bpp5ljHvVaKJp33v0rvMUXwAo4paRQUVIwooGFJQMbTaACkoGJTaAENJQhiU00gEqNqkYykpgJRQMbiigBhpKkBKbVANNMqQGGmNQScveW7RzPn1rX8O%2F6uVfxq2xG5RSKsJSUCEpjUwG0tABTTUgMNRmgDM1o402b6VxRPy1tAzkJAMZpbmTZEWqL6l9DnpjukLetMxUyETo5TpUyzv6Vl6lIlS5apkuj6VQy5a36I3zhvyrdttUtSP8AWY%2BoqRMvpdRN0cVKHB6EGrJFopgFJQAym0wEplIAplIAptMA7VEzYoERM1MpjG5pOtIBtNNICFxXP3l%2FJ5xCPgD2qkSyqt1MzY3mt6MPsAYkn3pFICCRULQjvQUJ5A9BSeSKBdBfIzR9mT%2B7SAd5I9B%2BVKYV9BVIA8tfSm%2BSvpT8w6jTGPQU0xL6UhETxD0FRfZ17qKTKuY14w887arFqkkcj1LuqRiA0ZqblbIevWte2j%2FdjNUnqVEs9KKHqaJDelHFIoSm1A%2Bg08UzqaQm7iUnSmJ2Gn2ptFxDW4pKm9hDTTTVNgJ1WkotcYyg80iUhOlFFriE%2BtHegBO1Iactg6iYo6ZqPUQUlX00C4dTTsijl6jbE9hR0NRZBshKRhVMBOfTmlbpSZK2G807PFVoU4id6AKnYkMDNIcEZppkzF79KTHFPqPUceaaenbp3pbsm%2Boh5I6UvOeB1o6l%2FEfUEMBkbfJV6u457BRSGgooGFFMYUVAxKZQAlJVDCkpANoxQxjTTKQxKY1IER0UDCkoASm0CGmmmkAlMpgBptSMZTDQBEyDvSRwJG%2B6Ndp9qlCLIfFO31QBuoqhC000DGUtIQU00AMaozSAzNc%2F5Bko7GuLI4reGxnLcSA8Nn1qhqE%2B75B071HUroZuKkSMsKl7XDyHiI%2BlSLHWLKSH7KNu2mi%2BpMq1btVJakSzbt0%2BWrG2mIdl1%2B67U4TSD0NVckkW4%2FvLTvPQ96YDgwbpzSUAJTaBjaSgQ003FMBjtUBNADKSpAaaWmA2mGgCGQ7UJrjpeWzViJ9Oj33kQ%2F2q6gJUsaEZKj2UDApRspgPEdLs4pisN200ipGMpMVQgxTSKLARsmTUM%2FyRE%2BlSM5eU5kJ9aiNQ0QCdakzUoscDTqTYyxbJvlWttBgUX6mkR1JR5midxp6UDpTLEao81BHUQ0zvStcJAaZQSJTaYIbSZ4qXuMaaSquieoUzdxQhPsJSd8UmMQ%2B1J2p6CCkpBqJSYpu9h7ahijr9Knci4lLigVxKaMirehSDtT%2BcVAMTvSVSDlA8GkH3aLkWCnYH5ipWiKlfdCHr602qViB3VaQ420ttRt3D3zSH8%2FxqXowWw7tmk%2FQUuotgxjjtTVz61Su4jitT6vorvMQooGFFBQUUgCkoGBopANNNoGJSUAJSUFDaaaQDaQ0gG02kAlJTGFNNMBhphpCG0VIDWppoGJimUgG4zSUxMKSgAzS7qBBvpd1MYZFKDQIKbTAYaYaQGVr3%2FIKlrjX6VpHYzkQebshb1rLf5mo2C4iR89K0orbalZyZUe4%2FyKd5NQaCiGl8upBj1hrQtLbvimI00TApcUEiYpKYCU2hAG2jc46NTEOEzd6Xzh3pgL5qetOyKAEqORsUAVmaoyaYBQakBg5paqwCVGaQFe6%2F493%2Blci3PNWiWXtGGdQjH411CrxUXGgxTCOaaGG2jbT2AdtoIoC5EaYRQAzFJQAUEU1YLjMVS1P5LNz%2BFAHLGo2rN6isIDUlZFIcKkXmmM1dOh4YmtACnfQ2SCmk1K1di%2FMYaUcU2khgajqUHQbnFIaRF9BlBpCGUw0xiUdaLkjDQaQhKbVINwNNpFWsIaQe9CEFJxQLUTik9aZLF70nIqHa4boSl61VwsGO9NotcoXNJ70PREig%2FNR0pq1rBshDSLwMdqm3KO4tGKoVxM%2FnTaEtBKzHfwYxSHpRfqDVg9aTGRis3Mkfk%2B1Jgc1SjfUBM8gU3r92qsB9YUV2GIUUFIKKVhhRVMoSlqQEopgFNNIY00lACU2kMSmmgBlJQMSilYBlIaBhTaBEZplAgxSUihpptAhtMpDEpDSENooASkpbAJSUwCjNAhd9JuqhBuppNIDP1ld%2BnTD2zXFNWsdiZFCYHNQiLJotqI39K0n935kgxmrr6d6VgykQnT2FRmzYdqRdyPySKTZzTGWIIMtWnFHgUCZLSUhCYpMUCGmm4oAMUhoENppoGMxTdvpTETx5C5JNROc1QERptFwEopANXrS0wGmmGmBUvzi0lI%2Fu1yb%2FAC8VQmamgc3%2Bf9muoA4qWEQIpuKSGJil21QDsUx6QiGjbTGMIphFACUUgGGsvW2xZH%2FeFA7nOGomqGSNqYDNQVHQeF9KsWsRMgqSkbka7RipKZ0CHrTTRYaEopN3AaWqPNCDmGn2ptDtYgTNMJqAQhpuKa2ENoql5CA0ylcYnFJTYIRs03tQAtNNC0ICkOKUtxidab9KSHbS4UtNiE7c0maNxbDucUg6%2B1MPMOKTpS8hCd6U0ytwzSc%2B%2FwCdK2uhIZ70daBdQIox6UXAT09KXHVhnFGzB6oRh70h5GBkU5WBLuO%2FiNGcpUx1ldEDH%2B96c0c9aqVtyj6worrMgooEFFUUFFDKCilYAopPUBKSjYY002gBKbikUxDTaYhpptSMWmmmAym0DG0UguMpKAFppoAaaZUiG0w0DG0UhDTTKACikMaaSgBKSgQlBoAbmm5oAik%2BdCrdDXN3WkOpZozkelaKVibGaNNnmuViSPLE4xWnaaA8E3%2BlptK%2FwmqlISRsbAOlGKyGJtppjFIZGYFPWo%2FsqZ6UhEiRBelPoQxKSqEJSUhjaSgQlJSAQ0wincQw0gGTQA%2BQ44qA1QDKQ0gEooASkpAJTDVAU9S%2F48pPpXLSVXQk2fDyZnc%2F3RXRikUgxSYpALiigBGqJqYDcc07FADHFQt1poYlFMQ2sPXmwqD1pMDBqF6zENqdOlZjROi5IrYs4tic0GsUWhTj0oZsNNJS2AbR2pWGNphp6E2GnimVLExOtNIphYaOTTaQhKSqsCEPWihCG0lLYQmc9aBgUdBid6ZTYgpKkQhP0oA%2BWqGhKO1AtRKXtQMXNJ9KRQGgdcmiWgmJRTtoQxKX3parUW40%2B1KOKUtRrQOp%2Bag%2B1A9UgPSkA60JqwtgJpMmnurEW10Fzx%2BNN9hTRVu4oANJ3qZeRMj6worsMQooGFFUUFFMYUUhhRQFwpKBjaZSGJRSAbTadwEplSAtNNMY0imGgGNpKQCYpSKAGU00gGU2mMSmVIhKbSAaabSAbS0hjTSUANooEJSUANphoAjakxTAo3UDRSC4h4ZTu%2FGtxryPV7BZ8BbuP5ZB%2FerQkzmGKYazYCUlAxtFJiEpKYxKSkISkoENpKAG0lAxppDTAjJp0PXNMQx%2FvVFTAbSVICUlMAooAbTTQBn6qf8AQiPU1zL8mtOhDN3w3%2FrJ%2FpXQCpe5QtGKBjqY3FAMiJpvWgLiilp2AQioyKQEZFNphYSuf8QH95GvtSsDMM1GRxUS2EMqeKs%2FUaZpWEG58ntWuB8tB0RQ4dKTrSNbCGmUMQlNzSeohpNMoQDTTMcUMVgoPSmxEdJ0qY9guJ1NFMkb0OKKYxuabQhoDSfw0XJSCk7UddAY09aShiDoKO9S9htgeppOoqraBYb3pe2KlE%2BQUg%2BlVJ6AtwzTqCugnekPWjYzdxu7tSjpilLYYYpKUQHZpp6U3ErqLnP1pB0Oc9KOS25DuHTvmk9u3Slyq9w2QHFCj24qQvzC96TFUpDSR9X0V2I5wopDCiqGFFMYUUxhRSAKQ0DEpKQDMUlDGNptIBuKMUDCkxQA2mkUhDSKTFMYYphoAYaaaQDTTaQDTTaQCU00gGtTaQDO9FACU2i4BSVICUlMBpptMBuKMUANIyKzQW0%2B7Eifd6YqkI1X2SxrNF91%2BarkUgG0lIY2kpCG0UAJTaAEooENphoAbRQA00wmgTIzUi%2F6omqAhNNpgNpKkLiGm0ALTTTASimK5ma1xa4965tutadCbnReHB%2B5mPuK3RUdSg608CkAhqKTkUxmWdPUnPnzfnSfYsDm4kqucRVVLmS7KRTylB%2FFmtuJCkYBYt7mnIEOxTGFSMiPWozQMaa5jXG3X2P7oFAjLamkfJUSDoRVZs4y8mKyHE6KCPYgFT0mdEUJTetIsTFJQMbTCafQQw02kmFxGpnalckKTtVbCGHpTaQAaaaBCHrnvSd6GITNJ0oEDetJ25ouOw2nDpTfkAygikhCYpuaBigdaTjGKXMLXcKKdnsDldh1oNPlFK4g4pei8UmIPegigdxMUlSTa4AZoA680%2BYNgNNP05qlsW2ONNpc7JiGOaXr0AFLpqLqGKYGPIApXFsO96OM9O1PluB9X0V1mIUUAFFWWFFMLBRSGFFAXCigLCU2goSmGkAYpuKACm0gCkNAxtNoAaaXFIBDUTUXAjopAMNNNADabSASm0DGGkpCGkUlK4DTTaQBSUgEptMYhpKBC0lADDUU8YkQqe9MRSs7hrKU28v%2Bpar8q4NNiIqbSGNopCG0lAxtJQIKSmMQ0ykA2kpEjDTDVANqX%2FljTC5Bim0AMpKQCGmjpTAWm0AJRQBk64f9HQf7Vc6fvVaZm9zqfDo%2F0KT%2FAK6VsgUnuaC4paGBExqNuakRDI6xrkmqatJet8m5YfX1rRAaEMSxJhamxUjExTGoAhNRmmA0jiuS1Y7tSlPvQDM81Js45rOQIb5XzcVradabfmPpWZpDc0qWlqjoG0h4oGMzzR3oFcbmmmgYwmkqUhDSc02gkbSGn5AIaZTEJniik0IbR0oAbSVO4gpKYMKbVdBAOtBouUxO9NNIl6i4NJU2JQlHaq6AA60d6ke4lA%2FWnZg3oA46UuaJIOWwmabn1qohew6m5qGtQFptO5MgzS%2Fw0S8hpid6cv49Kn1AYSaPurkDnHSrSEKaTPp0oldIh3ufWFFdJmFFMAoqhhRVFIKKBhRSAKKQCU007jQtMagYlNpDDFGKLCDFMNAhpWkxSKENLQA0ioiKAE20wrSKIzTTQSNNNpAJimmlcY2mmkA2mmkIbSUAJSUgGmigBKWgBKQ0xEbUykBUvbfzouPvdqZp91kfZ5eo%2B7VgWmFMNQA2koASm0xCUUgCkoAQ0ymAwim0hDDTaAG1Mf8AVCqAgpjUgGUhpgFM6UBYWm0AFJQBja4fkQe9YOOa1I6nV%2BHUxp%2BfVzW0OKTKGmmtUDIjUch2oSBn2pgzFuFvZpVYwPtHbtVtLm5UY%2BwMMVo7WJsSC9m72clX4m3xhiMe1SMdTGoGRtURpAJiuIuTundh60CZXPWphWUhotWkG%2BT2FbCrtFRc2ih9NNLzNApp5pFEfSg0WKGd6Sh9iBjU3FNC1ENJQgG96OlBI2me9IYlLS3AbTaAQ1qShuxn1uFFHQobmk61Wm4MSij0JYtNpO%2BwB9elInCAegpW6hdgaWqvoMb%2FABUtGgLUQ0AUr8qJdg7UUwbuJng0maXL1Cw6m5HehsVrgaUdKiwMaVzS4xVc2lhCGj%2BHmmMSjI7fyqb6kimk555PPvSbdyz6worqOcKK0GFFNAFFMpBRQMKKACkPSgZCm%2Ffz0qakAUw0DGkUlMB1JigApNtIAIpmKBiGkoAaajNIQzNJSKGsKiIoENIplSAlJQAw0lIBpqM0hjaSkISigBKbigBaWgY0000CGGmEUAMNZ1%2FAQfOi4b2poRbtLgXUPP8ArF%2B9TmoENpKBkRk%2BfGKcaQhtFMYlFAhKbSAbTDQA2mUhDam%2F5Y1QEFRmmMZSUhCUGmAlJSAKaaoRia5%2FrI%2FoaxT96tF0JZ2Ogrt0xOO5rSzUsoaajNIYw001IIrTXcUB%2Bc%2FgKiGoL3t5gvritEgLtvNHMuYmqYUhAaYaBjGqI0kBHIdqEj0rhm55HSnbQCP%2BKrSKW4rB7jRsW0AjTHerNI3iFNxQWJTDip5iug2kppgI3tUZzmgkRjTc81ICGm01ohMbQelCEMpuKQWE70tUmA2kpSeghDTTSBDKWqsAYpD0qWA0r60YqlYnrqOppqQYlJzRfoL1FpKdhiUCnaxIHtSmpeoWGZ4pSeBVXaAKOP0qG3cNwo%2F4CKpob2Ck6UuYLhzmk5%2BtK6F8QrCkP3cZqkxX1A80g6EUtxCt7UdD83Wi11qT7yeh9X0V0oyCirGFFBQUUx3CiqGFFABSUgQUUgEpKYxKShjFp2KAACloEQsKTtQMYabQMZTTUgMNJQAlNpANNREUgGmkpANNMNIBtNNAEdJSAKQ0gEpcUAJRQAlNNAxpphoEMNRuNwoEZMitZ3PmJ0rVV0mjEkfQ1QhpptSMbiigQ2igYhpKAEptBIlRmgY2kIpAMqWPmM1YiA1E1ADaSkAUUgEpKYCUlMEYWtH96o9BWOPvVqS9TtNIP%2FEsg%2BlXakaGmmmkMZVa%2Bk8m2LDqeBTW4DLK3EY8xgPMP6Vb69aTCxQu4%2Fski3UWQM%2FvB61pDpVdACmUgGmompAQXZxbSf7priT05607iGgfMK1bGD%2BM1jIqnuagHGaKhnSBplJFCUxqYxppM0thdBnSkNNAR02l1JENHagBhppoTuFhMUlArBSUMNRlFJsW4ZpjU7AkJijHFK5IpptABSdqOoMKbVWGItLSQhDSCmm7jvoL1pvei9iRenSlqWUNxQarQWgUn8qPtXIFPako6XC62A0g61Nk0MKOcUW6MQ78vxpmOMUn3HcOlJ9aqOwlo9RT270h5f8ADNK6kKW59Y0V0mAUVQwoqhoKKBhRVFBRQAUlAwpKACkpAJSUAOFPoAKKYhjCm7aZQhSmbakCNhTNtTYY0imEUgG02gYym0hDKZQISmmpuUMpppCGGkoYCUYpAFFMYlJQIQ0hpAMNMpiGmozSAgnjEsZU1nQyNZ3JV%2FuVXQDUPqKbUiG0hpANpRTGJSUCEpKBDTTKQDTTTTGMp8P3qYDJBg1CaYhhpKQAabQAtJTASkoAwda%2F4%2BMe1ZC8PWpB2ek%2F8gy3%2FwBwVfqCkNNMNIYw1Q1X%2FVRk9A%2BaqO4i9wKdUjKerf8AHg6926Vbi%2F1S%2FSr6EjqQ1JQw1GaYFTUPlsJyemyuMxxS6AyW1j3yit%2BJAi4rKRrAfjikqDUQ02kWIRTTSGRn0pM8UgZHQOlVcmw00ykxIQignikJsZSGrASk7UiRhNFSNB7U3vVJiE7Unai4CUU%2BgXEzxSN0pJCtd3E%2BtFIOolJVLzAMcUCj0JGmndRQNiZpjUkDFHHWkNJ%2BRNxe4pTiizZVhDR2qhegjGgGgkSgDip6lMUn2puD%2FDVcxKVxxwabWd%2BgWDt0ozTWghMkNTs%2B1FrrULn1fRXYYhRQIKKYwoplhRVDCigAooAbS0hoMUhoAbSimAuKdQAUUCCimMYaaaAGMOaaU4qRjMVE9SMjplAhhppqQEphoGMptIYlNpCGGmkUhiYopALRQIbTaYDabSAaaaaAG0w0AMqpd2%2Fmr70kDINPudp8iXj0q%2BwoZI2kpgJTKBi000gEpKCRKZTGNptIBppBw1MQ%2BXkZqsaAG0lACHpSdqAFptMBtJQBgaxg3jEegrK7mtSOp2mmjFlEPartQUJTDRsMbUNxCJ4Sh70wKMN59mPk3YIYdG61ZOoWwH%2BtFW49ibkEW6%2FnEjrtiQ8e9aYGBSfYYUhpANNMpDKGs8abN9K5Eil0Cxr2NsEjyetXhxWErnTFaD%2BDTKSZQ00w0%2BhQlMJ4oSAbTDUsBvWm%2BxqhBTWqdxDCeKaelBLEpKpRAQjimmhx0EIabUJ6DENB5WhPUljKKtgnoDUmeKnyFYOlI3SmNB0pv1odh%2FaFpKRMgpO9AWA0hA21RQlGKH7pI38aXrSJYgGKXtTvcd%2BjEo7UWFshNopaUiXoBPpRUpldRD17UU%2BcHsHfjtSe5ot1IuDdqTNES9kJn5uORTqGCPrCius5wooGFFWCCigoKKB2CigAopgJS0AFNNAwpaAFopCCihDCiqAKKYxu2kK0rCInGKgYVDGRNUdJjGmm4pCGGm1IxlJQAw0hpAMNNoaASikAU2kAlJRcBpphoAaabSAbTTTCw00w0hGdfQf8tU6jrU9hdeemw%2FfH60wLBptAgqM0DEpKVwEpKBCU00gG0hpiGGm1QyRfmjxVdqQhlJQAhpBTAWkpANphNUBz2rf8fRrM%2FvVsQdvYjFrEP9mrdZliGmGkDEqCefyXjBH3zijqIkdElTbIoZfQ1EtnbK2VhQfhTu7AWAMUtO4wptLcBDTKQGbrrbNNf3IFYVnBvfdSew0a%2Fel71mdCA02pZY1qa3Sp2AipKroBHSGgY3PWkqbCuNzQ1PlewhhFNPSh9hCHmko2ADTaCRpNIaQxhyDS0K5I09aa3WmD2FoI4pbjG0dRS6gJS96uwNje9KaTaATpSDikSLmjtTAbTRRzD0HfzpnOaL9CBaTB9aTdhhSUXE0OpuMCpGGKTnFX5Ec1hx9qbzWfJcpy0A%2Bnc03OVyOavUnUUnPUn6UCjn0JFpO%2Baz6lWPrGivQRgFFAwoposKKoAooGFFABRSBCUtUDCikMKKYBRWcgCiocrAFFZ84rhRVKqO4UVtGXMAxlqBkpsZAwqPFSMaaaaQERplAxMUzFSIbTaQCGmUAJTaQBSGgBKSkAhqOgBpptIBKZQAlMNADGFY13E1rOJY%2BlMRqW84njz371IaQDaYaYDaKQhDTaAA02gBlJQA002nYQKcGlkHcUwIDTaQCUUAFJQA00w0wOd1T%2FAI%2B2xWf0rYk7i1%2F1Ef0qxWaKEptADTWdqu4mAR%2Ff38VS1EJjVP8Aph%2BdL%2FxMv7sFNcgD4Xv%2FADAJoIgnqrVfolboMKbUgNNFSBma2N1si92eq1tEIkxSb0NYIlxiismbIjNIaQ0NpM8UwZGaZQMafQUnTrU7CGHFJTuSMNFF%2BwxppuaW5N9RBQelERiUyn1EJilqWJDOaQ0xCU3vTC4pptAbhRjin5gIKWpaEN%2BlH1osA08UUE7id%2FalzxTLDr3plBAUnek1oU9BaKCbCUUrWC4d6M8UwYn8XTikzULV3EFFaiF78UwY8sY6Vm97DDtSiixOtxf4ab%2FFxTe1x6o%2BsqK6jEKKoYUVSGFFMaCimMKKACigAooAKKA6hRUAFFZSkAUVk2AUUAFFABRTjKwBUbiuxSuhldhURFSMZUTVIxhFMxQA2mGkMYabUgBphoEJTakBKbQMKaaBDTTKAEooEMpDSGNphoAY1QzIJEIPegRkpLJY3fqhrZDB1BHQ0%2BpI2mmgobSUgCmUCCm0ANppoASm0xCYpyHPy0DI3FR0CGmigApKYCNUZ6UAc5qn%2FH3L%2FvYqinvWxC3O4t%2BIUH%2ByKnrJFiUlUA01BNCrujN%2FD0pXAlpGZV%2B8QKYCqysODmnUXAKSi4DTSUgKV58%2BB6VBismbR2EIporO5ohjdaaRxQWJnjmozQAw0h6UpMQ09fSm5pjsM5ptBIUw0hDTRjin1JG0daJFDTTTQLqFBphsN70hpX6AJ3pp60mFhKDRYdxOMUdquOxGwUlAhvenCiQxPWkqET1EIFJigoM0dqdhCdqO9TLQnm6CZoIqojt2Gk0tUw5gB5oNS9AFppoS94mwUh6UalaAetFJ92PoIMliKTPNO5KQuaQ8cigbvY%2BsqK6UcoUVQwopjCiqGFFMoKKQBRTYwoqWAUUkhBRUvQYUVzsQUUAFFABRQAUUAFFb0mMhZagcYrUZA1RmpGMpppARmm0gGUlIY2m0hDTTaQxtJSEFMNAxhpKYhKSpENNJQMaabQIYajNAFW8txLH79qqWFwYX8h%2Bn8qYjTNNpANNJQAlJQAhptAxppKBDaSgBtNPtTSEPPzrnvUJFMBKSkAlJTASoz0pgc5qXN3L9apRjLVqzM7uLlQfapKyRoJSVQDKzr%2F8AeXkNsfuN8xpoRbeRYICzdBVFLU3p826%2B7%2FCtHmAjQiwuojBwkhxitQdKT7gLSGgYw0x22ipBFFjk001izo6DTTe9SVYZSUyhhplKw%2Bg1qjNVsKwpNJ0qWMaelRGlrYQtMamAw0maW%2BhAZplMYtJjNF7iGtTO9G4rhQfeiwxGpOtO4Bjt75pKHIV%2B43v2%2FKkoEFJTDcKKV7oGBoqL9AGn6UpqmIb9KQ8UyROtKeRSkwa0G0uKXMC3EK%2FSkHFVbmiHmLQM45pWshX0EP5Ug%2BYetHPqPYccEceuab7U43JYh4pM0r8zsAuflxSU%2BUOovNJnPWofcvyPrKiutHMFFMoKKYBRVjCimOwUUwCikCCimAUUgCisZDCisGIKKACigAooAKKACiqpvUBr1WkrrGV2GKjNQUR0w0gGU2kgGGm0hjabUgJTKBCU2kAlJQAykpAJSUCEppoASo6QxKYaZJEaz762%2FwCWifeFCYyWwuvNTY5%2BcfrVqgQ002gBKSmAlNNIBKbQAUlADKaaBADtpzfNyKAIiKbTATvSUDEphpknNagc3cv%2B8arQLmUfWtiTuI%2BAB6VJWRYlJTAaarXtr56ZUhJB0bFMCstlM8oa7nEmPQVeHC035CKLRy3F7lxtjib5PetAdKG%2BgIKQ1I0Naqsz5qHsXFakFNIrJm4xuKSnYY2m9qBiGozUgRnrSGjQYym5pgITTanqIU9OKiNUIb2phNLcQUGqsAh6U0Cp2EJSEelLm1GJSNVWsIKbS16iHdaTtTHuNpD04osTsNo9qT2AWgdKaGLxSdAaRI080gobsPyGn71ItU9SULSZzU2C4tJVW0FcPpSUIoBndgUZ4NQ9BMD9fxpPx60lLTQXL3AU0mnuIKb%2FABdKcWMcelNU9jSuSxfpR%2FF97HtimpaDPrKiuk5woplBRVDCiqGFFAwophcKKY0FFIAooEFFJoAorlmDCipAKKACigAooAKKEAVXeuxaoZWeojUlEbVHUgMNNNIBhptIY2m0gG02gQlNouA002gApKQCU3NIBGptADKSgBtNoERNTTyKQGVcobeUSJWjBKJ49w696Yh1NoASkoASkpAJTaBBTaYxKbQIaabuKmmA44bpUZoAbSUwCom6UAc1en9%2FJ%2FvVFZ%2F8fMf%2B8K26GZ24606szToJSUAJSGkAxmAHNU5tQiU7E%2BdvaqURCQi7ebdIQif3avUOwBSVIyGVtoqm1ZSNYDaKg2GUh9KAI6KYDM0w0aXGMbrSdqAGmmUDGEUnalcmwlMahMY09KbQ9iUrCUUMV9RMZPHWk6UDEYDFIKQtUNakqug9A780vGaliaG0hqyQ6im9KL3DoN70n0qWwFpetGoBSEUuot0JSU2PQSm04q%2B4pMKOlKYPYBRmiWuhIueuMCm5%2BWjyKENJ9Kohhu%2Bal%2FKlbW4XEP4U3FPoPoOYncTTevPFZWS1FF62F6saTvVqww4FHekZn1lRXYZBRUlIKKsoKKsAooGFFABRQMKKACigQUUDCiuaohBRWYBRQAUUAFFABRQAVA9ddP4QKr1GaTLRE1RmkAw0ykA002pGNppoEIaZUgJTTQA00lAhKQ0DENNNADaaaQhtNpANptADDTTTAilQOpBrLQvZXP8As0CNZWDpuXkGm0CEptAxKKAENJSASm0wEoNAEZptIBvQ8U%2Fhx71QiMikpgJUbUAcxd%2F65%2F8AeptiM3kX%2B8K2ZB2o64p1ZFiUlFgEqrezTRLmKLf6n0poDPh%2F05i00x4%2F5Z0RwpFrSqnTys1ZJqinVmUFNNAFOY7mqHFZSOhAabUMsb3prdaBjaSkIYaYw5oH0GGmU%2BhYnWkoSEMakpksZTT1pWAbmmmkSJR2psLDcA8HpQaYeg2igBKaOKVrEgTzSZ5p%2FZDcTPNJSWwBijtSt1AaaOT0pgH1pKSFbqL%2FAA0lOQhDSU42ASk7UBYOooNTLQGhDTaaELRjim7CW4tN6cCoZelxMfhRn5feq0asQ97id6XjFAdBue1HHNIFHqL3HHHfmkPXjmk46k6icEdaTn1od1uLVs%2BtKK7TIKKYwopjCigYUUxhRTBBRQUFFAgooAKKACispoQUVzgFFABRQAUUAFFABUEldkdhlV%2BtRE1LLIiajNIQ00ypAbTTSGNpKBDKbUgBptAhhpKYCUlIY002gBppppMQ2m0gEpposA2mmn0AYaq3MAmT3pCKdlcNBL5MvT%2BVajUwGUlIBKWmAlNoEJTaQCUUxjTTKYhppppAG%2F8AvUpHpTAaetRtQgOVuf8AXP8AWl03m%2FgH%2B3W3mSdp%2FFTjWaKEpKYCUlSwKtzZRzHdyr%2F3hWd5NzaXgnk%2FfjGM5q0wsaVvdRTr8j59u9T1IBUUzYGKRRUNMrJm6G0nSkUIaZSGNNJRsMaeKYaHsFyOoz8vFLYBKG6UXEMprUxMbTDSCw3FNPSlfUkFpc1V7jYw0hoegK4lIKYmFJTiLoGKTHNJjQ3mkp2ELxTal32EIad2p%2BQ2Nx3oxxRcApPamiRKSl5j0QlNzg4%2FGiNwYGih3JCihdyW7MT6Unal1K0Ck9jQ%2FiH0AD5iemaSqRDYUdqOWxMuwlFTceyAcU3o1JK7FccabxSkEvhPrSiu4yCikMKKtAFFMYUUFBRQAUUwCikMKKACigAopMAorkkhBRSAKKACigAoojuAhqs5rsGV2qFqljIzUZqBjTTTQAxqZSASkpjEptQAlJTEMNNpAJTaQDaSgBtNNACU00CEptACYphpAMNMNAGdf22794n3hTtMuvMTy3PI6UAXTTaBCUUAFIaAGmmmqAbRSEJTDTASkqQEIpvTpTACc9aaeRVAzk7v%2FWt9aTT%2FAPj%2FAIP9%2BtWSjtQc80%2BsihKSmAlJQgEpDQBD9mjE3mKNp9qmp7gJmqkrZaokaQI2FNrI2G02kCENMNBQlMNAhpNMNNlEfWmEUMGLUecUgtoIajoJsNpMYp30BAaYRkVICUVQkJSGlYYh6UhpdSQpOlO4xGptPoJC009KW7EH8NMoCwveg8VQbBkAZPH1opWCwlN7UkriEpRTfYYlJ3%2FSk30EJRmgkTNIelHQXUaMqMUooeo9ApAKQrAaXt0p8xI00n1qtwlsLSdveosriT1sL070zrTUeg7rmDv70n8XtQtGI%2BtaK7TFBRQUFFAwoqhoKKLjCigApDQMSigGFFIAzS1QBS1IworKoiQorAAooAKKACirpbgNc8VUkrqYyBqiNQURmmGpGNpppCGGmUhiGm0hBTKAEopWGNphoENpKAGmkzQAw0lIBDTTQIQ02gAplIQ2o2oGR1lXcJgmE0f6U4gaNtOLiEN37inmgQlJSAWkpiEptIY2m0xCUUhjKKAG02mISo2WgDlr3%2Fj5l5%2FiplmcXUf%2B8K3epCO2%2FiNPrMsSkpAFJTASkouAUlICOVuMVVrNm0BGpKk1GHrTKQCGm0cwwqNqaENxTetS9SyPbTT0oAZmmmhoEIMUynEBvFBoE2MpDS3IG0dqEIbRmquAhpBSegCE02mmCFNJ3qdwE70lNuwXE5pKNwEpetMBckcimk0dRCc0VMhCd6Qmq5eorjerfzpTUy10QmN%2BlJjP0qvMBf4jnrmm0lruSIaB0ouN6gTkjFHakwYtID81NCDjOOnvimcbD%2BdPYcrCnNN%2FGkRuL0NHFK1wigP4YpvSlqkF%2FePrWiu8xCigYUUxhRVFBRUjCigBM0maYxpNFSAUUwEzS0wCnUgHUUNXAKK55wEFFZgFFNRuAU1mxXTCPKMrs9RMRVMZAxqM1AyOmmpAYaaaBjKbSASkNAhpptIBKSgApjVIEZpKAGmkoAaabQIKQ0ANpKQhKbSGMphpgMIqJ13LikIzFzY3HQ7DWoCJF3L0pjEpaBCUmaYCUlADTTaBCUGkMQ000ANpKBCYprCmByuoLi7lz61Baf8AHwn1rbzIW52%2FVs07NZlhSUIAopgJSUDEpKQFaQ5NRVkzeIUlQUMNMplDTRSkA00yncaGnpTaFqMYTUbHtQA0D2ptMWww0p6VK0ENpjGjzFcaaOtHmKwmKSiwbDKSnYANIanciwlIKdg6i03vQimM%2FCirFdC%2FjSdqlgJ1paL20G9BD1ptMaF%2BlIKnmuRcTGKM0wsNzQ3tSsyWN7Un%2BFLUbeggpCfWrtcNLACSOlHTml10FYKAKnm0BrUD1ppX5aaIl5Ct6ik%2BbHOKPUYNTOMc0o8wDt24565pBQRYXpTadhy01PrWivQMUFFIoKKBhRQMKKoYUUgGE0lAxKSkMM0ZoAKXNIBKXNNAODU7NMBaKVgCilyEhSE1WwyMvUTPRuBCzVGTUlEZqM1LGMJphqQG02gBppDQDG000guNpKkQUlMBKYaQxtNoENNNoAbTaAsFNpCENNoAaaSkA2mmgBpFR4oEV7qATRFTVOym%2BzyGKXpmgDUIphoAbRQAlJTAbSUAJS0ANNMNAgzSGkA2g0AZl9ppuZfMR1B7hqzf7Lu4mDbUbH901omgsdMHzzTqQC0UAFFMYlFTcBtMlPFJlR3K1JUGw0nFNpFBTHpWGM7U2h6gIelNqtAFph60tiiPtio8Ug2E7U3tQDRE1NzVCsKaZ3qLkoKYaA2EzSUBa4mKTmmDENN70EAaQUygpaUtBW0G02giwlJ2qWUtgpRzTvpcGDU2qXcVhM0VLsCeo00n0pj2BsFic03OTRcnm1DvR3qZag33Eph6VQuYceGxR60kF76CcZo5pcglcMc00e9abaAB60D361O%2BgBn5j35pnOOaz%2BGQaIcaQDB%2BXirexEhM9OKTp25pqzGj62oruMQopgFFIoKKaGFFAxKKdgG0lIBKbSHcSlpAJSUDCjNFwF3Uu6mA7fRupgL5lG%2BkA3zKbuoAYajNAEDU2kUB6VExqREZptSMSmmgQ2kNIY0000ANNJSEJSGkMSm0xCGmGgBtNNIBppKBDaDSGMpKQhtNoASkoEJTaAIyKoXtvuG5PvCgYmn3m4%2BRJ94dKvGgQ2koAbRQA2koEFJQAlJimAykpAJRTADSUhiU6qAN1G6gBd1G6mIM0UgEqB2y1TI0iR02s2boTFJ3oENNNNK4xvammi%2BoxDTDzSGgphqrAMb1phpbDGGmGq5rk3G4pMUgXmIeKSpt0ENamVS2Ew7UnakMPrTW%2B9Qrg9gptBmNbrSVXoXsBpKkBabmncWglIelPcBP5UtS0IWmdBSlcbEpKpMjQSkpJsEGeaaaGGgg9xS09RNiUdqQWQ3%2BKjNNCVhc0ZNGnUBufak68UPVhewnejvSZDFpPzpFCEEcNSDPv%2BBpqVhW1HD2pgPtnipmyrPY%2BtqK9I5wopjCilYAooGFFBQhopNgNNNoGhKSgYlIaQhKSmAUlIoKM0ALuo3UCEzSbqoBM03NSAwvSE0hkVFAwJqE0gGmm1IhKaaAG0lIYykoAZRSAQ0lAhKSi4CGozSASmmgQxqaaQxtIaAEptIQhptACUlMApKAGNULc0gMu9g8t%2FOUfWr1pcC4j%2F2u9AiY0w0AFJQAlJTAbSUDCkNAhKSluA2kp9ACkpCCigYlJSASkqhCZpN9MA3mmVlI2gFJSNBGplAAaYaSGMNMPFMdxGplJDA1GetF9ShhNJR0E0R009aTiIb2pKBAaaaGUIab1o21IaEpKYhneg9KLXEJimnpStcaCm07aikxMUlILig0jdKaQxBRSZA3NFFig7U32p6ibD60nvTEMbjtS%2B9DDQSkojFXAbTqlt3JsIaTj1qrPoFhuaKROwUoahL3bhcaaKcQTV9RO%2FXvSc4pv3kVpcKDzkUiUJ2oGPx%2BlJi3dhvT7vXNC9OKU7WBXR9b0V6ZggoqUMKKZQUUDCikAlIaBjaQ0ihtJS2JEpKYxKSkMSjNIBM0UWGxKKAAmm0XELmo2NAEYpTSGMpM0ANJphoAbTTUgNNNpgJSVIDDSUANNJSC4UlIBKTNADTTaYCGmGlYBhpppMQ2kJpAJTaBjaKQhtJTASigQ01GaAIpE3CspwbO6BT7tNDNaOQSx7hQaRI2ihDGUlMApKQwpKBDaQ0AJTaBBSUIYUlACUUAJTaYhKaaRQlLioepsgpjVNiwpDQA3NN7UMBp6cVH3xQMRhxUbUxEZpvaluWFNNHLYdyM0n3qXQkQ8DFNPSgYlJQSNPSo%2B2KLCuL0oPSlZoW4wc0nfmn0E46h0ptFwsIaO1MBDTWPNTygwNL1q%2Bg%2Bg3FNzgAHrUa3JDvzSZzVlCUlImwU2gQjUn8NRcYdzTOc9K1sSxT70nepepLFpMfLVofQaOOaBS0uTEXijHeo%2BFjG9O9J%2FC396q9CWugrfe4pookMUnNN49KXUlXEpaJNj6in9aZjv1zUFLU%2Bt6K9Q5gooGFFBQUUAFFIYlJQMbSUgEplAxKSgY2ikA3NJQMKM0hBRmgBtJTGBphpDG5ppoAbSVIDTTaAENMNAhKaaQDaQ0hCUlIYlMNAhKSkMSkoASkpANNNoAYaZQAw0lIBKbQISikAlNpgNooAbTaAGGq9xEJEIoAo28xtJvLk%2B7WrnPIp2EMNNpAJSUAJRQAlFADaSgBKSgYlB6UCsNpTTAbSUhiUlIQ01GW5xTGPFOqDYaeKYaCgptA7jKKgZGxpKAGmmGgLjGpmKLjEpKd9AZHtpuMUrjCkoUtAEpjGgkSm4oGI3WmtTEJxmg8mnawhtIakQjUU2A00mKZPQDTTUsA7UzrVx2ASl70aDuJ1J9aSpBiGmn2pIkQnBNJVxVyQ96T6UWG9QpOfyqbdBMO9IaNguHFGaW7FcaOlGeavyExPejqPmpdRpidMehoNFmTJ9hP4s0maXI7DXcXHpRSu3uJsbilxzwO1UxJJn1tRXeY7hRTAKKdi0FFSMKSmMSikA2mUwCkNSMjNJSASkoASkpIYtJQAUlAxDSUANzTKQxKQ0CG0lAhlJmkMa1MzQAlNqQEptIBaaaBhTDQIZRQAlJSGJSUCGUhpDGmmGgQ00ykAUlADaQmgQ2gmgQlJQMSm0AMNNpCKN9a%2BavHWk0y46wSfeHSn0GXWFMoEJSUAJSUmMKKBXG02mIKSgYhpKACkNIBKSkMQ0wtVDKk9wIxTLTdLNvP3MUNDW5eorM1EptBQduaaalANI4plMBKY3y0DGHmmGi3UYlRniq2GN6Ac0hqBDT1pvemMbQTSEJ1phqraBYaaMikSIaaaRXQbSUxJ2ENJ9KTIEppPrTHcO%2FNGabutRpDaTPFP4iRR0plTs7CGnrRVPyAXP6U2s0rg9BP4s0h6Vr9ogTFN%2FCp6lBSc0r2AD60lDloDEpAcVUtiAoqQi7aCdOtJ2ppiD0NFTrcYnpg4%2BlA5o1FcWmd%2Fwo1EIPc0tWSrCfSk%2BpwaiSKUuiPriivRMAopjQUUhiUtCKCkpjG0lJgJSGgYyipYxpptACUmaAG0lAC02kMKTNACE0lADabQAlJSAbSGgBlJSAaaZSASkpAIabQMKKBDaaaQDabSAQ02gANNoAaabSAaabTEJSGkAw0lJjEppoEJTaYCUlIQUlBQ2kNIkYeay72Axv50ec96aAt2t0J1wT%2B8qY0hiU2qEFJUgJSUIYlJTENopAJRTASkoGIaaaQEbtVK4uMfd5NAx1rYtcHzJ%2Fu1o7EQbUAAFDY0MpKzepqNzSUxoa1FA7DHppoGNNGKlgRdKjPJpjG0hp2GMzTTSYWGmm0AGKbQIb7UygOglJipvqTYKaadxjTTe9MLBR7UbiExTcU0FhDRVEjRSdKjrYAzxTTxQwButNqQYpppprTQmQUnUUdQGfWl4%2FyKvqSM70nSi2gwopEuQ00oquwkxKTp8tQWIeopRzjNVZtXYPQRfujIwT1pBR1IYtN6VKEtBx68U361HL1GlcbxTQo35A5rbaIuug8jimnPqTxUW7j8z64pK9A5xaKYxKSgoKWkAUlMYU01AxKbTAQ02gYhplABSGkA2koGFNpANpDQUFFBIlNzQUNpKQCUw0EjabmkUNNNpCCkpANpKBiUlIQhpKAGmmmgBKbSEIaZQMQ02kISkoGNptIQ00lAwphpiEpKQxppKAEopCG0hoAa1ROu4c0CMqVGtbjenStKCYTR5pgPppoGFFMQ00lIY3NFMQ00UhiGkoAKSgBtQyPgZpAZ0k7O22P860LOw2DfcdfSquOxbeT0qOoZaEphqdi0FIaBjTSVNikMNNNVfQGRmkzxSewxjGm9KNxDOaD7UDGMKYelKxQD7tNH3al9gQh6VGeOlMQDpSN0qhXI%2B9BNLqIbmkIp%2BoDD7UnQ0hi0meaQdQzzTTVEsaaT2pO5IU0imO4dKDSe4DKXbkZp37k2ENN7UAHHekoitRXG0nt3o0CQdaaPamFhvvS9qlu%2BhLsNpe3tVX90BnPpS9%2BO9F7Eh90fWk607uwdQzTaLWAVunvmk61HmHkJ0600%2FpTRNx2c%2B3tSCi%2Btir%2FZFHXnOKQnFL7Qtdj62zRXomNxBS0ANooKCkFAx1FMBppKQwpKAGU00hjaSgQlJSLEpDQA2kpAJSGgQmaKBjabUgFJTAYTTDSEJTTQMSm0gEpKQDTTaACkqQEpKBjTTaBDaKYDTTKQDaSgQlJSAbTaAENNosAU2kA2ikAlNoASigQYpppgNNNNAytPEJUKms6J2tJ8N92i%2FQDWDblyKSgQlFMBpptIYlJSAKbTQgNJQAlIenNIZWlmCjJqkBLdy7U4FUkM1re1itVHRn9aHfNSIZ1p9JmsRlJUFiGkNPQBDTOlBSG0h5pCZE4qPk8Uhhtop8wxhphNIe4xqbTGGaYKLXEJTO9AwFMP3qQkI1IcUCY0ik5p9BMaRSCl0EFMzQFhaQ0xDKTFFx7BTafmQJim4zUDTDvQevSq6CCk%2FSp6BawnNJinHQz6jW9z3pufm%2FCrt1G2mJSCh6h1DrSVLZLVwoqlsV5CUg9KWhOzCkFPbYfJcQ0D%2FdzSsgW4HrSE4b3oYrB36%2FpTeueKajpoAd%2BtLjsKnrYXUOaB96paHz6n1oTSV6JzoXPNGaooDSVIBSCmUOzSZoAM0lIYlJTAbTTSAaabSGJRSQxtFUA2kNSMSm0AJRSENNJSGJSZpjGU2kIbTaAG0lIBKbQA00VICUlAxtFIQ2m0DEpDTAbSGgQym1IgNJQMbTaAEptIQhptIYlJTASkoJEpaBiU2kISmGgYw1UvLfzUyOo6UBYhsLna%2FlSVfPNMQlJQUBpppiEpKQhKQ0hiUlMQxmxVS4n2j1poohtrWW8ffJ9z1rXUJAm2MUmBGzVHSYCqKeak0iMpDS6lhTaTGMpGosMbTKOghrdaa5wKkYykPAosMh5pOapOwxDzTGFDKEbpSUhDaRqodhO2aZUruSxn1ppo9SXoKBRmpGMam9qoGHakouZ3GdDSdqfmFxKQ1DGJzR0q7AJmmjrRawhTSUhDT7UlUNsQmjtRsQhKaBTbJ5E3cKQjmlJl2G96KSZElbYbS1TBNiHrSUW0FcTNL2pSsLmbYE03NTBXZQYpO%2FNU9USB%2B8aTtRFlXuL9aQ9DWai7mfUTtRmqcVa5a11PrOivSOcKWkUFJSGNo6UWGKKWmA2kpDG%2FjS0xje9NpCA0zNKxQlFACUlAxtJQISkpDG0UgEJptNgJTakY002kAhptADTTDQIKSkA2kpWASkoASkpAJTKYCUlIY2kNIQ00hpgNpKAG0napAQ02gBtJTASipEJSGmA2ikAlFADaaRSGNNNPSmBm6hbn%2FWp170%2BxuvMXa3WmIuUlAxKSkISkpgJTTSASmM2KBFGafPyr96rFtp2795cHg9qd7F3LrMAMIMCoiaQhlJQMevSlNItDDTam5YUlIY3pSUARk02kMaaa9MBlNPNIobjb0ptHMMYab2o9RiU3vRewkIaTFAyMmkp2JsMYUtHQT3GGkpWuAlN6UraiEoNUKyG8YptAmHU800j5jT2EHamtR5iG96QcUN6DA9aKW4mhKb19qlbib6CUd%2Fwp3BDaSnbUXUTODR296THzCE5XOKQVVhN9A%2BtIKNxCUcYobEA%2B70pO9TuwCmDj1oj2JtYcabx04qoSGL%2FEB%2FdpCBn5ulNy1EGOaTjtUcwWvoCnmgDk5P6Ur21BbH1lRXpmAlLQULRSGMpetMA6UUihKKAGmikAlNpDG000DCkpAJSUDG0ZpiG5pppWGNpKQBSUANpppANpKTGJRTEMNJSGNpKQhtNoASm0hhSUhDabQMSkpCG0UwGmkoEBphpDEppoEMNNpAFJQISikUFNpgNopCEptMApKQDKYaAGMMisq6iNvN5i%2FdPX2oA0La4E8ee%2FepaAGmigBKSmAhppoAhd8DniqJL3D7IwSKFoBp2tmkC75OX%2FAJVI75oYERpKVihKMU7CH0hqGaoZSUFDaKhDGtSU7jIyKaeKkYzrRTERN1o9qL9h9BrUw0IBKY%2FShFEfanGgY3%2BKkbrVCGetR1JN9BDSdRSE2NNNp7BcM0gp6CvYbRmh6C9BhpKBDe9HrSQDefwpP4aT1GGKT60wE9KQ80eYMQ8UlPzF5CHikzS0Zk9xlC9K0QxO9GalgB6UnJqU0J7hnnmmgfLV30DQKKchiE8df1oz%2BtQ9iRP50lNBIQ9aO9LqTe4fSk%2BtRu7BuHzdcc5oXg1dtCktRM9KXnrj5ajlewpNH1jRXpnOJQDQUOozQMbRSAM0hpooKSmMSg1ADaaaYIKQ9akobTaYBSUANNJSAbSUhiU00DEpKQhuaSgENpKBhSUhDTTaAG0lIBtNpDEpKYhKSpAaaSgBtJRYBKKLgIabQIQ0ykMSkoENNNoASkoAKKQxDTaBCUlIYlNoAKbQAlNNAhtQzR71IPSmMyV3WV1t%2FhNa0biSMMO9MQtFABTTUgMY1XmmCCgZXihkvJP9mtWKKO2TC9abAjdsmkpCG0tMYlApDHUh6VBqhlJQwG02kWBplADaibmkAmKQ802wGsKb7UkhjTnsKZ2prcY2mnrVCENM5qLGiE6UxuaRK3G0lDY0J1%2BlN9qad0ZsaelNp7jsNFKeaRNhjAEdM4pKrca0EpKgQ3iiq2E%2FIb0pKAsH1pKLXYxrUmT2qm0QAGetHG2odxjSB1ptS2yWIaKsSE43UhxQ2MKQdfwpIhiHfnAxRWjSHpYTOKav6VFxbC96bkfWqb6ALTSDkUloA7j%2BKkp9dRSSsJxupBn%2FAPVSvqRqwPNKBxx3qRtvYToD3pF44xSWoj6wor0rmQUVQ7C0VJSCkpMYlHemAUUhjKKGA2igYlNpAJTc0AFJQMSm0hCUlQxjTTTVDEptIYlNNACGkpCCkoASozQAlNzSEFNNBQlNoASkpANpKQhDTaYBmm0gsLTaAGmmGkAlJQIKbQAlJQMbRQISikA00lIY2koEFIaQhuaSmMaaYaAK93AJo8flWfaXD27%2BXL0prsM1MhhkUUCEpjNigZUmuMcDk0tpZG4%2FeSn5aNho0fkiXbGoAqMmkIZRQIKSmMKKkaFptSajTRSAYabQWNpvfFIQ1qZT3C9hufmopWuUMNN96BjWUN1FI3Ap3uBHQaGwQ3NR%2FwAVKxQGo%2BgpbCuJ2ooB7DKYafoSJmmk0CuNFLnjimEhKYajYlsbmiqEN%2BtNzVNjA9Kb2oiIM03t%2FWjqSJSUnqMOaTtVt6Esd1zUQNK1hWCkJ5qE0OQYpKpoQhpO1LoJyHdTTDVoLgOBmk7UW5tQA89uaTtS21CwnIPejrSuhS3G52nmncHmkRYKYPX1pR3LuO7UmcVVupAdabUc1mPVH1hRXomIUtMYlFAxaQ0ihM0maYBRSuA2imMKaaW4xKSkA2kpDEpKAG0UgG02kMQ0ymAhpCaQhKbQMQ02gYUlIQlJSAaaZSAKKAGUlMYlJUsQ2mmgY2kpCCkoAKSmAymmkxCUlIAppoGFNNAhtJSGIaSmIKSgYlNoEJSVIxKSmISmkUxjaz9Qttw3p94UIBmn3Wf3T9avGl1BkTyBRk1SmmaT5Y%2FzpiL1nYhf3k35VZZ%2BMDpQBCabSC4UUFBSUwFoFSwQGm1BqNPWikMQ0w9KYxlI1IBvak9qYDMc0VJRHjmmH7pp3AGNMPpUoBmMZpKd%2B40MK96aadyhDxTOKXMthbCU3PFMfQZTaDMSm0kTYBimtTGJmmmku4hBTTT8g6jTTadgEoNDASkwOlPclh0oqGwQ3%2BKiqWpLYnWmjpSv7wrWYNTcetNFMG%2FrTfeq3EJxSGmomcmKDjpQaWo7DQM0nGOaOoCDg9KX5eCO9TLcXMBNJRsF7CHrQPfj2qrpMNxCOego7fhTbW40B5o7VCM3KwH1FJgnrR1uXc%2Brc0V6DRiFLSAKSmUhaM0DG0UwEoqRiGikAlJSAQ03NFhjaKBiU00CEpKXqMbSUAJTTQMbSUAJSUAIabUgJSUANoNADaaaQxtLmgQ2mmiwCUlTYBDTaAGmkoGJTaQgzSUDEpKBDaQ0CEopgNptIBtFSAlJQMKSgBKSmA00UgGmkpiEpKBjDUZ561IzI1CLyZfNT8amhvA0Iz1qrCG%2FvJ2wo4rTt7VLYZPLUhEjvmoTQAlFMBKWkMSijcYUo6UioidaSpLG4ptIoMVHQMaabSGFM70hCU0mmMaeabjmloMa1RtR0GJ1FMIosAmaZ25qkUHaojSYuojZPFM2mlsA1wcYpvak2SI%2FNNNF%2BhIAUh4ouJjWpOKYhp6UlX01GJTe1L0JGnpQT8vegJDaO9K%2BhNxKaKN0AvvTR14FLZC8hTUeaLjFPBxRn5aqOohvfFBolG2oCU0jnmhaiaDvml9qVraIUROnSm5570xSDvSY9%2BKJO4hW60nOKXmPyEz60oo6lAcZpnQetMQvX160gHHvSt2ITDoOd34UZpu9rCZ9WUld97mQUtMoKKYBRSGJmkoGgozRYBKKQxKbQMKbSGJRUgJTaBjaSmIQ0lDAbmm1AxKbTAQ02gBKSkA2koASikA2kouA2kpAJTTTGJSGkAhpuaQDSaKAEptIQlJTASkNIQlJTGJQalgNNNpgJSUgEpDQAlFIBKbQISkoAbRTGJTaQDW6VETikMp3sq7MHBzWTbwFfnbpTEb9n5YiBWpmakAyimAlJQAmaKBhRQIKX%2BGkyoiUlQaiU2kA0000xjTTakENptAxppKZQlMJqXuIZ1ppHfNV5lDe1N6ihANpnOKQxP4aj7UeYB2ptK%2FQBhpKlpkDDTa005SUxT1603rSSGxtJuzQKxHSA9qpaiE6UnenYQlJjNDC1xOlHaptoA3%2BlHaiyJCo%2B22r6E3FOKTrmoGDU3FO9hdQ6U3qKExsXnFN%2FwC%2BfxotcAxRmgQmPm5phz3x0qtyRzdcim5pe6T6C96PyxSNLjc0ZoIBuTQx4561V%2BghOgFJ3piDcMil7dOKixSSlofVdJXoGAgNLSKFzRTASikMKKoY2lqBjaXNMBtJSGFNpAJSUAFNNAxtJQAlMJpAITTaChKSgBKSpEIaShgMooASkpCEptIY2kpgIabSC4U2gAptAxpptIQtNpAJSUwEpDSFYbSUDCkoAQ0w0AJRSEJRSASm0DCm0xiUlIQlJQAlNoAjY%2BlUbi5xwvLUALb2RkIkl6VYuIgYyo6U%2BoGbE7W1xtPIzWoDuGRSkJBQKBhTaAEopgFFJgFL2pMuI3vRUGgU1qQ0R0hoGJio6WwxuaT3oAQmm4qrjGUjfdqWPqBOetQdTTF1Gmmk4NCHYKa3SmBH9aQ8UItiGoyakhjSaYTzR1JDNNNOwBnFNpkjabQIZzR2pXFfUT601qdxMToKSk2tyubQQ0UgD6U3FU9NhMWmGjmvuToA5xTAflpeQhc0mT3zTKE9abn1oSJ%2ByL0pBxU8xIE0ZzjNXYpMQd6CfSk5WJew09KQU9CRR0pB71K12C9g78ZpOfWq13AXtmkPT2p8xN%2FeAHuKZzU9bl6WFx7D8qU9PpUy1fuiTtqfVVJXpHOJS0FIWikMSkoGLSZpXGFBoGNopgJSUAJnmkNIbEptACUlIY2koYhKbSQxDSUDGmm0gCkoAQ02gBKbSAKSkA2kNAhKbSKG0UCEpKBiUlAhppKQhDSUDCmmkA2koBiUlABSUgEptMBtLSAbSUABptABSUgG0lAgppoGMJqF5OKLCKEs5k%2BWPv3qxZaeEw0vpwKrYZePAwKhepAqXEIkFQWVwVbypOKANClpANopgNooYAaKBi0uOKTZcRp4oqGWNNNNAxhpKAEJppoAYajakUmJS0wG8U3IpFWG1GRxigBhptJFdBO1NqrkkZpv1qQY003FUMYeaZU9SBKT1qupNxKTpQVYaTzTfw5qbE2EpKHZEgaYRT6Axveina4rCUlNbiE6UtEn0BjaKTHFDaM%2BvNJrsTYSk71WwtbjT97rSUymFAqbE3DuTTeNtUmG4Yx6GioFYRsHvR6Y%2FSn6iQ3vRnFLfYmwZOPxo61TZd%2Bgbf50nBTNK%2FUztYQ8ClzzwKVubUG9dAPFC%2Fep%2BY72Z9UUZrvMAooKCigYlGaGMKKQCZpKZQU00AgozQAlJUjG5pKQDTSVQxKSpGNJpuaBBSGgBtNpAFJQMSkqRDTSUFCE02mIKSkAlNNIBtJQMKQ0gG0VQhtJUgJSUAJRQA2mmgBKbQAUVLAKSgBtJQIbSGgYlJQAlJSExKKYxpphPFIorTShRVL97cthRxVEmja2qQKCf9ZUxNIBhqM0hDWFULuA%2FfTtVIZYs7jzVw33qnqLAxtLTHYSkoAWkoAWlpSLiIaSs7mg2m0xDD1ptIpCUhp9AtqMptSUIRTf4aaJGsaZ3plhyKjas9mAnaojT6lAelNzRYVyNqjJqtguIc0g4pANqOp8iQNMrREBnim7qQ7ie9JmixO42mZ5oQkJzmilYbEpDz1qvMOgmKTtQQIfvClJotcYxs0h%2B7TDZAeKQDiouToJnIoPFPUe4ygnp1qxMGpwNTfoLVEdLVAG7sDTKQna4cUfjU%2BogycnjvQc45zVRDbYCKDxScriV9w5NJ61N%2Bw3uDH%2FACaQdKaViA%2BtA471Vij6nJoru2OcSloKYlFCASko6li0lIAooASkzRcYU2gBabSGNzSUAJSUAJTM0FCUVIhpNNNAxM0UAJTaQBTaQCUlMBDSUgEopjG0lSIaaZQMWkpsQlJUgIaSgYlJQISkoAYTSGgBKSgAoqQEooGNpKBCU2gBKSkMSkpiEpKQEbHFU57kJx3oWoEMNvJcvubpWnHGkSbUFNgITTaChDSUEjKjbpSAz5UNvLvTpV%2BCTzowwpjJMUUguJSUAFFABSmkzSI2g1BY3tTGoASmkUhiUlADMYpKBjTyKbimDEYUzHNR1Aa1R9RmmWhvaoifaqCwnak6ZzS3AjY1EetLcYHrTT1oJEzTfrSt1IbG7abT3FcaaaaYCL0o6ihBfoNJopvQjYYw%2FnSUraFbiHrRQIKQ0WHa4h6000bkyEPWkxSaDoJmkNEYmdxv5Up5q%2FhZSQlIMZwKnzG1oH8%2Feii32iNmJ35pKu9kLmE%2FipO%2FNJe8UtUHajp97vQ0JsOnegip2Ie4nbFGeKdgFbngU3tzio3HFCnk%2B9NPXp2q9LEtai5560g5xnvU6ApH1OTSZr0XsZBRQAUlMYUhNKxSEoFIYUU2AlNzSGLmjNMBM0lSMaaSkAlNzSGJTafmMSigQ2kNIYlJQAlNzSAKbmgQlJSKEpKYhKSgApKQDaaaQBSUxiUlDEJSUhCU2kMSm0gEopgJSUhCGkouMKSgBpopAJTaAEpDQAlJSENqKR8UDKE10c7U61LaWW%2F95NV7ITL%2FAAq4HAplSMZRQMSkpCENRkUDGSIGGDWdGz2kwX7woA1EbeuRSigQlFAxtFAhaWlI1QlNqCxpopiG0ykMaaKkYlMNAxlJmnYYlMJpAI3SoiKLjGN1phzjmgpDaTtzTvcBhpjcetICM02nYzGmm0xeYZIprdKCeo3oKSkMQ9KTNIncTimmi47CUnfimRbUbSZ4q%2Bg3oIpxSHpUXDqJSdadgsFNpsQjdKO1F7CEoFS9g1G56ik5qrpCYp9KTnFITQnSiny31BWGfxc9KXtyKT0JuBPNH1zRygw5pCaHGysAH7w4zQOnep2QO4nNN7Zqo9xp6ju%2FFAxjk%2FlTkhdRMClpc3YHHU%2BpjSV3mA2lpjCkpDEozQwIPN%2FebaloKDvRQxhTaQBmkoGFJSEBpuaQxlFAxKbSGGabmgQlJVDEpKTAbSUgEopANpKQCUlACUUrAFNpjG0UAIabSEFNoAKSgYhptIQ2kpAFJQAU2gBKKQDaSmAhpKQhKSgYhpKAEpppAQSSAVQeR7g7Ys80wL1rYCL55cM1WqNwI2plAxKSgBKDSASk60MBhGarXUPmJ7igRWsp2jPlSfrWjmn1KCkoEJS0hiiipZURtJUM0G4pKaQB2qHnNFgFpopDA0ygoDim0AMfpURyaV7jQrVGcGkIY3tUZzVoq4wmm1I4jTTT0osBGx5pOOaojm6EZ602k3rYmwdqaaN9BDaKpbANJptLYjYO9NPvUR3KAkYphptNoSD3ozzzVIUhtIakBD97ikHA4q7CuLnmojwnHLVXL1E0OfgnnOKbWVtRXQmaOlaIth3pvSk3YhITFKaYdBPrQenak%2FIXQaKP4elTqAevGKO%2Bat3IYmOaG7UnqPYXimjPIFQ7DXvCE4NLj8%2FeqS0ItqFJ9KpAn7wnApc5HNO73K%2BF8x9TUldzMBtFQMKQ0DEpKYFCX5btWq6KYxaWlcpCUUgG0UALTTSAbSUAJTaQxKSgYU2gYlJQAlNoAKSpAaaSkISmmmMKSkAGkoGJSUAJSUCEzTaQBmm0AFNoASigYlNoAKDUiEptABSUxjaSkxDaSkAUhoASkpgMZsVVnnCilYCssUl1JycCtKGFLcYWqfYQ8mmGoGJTaLjG0UAIaSgAptMBDUbVIFG9gLfvEzmn2Nz5ibWPzUMZcpKAEopiFFFSzSIlIaRQ3pSVPUYlNPWncYmKTFIBrdab3pANNNotcoaeaY3SmkMjzTD1zU6jQ3dxTT93iiwyLGKbVXBIaaaxFK9xDDTDQR1GUz1p6MUg4ooBDab19aL2E9BMUg5pdBCY5ptFwEPam96OYjYM0naqUeo90JSDP4UpaiVw60lNsYhpPrQg6DelID7VFjMSl6irRYnWihg2JmkJo2JDqabjnk0IUg%2FOheUGKW5Nw%2FCk6iquKSD7vtzQarXcYdOc0dAeay5erKG%2FxE0jdOOtVpcz6jqQdOtFuhUezFPHWm0l7ugSXQ%2BpqSvQMBtFLQApKB2EpKCkRugbqKf0FDQBRQMKSjoMb1ooAWm0hiUlIBtFMLCUykOwUUANpKkYhptNgJSGkAlJSASm0AFFIBKSmA2lpANopsBDTT1qRjTTadwCikAlFADaSgBKKQCUUwG0GgBtNpCEpKBiUlACVGzYpCKU1xjheTS21q0pDydKY9zQAVF2rTDUoVhnNO7UANNJQMSm5qgEpcUmIbRSKCmGgCMisu5ha3fzIh8vehdhGjBMJUzUlIBO9FDGApTUlxEoqLFiEU3tVAN6UjUgG%2FWkagoTFNJoAaaYam4xjZph5p6sYw00ikMZ0zUfWgY09KZVIENNRkc1QmNYYPNR96hbkiU00nuSxufSgmqSENpvehoLg1JSt7tg6jW603vQtib2Y0%2B9FV1sAgoNFhob0pO1AJ2F5HakzSaXUnqNo7U0IbSY%2FGnsITHPpS0bit3E%2BnFN9KGriE5p2KmStoNMTvSH7tWib3YH86b95cdM1HL1DQU0etOQn72oho7UrCE%2BUtt4oNW1oPm0sK%2FXIFN5K8cetRDzEH0p3QVdlEV2I%2BeOlJ35GamUhrc%2Bo6Aa7rGCCm00UN70UdRiZopB1EpaLjCkNAxKSgYUlIBOlJTAWmmpGNpKYIKaakoSkqhBTakYhppoYCUlAhCaKQDaSgoSkzSEFJQA3NFMLhSGkISmmkMbSUgCigBKSgYlNoEJRSASigGJRQA2m0xiUlIQlNNAETyYFUXnMnyoDQii1bWOz5phz6VbJpMRGTSUhXG9KAaYhKQ0ihKbQMKKGFhKKQhKbTGMNRSKGXBpiM1C1pcEZO01qI4Zc0hgaWgAXrSmoZcRDRSNBKZSEFNNUA2jqaTWgxO9MPWkMawqI1NxiUyqTAbimcCnuMZ2qLvUplIO3NRkUrhuM%2BppjdOtX1JI2Oab061LFEaaaTQyWxM4FNqgA4ptRcQ1mzSZ4pvQjUaaT9aSiNDTnNGSKtJJhYQ9aWrdrBcaaTrWZPUUZpp6%2B9FlKQ9BO9N7VpJWJuJSg1GrGNIzTenFOyDcd92k6%2BtJxEn0YmeKQ57UyWuon1oONvANO9g9RO9LR5ExfcTpTqTQ%2Bo09eKQk4zU%2FDoPcdk%2FKu75aaDwPetCBSWViPekNZ2UR7sTOKdt69yaGg2G98fN%2BNC%2BtTypLUWnU%2BoqTpXo3MQoNNCG5ptMoKWkMbQDQMWmmlfQaEooASkzSGJRQAU2kMSigBM02gYhpKBiUlADaKQhtNpABpKAEopFDaSgApKBCGkoAKSkMSkoENooAQ0gpAFJTAaaSkAlJQAtJUjEpDTsISkoAbTTSAYTVeWcLRYCqvmXT4TpWlBbJB7t603poBITUZqRjaKYgptACUhplCU01IhKWmMbSUCCikMaajNAivcxCVMGq9rJ5Z2NQM0MU3NADlpTUsuIlFQUNxSEUDTGE02goMUUbEjaQ0FDDTMA0hjKZRypDG8imYoGRNUQoXmAue1MzR5De2hGaY304pkjGphNBIhOTTKHoKw00m7nGKXULB1pnejS4hKKHLULaCNTKESIaT3ot1GHem4p36E8yuLzikosLqITnim55o2CSEpuDVBsLSHpUpqLJE9aStFvcAz1zSVPKAdqUe%2FWpFcTjv%2BFFU5e6PfQTjNJ2qUm9yWxCMke1KKaWpXQQ9fxpMH2xS6mcheR6UEZj5ojqArcN1ph4q9C0xaCe2eaEmD21F9%2FemjrzRq9iND6hors6mAlHagYlNJouUJSigBKSgaFopDGmkoGFNpAJSZqgCgGjoMKYaQCUUhiUlMQU2pZQlIaXUQ2kpjsJSUhCUtIoaaTrQISkpgJRSGJSUgCm0CG06kMYetFMQUlIY00lIQlJTAKSkDEpKGAlJQA001jigZTmnxUcFpJcfM33aoRqKFijCJTGNSMZSVIBSUABpKBDcU2mMM02kMTvS0wG0UAJSUIBKaaQrDGGap3UXG9fvU0NElnPvTa33hU5pPRgOSnGs2aRG0UXLENBPFJEkdJQhiUU7lDKbSGJjnrUZNJgMx3ph4pgNzmoiRRawIjJz2qMmiSLuAphqIidyNjUbHjrVW0IuMI4po96SADTelD00C4yihIQykp8ohO9FFrCGmkPKnii12DAjnNNovfRkjcUUxCUh5FML2BvvU3HOaYbiUgpyBageGNJWSWpIlJ0xmtUO4ueabSEL0FIakTEowCmDRfqJPuH8VIemKauNhmjsDT1JE%2BpNHUUWHuLjFNI9TmlyNCv0FPI6d6b%2FWkg6jipGO1AA6nGccGr3E5WAnPf9KbnDHiot9kNtT6dozXe2YC0vamUNzTWNACCloGJmkoKFoqRiGkoASkpgJSUgCkpDEzSUAJ3pKBhSUAJSUDCkNSIbSUANpKYwNJSEFJSAQ0lAxKSgBKShgFNpAJS0hjT1pKoQtNpDEptIBKKBCGkoASipGJTTQBG74FUpJmdtsdUgLttpnlfvLvj0HrU7EY44FFxENJSGJSUgCm96QWCihgJSUANpKYxtFIYh5pooAWkoAbSGgQ0ioyKWozPuFMLb1zir0Eomj3U2DJl6UtQyohSd6ksT%2BKkNMY3tQOtSAjUyqGhppgqX3GJmmGjoAxqYfSjoURmojwuBQhkdM7daNwEHTvmmE%2B9HmKxHTD1q%2BhLQe9RmouA1qZjjFG5NhTTD7VS2CPmNPvSHpS5iLCUgxRfQSYu7NNNCK3A981Hz2qSQpDirvqIKjxgcUgew7oBSd6G9bhsJmijW4hD83ccetJTExtHeqRQv5U3tipWuhIlL%2FDTsgEFHbniiMRSG%2B1HBFUFg6980dKhy7CYo%2B9R0WgpK%2BghHNJRz3VjOUQ5zxQenNI0iu4pGz5f0pAF2kiqu2ZSQDjHcUvfJ64rPmuylsfTRorvOcXNGauwCUUhjaM0MoSkpDFzSUAFNoKG0tFwEpKAEpKAEopAJSUFBSUANooGFIakQlNpDEpKBBTadhiUUgsJSUgEopjGmkoEFJQIQUlIYhopiEzTTSGFJSGJRSASkoAbSZoATNQSSAd6AII0lvZhHCM1tQWsGmxAn57v%2FwBBqmIrzStI5Zjkmoc1IxtFIBKSgBKSgAooASkoAbQaBjSKSkAlMNABSUALSGgQhphoGRyJlSDWdlref%2FZ96GxmtGwdAy9DS1m2WgpDUoobSU7jCmHihIBKKoaGGmGlsMa3tTc0t9QImNN70%2FIBjVESc5qCyOmEcU0xDO5pMcUuZAR0x6d%2BhLG000hbDTTaYr2Eam%2B1IY2mmjlsS5Dc%2FSinYjzEowAKNNh3EzzTTndSXcLiUVV2KXcTpSE0mSIaaafSwbMGpMHFGw5CL3o6Dii1yExop3SrcWF7iEj8aaT3pKPcLi0mcClbsLZiY3cYzSgccA8fpT3YMSg%2B9T1sHw6je9OGMc5%2FCq0RLEHXvRng8Ukxi5NN6URQ%2BgvRB3pOaNnykgeudvOaUHnGKbj1QRV9w24IxjFJzniptfUs%2BmaWu45dwoqhsKSpGFJTGhtBoASjNAxc0lSUhtJSAKKoBtJUgJRTGFNzSAKQmgYlIaBiUlAhKM0ANpKWwxKSgBKKBiUlIQlLQMQ02kISkpAFJTAbQaBjaWgBtFSISkoASm0WASm5oGVpZ8VLpumzak2%2FO2AfeeqWmomazSQ2MfkWGD%2Felx1qizZpMER02kMSkoAKSkISigYlFACUhoAbRTAQ02kNDTSUgGmikIKSgYlJQwGtVe4i8yPHekUS2ibIAvpU1Sy0JRSGIaO1ADO9IRQMSmk4pAMakoQxpqM%2FWmMjY0ztSAjYcUxqTZRGw9KSmURGmHpRZEjc1GeKLCGnpUeeelMkX%2BKmc%2BvNF%2BwhN3rSVMvMEMJzTcZo2JkN70U9WKwlJQK9w7000ctxB70hpFiZppJrQz8xM07nFJgNptJeYN6C4yTSAiqQkIRQB8vGKHqJu2wlJ%2BFJ7CAd6Wi4%2FMT3HFIee1U9VcQnXvSj3%2FLHWlpbQTEHXNC%2Fdx1PepmJrQM89aOtPRItXHetN4yc%2BlStxeY1jjPNDfcx60XvK5SuOZs9e9IeapdiVIXdv6Dj2o70ttQPpXNLmu45xaBTAKKACkpDG0lIoKSmFwzSUDEpKLB1Foo2GMoqLgFIaAEpKoBKKBhSUhiUw0AFNqWAtJTASkpDEpDSGNpKoQmaWiwBSUhiUlIBDSUCEpKB2GmgUhBSUgEpKoBKSpAjZ8CqUk%2BTheaYFuzsN2JbjhfT1rSubvzFEaKEjXsKYymWplSA2igApKBDaKQwooGNpaQhCKb0pjCmGmAlIakBKZSASkpiClpDsNzSGnsAGoyKBolX7tGKk0EoqR3EzSUhiGmmiwCEcU3HrQMawpp%2BtNghhqPPepKGNz3qJuKBjTUbdaeqAjP3TUfHWiIxmaQnNKxLIjTTzQSmJUfRuKZQnvTeooZmxKbmk9hCfSmUJgBptHUkSlPA4ouyhKbnmnHQiwdTTe1K5SEoJyabJfYToTml7ZpbgNPB56U2nYQZyaSixAppKNytL3Ge4p2fatH8OgCdTS1m9RIbSZ9KtK6HNpBnn2pf4RQZiUu7IPPHpUy8y35AP9qmqSY84G40rWQh568Z2g9aTcR34pbhoALA5Hy0zGVLe3FCVgUdR7Z3cdaQcdetaPcTXKKSd3JOab%2FFio9Bo%2Bk6M13nMh46UtMYopDUgLmmUD2EpKaKEopCCkzQig7UlAxO9FIBKSgBKKQDaKYwopAJSUhiGmUwCm0DCkpDCkpCEpKYDaKQDaKQC0lMBKSkAlJQMKaaQhKbQAtFIY00lMBKhkkC0CKTO0rYWrtrahNsj%2FeosBZZ6jNACU2kUFFSIbRVANopDEooAbTqBCGmUAJSGgYlNoASm0mMKbQISigBKKYwoqWA6ipNBKQ0DG0UgGmmmkMBSHrRYBjcVE1S9tCkMNR7z2HFNLTUob296j70eY2MOKY2KYiM9KZQPmGHpTaWwiHvRnFCsTyiUw8UyRucim1I2ug2k%2B7TI8hnb6UhpDsN6UDpTM7ie1HbtTGI%2FtSNwaUhDec9M%2FjS%2FWqv2KQ3tQBUX0JT1G55pDxVdADrSEccValYWgN3xTfQ0W6i9BetHvU%2BQ9lcT8MUjUWsFwzwKAKNiWhMc5pTUxbBa6CcZx3NJxgenWq5yXo7BSkfLUN%2FaC1wP3qacdDWkWNrQcFxu6UnB%2BtRPciwvI3UzGV44NaRty6FD2%2B8T3z2pPyxSd%2BpL1YNj%2BE55zSDGcmjnVh7M%2BjEfIzUtddzIeKUUxDqbTGGaKYxtJUMYmaShAFJVXGLmm0hhSUAFJQAlJSGFJQAUUhiUmaAG02kwEpKBiUUCCigBtJSQxKSgApKACkzQAlNoAWkqQEpKoY2kqRDc0tFhiU3dQwIJJdtVfLluJAB0pIRpQwLAP8AapSeadwGUlIYUlFwuFJQMSkpCCm5pgJRSAKTNACZpKdgGUUhjTSUgEooAYaKBiUlAgFHekIKKZQ6ioZqJSUgExSUgEpO1UMZ0pD1zUjGn61GRSQ7jG4qIim79AGUw9ahIaIiaaT6GrbBkLNTaS7gtENpuaXmAw0yi2l0IQmmdKoURKb%2BVIkYfej%2BGkxIioqtQEo5FIVhM57Un4VT0FYSkJqd9xsSkyQBmhIAFIfamJ7DaDRZkC%2B9IaPMBpoocg8xPpR1yPan1C4ZyetJzQPcM8c0dsg0NkPUPoaOEFU9tBiUnbnjFZu9hah70nPX860htqLW448Gg8delRIpe7oN4PQdKVen0quUd7MO%2FwBaa3T6VHUiW48%2FKzfWhSRznmmm2w5uomTxnn60A7c4qpQ6MUfiuj3qxuP4DWkpzXWYj91SA1Qxc0zd89Ah1LSKGmmmkMTNJTASigYUGkhiUlMBuaWkwEpKSYBSUIYlFMApKBjaQ1IxtIaBBSUAFJRuAlJSGFFACUlMBKQ0gG0UWASipGJQaYDaSlcBppKQCFuKqTTgdOtMBLe2M7B26VogLEuEpsQ0tUdSMSkoYBRQAlJQAUlAxtAouAU2gQGkpAJTaBiU00DA0lJCEptFgCkpAIaSgAooGFAoAdSVBoJRTGAprcUBYSmH0qRjTRTWgDGqJuRxQMjprUirERprDsRU7ARd6jzRewyI43cikNLoMYaQ1SJGknFRZIFFrIQfWmDHbvzVW0JA9KjbpUg1YaaYxPSq8xaCUi0MkOv1pPekAtNz05qQENJitES9ho%2Fi%2Bv50E0S3BbWE6emKDS63DlQ3vSEDHtVuZAo78U3NTuUJQM1NlYWorU2nF6gJjn8aXjuafmPYT60YxU82pHKIfl5o%2BtVuEVqA696RR8p3YP40%2Bg3pqO4PNNxS2Qtx3DdKTtTsAlHVOelTs9CXuPPfApnHsRVdCwBBYk880ds1CbvqR11FBHpTeaTjzDXvfCf%2F2QAA";

        String data = base64_1 + base64_2 + base64_3;
        data = data.replaceAll("%2F", "/");
        data = data.replaceAll("%2B", "+");
        byte[] decodedString = Base64.decode(data, Base64.DEFAULT);

        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        binding.imgviewCam.setImageBitmap(decodedByte);

        imgRetrievalCam.clearAnimation();
        imgRetrievalCam.setVisibility(View.GONE);
    }

    public static void processThermalCam(String pixelData){
        String[] list_pixelSTR = new String[68];

        list_pixelSTR = pixelData.split(",");
        for (int i = 0; i < 68; i++){
            list_pixelInt[i] = Integer.parseInt(list_pixelSTR[i]);
            NumberFormat nf = NumberFormat.getInstance();
            try {
                list_pixelBYT[i] = nf.parse(list_pixelSTR[i]).byteValue();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        canvasView.postInvalidate();
        imgRetrievalCam.clearAnimation();
        imgRetrievalCam.setVisibility(View.GONE);
    }

    public void processImageCam(String file_name, String location){
        storageReference = FirebaseStorage.getInstance().getReference( "/" + MainActivity.id_selected + "/" + location + "/" + file_name + ".jpg");

        try {
            File localFile = File.createTempFile("tempfile", ".jpg");
            storageReference.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                            Resources r = getResources();
                            int px = Math.round(TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP, 340,r.getDisplayMetrics()));
                            binding.imgviewCam.setImageBitmap(Bitmap.createScaledBitmap(bitmap, px, px, false));

                            imgRetrievalCam.clearAnimation();
                            imgRetrievalCam.setVisibility(View.GONE);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Failed to retrieve the image", Toast.LENGTH_SHORT);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] get_pixs() {
        return list_pixelBYT;
    }
    public static int[] get_pixsInt() { return list_pixelInt;}
}