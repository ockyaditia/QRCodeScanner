package com.example.ockyaditiasaputra.qrcodescanner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoginFragment extends Fragment implements OnClickListener {
    EditText user, pass;
    Button bLogin;
    ProgressDialog pDialog;

    TextView error;

    JSONParser jsonParser = new JSONParser();
    private static final String LOGIN_URL = new Connections().getUser();
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    UserSession session;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /*final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();*/

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        session = new UserSession(view.getContext());

        error = (TextView) view.findViewById(R.id.error);

        user = (EditText) view.findViewById(R.id.username);
        pass = (EditText) view.findViewById(R.id.password);
        bLogin = (Button) view.findViewById(R.id.loginButton);
        bLogin.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.loginButton:
                new AttemptLogin().execute();

            default:
                break;
        }
    }

    class AttemptLogin extends AsyncTask<String, String, String> {
        boolean failure = false;

        int success;
        String username;
        String password;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(getContext());
            pDialog.setMessage("Attempting for login...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // TODO Auto-generated method stub
            username = user.getText().toString();
            password = pass.getText().toString();

            //if (isInternetConnected(new Connections().con())) {
                try {
                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("username", username));
                    params.add(new BasicNameValuePair("password", password));

                    JSONObject json = jsonParser.makeHttpRequest(
                            LOGIN_URL, "POST", params);

                    success = json.getInt(TAG_SUCCESS);
                    if (success == 3) {
                        session.createUserLoginSession(username, password);

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivityForResult(intent, 0);
                        getActivity().finish();

                    } else {
                        return json.getString(TAG_MESSAGE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            /*} else {
                pDialog.dismiss();

                success = -1;
            }*/

            return null;
        }

        /**
         * Once the background process is done we need to  Dismiss the progress dialog asap
         **/
        protected void onPostExecute(String message) {
            pDialog.dismiss();

            if (message != null) {
                error.setText(message);
            }

            if (success == -1) {

                showAlertDialog(getContext(), "Internet Connection",
                        "Please check your internet connection", false);

            } else if (success == 0) {
                user.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg_error));
                pass.setBackground(getResources().getDrawable(R.drawable.edittext_bottom_bg));
            } else if (success == 1) {
                user.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg));
                pass.setBackground(getResources().getDrawable(R.drawable.edittext_bottom_bg_error));
            } else if (success == 2) {
                user.setBackground(getResources().getDrawable(R.drawable.edittext_top_bg_error));
                pass.setBackground(getResources().getDrawable(R.drawable.edittext_bottom_bg_error));
            }
        }
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
