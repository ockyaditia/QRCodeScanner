package com.example.ockyaditiasaputra.qrcodescanner;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    int addHarga, delHarga = 0;
    TextView hargaTxt, listTxt, barangTxt;
    String listBarangAdd, listBarangDel = "";

    SharedPreferences.Editor ed;

    InputStream is = null;
    String result = null;
    String line = null;
    int code;

    String barangField, qtyField, hargaField, totalField, totalHargaField = "";

    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    private ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = false;

    String scanResult;

    protected PowerManager.WakeLock mWakeLock;

    UserSession session;

    String sharedName, sharedPassword;

    JSONParser jsonParser = new JSONParser();
    private static final String LOGIN_URL = new Connections().getBarang();
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_NAMA_BARANG = "data1";
    private static final String TAG_HARGA = "data2";
    private static final String TAG_JUMLAH_STOK = "data3";

    int success, harga, jumlah_stok, hargaTemp;
    String nama_barang;

    String listBarangAddChange, listBarangAddCancel, listBarangDelChange;

    ArrayList<String> dataBarang = new ArrayList<String>();
    ArrayList<String> dataHarga = new ArrayList<String>();
    ArrayList<String> dataStok = new ArrayList<String>();
    ArrayList<Integer> dataTotal = new ArrayList<Integer>();

    String listDatabase = "";

    int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    EditText prompt, discount, prompt2;
    SharedPreferences sp;
    int dataPut;
    String listChange = "";

    EditText editTextNominal;

    boolean cek2 = true;
    int cek3 = 1;

    Switch swicth;

    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        session = new UserSession(getApplicationContext());

        if (!session.checkLogin()) {
            //setContentView(R.layout.activity_main);

            Intent intent = new Intent(getBaseContext(), Home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivityForResult(intent, 0);
            finish();

        } else {

            setContentView(R.layout.activity_main);

            /*final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();*/

            Intent intent = getIntent();

            if (intent.hasExtra("Camera")) {
                currentCameraId = intent.getIntExtra("Camera", Camera.CameraInfo.CAMERA_FACING_FRONT);
            }

            releaseCamera();
            initControls();

            SharedPreferences sp2 = getSharedPreferences("usersession_qrcodescanner", MODE_PRIVATE);
            sharedName = sp2.getString("Username", "");

            TextView loginUser = (TextView) findViewById(R.id.loginUser);
            loginUser.setText(Html.fromHtml("Nama Kasir: <b>" + sharedName + "</b>"));

            SharedPreferences sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
            ed = sp.edit();

            /*int hargaTemp = 0;

            if (intent.hasExtra("Harga")) {
                try {
                    addHarga = Integer.parseInt(intent.getStringExtra("Harga"));
                    delHarga = Integer.parseInt(intent.getStringExtra("Harga"));
                    hargaTemp = Integer.parseInt(intent.getStringExtra("Harga"));
                } catch (NumberFormatException e) {
                    showAlertDialog(this, "Internet Connection",
                            "Please check your internet connection", false);
                }
            }*/

            hargaTxt = (TextView) findViewById(R.id.harga);
            listTxt = (TextView) findViewById(R.id.listBarang);
            barangTxt = (TextView) findViewById(R.id.barang);

            if (intent.hasExtra("listDatabase")) {

                sharedName = intent.getStringExtra("sharedName");
                barangField = intent.getStringExtra("barangField");
                qtyField = intent.getStringExtra("qtyField");
                hargaField = intent.getStringExtra("hargaField");
                totalField = intent.getStringExtra("totalField");
                totalHargaField = intent.getStringExtra("totalHargaField");
                listDatabase = intent.getStringExtra("listDatabase");
                dataPut = intent.getIntExtra("dataPut", 0);
                listChange = intent.getStringExtra("listChange");

                listTxt.setText(listDatabase);
                totalHargaField = Integer.toString(intent.getIntExtra("dataPut", 0));
                hargaTxt.setText("Rp. " + totalHargaField + ",-");

            }
            else {
                if (addHarga == 0 && delHarga == 0) {
                    ed.putString("Harga_Temp", Integer.toString(0));
                    ed.putString("List", "");
                    ed.commit();
                }
            }

            frameLayout = (FrameLayout) findViewById(R.id.cameraPreview);
            frameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    getAlertDialog2(prompt2.getText().toString(), prompt.getText().toString());

                                    if (addHarga > 0) {

                                        ed.putString("List", listBarangAddChange);
                                        ed.putString("Harga_Temp", Integer.toString(addHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                                        String[] listTemp1 = listBarangAdd.split("\n");

                                        dataBarang.clear();
                                        dataHarga.clear();
                                        dataStok.clear();
                                        dataTotal.clear();

                                        for (int i = 0; i < listTemp1.length; i++) {

                                            String[] listTemp2 = listTemp1[i].split("\t");

                                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                                    dataBarang.add(listTemp2[j]);
                                                    dataHarga.add(listTemp2[j + 1]);
                                                    dataStok.add(listTemp2[j + 2].substring(1));
                                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                                }
                                            }
                                        }

                                        listDatabase = "";
                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";

                                        for (int i = 0; i < dataBarang.size(); i++) {
                                            listDatabase = dataBarang.get(i) + "   " +
                                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                                    dataTotal.get(i) + "\n" + listDatabase;

                                            barangField = dataBarang.get(i) + "\n" + barangField;
                                            qtyField = dataStok.get(i) + "\n" + qtyField;
                                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                                            totalField = dataTotal.get(i) + "\n" + totalField;
                                        }

                                        listTxt.setText(listDatabase);
                                        totalHargaField = Integer.toString(addHarga);
                                        dataPut = addHarga;
                                        listChange = listBarangAddChange;
                                    } else {
                                        addHarga = 0;

                                        ed.putString("List", "");
                                        ed.putString("Harga_Temp", Integer.toString(addHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");
                                        listTxt.setText("");

                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";
                                        totalHargaField = Integer.toString(addHarga);

                                        dataPut = addHarga;
                                        listChange = "";
                                    }

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    getAlertDialog2(prompt2.getText().toString(), prompt.getText().toString());

                                    if (delHarga > 0) {
                                        ed.putString("List", listBarangDelChange);
                                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");

                                        String[] listTemp1 = listBarangDel.split("\n");

                                        dataBarang.clear();
                                        dataHarga.clear();
                                        dataStok.clear();
                                        dataTotal.clear();

                                        for (int i = 0; i < listTemp1.length; i++) {

                                            String[] listTemp2 = listTemp1[i].split("\t");

                                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                                    dataBarang.add(listTemp2[j]);
                                                    dataHarga.add(listTemp2[j + 1]);
                                                    dataStok.add(listTemp2[j + 2].substring(1));
                                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                                }
                                            }
                                        }

                                        listDatabase = "";
                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";

                                        for (int i = 0; i < dataBarang.size(); i++) {
                                            listDatabase = dataBarang.get(i) + "   " +
                                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                                    dataTotal.get(i) + "\n" + listDatabase;

                                            barangField = dataBarang.get(i) + "\n" + barangField;
                                            qtyField = dataStok.get(i) + "\n" + qtyField;
                                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                                            totalField = dataTotal.get(i) + "\n" + totalField;
                                        }

                                        listTxt.setText(listDatabase);
                                        totalHargaField = Integer.toString(delHarga);
                                        dataPut = delHarga;
                                        listChange = listBarangDelChange;
                                    } else {
                                        delHarga = 0;

                                        ed.putString("List", "");
                                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");
                                        listTxt.setText("");

                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";
                                        totalHargaField = Integer.toString(delHarga);
                                        dataPut = delHarga;
                                        listChange = "";
                                    }

                                    break;

                                case DialogInterface.BUTTON_NEUTRAL:

                                    break;
                            }
                        }
                    };

                    LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);

                    View promptView = layoutInflater.inflate(R.layout.activity_prompts, null);

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                    alertDialog.setView(promptView);

                    prompt2 = (EditText) promptView.findViewById(R.id.userInput2);
                    prompt = (EditText) promptView.findViewById(R.id.userInput);

                    alertDialog.setTitle("Success");
                    alertDialog.setMessage("").setPositiveButton("Add", dialogClickListener)
                            .setNegativeButton("Delete", dialogClickListener).setNeutralButton("Cancel", dialogClickListener);
                    alertDialog.setIcon((true) ? R.drawable.success : R.drawable.fail);

                    alertDialog.show();

                    //getAlertDialog("aaaa");

                }
            });


            Button camera = (Button) findViewById(R.id.switchCamera);
            camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    else
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

                    releaseCamera();

                    Intent i = new Intent(v.getContext(), MainActivity.class);
                    i.putExtra("Camera", currentCameraId);
                    i.putExtra("sharedName", sharedName);
                    i.putExtra("barangField", barangField);
                    i.putExtra("qtyField", qtyField);
                    i.putExtra("hargaField", hargaField);
                    i.putExtra("totalField", totalField);
                    i.putExtra("totalHargaField", totalHargaField);
                    i.putExtra("listDatabase", listDatabase);
                    i.putExtra("dataPut", dataPut);
                    i.putExtra("listChange", listChange);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivityForResult(i, 0);

                }
            });

            Button onCamera = (Button) findViewById(R.id.onCamera);
            onCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    /*if (previewing) {

                        previewing = false;
                        mCamera.setPreviewCallback(null);
                        mCamera.stopPreview();

                    } else {

                        if (cek2) {
                            initControls();
                            cek2 = false;
                        }
                        else {
                            barcodeScanned = false;
                            previewing = true;
                            mCamera.setPreviewCallback(previewCb);
                            mCamera.startPreview();
                        }

                    }*/

                    barcodeScanned = false;
                    previewing = true;
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                }
            });

            Button done = (Button) findViewById(R.id.doneButton);
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (dataPut == 0) {
                        showAlertDialog(v.getContext(), "Failed",
                                "Tidak Ada List Barang Pembelian", false);
                    } else {
                        releaseCamera();

                        Intent i = new Intent(v.getContext(), Results.class);
                        i.putExtra("sharedName", sharedName);
                        i.putExtra("barangField", barangField);
                        i.putExtra("qtyField", qtyField);
                        i.putExtra("hargaField", hargaField);
                        i.putExtra("totalField", totalField);
                        i.putExtra("totalHargaField", totalHargaField);
                        i.putExtra("listDatabase", listDatabase);
                        i.putExtra("dataPut", dataPut);
                        i.putExtra("listChange", listChange);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivityForResult(i, 0);
                    }
                }
            });

            Button logout = (Button) findViewById(R.id.logout);
            logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:

                                    session.logoutUser();

                                    releaseCamera();

                                    Intent intent = new Intent(getBaseContext(), Home.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivityForResult(intent, 0);

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                    ab.setMessage("Are you sure want to Logout?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });

            Button clear = (Button) findViewById(R.id.clear);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    barangField = "";
                    qtyField = "";
                    hargaField = "";
                    totalField = "";
                    totalHargaField = "";
                    listDatabase = "";
                    dataPut = 0;
                    listChange = "";

                    dataBarang.clear();
                    dataHarga.clear();
                    dataStok.clear();
                    dataTotal.clear();

                    listBarangAddChange = "";
                    addHarga = 0;
                    listBarangAdd = "";
                    listBarangDelChange = "";
                    listBarangDel = "";
                    delHarga = 0;

                    listChange = "";
                    listBarangAddCancel = "";

                    SharedPreferences sp;

                    sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                    ed = sp.edit();

                    ed.putString("List", "");
                    ed.putString("Harga_Temp", "");
                    ed.commit();

                    hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");
                    listTxt.setText(listDatabase);
                }
            });

            editTextNominal = (EditText) findViewById(R.id.masukkanHarga);
            discount = (EditText) findViewById(R.id.diskon);

            Button ok = (Button) findViewById(R.id.button);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        int nominalHarga = Integer.parseInt(editTextNominal.getText().toString());


                        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();

                        SharedPreferences sp;

                        hargaTxt = (TextView) findViewById(R.id.harga);
                        listTxt = (TextView) findViewById(R.id.listBarang);
                        barangTxt = (TextView) findViewById(R.id.barang);

                        sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                        ed = sp.edit();

                        nama_barang = "Lain-Lain";
                        int qty = 1;

                        addHarga = nominalHarga;

                        listBarangAdd = nama_barang + "\t" + addHarga;

                        if (hashMap.containsKey(listBarangAdd)) {
                            hashMap.put(listBarangAdd, hashMap.get(listBarangAdd) + qty);
                        } else {
                            hashMap.put(listBarangAdd, qty);
                        }

                        if (!sp.getString("List", "").isEmpty()) {

                            listBarangAddChange = "";

                            for (int i = 1; i <= qty; i++) {
                                listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                            }

                            listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                            String[] listTemp = sp.getString("List", "").split("\n");

                            for (int i = 0; i < listTemp.length; i++) {
                                if (hashMap.containsKey(listTemp[i])) {
                                    hashMap.put(listTemp[i], hashMap.get(listTemp[i]) + 1);
                                } else {
                                    hashMap.put(listTemp[i], 1);
                                }
                            }
                        } else {
                            listBarangAddChange = "";

                            for (int i = 1; i <= qty; i++) {
                                listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                            }
                        }

                        listBarangAdd = "";

                        for (Map.Entry<String, Integer> s : hashMap.entrySet()) {
                            listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                        }

                        try {

                            addHarga = (addHarga * qty) + Integer.parseInt(sp.getString("Harga_Temp", ""));

                        } catch (NumberFormatException e) {
                            //Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                    //Toast.LENGTH_LONG).show();
                        }

                        ed.putString("List", listBarangAddChange);
                        ed.putString("Harga_Temp", Integer.toString(addHarga));
                        ed.commit();

                        hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                        String[] listTemp1 = listBarangAdd.split("\n");

                        dataBarang.clear();
                        dataHarga.clear();
                        dataStok.clear();
                        dataTotal.clear();

                        for (int i = 0; i < listTemp1.length; i++) {

                            String[] listTemp2 = listTemp1[i].split("\t");

                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                    dataBarang.add(listTemp2[j]);
                                    dataHarga.add(listTemp2[j + 1]);
                                    dataStok.add(listTemp2[j + 2].substring(1));
                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                }
                            }
                        }

                        listDatabase = "";
                        barangField = "";
                        qtyField = "";
                        hargaField = "";
                        totalField = "";

                        for (int i = 0; i < dataBarang.size(); i++) {
                            listDatabase = dataBarang.get(i) + "   " +
                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                    dataTotal.get(i) + "\n" + listDatabase;

                            barangField = dataBarang.get(i) + "\n" + barangField;
                            qtyField = dataStok.get(i) + "\n" + qtyField;
                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                            totalField = dataTotal.get(i) + "\n" + totalField;
                        }

                        listTxt.setText(listDatabase);
                        totalHargaField = Integer.toString(addHarga);
                        dataPut = addHarga;
                        listChange = listBarangAddChange;

                        editTextNominal.setText("");

                        try {
                            int discountHarga = Integer.parseInt(discount.getText().toString());


                            HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();

                            hargaTxt = (TextView) findViewById(R.id.harga);
                            listTxt = (TextView) findViewById(R.id.listBarang);
                            barangTxt = (TextView) findViewById(R.id.barang);

                            sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                            ed = sp.edit();

                            nama_barang = "Discount";
                            qty = 1;

                            addHarga = discountHarga;

                            listBarangAdd = nama_barang + "\t" + addHarga;

                            if (hashMap2.containsKey(listBarangAdd)) {
                                hashMap2.put(listBarangAdd, hashMap2.get(listBarangAdd) + qty);
                            } else {
                                hashMap2.put(listBarangAdd, qty);
                            }

                            if (!sp.getString("List", "").isEmpty()) {

                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }

                                listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMap2.containsKey(listTemp[i])) {
                                        hashMap2.put(listTemp[i], hashMap2.get(listTemp[i]) + 1);
                                    } else {
                                        hashMap2.put(listTemp[i], 1);
                                    }
                                }
                            } else {
                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }
                            }

                            listBarangAdd = "";

                            for (Map.Entry<String, Integer> s : hashMap2.entrySet()) {
                                listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                            }

                            try {

                                addHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (addHarga * qty);

                            } catch (NumberFormatException e) {
                               // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                     //   Toast.LENGTH_LONG).show();
                            }

                            ed.putString("List", listBarangAddChange);
                            ed.putString("Harga_Temp", Integer.toString(addHarga));
                            ed.commit();

                            hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                            String[] listTemp3 = listBarangAdd.split("\n");

                            dataBarang.clear();
                            dataHarga.clear();
                            dataStok.clear();
                            dataTotal.clear();

                            for (int i = 0; i < listTemp3.length; i++) {

                                String[] listTemp2 = listTemp3[i].split("\t");

                                for (int j = 0; j < listTemp2.length - 2; j++) {
                                    if (!listTemp2[j + 2].substring(1).equals("0")) {
                                        dataBarang.add(listTemp2[j]);
                                        dataHarga.add(listTemp2[j + 1]);
                                        dataStok.add(listTemp2[j + 2].substring(1));
                                        dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                    }
                                }
                            }

                            listDatabase = "";
                            barangField = "";
                            qtyField = "";
                            hargaField = "";
                            totalField = "";

                            for (int i = 0; i < dataBarang.size(); i++) {
                                listDatabase = dataBarang.get(i) + "   " +
                                        dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                        dataTotal.get(i) + "\n" + listDatabase;

                                barangField = dataBarang.get(i) + "\n" + barangField;
                                qtyField = dataStok.get(i) + "\n" + qtyField;
                                hargaField = dataHarga.get(i) + "\n" + hargaField;
                                totalField = dataTotal.get(i) + "\n" + totalField;
                            }

                            listTxt.setText(listDatabase);
                            totalHargaField = Integer.toString(addHarga);
                            dataPut = addHarga;
                            listChange = listBarangAddChange;

                            discount.setText("");
                        } catch(NumberFormatException e) {

                        }

                    } catch(NumberFormatException e) {
                        try {
                            int discountHarga = Integer.parseInt(discount.getText().toString());


                            HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();

                            hargaTxt = (TextView) findViewById(R.id.harga);
                            listTxt = (TextView) findViewById(R.id.listBarang);
                            barangTxt = (TextView) findViewById(R.id.barang);

                            SharedPreferences sp;

                            sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                            ed = sp.edit();

                            nama_barang = "Discount";
                            int qty = 1;

                            addHarga = discountHarga;

                            listBarangAdd = nama_barang + "\t" + addHarga;

                            if (hashMap2.containsKey(listBarangAdd)) {
                                hashMap2.put(listBarangAdd, hashMap2.get(listBarangAdd) + qty);
                            } else {
                                hashMap2.put(listBarangAdd, qty);
                            }

                            if (!sp.getString("List", "").isEmpty()) {

                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }

                                listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMap2.containsKey(listTemp[i])) {
                                        hashMap2.put(listTemp[i], hashMap2.get(listTemp[i]) + 1);
                                    } else {
                                        hashMap2.put(listTemp[i], 1);
                                    }
                                }
                            } else {
                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }
                            }

                            listBarangAdd = "";

                            for (Map.Entry<String, Integer> s : hashMap2.entrySet()) {
                                listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                            }

                            try {

                                addHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (addHarga * qty);

                            } catch (NumberFormatException ef) {
                               // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                    //    Toast.LENGTH_LONG).show();
                            }

                            ed.putString("List", listBarangAddChange);
                            ed.putString("Harga_Temp", Integer.toString(addHarga));
                            ed.commit();

                            hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                            String[] listTemp3 = listBarangAdd.split("\n");

                            dataBarang.clear();
                            dataHarga.clear();
                            dataStok.clear();
                            dataTotal.clear();

                            for (int i = 0; i < listTemp3.length; i++) {

                                String[] listTemp2 = listTemp3[i].split("\t");

                                for (int j = 0; j < listTemp2.length - 2; j++) {
                                    if (!listTemp2[j + 2].substring(1).equals("0")) {
                                        dataBarang.add(listTemp2[j]);
                                        dataHarga.add(listTemp2[j + 1]);
                                        dataStok.add(listTemp2[j + 2].substring(1));
                                        dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                    }
                                }
                            }

                            listDatabase = "";
                            barangField = "";
                            qtyField = "";
                            hargaField = "";
                            totalField = "";

                            for (int i = 0; i < dataBarang.size(); i++) {
                                listDatabase = dataBarang.get(i) + "   " +
                                        dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                        dataTotal.get(i) + "\n" + listDatabase;

                                barangField = dataBarang.get(i) + "\n" + barangField;
                                qtyField = dataStok.get(i) + "\n" + qtyField;
                                hargaField = dataHarga.get(i) + "\n" + hargaField;
                                totalField = dataTotal.get(i) + "\n" + totalField;
                            }

                            listTxt.setText(listDatabase);
                            totalHargaField = Integer.toString(addHarga);
                            dataPut = addHarga;
                            listChange = listBarangAddChange;

                            discount.setText("");
                        } catch(NumberFormatException ef) {

                        }
                    }
                }
            });

            Button del = (Button) findViewById(R.id.deleteOther);
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        int nominalHarga = Integer.parseInt(discount.getText().toString());

                        HashMap<String, Integer> hashMapDelete = new HashMap<String, Integer>();

                        SharedPreferences sp;

                        hargaTxt = (TextView) findViewById(R.id.harga);
                        listTxt = (TextView) findViewById(R.id.listBarang);
                        barangTxt = (TextView) findViewById(R.id.barang);

                        sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                        ed = sp.edit();

                        nama_barang = "Discount";
                        int qty = 1;

                        delHarga = nominalHarga;
                        addHarga = nominalHarga;

                        if (!sp.getString("List", "").isEmpty()) {

                            String[] listTemp = sp.getString("List", "").split("\n");

                            for (int i = 0; i < listTemp.length; i++) {
                                if (hashMapDelete.containsKey(listTemp[i])) {
                                    hashMapDelete.put(listTemp[i], hashMapDelete.get(listTemp[i]) + 1);
                                } else {
                                    hashMapDelete.put(listTemp[i], 1);
                                }
                            }
                        } else {

                        }

                        listBarangDel = nama_barang + "\t" + delHarga;
                        boolean cekMinus = false;
                        int tempMinus = -1;

                        if (hashMapDelete.containsKey(listBarangDel)) {
                            if (hashMapDelete.get(listBarangDel) < qty) {
                                cekMinus = true;
                                tempMinus = hashMapDelete.get(listBarangDel);
                                hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - hashMapDelete.get(listBarangDel));
                            } else {
                                hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - qty);
                            }

                            listBarangDel = "";
                            listBarangDelChange = "";

                            for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                            }

                            String temp = nama_barang + "\t" + addHarga;
                            String[] listTemp = sp.getString("List", "").split("\n");

                            int inc = -1;

                            if (cekMinus) {
                                inc = tempMinus;
                            } else {
                                inc = qty;
                            }

                            for (int i = listTemp.length - 1; i >= 0; i--) {
                                if (listTemp[i].equals(temp)) {
                                    if (inc == 1) {
                                        temp = "";
                                    }
                                    inc--;
                                } else {
                                    listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                }
                            }

                            try {

                                int inc2 = -1;

                                if (cekMinus) {
                                    inc2 = tempMinus;
                                } else {
                                    inc2 = qty;
                                }

                                delHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (delHarga * inc2);

                            } catch (NumberFormatException e) {
                                //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                //    Toast.LENGTH_LONG).show();
                            }

                        } else {
                            //hashMapDelete.put(listBarangDel, 1);

                            listBarangDel = "";
                            listBarangDelChange = "";

                            for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                            }

                            String[] listTemp = sp.getString("List", "").split("\n");

                            for (int i = listTemp.length - 1; i >= 0; i--) {
                                listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                            }

                            try {

                                delHarga = Integer.parseInt(sp.getString("Harga_Temp", ""));

                            } catch (NumberFormatException e) {
                                //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                //      Toast.LENGTH_LONG).show();
                            }
                        }

                        ed.putString("List", listBarangDelChange);
                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                        ed.commit();

                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");

                        String[] listTemp1 = listBarangDel.split("\n");

                        dataBarang.clear();
                        dataHarga.clear();
                        dataStok.clear();
                        dataTotal.clear();

                        for (int i = 0; i < listTemp1.length; i++) {

                            String[] listTemp2 = listTemp1[i].split("\t");

                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                    dataBarang.add(listTemp2[j]);
                                    dataHarga.add(listTemp2[j + 1]);
                                    dataStok.add(listTemp2[j + 2].substring(1));
                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                }
                            }
                        }

                        listDatabase = "";
                        barangField = "";
                        qtyField = "";
                        hargaField = "";
                        totalField = "";

                        for (int i = 0; i < dataBarang.size(); i++) {
                            listDatabase = dataBarang.get(i) + "   " +
                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                    dataTotal.get(i) + "\n" + listDatabase;

                            barangField = dataBarang.get(i) + "\n" + barangField;
                            qtyField = dataStok.get(i) + "\n" + qtyField;
                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                            totalField = dataTotal.get(i) + "\n" + totalField;
                        }

                        listTxt.setText(listDatabase);
                        totalHargaField = Integer.toString(delHarga);
                        dataPut = delHarga;
                        listChange = listBarangDelChange;

                    } catch(NumberFormatException e) {

                    }

                    discount.setText("");

                    try {
                        int nominalHarga = Integer.parseInt(editTextNominal.getText().toString());


                        HashMap<String, Integer> hashMapDelete = new HashMap<String, Integer>();

                        SharedPreferences sp;

                        hargaTxt = (TextView) findViewById(R.id.harga);
                        listTxt = (TextView) findViewById(R.id.listBarang);
                        barangTxt = (TextView) findViewById(R.id.barang);

                        sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                        ed = sp.edit();

                        nama_barang = "Lain-Lain";
                        int qty = 1;

                        delHarga = nominalHarga;
                        addHarga = nominalHarga;

                        if (!sp.getString("List", "").isEmpty()) {

                            String[] listTemp = sp.getString("List", "").split("\n");

                            for (int i = 0; i < listTemp.length; i++) {
                                if (hashMapDelete.containsKey(listTemp[i])) {
                                    hashMapDelete.put(listTemp[i], hashMapDelete.get(listTemp[i]) + 1);
                                } else {
                                    hashMapDelete.put(listTemp[i], 1);
                                }
                            }
                        } else {

                        }

                        listBarangDel = nama_barang + "\t" + delHarga;
                        boolean cekMinus = false;
                        int tempMinus = -1;

                        if (hashMapDelete.containsKey(listBarangDel)) {
                            if (hashMapDelete.get(listBarangDel) < qty) {
                                cekMinus = true;
                                tempMinus = hashMapDelete.get(listBarangDel);
                                hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - hashMapDelete.get(listBarangDel));
                            } else {
                                hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - qty);
                            }

                            listBarangDel = "";
                            listBarangDelChange = "";

                            for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                            }

                            String temp = nama_barang + "\t" + addHarga;
                            String[] listTemp = sp.getString("List", "").split("\n");

                            int inc = -1;

                            if (cekMinus) {
                                inc = tempMinus;
                            } else {
                                inc = qty;
                            }

                            for (int i = listTemp.length - 1; i >= 0; i--) {
                                if (listTemp[i].equals(temp)) {
                                    if (inc == 1) {
                                        temp = "";
                                    }
                                    inc--;
                                } else {
                                    listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                }
                            }

                            try {

                                int inc2 = -1;

                                if (cekMinus) {
                                    inc2 = tempMinus;
                                } else {
                                    inc2 = qty;
                                }

                                delHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (delHarga * inc2);

                            } catch (NumberFormatException e) {
                              //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                    //    Toast.LENGTH_LONG).show();
                            }

                        } else {
                            //hashMapDelete.put(listBarangDel, 1);

                            listBarangDel = "";
                            listBarangDelChange = "";

                            for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                            }

                            String[] listTemp = sp.getString("List", "").split("\n");

                            for (int i = listTemp.length - 1; i >= 0; i--) {
                                listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                            }

                            try {

                                delHarga = Integer.parseInt(sp.getString("Harga_Temp", ""));

                            } catch (NumberFormatException e) {
                              //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                  //      Toast.LENGTH_LONG).show();
                            }
                        }

                        ed.putString("List", listBarangDelChange);
                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                        ed.commit();

                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");

                        String[] listTemp1 = listBarangDel.split("\n");

                        dataBarang.clear();
                        dataHarga.clear();
                        dataStok.clear();
                        dataTotal.clear();

                        for (int i = 0; i < listTemp1.length; i++) {

                            String[] listTemp2 = listTemp1[i].split("\t");

                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                    dataBarang.add(listTemp2[j]);
                                    dataHarga.add(listTemp2[j + 1]);
                                    dataStok.add(listTemp2[j + 2].substring(1));
                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                }
                            }
                        }

                        listDatabase = "";
                        barangField = "";
                        qtyField = "";
                        hargaField = "";
                        totalField = "";

                        for (int i = 0; i < dataBarang.size(); i++) {
                            listDatabase = dataBarang.get(i) + "   " +
                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                    dataTotal.get(i) + "\n" + listDatabase;

                            barangField = dataBarang.get(i) + "\n" + barangField;
                            qtyField = dataStok.get(i) + "\n" + qtyField;
                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                            totalField = dataTotal.get(i) + "\n" + totalField;
                        }

                        listTxt.setText(listDatabase);
                        totalHargaField = Integer.toString(delHarga);
                        dataPut = delHarga;
                        listChange = listBarangDelChange;

                    } catch(NumberFormatException e) {

                    }

                    editTextNominal.setText("");

                }
            });

            swicth = (Switch)  findViewById(R.id.switch1);

            swicth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        cek3 = 2;
                    } else {
                        cek3 = 1;
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    private void initControls() {

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance(currentCameraId);

        // Instance barcode scanner
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(MainActivity.this, mCamera, previewCb,
                autoFocusCB);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            releaseCamera();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance(int currentCameraId) {
        Camera c = null;
        try {
            c = Camera.open(currentCameraId);
        } catch (Exception e) {
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                //releaseCamera();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {

                    /*Log.i("<<<<<<Asset Code>>>>> ",
                            "<<<<Bar Code>>> " + sym.getData());*/
                    scanResult = sym.getData().trim();

                    barcodeScanned = true;

                    getAlertDialog(scanResult);

                    break;
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    private void showAlertDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private void getAlertDialog2(String kode_barang, String qty2) {
        //if (isInternetConnected(new Connections().con())) {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("kode_barang", kode_barang));

            JSONObject json = jsonParser.makeHttpRequest(
                    LOGIN_URL, "POST", params);

            success = json.getInt(TAG_SUCCESS);

            if (success == 0) {
                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();

                alertDialog.setTitle("Failed");
                alertDialog.setMessage("Data barang tidak dikenali");
                alertDialog.setIcon((false) ? R.drawable.success : R.drawable.fail);

                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                alertDialog.show();
            } else {
                    /*releaseCamera();

                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.putExtra("Harga", Integer.toString(harga));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivityForResult(intent, 0);*/

                nama_barang = json.getString(TAG_NAMA_BARANG);
                harga = json.getInt(TAG_HARGA);
                jumlah_stok = json.getInt(TAG_JUMLAH_STOK);

                hargaTemp = 0;

                addHarga = harga;
                delHarga = harga;
                hargaTemp = harga;

                if (addHarga == 0 && delHarga == 0) {
                    ed.putString("Harga_Temp", Integer.toString(0));
                    ed.putString("List", "");
                    ed.commit();
                }

                sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                ed = sp.edit();

                hargaTemp = 0;

                try {

                    int qty = -1;
                    if (prompt.getText().toString().equals("")) {
                        qty = 1;
                    } else {
                        qty = Integer.parseInt(qty2);
                    }

                    //Toast.makeText(MainActivity.this, ""+qty, Toast.LENGTH_LONG).show();

                    addHarga = harga;
                    delHarga = harga;
                    hargaTemp = harga;

                    hargaTxt = (TextView) findViewById(R.id.harga);
                    listTxt = (TextView) findViewById(R.id.listBarang);
                    barangTxt = (TextView) findViewById(R.id.barang);

                    if (addHarga == 0 && delHarga == 0) {
                        ed.putString("Harga_Temp", Integer.toString(0));
                        ed.putString("List", "");
                        ed.commit();
                    }

                    HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();
                    HashMap<String, Integer> hashMapDelete = new HashMap<String, Integer>();
                    HashMap<String, Integer> hashMap = new HashMap<String, Integer>();

                    // ############ Button Add dan Change Awal ############
                    listBarangAdd = nama_barang + "\t" + addHarga;

                    if (hashMap.containsKey(listBarangAdd)) {
                        hashMap.put(listBarangAdd, hashMap.get(listBarangAdd) + qty);
                    } else {
                        hashMap.put(listBarangAdd, qty);
                    }

                    if (!sp.getString("List", "").isEmpty()) {

                        listBarangAddChange = "";

                        for (int i = 1; i <= qty; i++) {
                            listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                        }

                        listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                        String[] listTemp = sp.getString("List", "").split("\n");

                        for (int i = 0; i < listTemp.length; i++) {
                            if (hashMap.containsKey(listTemp[i])) {
                                hashMap.put(listTemp[i], hashMap.get(listTemp[i]) + 1);
                            } else {
                                hashMap.put(listTemp[i], 1);
                            }
                        }
                    } else {
                        listBarangAddChange = "";

                        for (int i = 1; i <= qty; i++) {
                            listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                        }
                    }

                    listBarangAdd = "";

                    for (Map.Entry<String, Integer> s : hashMap.entrySet()) {
                        listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                    }
                    // ############ Button Add Akhir ############


                    // ############ Button Cancel Awal ############
                    if (!sp.getString("List", "").isEmpty()) {

                        String[] listTemp = sp.getString("List", "").split("\n");

                        for (int i = 0; i < listTemp.length; i++) {
                            if (hashMapCancel.containsKey(listTemp[i])) {
                                hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                            } else {
                                hashMapCancel.put(listTemp[i], 1);
                            }
                        }
                    }

                    listBarangAddCancel = "";

                    for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                        listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                    }
                    // ############ Button Cancel Akhir ############


                    // ############ Button Delete Awal ############
                    if (!sp.getString("List", "").isEmpty()) {

                        String[] listTemp = sp.getString("List", "").split("\n");

                        for (int i = 0; i < listTemp.length; i++) {
                            if (hashMapDelete.containsKey(listTemp[i])) {
                                hashMapDelete.put(listTemp[i], hashMapDelete.get(listTemp[i]) + 1);
                            } else {
                                hashMapDelete.put(listTemp[i], 1);
                            }
                        }
                    } else {

                    }

                    listBarangDel = nama_barang + "\t" + delHarga;
                    boolean cekMinus = false;
                    int tempMinus = -1;

                    if (hashMapDelete.containsKey(listBarangDel)) {
                        if (hashMapDelete.get(listBarangDel) < qty) {
                            cekMinus = true;
                            tempMinus = hashMapDelete.get(listBarangDel);
                            hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - hashMapDelete.get(listBarangDel));
                        } else {
                            hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - qty);
                        }

                        listBarangDel = "";
                        listBarangDelChange = "";

                        for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                            listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                        }

                        String temp = nama_barang + "\t" + addHarga;
                        String[] listTemp = sp.getString("List", "").split("\n");

                        int inc = -1;

                        if (cekMinus) {
                            inc = tempMinus;
                        } else {
                            inc = qty;
                        }

                        for (int i = listTemp.length - 1; i >= 0; i--) {
                            if (listTemp[i].equals(temp)) {
                                if (inc == 1) {
                                    temp = "";
                                }
                                inc--;
                            } else {
                                listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                            }
                        }

                        try {

                            int inc2 = -1;

                            if (cekMinus) {
                                inc2 = tempMinus;
                            } else {
                                inc2 = qty;
                            }

                            delHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (delHarga * inc2);

                        } catch (NumberFormatException e) {
                           // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                //    Toast.LENGTH_LONG).show();
                        }

                    } else {
                        //hashMapDelete.put(listBarangDel, 1);

                        listBarangDel = "";
                        listBarangDelChange = "";

                        for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                            listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                        }

                        String[] listTemp = sp.getString("List", "").split("\n");

                        for (int i = listTemp.length - 1; i >= 0; i--) {
                            listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                        }

                        try {

                            delHarga = Integer.parseInt(sp.getString("Harga_Temp", ""));

                        } catch (NumberFormatException e) {
                           // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                //    Toast.LENGTH_LONG).show();
                        }
                    }

                    // ############ Button Delete Akhir ############

                    try {

                        addHarga = (addHarga * qty) + Integer.parseInt(sp.getString("Harga_Temp", ""));

                    } catch (NumberFormatException e) {
                       // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                              //  Toast.LENGTH_LONG).show();
                    }

                    //hargaTxt.setText("Rp. " + sp.getString("Harga_Temp", "") + ",-");
                    //listTxt.setText(listBarangAdd);

                    //barangTxt.setText(Integer.toString(hargaTemp));

                } catch (NumberFormatException e) {

                    HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();

                    // ############ Button Cancel Awal ############
                    if (!sp.getString("List", "").isEmpty()) {

                        String[] listTemp = sp.getString("List", "").split("\n");

                        for (int i = 0; i < listTemp.length; i++) {
                            if (hashMapCancel.containsKey(listTemp[i])) {
                                hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                            } else {
                                hashMapCancel.put(listTemp[i], 1);
                            }
                        }
                    }

                    listBarangAddCancel = "";

                    for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                        listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                    }
                    // ############ Button Cancel Akhir ############

                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        /*} else {

            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();

            alertDialog.setTitle("Internet Connection");
            alertDialog.setMessage("Please check your internet connection");
            alertDialog.setIcon((false) ? R.drawable.success : R.drawable.fail);

            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    barcodeScanned = false;
                    previewing = true;
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                }
            });

            alertDialog.show();
        }*/
    }


    private void getAlertDialog(String kode_barang) {
        //if (isInternetConnected(new Connections().con())) {
            try {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("kode_barang", kode_barang));

                JSONObject json = jsonParser.makeHttpRequest(
                        LOGIN_URL, "POST", params);

                success = json.getInt(TAG_SUCCESS);

                if (success == 0) {
                    android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();

                    alertDialog.setTitle("Failed");
                    alertDialog.setMessage("Data barang tidak dikenali");
                    alertDialog.setIcon((false) ? R.drawable.success : R.drawable.fail);

                    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            barcodeScanned = false;
                            previewing = true;
                            mCamera.setPreviewCallback(previewCb);
                            mCamera.startPreview();
                        }
                    });

                    alertDialog.show();
                } else {

                    MediaPlayer mp = MediaPlayer.create(this, R.raw.beep);
                    mp.start();

                    /*releaseCamera();

                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.putExtra("Harga", Integer.toString(harga));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivityForResult(intent, 0);*/

                    nama_barang = json.getString(TAG_NAMA_BARANG);
                    harga = json.getInt(TAG_HARGA);
                    jumlah_stok = json.getInt(TAG_JUMLAH_STOK);

                    hargaTemp = 0;

                    addHarga = harga;
                    delHarga = harga;
                    hargaTemp = harga;

                    if (addHarga == 0 && delHarga == 0) {
                        ed.putString("Harga_Temp", Integer.toString(0));
                        ed.putString("List", "");
                        ed.commit();
                    }

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

//###########################################################################################################################

                            sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                            ed = sp.edit();

                            hargaTemp = 0;

                            try {

                                int qty = -1;
                                if (prompt.getText().toString().equals("")) {
                                    qty = 1;
                                } else {
                                    qty = Integer.parseInt(prompt.getText().toString());
                                }

                                //Toast.makeText(MainActivity.this, ""+qty, Toast.LENGTH_LONG).show();

                                addHarga = harga;
                                delHarga = harga;
                                hargaTemp = harga;

                                hargaTxt = (TextView) findViewById(R.id.harga);
                                listTxt = (TextView) findViewById(R.id.listBarang);
                                barangTxt = (TextView) findViewById(R.id.barang);

                                if (addHarga == 0 && delHarga == 0) {
                                    ed.putString("Harga_Temp", Integer.toString(0));
                                    ed.putString("List", "");
                                    ed.commit();
                                }

                                HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();
                                HashMap<String, Integer> hashMapDelete = new HashMap<String, Integer>();
                                HashMap<String, Integer> hashMap = new HashMap<String, Integer>();

                                // ############ Button Add dan Change Awal ############
                                listBarangAdd = nama_barang + "\t" + addHarga;

                                if (hashMap.containsKey(listBarangAdd)) {
                                    hashMap.put(listBarangAdd, hashMap.get(listBarangAdd) + qty);
                                } else {
                                    hashMap.put(listBarangAdd, qty);
                                }

                                if (!sp.getString("List", "").isEmpty()) {

                                    listBarangAddChange = "";

                                    for (int i = 1; i <= qty; i++) {
                                        listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                    }

                                    listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    for (int i = 0; i < listTemp.length; i++) {
                                        if (hashMap.containsKey(listTemp[i])) {
                                            hashMap.put(listTemp[i], hashMap.get(listTemp[i]) + 1);
                                        } else {
                                            hashMap.put(listTemp[i], 1);
                                        }
                                    }
                                } else {
                                    listBarangAddChange = "";

                                    for (int i = 1; i <= qty; i++) {
                                        listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                    }
                                }

                                listBarangAdd = "";

                                for (Map.Entry<String, Integer> s : hashMap.entrySet()) {
                                    listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                                }
                                // ############ Button Add Akhir ############


                                // ############ Button Cancel Awal ############
                                if (!sp.getString("List", "").isEmpty()) {

                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    for (int i = 0; i < listTemp.length; i++) {
                                        if (hashMapCancel.containsKey(listTemp[i])) {
                                            hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                                        } else {
                                            hashMapCancel.put(listTemp[i], 1);
                                        }
                                    }
                                }

                                listBarangAddCancel = "";

                                for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                                    listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                                }
                                // ############ Button Cancel Akhir ############


                                // ############ Button Delete Awal ############
                                if (!sp.getString("List", "").isEmpty()) {

                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    for (int i = 0; i < listTemp.length; i++) {
                                        if (hashMapDelete.containsKey(listTemp[i])) {
                                            hashMapDelete.put(listTemp[i], hashMapDelete.get(listTemp[i]) + 1);
                                        } else {
                                            hashMapDelete.put(listTemp[i], 1);
                                        }
                                    }
                                } else {

                                }

                                listBarangDel = nama_barang + "\t" + delHarga;
                                boolean cekMinus = false;
                                int tempMinus = -1;

                                if (hashMapDelete.containsKey(listBarangDel)) {
                                    if (hashMapDelete.get(listBarangDel) < qty) {
                                        cekMinus = true;
                                        tempMinus = hashMapDelete.get(listBarangDel);
                                        hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - hashMapDelete.get(listBarangDel));
                                    } else {
                                        hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - qty);
                                    }

                                    listBarangDel = "";
                                    listBarangDelChange = "";

                                    for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                        listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                                    }

                                    String temp = nama_barang + "\t" + addHarga;
                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    int inc = -1;

                                    if (cekMinus) {
                                        inc = tempMinus;
                                    } else {
                                        inc = qty;
                                    }

                                    for (int i = listTemp.length - 1; i >= 0; i--) {
                                        if (listTemp[i].equals(temp)) {
                                            if (inc == 1) {
                                                temp = "";
                                            }
                                            inc--;
                                        } else {
                                            listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                        }
                                    }

                                    try {

                                        int inc2 = -1;

                                        if (cekMinus) {
                                            inc2 = tempMinus;
                                        } else {
                                            inc2 = qty;
                                        }

                                        delHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (delHarga * inc2);

                                    } catch (NumberFormatException e) {
                                       // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                           //     Toast.LENGTH_LONG).show();
                                    }

                                } else {
                                    //hashMapDelete.put(listBarangDel, 1);

                                    listBarangDel = "";
                                    listBarangDelChange = "";

                                    for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                        listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                                    }

                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    for (int i = listTemp.length - 1; i >= 0; i--) {
                                        listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                    }

                                    try {

                                        delHarga = Integer.parseInt(sp.getString("Harga_Temp", ""));

                                    } catch (NumberFormatException e) {
                                      //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                          //      Toast.LENGTH_LONG).show();
                                    }
                                }

                                // ############ Button Delete Akhir ############

                                try {

                                    addHarga = (addHarga * qty) + Integer.parseInt(sp.getString("Harga_Temp", ""));

                                } catch (NumberFormatException e) {
                                  //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                      //      Toast.LENGTH_LONG).show();
                                }

                                //hargaTxt.setText("Rp. " + sp.getString("Harga_Temp", "") + ",-");
                                //listTxt.setText(listBarangAdd);

                                //barangTxt.setText(Integer.toString(hargaTemp));

                            } catch (NumberFormatException e) {

                                HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();

                                // ############ Button Cancel Awal ############
                                if (!sp.getString("List", "").isEmpty()) {

                                    String[] listTemp = sp.getString("List", "").split("\n");

                                    for (int i = 0; i < listTemp.length; i++) {
                                        if (hashMapCancel.containsKey(listTemp[i])) {
                                            hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                                        } else {
                                            hashMapCancel.put(listTemp[i], 1);
                                        }
                                    }
                                }

                                listBarangAddCancel = "";

                                for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                                    listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                                }
                                // ############ Button Cancel Akhir ############

                            }

