package com.dremanru.fireewd_2.ui.temprh;

import static com.dremanru.fireewd_2.MainActivity.Set_Constraint_Value;
import static com.dremanru.fireewd_2.MainActivity.Set_Cont_State;
import static com.dremanru.fireewd_2.MainActivity.Set_Cont_State_BINtoINT;
import static com.dremanru.fireewd_2.MainActivity.Set_Stakeholder_Changes;
import static com.dremanru.fireewd_2.MainActivity.emergencyNotification;
import static com.dremanru.fireewd_2.MainActivity.list_emerx_constraint;
import static com.dremanru.fireewd_2.ui.cam.CamFragment.Set_Cam_All_State;
import static com.dremanru.fireewd_2.ui.cam.CamFragment.Set_Cam_All_State_BINtoINT;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dremanru.fireewd_2.MainActivity;
import com.dremanru.fireewd_2.R;
import com.dremanru.fireewd_2.databinding.FragmentTemprhBinding;
import com.dremanru.fireewd_2.ui.TransparentActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TempRHFragment extends Fragment {
    private DatabaseReference database;
    public static boolean retrieve_id_success = false;
    final int color_temp = Color.parseColor("#09BD3F");
    final int color_humid = Color.parseColor("#C97F28");
    final int color_thermal = Color.parseColor("#0089C4");
    int color_streamingStart, color_defaultButton;

    static String date_picker = "";
    String raw_temperature = "";
    String raw_humidity = "";
    String raw_clockTemp = "";
    //String raw_clockHumid = "";   --> it should be the same with clock temperature

    /*      THE 288 LENGTH OF ARRAY IS MEANT TO BE A 5 MINUTE DATA UPDATE IN A DAY      */
    String[] list_temperatureSTR = new String[288];
    String[] list_humiditySTR = new String[288];
    String[] list_clockTempSTR = new String[288];
    //String[] list_clockHumid = new String[288]; --> it should be the same as list_clockTemp

    //--------------------------------------------------------------------------------------------

    //OLD CHART PROCESS VARIABLE (Redacted)
    ArrayList<String> AL_clock_hourSTR = new ArrayList<String>();
    ArrayList<String> AL_clock_minuteSTR = new ArrayList<String>();
    ArrayList<Float> AL_list_temperatureFLT = new ArrayList<Float>();
    ArrayList<Float> AL_list_humidityFLT = new ArrayList<Float>();

    //NEW CHART PROCESS VARIABLE (V2)
    ArrayList<String> AL_str_temp = new ArrayList<>();
    ArrayList<String> AL_str_humid = new ArrayList<>();
    ArrayList<String> AL_str_thermal = new ArrayList<>();
    ArrayList<String> AL_str_r_clock = new ArrayList<>();
    ArrayList<Float> AL_float_r_temp = new ArrayList<>();
    ArrayList<Float> AL_float_r_humid = new ArrayList<>();
    ArrayList<Float> AL_float_r_thermal = new ArrayList<>();


    String date_updated;

    int dateEdited = 0;
    boolean finished_getdata = false;
    boolean retrieveState_old = false;
    boolean retrieveState_current = false;
    boolean retrieveState_recent = false;
    boolean reqDataofDate_clicked = false;

    float tempEmerxLimit = 50, humidEmerxLimit = 90;

    LineChart tempRH_lineChart;
    EditText datePicker1, datePicker2;
    EditText eTempEmerxSetting, eHumidEmerxSetting;
    Button bLive, bDataofDateChart, bLastUpdatedChart, bTempEmerxSet, bHumidEmerxSet;
    TextView tAmbient, tempCurrentData, humidCurrentData, tempCurrentData_spare, humidCurrentData_spare;
    TextView tTempEmerx, tHumidEmerx, tEmerx;
    TextView tTempChartLabel, tHumidChartLabel;
    Calendar myCalendar;
    SwitchCompat swThermal;

    ValueEventListener clockListener, tempListener, humidityListener;


    /*
    * SPECIAL NOTE: THE USER NEED TO CALL OTHER FRAGMENT TO START GET GET PROCESS
     */

    private FragmentTemprhBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TempRHViewModel homeViewModel =
                new ViewModelProvider(this).get(TempRHViewModel.class);

        binding = FragmentTemprhBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        color_streamingStart = ContextCompat.getColor(getContext(), R.color.streaming);
        color_defaultButton = ContextCompat.getColor(getContext(), R.color.purple_500);

        database = FirebaseDatabase.getInstance().getReference();

        swThermal = binding.switchRecordedThermal;

        tTempChartLabel = binding.textTempChartLabel;
        tHumidChartLabel = binding.textHumidChartLabel;

        datePicker1 = binding.editTimePicker1;
        datePicker2 = binding.editTimePicker2;
        bLive = binding.buttonReqCurrent;
        bDataofDateChart = binding.buttonReqDataofDate;
        bLastUpdatedChart = binding.buttonReqLastUpdated;
        tempRH_lineChart = binding.tempRHChart;

        tAmbient = binding.textAmbient;
        tempCurrentData = binding.textTempCurrentData;
        humidCurrentData = binding.textHumidCurrentData;
        tempCurrentData_spare = binding.textTempCurrentDataSSpare;
        humidCurrentData_spare = binding.textHumidCurrentDataSpare;

        eTempEmerxSetting = binding.editTempEmerxLimitSetting;
        eHumidEmerxSetting = binding.editHumidEmerxLimit;
        tTempEmerx = binding.textTempEmerx;
        tHumidEmerx = binding.textHumidEmerx;
        tEmerx = binding.textEmerx;
        bTempEmerxSet = binding.buttonTempEmerxConstraintSet;
        bHumidEmerxSet = binding.buttonHumidEmerxConstraintSet;

        //Set the cam cont state to OFF at first fragment interaction
        if(MainActivity.id_selected != null) {
            String ambient_text = "Ambient <font color=#09BD3F>Temp</font> & <font color=#C97F28>RH</font>";
            tAmbient.setText(Html.fromHtml(ambient_text, Html.FROM_HTML_MODE_LEGACY));
            tTempChartLabel.setVisibility(View.INVISIBLE);
            tHumidChartLabel.setVisibility(View.INVISIBLE);

            Set_Stakeholder_Changes(database, false);
            Set_Cont_State_BINtoINT(database, 5, 0, 4);
            Set_Cam_All_State_BINtoINT(database, 0, 0, 3);
        }

        myCalendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                if(dateEdited == 1){
                    updateDate1();
                }else if(dateEdited == 2){
                    updateDate2();
                }
            }
        };
        datePicker1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateEdited = 1;
                new DatePickerDialog(getContext(),date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        datePicker2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateEdited = 2;
                new DatePickerDialog(getContext(),date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        bLastUpdatedChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reqDataofDate_clicked = false;
                if(!retrieveState_recent && MainActivity.id_selected != null) {
                    retrieveState_old = false;
                    retrieveState_recent = true;

                    bLastUpdatedChart.setText("Stop");
                    bLastUpdatedChart.setBackgroundColor(Color.parseColor("#ED1C24"));
                    bDataofDateChart.setBackgroundColor(color_defaultButton);
                    bDataofDateChart.setText("Req");

                    String path = "/" + MainActivity.id_selected + "/DataLog";
                    database.child(path+"/date_updated").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String c = snapshot.getValue(String.class);
                            if (c != null) {
                                date_updated = c;
                                date_picker = c;
                                datePicker1.setText(date_updated);

                                //Clearing all data from the list
                                AL_str_temp.clear();
                                AL_str_humid.clear();
                                AL_str_r_clock.clear();
                                AL_float_r_temp.clear();
                                AL_float_r_humid.clear();

                                //createChart(date_updated);
                                createChart_v2(date_updated);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast toast = Toast.makeText(getContext(), "FEWD ID may not have any data or fit to the rules",Toast.LENGTH_SHORT);
                        }
                    });
                }
            }
        });
        bDataofDateChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reqDataofDate_clicked = true;
                if(retrieveState_recent){
                    retrieveState_recent = false;
                    startActivity(new Intent(getActivity(), TransparentActivity.class));
                }else if(!retrieveState_old && !Objects.equals(date_picker, "") && MainActivity.id_selected != null){
                    retrieveState_old = true;

                    tempRH_lineChart.clear();
                    tempRH_lineChart.invalidate();
                    bLastUpdatedChart.setBackgroundColor(color_defaultButton);
                    bLastUpdatedChart.setText("Recent");

                    //Clearing all data from the list
                    AL_str_temp.clear();
                    AL_str_humid.clear();
                    AL_str_r_clock.clear();
                    AL_float_r_temp.clear();
                    AL_float_r_humid.clear();

                    //createChart(date_picker);
                    createChart_v2(date_picker);
                }
            }
        });
        bLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reqDataofDate_clicked = false;
                if(!retrieveState_current && MainActivity.id_selected != null){
                    Set_Stakeholder_Changes(database, true);
                    retrieveState_current = true;
                    bDataofDateChart.setBackgroundColor(color_defaultButton);
                    bDataofDateChart.setText("Req");
                    bLive.setText("Stop");
                    bLive.setBackgroundColor(Color.parseColor("#ED1C24"));

                    tempCurrentData.setTextColor(color_temp);
                    humidCurrentData.setTextColor(color_humid);
                    tempCurrentData_spare.setTextColor(color_temp);
                    humidCurrentData_spare.setTextColor(color_humid);

                    Set_Cont_State_BINtoINT(database, 0, 1, 4);
                    streamCurrentData();

                }else if(retrieveState_current && MainActivity.id_selected != null){
                    Set_Stakeholder_Changes(database, false);
                    retrieveState_current = false;
                    bLive.setText("Live Data");
                    bLive.setBackgroundColor(color_defaultButton);

                    Set_Cont_State_BINtoINT(database, 0, 0, 4);

                    tempCurrentData.setTextColor(Color.parseColor("#808080"));
                    humidCurrentData.setTextColor(Color.parseColor("#808080"));
                    tempCurrentData_spare.setTextColor(Color.parseColor("#808080"));
                    humidCurrentData_spare.setTextColor(Color.parseColor("#808080"));

                    startActivity(new Intent(getActivity(), TransparentActivity.class));
                }
            }
        });

        return root;
    }


    /*
    * To avoid null detected by Firebase.listener process
    * ESP32 need to always initialize the 24 hr data of temp and RH with "0"
    * */

    @Override
    public void onStart() {
        super.onStart();

        if(MainActivity.id_selected != null) {

            //Filing the date-updated on time picker
            String path = "/" + MainActivity.id_selected + "/DataLog/date_updated";
            database.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String c = snapshot.getValue(String.class);
                    if (c != null) {
                        date_updated = c;
                        date_picker = c;
                        datePicker1.setText(date_updated);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
            //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
            //Filing the temperature and humidity emergency constraint setting

            if(MainActivity.list_emerx_constraint != null){
                eTempEmerxSetting.setText(list_emerx_constraint[2]);
                eHumidEmerxSetting.setText(list_emerx_constraint[3]);
            }

            bTempEmerxSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eTempEmerxSetting.getText()).equals("")){
                        float c = Float.parseFloat(String.valueOf(eTempEmerxSetting.getText()));
                        if(c > 0 && c <= 80){
                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 2, c);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Emergency Temp. Limit need to be higher than 0°C or less than 80°C", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up the emergency temp. limit", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            bHumidEmerxSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!String.valueOf(eHumidEmerxSetting.getText()).equals("")){
                        float c = Float.parseFloat(String.valueOf(eHumidEmerxSetting.getText()));
                        if(c > 0 && c <= 100){
                            //  0 = light, 1 = flame, 2 = temp, 3 = humid, 4 = min_thermal, 5 = max_thermal, 6 = thermal_emerx, 7 = thermal_hotspot
                            Set_Constraint_Value(database, 3, c);
                            Set_Stakeholder_Changes(database, true);
                        }else{
                            Toast.makeText(getContext(), "Emergency Humid. Limit need to be higher than 0 %RH or less than 100 %RH", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getContext(), "Please fill up the emergency humid. limit", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //Listening to Emergency State of Temperature-Data dan Humidity-Data Log
            path = "/" + MainActivity.id_selected + "/DataLog/All_emerx_state";
            database.child(path).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean emerx_temp = false;
                    boolean emerx_humid = false;
                    String c = snapshot.getValue(String.class);

                    if(c != null && c.length() == 9) {
                        char[] emerx_state_charArr = c.toCharArray();
                        if(emerx_state_charArr[4] == '1')
                            emerx_temp = true;
                        if(emerx_state_charArr[6] == '1')
                            emerx_humid = true;
                    }

                    if(emerx_temp){
                        String path = "/" + MainActivity.id_selected + "/DataLog/Temp/emergency";
                        database.child(path+"/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                if(c != null) {
                                    tTempEmerx.setText(String.valueOf(c) + " °C");
                                    tEmerx.setVisibility(View.VISIBLE);
                                    tTempEmerx.setVisibility(View.VISIBLE);

                                    emergencyNotification(getActivity(), getContext(), 1, (float) c);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }else{
                        tEmerx.setVisibility(View.GONE);
                        tTempEmerx.setVisibility(View.GONE);
                    }

                    if(emerx_humid){
                        String path = "/" + MainActivity.id_selected + "/DataLog/Humid/emergency";
                        database.child(path+"/value").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Float c = snapshot.getValue(Float.class);
                                if(c != null) {
                                    tHumidEmerx.setText(String.valueOf(c) + " %");
                                    tEmerx.setVisibility(View.VISIBLE);
                                    tHumidEmerx.setVisibility(View.VISIBLE);

                                    emergencyNotification(getActivity(), getContext(), 2, (float) c);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }else{
                        tEmerx.setVisibility(View.GONE);
                        tHumidEmerx.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY
            //EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY   EMERGENCY

            swThermal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createChart_v2(date_picker);
                }
            });
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        //Set_Cont_State(database, 5, 0); //all OFF cont state
        Set_Cont_State_BINtoINT(database, 5, 0, 4);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

    }

    public void streamCurrentData(){
        String path = "/" + MainActivity.id_selected + "/DataLog";

        database.child(path).child("Temp/cont").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Float c = snapshot.getValue(Float.class);
                if (c != null) {
                    String d = String.valueOf(c);
                    d += " °C";
                    tempCurrentData.setText(d);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast toast = Toast.makeText(getContext(), "FEWD ID may not have any data or fit to the rules",Toast.LENGTH_SHORT);
            }
        });
        database.child(path).child("TempHDC/cont").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Float c = snapshot.getValue(Float.class);
                if (c != null) {
                    String d = String.valueOf(c);
                    d += " °C";
                    tempCurrentData_spare.setText(d);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast toast = Toast.makeText(getContext(), "FEWD ID may not have any data or fit to the rules",Toast.LENGTH_SHORT);
            }
        });

        database.child(path).child("Humid/cont").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Float c = snapshot.getValue(Float.class);
                if (c != null) {
                    String d = String.valueOf(c);
                    d += " %";
                    humidCurrentData.setText(d);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        database.child(path).child("HumidHDC/cont").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Float c = snapshot.getValue(Float.class);
                if (c != null) {
                    String d = String.valueOf(c);
                    d += " %";
                    humidCurrentData_spare.setText(d);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void processChart(String date){
        if(!Objects.equals(raw_temperature, "") && !Objects.equals(raw_humidity, "") && !Objects.equals(raw_clockTemp, "")) {
            //Clearing all data from the list
            AL_list_temperatureFLT.clear();
            AL_list_humidityFLT.clear();
            AL_clock_hourSTR.clear();
            AL_clock_minuteSTR.clear();

            list_temperatureSTR = raw_temperature.split(",");
            list_humiditySTR = raw_humidity.split(",");
            list_clockTempSTR = raw_clockTemp.split(",");

            int length_temperatureArray = list_temperatureSTR.length;
            int length_humidityArray = list_humiditySTR.length;
            int len = 0;

            len = length_temperatureArray;
            if (length_temperatureArray <= length_humidityArray) {
                len = length_temperatureArray;
            } else if (length_humidityArray <= length_temperatureArray){
                len = length_humidityArray;
            }

            /*int length_temperatureArray = 0;
            for(int i=0; i<288; i++){
                if(list_temperatureSTR[i] != null)
                    length_temperatureArray++;
            }*/

            for (int i = 0; i < len; i++) {
                float ft = Float.parseFloat(list_temperatureSTR[i]);
                float fh = Float.parseFloat(list_humiditySTR[i]);

                if (ft != 0) {
                    AL_list_temperatureFLT.add(ft);
                }
                if (fh != 0) {
                    AL_list_humidityFLT.add(fh);
                }
                String clockSTR = list_clockTempSTR[i];
                AL_clock_hourSTR.add(clockSTR.substring(0, 2));
                AL_clock_minuteSTR.add(clockSTR.substring(3, 5));
            }

            //Process of creating the line chart
            LineDataSet lineDataSetTemp = new LineDataSet(dataTemps(date), "Temperature");
            LineDataSet lineDataSetHumid = new LineDataSet(dataHumids(date), "Relative Humidity");
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();

            lineDataSetTemp.setAxisDependency(tempRH_lineChart.getAxisLeft().getAxisDependency());
            lineDataSetHumid.setAxisDependency(tempRH_lineChart.getAxisRight().getAxisDependency());
            tempRH_lineChart.getAxisLeft().setAxisMaximum(50);
            tempRH_lineChart.getAxisLeft().setAxisMinimum(20);
            tempRH_lineChart.getAxisRight().setAxisMaximum(100);
            tempRH_lineChart.getAxisRight().setAxisMinimum(20);


            XAxis xAxis = tempRH_lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularityEnabled(true);
            xAxis.setGranularity(5 * 60 * 1000);  //in milisecond
            xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);

                @Override
                public String getFormattedValue(float value) {
                    //long millis = TimeUnit.MINUTES.toMillis((long) value);
                    long millis = (long) value;
                    return sdf.format(new Date(millis));
                }
            });

            dataSets.add(lineDataSetTemp);
            dataSets.add(lineDataSetHumid);

            lineDataSetTemp.setLineWidth(3);
            lineDataSetHumid.setLineWidth(3);
            lineDataSetTemp.setValueTextSize(8);
            lineDataSetHumid.setValueTextSize(8);
            //lineDataSetTemp.setCircleColor(color_temp);
            //lineDataSetHumid.setCircleColor(color_humid);
            lineDataSetTemp.setDrawCircles(false);
            lineDataSetHumid.setDrawCircles(false);
            lineDataSetTemp.setColor(color_temp);
            lineDataSetHumid.setColor(color_humid);

            Description description = new Description();
            description.setText("Date: " + date_picker);
            description.setTextSize(12);
            description.setTextColor(Color.DKGRAY);
            tempRH_lineChart.setDescription(description);

            Legend legend = tempRH_lineChart.getLegend();
            legend.setEnabled(true);
            legend.setForm(Legend.LegendForm.LINE); //the form of the legend icons
            legend.setFormSize(20);

            lineDataSetTemp.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(value + " °C");
                }
            });
            lineDataSetHumid.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(value + " %");
                }
            });

            LineData data = new LineData(dataSets);
            tempRH_lineChart.setData(data);
            tempRH_lineChart.invalidate();
            tempRH_lineChart.animateX(500);
        }
    }

    public void processChart_v2(String date){
        if(AL_float_r_temp.size() > 0 && AL_float_r_humid.size() > 0) {
            for (int i = 0; i < AL_str_r_clock.size(); i++) {
                AL_clock_hourSTR.add(AL_str_r_clock.get(i).substring(0, 2));
                AL_clock_minuteSTR.add(AL_str_r_clock.get(i).substring(3, 5));
            }

            int[] size = {AL_float_r_temp.size(), AL_float_r_humid.size(), AL_float_r_thermal.size()};
            int tmp = 999, lowest_size = 0;
            for(int i = 0; i < 3; i++){
                if(size[i] < tmp){
                    tmp = size[i];
                }
            }lowest_size = tmp;

            //Process of creating the line chart
            LineDataSet lineDataSetTemp = new LineDataSet(dataTemps_v2(date, lowest_size), "Temperature");
            LineDataSet lineDataSetHumid = new LineDataSet(dataHumids_v2(date, lowest_size), "Relative Humidity");
            LineDataSet lineDataSetThermal = null;
            if(swThermal.isChecked() && AL_float_r_thermal.size() > 0){
                lineDataSetThermal = new LineDataSet(dataThermals_v2(date, lowest_size), "Thermal Cam");
            }
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();

            lineDataSetTemp.setAxisDependency(tempRH_lineChart.getAxisLeft().getAxisDependency());
            lineDataSetHumid.setAxisDependency(tempRH_lineChart.getAxisRight().getAxisDependency());
            if(lineDataSetThermal != null){
                lineDataSetThermal.setAxisDependency(tempRH_lineChart.getAxisLeft().getAxisDependency());
            }
            tempRH_lineChart.getAxisLeft().setAxisMaximum(50);
            tempRH_lineChart.getAxisLeft().setAxisMinimum(20);
            tempRH_lineChart.getAxisRight().setAxisMaximum(100);
            tempRH_lineChart.getAxisRight().setAxisMinimum(20);

            XAxis xAxis = tempRH_lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularityEnabled(true);
            xAxis.setGranularity(5 * 60 * 1000);  //in milisecond
            xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);

                @Override
                public String getFormattedValue(float value) {
                    //long millis = TimeUnit.MINUTES.toMillis((long) value);
                    long millis = (long) value;
                    return sdf.format(new Date(millis));
                }
            });

            dataSets.add(lineDataSetTemp);
            dataSets.add(lineDataSetHumid);
            if(lineDataSetThermal != null){
                dataSets.add(lineDataSetThermal);
            }

            lineDataSetTemp.setLineWidth(3);
            lineDataSetHumid.setLineWidth(3);
            lineDataSetTemp.setValueTextSize(0);
            lineDataSetHumid.setValueTextSize(0);
            //lineDataSetTemp.setCircleColor(color_temp);
            //lineDataSetHumid.setCircleColor(color_humid);
            lineDataSetTemp.setDrawCircles(false);
            lineDataSetHumid.setDrawCircles(false);

            lineDataSetTemp.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSetHumid.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            lineDataSetTemp.setColor(color_temp);
            lineDataSetHumid.setColor(color_humid);
            lineDataSetTemp.setDrawFilled(true);
            lineDataSetHumid.setDrawFilled(true);
            lineDataSetTemp.setFillColor(color_temp);
            lineDataSetTemp.setFillAlpha(80);
            lineDataSetHumid.setFillColor(color_humid);
            lineDataSetHumid.setFillAlpha(80);

            if(lineDataSetThermal != null){
                lineDataSetThermal.setLineWidth(3);
                lineDataSetThermal.setValueTextSize(0);
                lineDataSetThermal.setDrawCircles(false);
                lineDataSetThermal.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                lineDataSetThermal.setColor(color_thermal);
                lineDataSetThermal.setDrawFilled(true);
                lineDataSetThermal.setFillColor(color_temp);
                lineDataSetThermal.setFillAlpha(80);
            }

            Description description = new Description();
            description.setText("Date: " + date_picker);
            description.setTextSize(12);
            description.setTextColor(Color.DKGRAY);
            tempRH_lineChart.setDescription(description);

            Legend legend = tempRH_lineChart.getLegend();
            legend.setEnabled(true);
            legend.setForm(Legend.LegendForm.LINE); //the form of the legend icons
            legend.setFormSize(20);

            lineDataSetTemp.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(value + " °C");
                }
            });
            lineDataSetHumid.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf(value + " %");
                }
            });
            if(lineDataSetThermal != null){
                lineDataSetThermal.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf(value + " °C");
                    }
                });
            }

            LineData data = new LineData(dataSets);

            //IMarker marker = new THMarkerView();
            //tempRH_lineChart.setMarker(marker);

            tempRH_lineChart.setData(data);
            tempRH_lineChart.invalidate();
            tempRH_lineChart.animateX(500);
        }
    }

    public void createChart_v2(String date /* in dd-MM-yyyy date format */){
        AL_str_temp.clear();
        AL_str_humid.clear();
        AL_str_thermal.clear();
        AL_str_r_clock.clear();
        AL_float_r_temp.clear();
        AL_float_r_humid.clear();
        AL_float_r_thermal.clear();

        tTempChartLabel.setVisibility(View.VISIBLE);
        tHumidChartLabel.setVisibility(View.VISIBLE);

        String path = "/" + MainActivity.id_selected + "/DataLog";

        //ESP32 need to send simple format of date eg. 1-1, 12-9, 2-11 (day-month)
        String temp_path, humidity_path, thermal_path;
        temp_path = path + "/Temp/" + date;
        humidity_path = path + "/Humid/" + date;
        thermal_path = path + "/Thermal/" + date;

        database.child(temp_path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(reqDataofDate_clicked) {
                    bDataofDateChart.setBackgroundColor(color_streamingStart);
                    bDataofDateChart.setText("Loaded");
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    if(!Objects.equals(ds.getKey(), "num")) {
                        if(ds.getKey() != null) {
                            String data = ds.getValue(String.class);
                            AL_str_temp.add(data);
                        }
                    }
                }
                if(AL_str_temp.size() > 0){
                    for(int i = 0; i < AL_str_temp.size(); i++) {
                        String[] arr = AL_str_temp.get(i).split(",");
                        AL_str_r_clock.add(arr[0]);
                        AL_float_r_temp.add(Float.parseFloat(arr[1]));
                    }
                    processChart_v2(date);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        database.child(humidity_path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if(ds.getKey() != null || !Objects.equals(ds.getKey(), "num")) {
                        String data = ds.getValue(String.class);
                        AL_str_humid.add(data);
                    }
                }
                if(AL_str_humid.size() > 0){
                    for(int i = 0; i < AL_str_humid.size(); i++) {
                        String[] arr = AL_str_humid.get(i).split(",");
                        AL_float_r_humid.add(Float.parseFloat(arr[1]));
                    }
                    processChart_v2(date);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        database.child(thermal_path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if(ds.getKey() != null || !Objects.equals(ds.getKey(), "num")) {
                        String data = ds.getValue(String.class);
                        AL_str_thermal.add(data);
                    }
                }
                if(AL_str_thermal.size() > 0){
                    for(int i = 0; i < AL_str_thermal.size(); i++) {
                        String[] arr = AL_str_thermal.get(i).split(",");
                        AL_float_r_thermal.add(Float.parseFloat(arr[1]));
                    }
                    processChart_v2(date);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void createChart(String date /* in dd-MM-yyyy date format */){
        String path = "/" + MainActivity.id_selected + "/DataLog";

        //ESP32 need to send simple format of date eg. 1-1, 12-9, 2-11 (day-month)
        String temp_path, humidity_path, clock_path;
        temp_path = path + "/Temp/" + date + "/data";
        humidity_path = path + "/Humid/" + date + "/data";
        clock_path = path + "/Temp/" + date + "/clock";

        clockListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String c = snapshot.getValue(String.class);
                if (c != null) {
                    raw_clockTemp = c;
                    if(reqDataofDate_clicked) {
                        bDataofDateChart.setBackgroundColor(color_streamingStart);
                        bDataofDateChart.setText("Loaded");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast toast = Toast.makeText(getContext(), "FEWD ID may not have any data or fit to the rules",Toast.LENGTH_SHORT);
            }
        };

        tempListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String t = snapshot.getValue(String.class);
                if (t != null) {
                    raw_temperature = t;
                    processChart(date);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        humidityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String rh = snapshot.getValue(String.class);
                if (rh != null) {
                    raw_humidity = rh;
                    processChart(date);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        database.child(clock_path).addValueEventListener(clockListener);
        database.child(temp_path).addValueEventListener(tempListener);
        database.child(humidity_path).addValueEventListener(humidityListener);
    }

    private void updateDate1(){
        String myFormat="yyyy-MM-dd";
        SimpleDateFormat dateFormat=new SimpleDateFormat(myFormat, Locale.US);
        String c = dateFormat.format(myCalendar.getTime());
        datePicker1.setText(c);
        date_picker = c;

        retrieveState_old = false;
        retrieveState_recent = false;
        bDataofDateChart.setBackgroundColor(color_defaultButton);
        bDataofDateChart.setText("Req");
    }
    private void updateDate2(){
        /*String myFormat="dd-MM-yyyy";
        SimpleDateFormat dateFormat=new SimpleDateFormat(myFormat, Locale.US);
        String c = dateFormat.format(myCalendar.getTime());
        datePicker2.setText(c);
        reqDataofDate.setBackgroundColor(color_defaultButton);
        reqDataofDate.setText("Req");*/
    }

    private ArrayList<Entry> dataTemps(String date) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        if(!AL_list_temperatureFLT.isEmpty()) {
            for (int i = 0; i < AL_list_temperatureFLT.size(); i++) {
                if(AL_list_temperatureFLT.get(i) != 0){
                    String date_time = date.substring(0,4) + "/" + date.substring(5,7) + "/" + date.substring(8,10) + " " +
                            AL_clock_hourSTR.get(i) + ":" + AL_clock_minuteSTR.get(i) + ":" + "00";
                    LocalDateTime localDateTime = LocalDateTime.parse(date_time, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    dataVals.add(new Entry(millis, AL_list_temperatureFLT.get(i)));
                }
            }
        }
        return dataVals;
    }

    private ArrayList<Entry> dataHumids(String date) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        if(!AL_list_humidityFLT.isEmpty()) {
            for (int i = 0; i < AL_list_humidityFLT.size(); i++) {
                if(AL_list_humidityFLT.get(i) != 0){
                    String date_time = date.substring(0,4) + "/" + date.substring(5,7) + "/" + date.substring(8,10) + " " +
                            AL_clock_hourSTR.get(i) + ":" + AL_clock_minuteSTR.get(i) + ":" + "00";
                    LocalDateTime localDateTime = LocalDateTime.parse(date_time, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    dataVals.add(new Entry(millis, AL_list_humidityFLT.get(i)));
                }
            }
        }
        return dataVals;
    }

    private ArrayList<Entry> dataTemps_v2(String date, int size) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        if(!AL_float_r_temp.isEmpty()) {
            for (int i = 0; i < size; i++) {
                if(AL_float_r_temp.get(i) != 0){
                    String date_time = date.substring(0,4) + "/" + date.substring(5,7) + "/" + date.substring(8,10) + " " +
                            AL_str_r_clock.get(i).substring(0, 2) + ":" + AL_str_r_clock.get(i).substring(3, 5) + ":" + "00";
                    LocalDateTime localDateTime = LocalDateTime.parse(date_time, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    dataVals.add(new Entry(millis, AL_float_r_temp.get(i)));
                }
            }
        }
        return dataVals;
    }

    private ArrayList<Entry> dataHumids_v2(String date, int size) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        if(!AL_float_r_humid.isEmpty()) {
            for (int i = 0; i < size; i++) {
                if(AL_float_r_humid.get(i) != 0){
                    String date_time = date.substring(0,4) + "/" + date.substring(5,7) + "/" + date.substring(8,10) + " " +
                            AL_str_r_clock.get(i).substring(0, 2) + ":" + AL_str_r_clock.get(i).substring(3, 5) + ":" + "00";
                    LocalDateTime localDateTime = LocalDateTime.parse(date_time, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    dataVals.add(new Entry(millis, AL_float_r_humid.get(i)));
                }
            }
        }
        return dataVals;
    }

    private ArrayList<Entry> dataThermals_v2(String date, int size) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        if(!AL_float_r_thermal.isEmpty()) {
            for (int i = 0; i < size; i++) {
                if(AL_float_r_thermal.get(i) != 0){
                    String date_time = date.substring(0,4) + "/" + date.substring(5,7) + "/" + date.substring(8,10) + " " +
                            AL_str_r_clock.get(i).substring(0, 2) + ":" + AL_str_r_clock.get(i).substring(3, 5) + ":" + "00";
                    LocalDateTime localDateTime = LocalDateTime.parse(date_time, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                    long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    dataVals.add(new Entry(millis, AL_float_r_thermal.get(i)));
                }
            }
        }
        return dataVals;
    }
}