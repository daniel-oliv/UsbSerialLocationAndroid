package com.testes.danieloliveiraf.usbserialreader;

import android.location.Location;

import java.util.Date;
import java.util.LinkedHashMap;

public class Device {

    final String ID_SEPARATOR = ">";
    final String END_MSG_CHAR = ";";
    final String UPDATE_STATE_STR = "U";
    final String LAT_STR = "Lat:";
    final String LONG_STR = "Long:";
    final int MAX_ATTEMPTS_UPDATE = 1;
    final String LORA_NO_JOIN = "_NJ_";
    final String STR_READY_TO_SEND = "_RS_";
    final String STR_SENDING = "_SD_";
    final String STR_SENT = "_ST_";
.
    public static String[] devNames = {"19","16", "4", "3", "17", "12"};
    public static int numDevices = 2;
    public static int currentIndex = 0;
    public static Device current;
    public static Location location = null;

    public DevState state;
    public String receive;
    public Date timeRec;
    public int attempts;
    public String name;

    enum DevState {
        UNTOUCHED,
        UNREACHED,
        NO_JOIN,
        READY_TO_SEND,
        SENDING,
        SENT
    }

    Device(String name){
        state = DevState.UNTOUCHED;
        receive = "";
        attempts = 0;
        this.name = name;
    }

    public void treatMessage(Date time, String msg){
        this.attempts = 0;
        this.timeRec = time;
        if(msg.contains(LORA_NO_JOIN)){
            this.state = DevState.NO_JOIN;
        }
        else if(msg.contains(STR_READY_TO_SEND)){
            this.state = DevState.READY_TO_SEND;
        }
        else if(msg.contains(STR_SENDING)){
            this.state = DevState.SENDING;
        }
        else if(msg.contains(STR_SENT)){
            this.state = DevState.SENT;
        }
        next();
        this.receive = MainActivity.dateTimeFormat.format(time) + " - [" + msg+"]  ST{"+ this.state.name() +")";
    }

    //   >19>U;
    public byte[] cmdRequestState(){
        this.attempts++;
        if(attempts >= MAX_ATTEMPTS_UPDATE){
            this.state = DevState.UNREACHED;
        }
        return ( this.ID_STR() + UPDATE_STATE_STR + END_MSG_CHAR).getBytes();
    }

    //   >19>Lat:-18.87878,Long:-42.787898798;
    public byte[] cmdSendLatLong(){
        return (  this.strLatLong() ).getBytes();
    }

    public String strLatLong(){
        if( location != null ){
            return (this.ID_STR() + LAT_STR + location.getLatitude() + "," + LONG_STR + location.getLongitude() + END_MSG_CHAR);
        }
        else{
            return "";
        }
    }

    public String ID_STR(){
        return ID_SEPARATOR + this.name + ID_SEPARATOR;
    }

    public static void next(){
        currentIndex++;
        if(currentIndex == numDevices){
            currentIndex = 0;
        }
        current = list.get(devNames[currentIndex]);
    }

    public static String getReceivedMsgs(){
        String ret="";
        for (int i = 0; i < numDevices; i++) {
            ret +=  " - " + devNames[i] + " : [" + list.get(devNames[i]).receive + "] \r\n";
        }
        return ret;
    }

    public static LinkedHashMap<String, Device> list;
    public static void initList(){
        list = new LinkedHashMap<>();
        for (int i = 0; i < numDevices; i++) {
            list.put(devNames[i], new Device(devNames[i]));
        }
        current = list.get(devNames[0]);
    }

}