//###########################################################################################################################

                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:

                                    if (addHarga > 0) {

                                        ed.putString("List", listBarangAddChange);
                                        ed.putString("Harga_Temp", Integer.toString(addHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                                        String[] listTemp1 = listBarangAdd.split("\n");

                                        dataBarang.clear();
                                        dataHarga.clear();
                                        dataStok.clear();
                                        dataTotal.clear();

                                        for (int i = 0; i < listTemp1.length; i++) {

                                            String[] listTemp2 = listTemp1[i].split("\t");

                                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                                    dataBarang.add(listTemp2[j]);
                                                    dataHarga.add(listTemp2[j + 1]);
                                                    dataStok.add(listTemp2[j + 2].substring(1));
                                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                                }
                                            }
                                        }

                                        listDatabase = "";
                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";

                                        for (int i = 0; i < dataBarang.size(); i++) {
                                            listDatabase = dataBarang.get(i) + "   " +
                                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                                    dataTotal.get(i) + "\n" + listDatabase;

                                            barangField = dataBarang.get(i) + "\n" + barangField;
                                            qtyField = dataStok.get(i) + "\n" + qtyField;
                                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                                            totalField = dataTotal.get(i) + "\n" + totalField;
                                        }

                                        listTxt.setText(listDatabase);
                                        totalHargaField = Integer.toString(addHarga);
                                        dataPut = addHarga;
                                        listChange = listBarangAddChange;
                                    } else {
                                        addHarga = 0;

                                        ed.putString("List", "");
                                        ed.putString("Harga_Temp", Integer.toString(addHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");
                                        listTxt.setText("");

                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";
                                        totalHargaField = Integer.toString(addHarga);

                                        dataPut = addHarga;
                                        listChange = "";
                                    }

                                    barcodeScanned = false;
                                    previewing = true;
                                    mCamera.setPreviewCallback(previewCb);
                                    mCamera.startPreview();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    if (delHarga > 0) {
                                        ed.putString("List", listBarangDelChange);
                                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");

                                        String[] listTemp1 = listBarangDel.split("\n");

                                        dataBarang.clear();
                                        dataHarga.clear();
                                        dataStok.clear();
                                        dataTotal.clear();

                                        for (int i = 0; i < listTemp1.length; i++) {

                                            String[] listTemp2 = listTemp1[i].split("\t");

                                            for (int j = 0; j < listTemp2.length - 2; j++) {
                                                if (!listTemp2[j + 2].substring(1).equals("0")) {
                                                    dataBarang.add(listTemp2[j]);
                                                    dataHarga.add(listTemp2[j + 1]);
                                                    dataStok.add(listTemp2[j + 2].substring(1));
                                                    dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                                }
                                            }
                                        }

                                        listDatabase = "";
                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";

                                        for (int i = 0; i < dataBarang.size(); i++) {
                                            listDatabase = dataBarang.get(i) + "   " +
                                                    dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                                    dataTotal.get(i) + "\n" + listDatabase;

                                            barangField = dataBarang.get(i) + "\n" + barangField;
                                            qtyField = dataStok.get(i) + "\n" + qtyField;
                                            hargaField = dataHarga.get(i) + "\n" + hargaField;
                                            totalField = dataTotal.get(i) + "\n" + totalField;
                                        }

                                        listTxt.setText(listDatabase);
                                        totalHargaField = Integer.toString(delHarga);
                                        dataPut = delHarga;
                                        listChange = listBarangDelChange;
                                    } else {
                                        delHarga = 0;

                                        ed.putString("List", "");
                                        ed.putString("Harga_Temp", Integer.toString(delHarga));
                                        ed.commit();

                                        hargaTxt.setText("Rp. " + Integer.toString(delHarga) + ",-");
                                        listTxt.setText("");

                                        barangField = "";
                                        qtyField = "";
                                        hargaField = "";
                                        totalField = "";
                                        totalHargaField = Integer.toString(delHarga);
                                        dataPut = delHarga;
                                        listChange = "";
                                    }

                                    barcodeScanned = false;
                                    previewing = true;
                                    mCamera.setPreviewCallback(previewCb);
                                    mCamera.startPreview();

                                    break;

                                case DialogInterface.BUTTON_NEUTRAL:

                                    String[] listTemp1 = listBarangAddCancel.split("\n");

                                    dataBarang.clear();
                                    dataHarga.clear();
                                    dataStok.clear();
                                    dataTotal.clear();

                                    for (int i = 0; i < listTemp1.length; i++) {

                                        String[] listTemp2 = listTemp1[i].split("\t");

                                        for (int j = 0; j < listTemp2.length - 2; j++) {
                                            dataBarang.add(listTemp2[j]);
                                            dataHarga.add(listTemp2[j + 1]);
                                            dataStok.add(listTemp2[j + 2].substring(1));
                                            dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                        }
                                    }

                                    listDatabase = "";
                                    barangField = "";
                                    qtyField = "";
                                    hargaField = "";
                                    totalField = "";

                                    for (int i = 0; i < dataBarang.size(); i++) {
                                        listDatabase = dataBarang.get(i) + "   " +
                                                dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                                dataTotal.get(i) + "\n" + listDatabase;

                                        barangField = dataBarang.get(i) + "\n" + barangField;
                                        qtyField = dataStok.get(i) + "\n" + qtyField;
                                        hargaField = dataHarga.get(i) + "\n" + hargaField;
                                        totalField = dataTotal.get(i) + "\n" + totalField;
                                    }

                                    listTxt.setText(listDatabase);
                                    totalHargaField = Integer.toString(addHarga);
                                    dataPut = addHarga;
                                    listChange = listBarangAddCancel;

                                    barcodeScanned = false;
                                    previewing = true;
                                    mCamera.setPreviewCallback(previewCb);
                                    mCamera.startPreview();

                                    break;
                            }
                        }
                    };

                    if (cek3 == 2) {
                        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);

                        View promptView = layoutInflater.inflate(R.layout.activity_prompts2, null);

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                        alertDialog.setView(promptView);

                        prompt = (EditText) promptView.findViewById(R.id.userInput);

                        alertDialog.setTitle("Success");
                        alertDialog.setMessage(nama_barang + ", " + harga).setPositiveButton("Add", dialogClickListener)
                                .setNegativeButton("Delete                             ", dialogClickListener).setNeutralButton("Cancel", dialogClickListener);
                        alertDialog.setIcon((true) ? R.drawable.success : R.drawable.fail);

                        alertDialog.show();
                    }
                    else if (cek3 == 1) {


                        sp = getSharedPreferences("QRCodeScanner", MODE_PRIVATE);
                        ed = sp.edit();

                        hargaTemp = 0;

                        try {

                            int qty = 1;

                            //Toast.makeText(MainActivity.this, ""+qty, Toast.LENGTH_LONG).show();

                            addHarga = harga;
                            delHarga = harga;
                            hargaTemp = harga;

                            hargaTxt = (TextView) findViewById(R.id.harga);
                            listTxt = (TextView) findViewById(R.id.listBarang);
                            barangTxt = (TextView) findViewById(R.id.barang);

                            if (addHarga == 0 && delHarga == 0) {
                                ed.putString("Harga_Temp", Integer.toString(0));
                                ed.putString("List", "");
                                ed.commit();
                            }

                            HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();
                            HashMap<String, Integer> hashMapDelete = new HashMap<String, Integer>();
                            HashMap<String, Integer> hashMap = new HashMap<String, Integer>();

                            // ############ Button Add dan Change Awal ############
                            listBarangAdd = nama_barang + "\t" + addHarga;

                            if (hashMap.containsKey(listBarangAdd)) {
                                hashMap.put(listBarangAdd, hashMap.get(listBarangAdd) + qty);
                            } else {
                                hashMap.put(listBarangAdd, qty);
                            }

                            if (!sp.getString("List", "").isEmpty()) {

                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }

                                listBarangAddChange = listBarangAddChange + sp.getString("List", "");
                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMap.containsKey(listTemp[i])) {
                                        hashMap.put(listTemp[i], hashMap.get(listTemp[i]) + 1);
                                    } else {
                                        hashMap.put(listTemp[i], 1);
                                    }
                                }
                            } else {
                                listBarangAddChange = "";

                                for (int i = 1; i <= qty; i++) {
                                    listBarangAddChange = nama_barang + "\t" + addHarga + "\n" + listBarangAddChange;
                                }
                            }

                            listBarangAdd = "";

                            for (Map.Entry<String, Integer> s : hashMap.entrySet()) {
                                listBarangAdd = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAdd;
                            }
                            // ############ Button Add Akhir ############


                            // ############ Button Cancel Awal ############
                            if (!sp.getString("List", "").isEmpty()) {

                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMapCancel.containsKey(listTemp[i])) {
                                        hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                                    } else {
                                        hashMapCancel.put(listTemp[i], 1);
                                    }
                                }
                            }

                            listBarangAddCancel = "";

                            for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                                listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                            }
                            // ############ Button Cancel Akhir ############


                            // ############ Button Delete Awal ############
                            if (!sp.getString("List", "").isEmpty()) {

                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMapDelete.containsKey(listTemp[i])) {
                                        hashMapDelete.put(listTemp[i], hashMapDelete.get(listTemp[i]) + 1);
                                    } else {
                                        hashMapDelete.put(listTemp[i], 1);
                                    }
                                }
                            } else {

                            }

                            listBarangDel = nama_barang + "\t" + delHarga;
                            boolean cekMinus = false;
                            int tempMinus = -1;

                            if (hashMapDelete.containsKey(listBarangDel)) {
                                if (hashMapDelete.get(listBarangDel) < qty) {
                                    cekMinus = true;
                                    tempMinus = hashMapDelete.get(listBarangDel);
                                    hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - hashMapDelete.get(listBarangDel));
                                } else {
                                    hashMapDelete.put(listBarangDel, hashMapDelete.get(listBarangDel) - qty);
                                }

                                listBarangDel = "";
                                listBarangDelChange = "";

                                for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                    listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                                }

                                String temp = nama_barang + "\t" + addHarga;
                                String[] listTemp = sp.getString("List", "").split("\n");

                                int inc = -1;

                                if (cekMinus) {
                                    inc = tempMinus;
                                } else {
                                    inc = qty;
                                }

                                for (int i = listTemp.length - 1; i >= 0; i--) {
                                    if (listTemp[i].equals(temp)) {
                                        if (inc == 1) {
                                            temp = "";
                                        }
                                        inc--;
                                    } else {
                                        listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                    }
                                }

                                try {

                                    int inc2 = -1;

                                    if (cekMinus) {
                                        inc2 = tempMinus;
                                    } else {
                                        inc2 = qty;
                                    }

                                    delHarga = Integer.parseInt(sp.getString("Harga_Temp", "")) - (delHarga * inc2);

                                } catch (NumberFormatException e) {
                                   // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                      //      Toast.LENGTH_LONG).show();
                                }

                            } else {
                                //hashMapDelete.put(listBarangDel, 1);

                                listBarangDel = "";
                                listBarangDelChange = "";

                                for (Map.Entry<String, Integer> s : hashMapDelete.entrySet()) {
                                    listBarangDel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangDel;
                                }

                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = listTemp.length - 1; i >= 0; i--) {
                                    listBarangDelChange = listTemp[i] + "\n" + listBarangDelChange;
                                }

                                try {

                                    delHarga = Integer.parseInt(sp.getString("Harga_Temp", ""));

                                } catch (NumberFormatException e) {
                                  //  Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                      //      Toast.LENGTH_LONG).show();
                                }
                            }

                            // ############ Button Delete Akhir ############

                            try {

                                addHarga = (addHarga * qty) + Integer.parseInt(sp.getString("Harga_Temp", ""));

                            } catch (NumberFormatException e) {
                               // Toast.makeText(getApplication(), "Data yang dimasukkan bukan Angka",
                                   //     Toast.LENGTH_LONG).show();
                            }

                            //hargaTxt.setText("Rp. " + sp.getString("Harga_Temp", "") + ",-");
                            //listTxt.setText(listBarangAdd);

                            //barangTxt.setText(Integer.toString(hargaTemp));

                        } catch (NumberFormatException e) {

                            HashMap<String, Integer> hashMapCancel = new HashMap<String, Integer>();

                            // ############ Button Cancel Awal ############
                            if (!sp.getString("List", "").isEmpty()) {

                                String[] listTemp = sp.getString("List", "").split("\n");

                                for (int i = 0; i < listTemp.length; i++) {
                                    if (hashMapCancel.containsKey(listTemp[i])) {
                                        hashMapCancel.put(listTemp[i], hashMapCancel.get(listTemp[i]) + 1);
                                    } else {
                                        hashMapCancel.put(listTemp[i], 1);
                                    }
                                }
                            }

                            listBarangAddCancel = "";

                            for (Map.Entry<String, Integer> s : hashMapCancel.entrySet()) {
                                listBarangAddCancel = s.getKey() + "\tx" + s.getValue() + "\n" + listBarangAddCancel;
                            }
                            // ############ Button Cancel Akhir ############

                        }


                        if (addHarga > 0) {

                            ed.putString("List", listBarangAddChange);
                            ed.putString("Harga_Temp", Integer.toString(addHarga));
                            ed.commit();

                            hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");

                            String[] listTemp1 = listBarangAdd.split("\n");

                            dataBarang.clear();
                            dataHarga.clear();
                            dataStok.clear();
                            dataTotal.clear();

                            for (int i = 0; i < listTemp1.length; i++) {

                                String[] listTemp2 = listTemp1[i].split("\t");

                                for (int j = 0; j < listTemp2.length - 2; j++) {
                                    if (!listTemp2[j + 2].substring(1).equals("0")) {
                                        dataBarang.add(listTemp2[j]);
                                        dataHarga.add(listTemp2[j + 1]);
                                        dataStok.add(listTemp2[j + 2].substring(1));
                                        dataTotal.add(Integer.parseInt(listTemp2[j + 1]) * Integer.parseInt(listTemp2[j + 2].substring(1)));
                                    }
                                }
                            }

                            listDatabase = "";
                            barangField = "";
                            qtyField = "";
                            hargaField = "";
                            totalField = "";

                            for (int i = 0; i < dataBarang.size(); i++) {
                                listDatabase = dataBarang.get(i) + "   " +
                                        dataStok.get(i) + "   " + dataHarga.get(i) + "   " +
                                        dataTotal.get(i) + "\n" + listDatabase;

                                barangField = dataBarang.get(i) + "\n" + barangField;
                                qtyField = dataStok.get(i) + "\n" + qtyField;
                                hargaField = dataHarga.get(i) + "\n" + hargaField;
                                totalField = dataTotal.get(i) + "\n" + totalField;
                            }

                            listTxt.setText(listDatabase);
                            totalHargaField = Integer.toString(addHarga);
                            dataPut = addHarga;
                            listChange = listBarangAddChange;
                        } else {
                            addHarga = 0;

                            ed.putString("List", "");
                            ed.putString("Harga_Temp", Integer.toString(addHarga));
                            ed.commit();

                            hargaTxt.setText("Rp. " + Integer.toString(addHarga) + ",-");
                            listTxt.setText("");

                            barangField = "";
                            qtyField = "";
                            hargaField = "";
                            totalField = "";
                            totalHargaField = Integer.toString(addHarga);

                            dataPut = addHarga;
                            listChange = "";
                        }

                        barcodeScanned = false;
                        previewing = true;
                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        /*} else {

            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();

            alertDialog.setTitle("Internet Connection");
            alertDialog.setMessage("Please check your internet connection");
            alertDialog.setIcon((false) ? R.drawable.success : R.drawable.fail);

            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    barcodeScanned = false;
                    previewing = true;
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                }
            });

            alertDialog.show();
        }*/
    }

    /*@Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }*/

    public boolean isInternetConnected(String mUrl) {
        final int CONNECTION_TIMEOUT = 1500;
        if (isNetworksAvailable()) {
            try {
                HttpURLConnection mURLConnection = (HttpURLConnection) (new URL(mUrl).openConnection());
                mURLConnection.setRequestProperty("User-Agent", "ConnectionTest");
                mURLConnection.setRequestProperty("Connection", "close");
                mURLConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                mURLConnection.setReadTimeout(CONNECTION_TIMEOUT);
                mURLConnection.connect();
                return (mURLConnection.getResponseCode() == 200);
            } catch (IOException ioe) {
                Log.e("isInternetConnected", "Exception occured while checking for Internet connection: ", ioe);
            }
        } else {
            Log.e("isInternetConnected", "Not connected to WiFi/Mobile and no Internet available.");
        }
        return false;
    }

    public boolean isNetworksAvailable() {
        ConnectivityManager mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnMgr != null) {
            NetworkInfo[] mNetInfo = mConnMgr.getAllNetworkInfo();
            if (mNetInfo != null) {
                for (int i = 0; i < mNetInfo.length; i++) {
                    if (mNetInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void showAlertDialog(Context context, String title, String message, Boolean status) {
        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(context).create();

        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alertDialog.show();
    }
}
