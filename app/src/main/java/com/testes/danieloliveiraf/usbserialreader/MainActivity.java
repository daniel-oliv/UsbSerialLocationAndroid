package com.testes.danieloliveiraf.usbserialreader;

import android.Manifest;
import android.content.DialogInterface;
import android.location.Location;


import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.AlertDialog;

import android.os.ParcelFileDescriptor;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.content.BroadcastReceiver;
import android.content.ComponentName;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Calendar;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity 
        implements GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener, LocationListener{

    public static int modeToSend = 1;
    public static boolean setModeToSend(int mode){
        if(isValidMode(mode)){
            modeToSend = mode;
            return true;
        }
        return false;
    }
    public static boolean isValidMode(int mode){
        if(mode == 1 || mode == 0){
            return true;
        }
        return false;
    }
    private int nCounter = 0;
    static int curTime = 0, sentTime = 0, curTimeDelayed = 0, infoTime = 0;
    int TIMER_DELAY = 215;
    public static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM - HH:mm:ss");
    public static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    public String fieldsToShow [];
    public void restartFields(){
        fieldsToShow = new String[Device.devNames.length + 1];
        for (String str: fieldsToShow) {
            str = "";
        }
    }
    public void setField(int i, String str){
        if(fieldsToShow != null && fieldsToShow.length > i){
            fieldsToShow[i] = str;
        }else{
            display.append("Fields null");
        }
    }
    public void setAnotherField(String str){
        if(fieldsToShow != null && fieldsToShow.length > 1){
            fieldsToShow[fieldsToShow.length - 1] = str;
        }else{
            display.append("Fields null");
        }
    }
    public void printFields(){
        if(display != null) {
            display.setText("Campos " + "\n");
            if (fieldsToShow != null) {
                for (String str : fieldsToShow) {
                    if (str != null)
                        display.append(str);
                    else {
                        display.append("Field null " + "\n");
                    }
                }
            } else {
                display.append("Fields null");
            }
        }

    }


    /// location variables
    private Location location;
    private Location preLocation;
    private TextView locationTv;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 500, FASTEST_INTERVAL = 500; // = 500 ms 5 seconds
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;


    /*
     * Notifications from UsbService will be received here.
     */

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    public static String lastRxCount = "";
    public static String textToPrint = "";
    public static int lastSec = 0;


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION)
        {
            int grantResultsLength = grantResults.length;
            if(grantResultsLength > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getApplicationContext(), "You grant write external storage permission. Please click original button again to continue.", Toast.LENGTH_LONG).show();
            }else
            {
                Toast.makeText(getApplicationContext(), "You denied write external storage permission.", Toast.LENGTH_LONG).show();
            }
        }

        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                if (!hasPermission(perm)) {
                    permissionsRejected.add(perm);
                }
                }

                if (permissionsRejected.size() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    new AlertDialog.Builder(MainActivity.this).
                        setMessage("These permissions are mandatory to get your location. You need to allow them.").
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(permissionsRejected.
                                    toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                            }
                            }
                        }).setNegativeButton("Cancel", null).create().show();

                        return;
                    }
                }
                } else {
                if (googleApiClient != null) {
                    googleApiClient.connect();
                }
            }
            break;
        }
    
    }

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            //
        }
    }

    private void createFile(String dataToWrite)
    {
        try {
            if(ExternalStorageUtil.isExternalStorageMounted()) {

                // Check whether this app has write external storage permission or not.
                int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                // If do not grant write external storage permission.
                if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
                {
                    // Request user to grant write external storage permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }else {

                    // Save email_public.txt file to /storage/emulated/0/DCIM folder
                    String publicDcimDirPath = ExternalStorageUtil.getPublicExternalStorageBaseDir(Environment.DIRECTORY_DCIM);

                    File newFile = new File(publicDcimDirPath, "DriveTestLoraFile.txt");

                    FileWriter fw = new FileWriter(newFile);
                    if(!newFile.exists()) newFile.createNewFile();
                    fw.write(dataToWrite);
                    fw.append(dataToWrite);

                    fw.flush();

                    fw.close();

                    Toast.makeText(getApplicationContext(), "Save to public external storage success. File Path " + newFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }

        }catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), "Save to public external storage failed. Error message is " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private Handler timerHandler;
    static int iTimer = 0;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTv = findViewById(R.id.location);
        // we add permissions we need to request location of the users
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (permissionsToRequest.size() > 0) {
            requestPermissions(permissionsToRequest.toArray(
                    new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
        addApi(LocationServices.API).
        addConnectionCallbacks(this).
        addOnConnectionFailedListener(this).build();

        mHandler = new MyHandler(this);
        timerHandler = new Handler();

        display = (TextView) findViewById(R.id.textView1);
        display.setMovementMethod(new ScrollingMovementMethod());

        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                        //createFile(data + "\r\n");
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        String cmdStr = Device.scheduleCommand(data);
                        if(cmdStr.length() > 0){
                            //usbService.write(cmdStr.getBytes()); mandar comando na janela de tempo do dispositivo
                            display.append("Comando agendado: " + data);
                            display.append("\n");
                        } else{
                            display.append("Comando não reconhecido!!!" + "\n");
                        }
                    }
                }
            }
        });

        Device.initList();
        restartFields();
        timerHandler.post(new MyRunnable(timerHandler, iTimer, display));
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

        if (!checkPlayServices()) {
            locationTv.setText("You need to install Google Play Services to use the App properly");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);

        // stop location updates
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public static final String LORA_MSG_SENT_MARKER = "_LS_";
        public static String buffer = "";
        public static String anotherData = "";
        public static String[] devNames = {"16", "19"};
        public static int numDevices = 2;
        public static int currentDev = 0;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        private String parseData(String data, String key){
            int iKey = data.indexOf(key);
            return data.substring(iKey, data.indexOf("\r\n", iKey));
        }

        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        public File getPrivateStorageDir(Context context, String albumName) {
            // Get the directory for the app's private pictures directory.
            File file = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), albumName);
            if (!file.mkdirs()) {

            }
            return file;
        }

        private static final int WRITE_REQUEST_CODE = 43;
        private void createFile(String mimeType, String fileName) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Create a file with the requested MIME type.
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
        }

        private void alterDocument(Uri uri) {
            try {
                ParcelFileDescriptor pfd = mActivity.get().getContentResolver().
                        openFileDescriptor(uri, "w");
                FileOutputStream fileOutputStream =
                        new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.write(("Overwritten by MyCloud at " +
                        System.currentTimeMillis() + "\n").getBytes());
                // Let the document provider know you're done by closing the stream.
                fileOutputStream.close();
                pfd.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    Date currentTime = Calendar.getInstance().getTime();
                    String timeStr = MainActivity.dateTimeFormat.format(currentTime)+ " - ";
                    mActivity.get().display.append(timeStr + data);
                    {
                        buffer += data;
                        int chStartI = buffer.indexOf("!");
                        int chEndI = buffer.indexOf(";");
                        if( (buffer.contains("!") && buffer.contains(";"))  &&
                            chStartI < chEndI ) {
                            String currentBuffer = buffer.substring(chStartI, chEndI+1);
                            if(chStartI > 0){
                                buffer = buffer.substring(0,chStartI) + buffer.substring(chEndI + 1);
                            }
                            else {
                                buffer = buffer.substring(chEndI + 1);
                            }
                            int chNameI =  currentBuffer.indexOf(">");
                            String devName = null;
                            Device dev = null;
                            // formato mensagem !XX>1121212;
                            if(chNameI >= 3){
                                devName = currentBuffer.substring(chNameI-2, chNameI);
                            }
                            if(devName != null){
                                dev = Device.list.get(devName);
                            }
                            if(dev!=null && chNameI+1 < currentBuffer.length()){
                                dev.treatMessage(currentTime, currentBuffer.substring(chNameI+1));
                            }
                            else{
                                anotherData = " - AN : [" + timeStr + currentBuffer + "]";
                            }

                            textToPrint = Device.getReceivedMsgs();
                            textToPrint += anotherData;
                            mActivity.get().display.setText(textToPrint);
                        }
                        if(buffer.length() > 32){
                            anotherData = " - AN : [" + timeStr + buffer + "] > 32";
                            buffer = "";
                            textToPrint = Device.getReceivedMsgs();
                            textToPrint += anotherData;
                            mActivity.get().display.setText(textToPrint);
                        }
                    }
                    //mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

  private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
    ArrayList<String> result = new ArrayList<>();

    for (String perm : wantedPermissions) {
      if (!hasPermission(perm)) {
        result.add(perm);
      }
    }

    return result;
  }

  private boolean hasPermission(String permission) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    return true;
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (googleApiClient != null) {
      googleApiClient.connect();
    }
  }

  private boolean checkPlayServices() {
    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
    int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

    if (resultCode != ConnectionResult.SUCCESS) {
      if (apiAvailability.isUserResolvableError(resultCode)) {
        apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
      } else {
        finish();
      }

      return false;
    }

    return true;
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    if (ActivityCompat.checkSelfPermission(this, 
	                 Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        &&  ActivityCompat.checkSelfPermission(this, 
		             Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        display.append("onConnected - USB não reconhecido. Reinicie o aplicativo.\r\n");
      return;
    }

    // Permissions ok, we get last location
    location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

    if (location != null) {
      locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
    }

    startLocationUpdates();
  }

  private void startLocationUpdates() {
    locationRequest = new LocationRequest();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(UPDATE_INTERVAL);
    locationRequest.setFastestInterval(FASTEST_INTERVAL);

    if (ActivityCompat.checkSelfPermission(this, 
	          Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
          &&  ActivityCompat.checkSelfPermission(this, 
		      Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
    }

    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
  }

  @Override
  public void onConnectionSuspended(int i) {
      display.append("onConnectionSuspended - USB não reconhecido. Reinicie o aplicativo.\r\n");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
      display.append("onConnectionFailed - USB não reconhecido. Reinicie o aplicativo.\r\n");
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
        //if(this.location == null || location.distanceTo(this.location) > 12){
        this.location = location;
        Device.location = location;
        locationTv.setText("Lat. : " + location.getLatitude() + ", Long.: " + location.getLongitude());
    }
  }

    class MyRunnable implements Runnable {
        private Handler handler;
        private int i;
        private TextView textView;
        private ArrayList<Device> readyToSend;
        private String timeStr;
        private Date realTime;

        public MyRunnable(Handler handler, int i, TextView textView) {
            this.handler = handler;
            this.textView = textView;
        }
        @Override
        public void run() {
            realTime = Calendar.getInstance().getTime();
            timeStr = "               Hora: " + MainActivity.timeFormat.format(realTime)+ "        " + "\n";
            //this.handler.postDelayed(this, TIMER_DELAY);
            iTimer++;
//            display.append("Thread curTime: " + curTime + "\n");
            //this.textView.setText(String.format("%d", i));
            if(display.getText().length() > 3000){
                display.setText("");
            }
            curTimeDelayed = curTime;
            curTime = iTimer * TIMER_DELAY;
            if(curTime - infoTime >= 5000){
                infoTime = curTime;
                String anotherData;
                anotherData = ("Modo de envio: " + modeToSend + "\n" );
                anotherData += ("Offset time: " + Device.timeToSumInPacketTime + "\n" );
                anotherData += ("Tempo de espera: " + Device.printArray(Device.getWaitTimeArray()) + "\n" );
                setAnotherField(anotherData);
            }
            if(modeToSend == 0){
                runMode0();
            } else if(modeToSend == 1){
                runMode1();
            }


            if(curTime > 1000000000){
                setAnotherField("Zerei!!!!!!!: " + "\n" );
                curTime = 0;
                sentTime = 0;
                iTimer = 0;
                for (int m = 0; m < Device.devNames.length; m++) {
                    Device dev = Device.list.get(Device.devNames[m]);
                    dev.sentTime = 0;
                }

            }
            printFields();
            this.handler.postDelayed(this, TIMER_DELAY);
        }


        ////  mandar uma vez a cada chamado da thread (run()), observando apenas se
        public void runMode1(){
            int currentIndex = -1;
            Device devToSent = null;
            for (int m = 0; m < Device.devNames.length; m++){
                Device dev = Device.list.get(Device.devNames[m]);
                int waitTime = dev.getWaitTime();
                if(curTime - dev.sentTime >= waitTime) { /// se o device está disponível para enviar, já é candidato
                    if (devToSent != null) {
                        if (devToSent.sentTime > dev.sentTime) { /// se o dispositivo selecionado para enviar tiver enviado depois, pegar o outro
                            devToSent = dev;
                            currentIndex = m;
                        }
                    }else{ /// se não foi selecionado nenhum, pegar esse por enquanto
                        devToSent = dev;
                        currentIndex = m;
                    }
                }

            }

            if(devToSent != null){
                /// treating SF changed by the user
                if(devToSent.treatCommand().length() == 0){
                    setAnotherField("Problema ao executar comando" + "\n" );
                }
                if (usbService != null) {
                    String strCmd = devToSent.strSfAndLatLong();
                    if(strCmd.isEmpty()){
                        setAnotherField("Ative a localização. \n" );
                    }else{
                        setField(currentIndex, timeStr + strCmd + "\n" );
                        usbService.write(strCmd.getBytes());
                        sentTime = curTime;
                        devToSent.sentTime = sentTime;
                    }
                }
            }
        }

        public void runMode0(){
            if(Device.current == null)
            {
                setAnotherField("null device");
                Device.next();
            }

            int waitTime = Device.MAX_PACKET_TIME + 200; // 4200 ms
            if(Device.current != null){
                waitTime = Device.current.getWaitTime();
            }

            if(Device.current != null && curTime - sentTime >= waitTime) {
                Device.next();
                /// treating SF changed by the user
                if(Device.current.treatCommand().length() == 0){
                    setAnotherField("Problema ao executar comando" + "\n" );
                }
                if (usbService != null && Device.current != null) {
                    String strCmd = Device.current.strSfAndLatLong();
                    if(strCmd.isEmpty()){
                        setAnotherField("Ative a localização. \n" );
                    }else{
                        setField(Device.currentIndex, timeStr + strCmd + "\n");
                        usbService.write(strCmd.getBytes());
                        sentTime = curTime;
                    }
                }
            }
        }
    }

}