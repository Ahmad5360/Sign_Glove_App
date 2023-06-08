package com.example.signtospeechapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Observable;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btn_speak, connect,disconnect, clear, translate, btn_speak2;
    TextView txt1,stat_dis;
    EditText edt1;
    TextToSpeech textToSpeech, textToSpeech2;

    private Translator translatorUrdu;
    private Boolean booleanUrdu = false;

    BluetoothAdapter bluetoothAdapter;
    private static final int Request_Enable =1;
    private ArrayAdapter<String> deviceListAdapter;
    private List<String> deviceList;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;







    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_speak = findViewById(R.id.speak);
        btn_speak2 = findViewById(R.id.speak2);
        connect = findViewById(R.id.connect);
        disconnect= findViewById(R.id.dis_connect);
        edt1 = findViewById(R.id.trans_text);
        txt1 = findViewById(R.id.txt_view);
        stat_dis=findViewById(R.id.dis_connect_stat);
        clear = findViewById(R.id.clear);
        translate = findViewById(R.id.translate);



        edt1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = edt1.getText().toString();
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

            }
        });


        // Initialize Bluetooth adapter

        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();

        // Initialize device list adapter and list view

        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        ListView deviceListView = findViewById(R.id.listView);
        deviceListView.setAdapter(deviceListAdapter);

        connect.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.isEnabled())
                {
                    Toast.makeText(MainActivity.this, "Turning ON Bluetooth", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,1);
                    Toast.makeText(MainActivity.this, "Click on Connect Again", Toast.LENGTH_LONG).show();
                }

                else
                {
//                    Toast.makeText(MainActivity.this, "Already Turned ON", Toast.LENGTH_SHORT).show();

//                    txt1.setText("Paired Devices");
                    txt1.setVisibility(View.VISIBLE);
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size()>0)
                    {
                        deviceList.clear();
                        for (BluetoothDevice device: pairedDevices)
                        {
                            deviceList.add("Device Name : "+device.getName()+ "\nDevice Address : "+ device.getAddress());

                        }

                        deviceListAdapter.notifyDataSetChanged();

                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "No Paired Device Found", Toast.LENGTH_SHORT).show();
                    }


                    // Connect to a selected device when it is clicked in the device list view
                    deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            BluetoothDevice selectedDevice = bluetoothAdapter.getRemoteDevice(deviceList.get(i).substring(deviceList.get(i).length() - 17));
                            try {
                                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                                socket = selectedDevice.createRfcommSocketToServiceRecord(uuid);
                                socket.connect();
                                inputStream = socket.getInputStream();
                                outputStream = socket.getOutputStream();
                                isConnected = true;
                                stat_dis.setText("Connected");
                                stat_dis.setTextColor(Color.parseColor("#49B84E"));
                                txt1.setVisibility(View.GONE);
                                deviceListView.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();

                                // Create a separate thread to read data from input stream continuously
                                Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            while (isConnected) {
                                                // Read data from the input stream
                                                byte[] buffer = new byte[1024];
                                                int bytes = inputStream.read(buffer);

                                                // Convert bytes to string
                                                String data = new String(buffer, 0, bytes);

                                                // Update the TextView in the UI thread
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        edt1.setText(data);

                                                    }
                                                });
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

//                                        try {
//                                            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
//                                            while (isConnected) {
//                                                try {
//                                                    String line = br.readLine();
//                                                    if (line != null) {
//                                                        String finalLine = line.trim();
//                                                        runOnUiThread(new Runnable() {
//                                                            public void run() {
//                                                                edt1.setText(finalLine);
//                                                            }
//                                                        });
//                                                    }
//                                                } catch (IOException e) {
//                                                    e.printStackTrace();
//                                                    break;
//                                                }
//                                            }
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
                                    }
                                });

// Start the thread
                                thread.start();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, "Failed to connect to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            } catch (IllegalArgumentException e) {
                                Toast.makeText(MainActivity.this, "Invalid UUID", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    });



                    disconnect.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (isConnected) {
                                try {
                                    socket.close();
                                    inputStream.close();
                                    outputStream.close();
                                    isConnected = false;
                                    stat_dis.setText("Disconnected");
                                    stat_dis.setTextColor(Color.parseColor("#F44336"));
                                    txt1.setVisibility(View.VISIBLE);
                                    deviceListView.setVisibility(View.VISIBLE);
                                    Toast.makeText(MainActivity.this, "Disconnected from device", Toast.LENGTH_SHORT).show();

                                } catch (IOException e) {
                                    Toast.makeText(MainActivity.this, "Error disconnecting from device", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Not Connected to any device currently", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });



        // Initialize TextToSpeech object with appropriate language and accent


        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale urdu = new Locale("en", "PK");
                    int result = textToSpeech2.setLanguage(urdu);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        // Set speech rate and pitch if desired
                        textToSpeech2.setSpeechRate(1.0f);
                        textToSpeech2.setPitch(1.0f);
                        // Set audio attributes
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        textToSpeech2.setAudioAttributes(audioAttributes);

                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
        //Urdu Text To Speech
        textToSpeech2 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale urdu = new Locale("ur", "PK");
                    int result = textToSpeech2.setLanguage(urdu);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        // Set speech rate and pitch if desired
                        textToSpeech2.setSpeechRate(1.0f);
                        textToSpeech2.setPitch(1.0f);
                        // Set audio attributes
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        textToSpeech2.setAudioAttributes(audioAttributes);

                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });


        btn_speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String text = edt1.getText().toString();
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

            }

        });

        btn_speak2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String text = edt1.getText().toString();
                textToSpeech2.speak(text, TextToSpeech.QUEUE_FLUSH, null);

            }

        });


        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt1.setText(" ");
            }
        });

        // English-Urdu translation
        TranslatorOptions translatorOptionsUrdu = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.URDU)
                .build();
        translatorUrdu = Translation.getClient(translatorOptionsUrdu);
        downloadModel();

    }

    private void downloadModel() {
        DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translatorUrdu.downloadModelIfNeeded(downloadConditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        booleanUrdu = true;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        booleanUrdu = false;
                    }
                });
    }

    public void buttonDownloadModel(View view) {
        downloadModel();
    }

    public void buttonUrdu(View view) {

        if (booleanUrdu) {
            translatorUrdu.translate(edt1.getText().toString())
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            edt1.setText(s);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            edt1.setText(e.toString());
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        } else if (textToSpeech2 != null) {
            textToSpeech2.stop();
            textToSpeech2.shutdown();
        }

        super.onDestroy();
    }








}