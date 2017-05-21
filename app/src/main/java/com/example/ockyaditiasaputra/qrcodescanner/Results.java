package com.example.ockyaditiasaputra.qrcodescanner;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.sourceforge.zbar.ImageScanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Results extends AppCompatActivity {

    InputStream is = null;
    String result = null;
    String line = null;
    int code;

    String barangField, qtyField, hargaField, totalField, totalHargaField, listChange = "";

    String sharedName, sharedPassword;

    String listDatabase;

    int dataPut;

    protected PowerManager.WakeLock mWakeLock;

    EditText nominal;
    TextView kembalianView, user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        /*final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();*/

        user = (TextView) findViewById(R.id.loginUser);

        Intent i = getIntent();
        sharedName = i.getStringExtra("sharedName");
        barangField = i.getStringExtra("barangField");
        qtyField = i.getStringExtra("qtyField");
        hargaField = i.getStringExtra("hargaField");
        totalField = i.getStringExtra("totalField");
        totalHargaField = i.getStringExtra("totalHargaField");
        listDatabase = i.getStringExtra("listDatabase");
        dataPut = i.getIntExtra("dataPut", 0);
        listChange = i.getStringExtra("listChange");

        TextView listTxt = (TextView) findViewById(R.id.listBarang);
        listTxt.setText(listDatabase);

        user.setText("Nama Kasir : " + sharedName);

        TextView hargaTxt = (TextView) findViewById(R.id.harga);
        hargaTxt.setText("Rp. " + Integer.toString(dataPut) + ",-");

        nominal = (EditText) findViewById(R.id.nominal);
        kembalianView = (TextView) findViewById(R.id.kembalian);

        Button ok = (Button) findViewById(R.id.okButton);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int nominal_harga = Integer.parseInt(nominal.getText().toString());
                    int kembalian = nominal_harga - dataPut;

                    if (kembalian < 0) {
                        showAlertDialog(v.getContext(), "Failed",
                                "Pembayaran Kurang", false);
                    } else {
                        kembalianView.setText("Rp. " + kembalian + ",-");
                    }

                } catch(NumberFormatException n) {
                    showAlertDialog(v.getContext(), "Failed",
                            "Masukkan Nominal Pembayaran", false);
                }
            }
        });

        Button done = (Button) findViewById(R.id.doneButton);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //if (isInternetConnected(new Connections().con())) {

                String[] barang = barangField.split("\n");
                String[] qty = qtyField.split("\n");

                for (int i = 0; i < barang.length; i++) {
                    update(barang[i], qty[i]);
                }

                insert();
                /*} else {
                    showAlertDialog(v.getContext(), "Internet Connection",
                            "Please check your internet connection", false);
                }*/
            }
        });

        Button ubah = (Button) findViewById(R.id.ubahButton);
        ubah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(v.getContext(), MainActivity.class);
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

    public void update(String barang, String qty) {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();

        nameValuePairs.add(new BasicNameValuePair("barang", barang));
        nameValuePairs.add(new BasicNameValuePair("qty", qty));

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(new Connections().updateBarang());
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.e("Pass 1", "Connection Success");
        } catch (Exception e) {
            Log.e("Fail 1", e.toString());
            showAlertDialog(this, "Internet Connection",
                    "Please check your internet connection", false);
        }

        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
            Log.e("Pass 2", "Connection Success");
        } catch (Exception e) {
            Log.e("Fail 2", e.toString());
        }

        try {
            JSONObject json_data = new JSONObject(result);
            code = (json_data.getInt("code"));

            if (code == 1) {

            } else {

                showAlertDialog(this, "Failed",
                        "Data gagal masuk", false);
            }
        } catch (Exception e) {
            Log.e("Fail 3", e.toString());
        }
    }

    public void insert() {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();

        nameValuePairs.add(new BasicNameValuePair("user", sharedName));
        nameValuePairs.add(new BasicNameValuePair("barang", barangField));
        nameValuePairs.add(new BasicNameValuePair("qty", qtyField));
        nameValuePairs.add(new BasicNameValuePair("harga", hargaField));
        nameValuePairs.add(new BasicNameValuePair("jumlah", totalField));
        nameValuePairs.add(new BasicNameValuePair("total", totalHargaField));
        nameValuePairs.add(new BasicNameValuePair("nominal", nominal.getText().toString()));

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(new Connections().insertTransaksi());
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.e("Pass 1", "Connection Success");
        } catch (Exception e) {
            Log.e("Fail 1", e.toString());
            showAlertDialog(this, "Internet Connection",
                    "Please check your internet connection", false);
        }

        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
            Log.e("Pass 2", "Connection Success");
        } catch (Exception e) {
            Log.e("Fail 2", e.toString());
        }

        try {
            JSONObject json_data = new JSONObject(result);
            code = (json_data.getInt("code"));

            if (code == 1) {

                //showAlertDialog(this, "Success",
                  //      "Data berhasil masuk", true);



                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivityForResult(intent, 0);

            } else {

                showAlertDialog(this, "Failed",
                        "Data gagal masuk", false);
            }
        } catch (Exception e) {
            Log.e("Fail 3", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
