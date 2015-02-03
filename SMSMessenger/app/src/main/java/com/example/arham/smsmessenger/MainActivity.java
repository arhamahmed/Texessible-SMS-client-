package com.example.arham.smsmessenger;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    Button sendBtn;
    EditText txtphoneNo;
    EditText txtMessage;
    RequestQueue queue;
    private  Toast toast;
    boolean volumeMode = false;
    public Hub hub;

    EditText etName;
    EditText etEmail;
    EditText etUser;
    EditText etPass;

    String username ="";
    String pass = "";
    String userMail = "";
    String name = "";

    int currArticle = -1;

    String[] sendspace = new String[]{"","","",""};

    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            showToast("Connected");
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            showToast("Disconnected");
        }

        // onPose() is called whenever the Myo detects that the person wearing it has changed their pose, for example,
        // making a fist, or not making a fist anymore.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    volumeMode = false;
                    showToast("Unknown");
                    break;
                case REST:
                case DOUBLE_TAP:
                    volumeMode = false;
                    showToast("Double Tap");
                    break;
                case FIST:
                    //record
                    volumeMode = false;
                    showToast("Fist");
                    break;
                case WAVE_IN:
                    volumeMode = false;
                    if(currArticle != 0) {
                        currArticle--;
                        try {
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage("+18737000071", null, "news " + currArticle, null, null);
                            Toast.makeText(getApplicationContext(), "SMS sent. in",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "SMS faild, please try again.",
                                    Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                    break;
                case WAVE_OUT:
                    volumeMode = false;
                    if(currArticle != 10) {
                        currArticle++;
                        try {
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage("+18737000071", null, "news " + currArticle, null, null);
                            Toast.makeText(getApplicationContext(), "SMS sent. out",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "SMS faild, please try again.",
                                    Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                    break;
                case FINGERS_SPREAD:
                    //SEND THIS SHIT
                    volumeMode = false;
                    showToast("Spread Fingers");
                    sendSMSMessage();
                    break;
            }
            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);
                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendBtn = (Button) findViewById(R.id.btnSendSMS);
        txtphoneNo = (EditText) findViewById(R.id.editTextPhoneNo);
        txtMessage = (EditText) findViewById(R.id.editTextSMS);

        etName = (EditText) findViewById(R.id.name);
        etEmail = (EditText) findViewById(R.id.email);
        etUser = (EditText) findViewById(R.id.user);
        etPass = (EditText) findViewById(R.id.pass);

        //LOAD INTENT DATA
        try {
            openFile("data");
        }
        catch(Exception e) {
            //file not found? RIP
        }

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendSMSMessage();
            }
        });

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                        latitude + "," + longitude + "&key=" + "AIzaSyD_65i2dXl-bgq4Ei9ARkxTwg793I7_DV8";

                decodeCoordinates(url);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e("Hub Error: ", "Could not initialize the Hub.");
            finish();
            return;
        }
        Hub.getInstance().attachToAdjacentMyo();
    }

    @Override
    protected void onStop(){
        try {
            if(!etName.getText().toString().isEmpty())
                sendspace[0] = etName.getText().toString();
            if(!etEmail.getText().toString().isEmpty())
                sendspace[1] = etEmail.getText().toString();
            if(!etUser.getText().toString().isEmpty())
                sendspace[2] = etUser.getText().toString();
            if(!etPass.getText().toString().isEmpty())
                sendspace[3] = etPass.getText().toString();

            FileOutputStream output = getApplicationContext().openFileOutput("data", Context.MODE_PRIVATE);
            writeJsonStream(output, sendspace);
            output.close();
        }
        catch(Exception e){
        }

        super.onStop();
    }

    public void openFile(String fileName) throws java.io.IOException{
        //InputStream input = getAssets().open(fileName);
        InputStream input = openFileInput(fileName);
        readJsonStream(input);
        input.close();
    }

    public void readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();
        Gson gs = new Gson();
        int i = 0;
        while (reader.hasNext()) {
            String recip = gs.fromJson(reader, String.class);
            sendspace[i] = recip;
            if(i == 0 && recip != "")
                etName.setText(recip);
            else if(i ==1 && recip != "")
                etEmail.setText(recip);
            else if(i ==2 && recip != "")
                etUser.setText(recip);
            else if(i ==3 && recip != "")
                etPass.setText(recip);
            i++;
        }
        reader.endArray();
        reader.close();
        return;
    }

    public void writeJsonStream(OutputStream out, String[] sndspc) throws IOException {
        Gson gs = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();
        for (String message : sndspc) {
            gs.toJson(message, String.class, writer);
        }
        writer.endArray();
        writer.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Hub.getInstance().removeListener(mListener);
        Hub.getInstance().shutdown();
    }

    public void decodeCoordinates(String url)
    {
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);

        // Start the queue
        queue.start();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] x = response.split(",");

                        String address = x[37]+x[38]+x[39]+x[40]+x[41];
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        txtMessage.setText("An error happened.");
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void showToast(String text) {
        if (toast == null) {
            toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
        toast.show();
    }

    protected void sendSMSMessage() {
        Log.i("Send SMS", "");

        String phoneNo = txtphoneNo.getText().toString();

        String message = txtMessage.getText().toString();

        List<String> l;
        l = Arrays.asList(message.split("\\s+"));

        username = etUser.getText().toString();
        pass = etPass.getText().toString();
        name = etName.getText().toString();
        userMail = etEmail.getText().toString();

        if(l.get(0).toLowerCase().equals("news") && Integer.parseInt(l.get(1)) <= 10)
            currArticle = Integer.parseInt(l.get(1));


        int count = 0;
        if(l.get(0).toLowerCase().equals("email")) {
            String str = "";
            if (!username.equals("")) {
                str = str + username + " ";
                count++;
            }
            if (!pass.equals("")) {
                str = str + pass + " ";
                count++;
            }
            if (!name.equals("")) {
                str = str + name + " ";
                count++;
            }
            if (!userMail.equals("")) {
                str = str + userMail;
                count++;
            }

            if(count==4)
                message = " email " + str + message;
            else
                message = "";
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "SMS faild, please try again.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem myoConnect = menu.findItem(R.id.myo_connect);
        Intent intent = new Intent(this, ScanActivity.class);
        myoConnect.setIntent(intent);
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
