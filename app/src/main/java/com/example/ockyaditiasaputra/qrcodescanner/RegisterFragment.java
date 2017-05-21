package com.example.ockyaditiasaputra.qrcodescanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.net.URL;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegisterFragment extends Fragment {

    InputStream is = null;
    String result = null;
    String line = null;
    int code;

    String username, password, email, fullname, contact = "";
    EditText usernameTxt, passwordTxt, emailTxt, fullnameTxt, contactTxt;

    TextView error;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /*final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();*/

        View view = inflater.inflate(R.layout.fragment_register, container, false);

        usernameTxt = (EditText) view.findViewById(R.id.username);
        passwordTxt = (EditText) view.findViewById(R.id.password);
        emailTxt = (EditText) view.findViewById(R.id.email);
        fullnameTxt = (EditText) view.findViewById(R.id.fullname);
        contactTxt = (EditText) view.findViewById(R.id.contact);

        error = (TextView) view.findViewById(R.id.error);

        Button register = (Button) view.findViewById(R.id.registerButton);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullname = fullnameTxt.getText().toString();
                username = usernameTxt.getText().toString();
                email = emailTxt.getText().toString();
                password = passwordTxt.getText().toString();
                contact = contactTxt.getText().toString();

                if (fullname.length() <= 0) {
                    error.setText("Insert Your Fullname.");
                    fullnameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg_error));
                    usernameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                    emailTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                    passwordTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                } else if (username.length() <= 0) {
                    error.setText("Insert Your Username.");
                    usernameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg_error));
                    fullnameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg));
                    emailTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                    passwordTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                } else if (email.length() <= 0) {
                    error.setText("Insert Your Email.");
                    emailTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg_error));
                    fullnameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg));
                    usernameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                    passwordTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                } else if (password.length() <= 0) {
                    error.setText("Insert Your Password.");
                    passwordTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg_error));
                    fullnameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg));
                    usernameTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                    emailTxt.setBackground(getResources().getDrawable(R.drawable.edittext_default_bg));
                } else {
                    //if (isInternetConnected(new Connections().con())) {
                        insert();
                    /*} else {
                        showAlertDialog(v.getContext(), "Internet Connection",
                                "Please check your internet connection", false);
                    }*/
                }
            }
        });

        return view;
    }

    public boolean isInternetConnected(String mUrl) {
        final int CONNECTION_TIMEOUT = 5000;
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
        ConnectivityManager mConnMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnMgr != null)  {
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

    public void insert() {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();

        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("email", email));
        nameValuePairs.add(new BasicNameValuePair("contact", contact));
        nameValuePairs.add(new BasicNameValuePair("fullname", fullname));

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(new Connections().insertUser());
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            Log.e("Pass 1", "Connection Success");
        } catch (Exception e) {
            Log.e("Fail 1", e.toString());
            Toast.makeText(getContext(), "Connection Failed",
                    Toast.LENGTH_LONG).show();
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
                Toast.makeText(getContext(), "Inserted Successful",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Inserted Failed",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Fail 3", e.toString());
        }
    }

    public void showAlertDialog(Context context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

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
