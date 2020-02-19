package com.testes.danieloliveiraf.usbserialreader;

import android.location.Location;

import java.util.Date;
import java.util.LinkedHashMap;

public class Device {

    final String ID_SEPARATOR = ">";
    final String END_MSG_CHAR = ";";
    final String END_CMD_CHAR = "|";
    final String UPDATE_STATE_STR = "U";
    final String SF_STR = "SF:";
    final String LAT_STR = "Lat:";
    final String LONG_STR = "Long:";

    final int MAX_ATTEMPTS_UPDATE = 1;
    final String LORA_NO_JOIN = "_NJ_";
    final String STR_READY_TO_SEND = "_RS_";
    final String STR_SENDING = "_SD_";
    final String STR_SENT = "_ST_";

//    public static String[] devNames = {"3", "4", "12", "16", "17", "19"};
//    public static int initialDevSFs[] = {7, 8, 9, 11, 12, 10};

    public static String[] devNames = {"3", "4", "12", "16", "17"};
    public static int initialDevSFs[] = {7, 8, 9, 11, 12};

//    public static String[] devNames = {"4"};
//    public static int initialDevSFs[] = {8};

    /// ESTÁ EM OUTRA ORDEM
    /// para 70 bytes - ou seja, string
    /// SFs - 7 a 12
    public static int allSFS[] = {7, 8, 9, 10, 11, 12};
    public static int[] packetTime_ms = {130, 230, 420, 790, 1400, 2640};
//    public static int[] packetTime_ms = {200, 3000, 200, 200, 100, 2000};
    public static int MAX_PACKET_TIME = 4000;

    public static int numDevices = devNames.length;
    public static int currentIndex = 0;
    public static Device current;
    public static Location location = null;

    public DevState state;
    public String receive;
    public Date timeRec;
    public int attempts;
    public String name;
    int SF;
    public String pendentCmds = "";
    public void setSF(int SF){
        for (int ind = 0; ind < allSFS.length; ind++) {
            if(allSFS[ind] == SF){
                this.SF = SF;
                break;
            }
        }
    }

    enum DevState {
        UNTOUCHED,
        UNREACHED,
        NO_JOIN,
        READY_TO_SEND,
        SENDING,
        SENT
    }

    Device(String name, int SF){
        state = DevState.UNTOUCHED;
        receive = "";
        attempts = 0;
        this.name = name;
        this.SF = SF;
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

//    public String strSetSF(int SF){
//        String ret="";
//        for (int i = 0; i < allSFS.length; i++) {
//            if(SF == allSFS[i] )
//                return (this.ID_STR() + SF_STR + SF + END_CMD_CHAR + END_MSG_CHAR);
//        }
//        return ret;
//    }

    //   >19>Lat:-18.87878,Long:-42.787898798;
    public byte[] cmdSendLatLong(){
        return (  this.strLatLong() ).getBytes();
    }

    public String strSfAndLatLong(){
        if( location != null ){
            return (this.ID_STR() + SF_STR + SF + END_CMD_CHAR + LAT_STR + location.getLatitude() + "," + LONG_STR + location.getLongitude() + END_MSG_CHAR);
        }
        else{
            return "";
        }
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

    public int getPackTime(){
        for (int ind = 0; ind < packetTime_ms.length; ind++) {
            if(allSFS[ind] == this.SF){
                return  packetTime_ms[ind];
            }
        }
        return MAX_PACKET_TIME;
    }

    public int getWaitTime(){
       return this.getPackTime() + 200;
    }

    public static String scheduleCommand(String str){
        String ret = "";

        try {
            /// Set SF -> Ex.: SF,4,12 -> SF, DISP, SF_NUM
            if(str.contains("SF")){
                String params [] = str.split(",");
                if(params.length == 3){
                    int SF = Integer.parseInt(params[2]);
                    Device dev = list.get(params[1]);
                    dev.pendentCmds = str;
                    ret = "OK";
                }
            }

        }catch (Exception e){
            //TODO
        }
        return  ret;
    }

    public String treatCommand(){
        String ret = "";

        try {
            if (pendentCmds.length() == 0){
                return "NADA";
            }
            /// Set SF -> Ex.: SF,4,12 -> SF, DISP, SF_NUM
            if(pendentCmds.contains("SF")){
                String params [] = pendentCmds.split(",");
                if(params.length == 3){
                    int SF = Integer.parseInt(params[2]);
                    this.pendentCmds = "";
                    this.setSF(SF);
                    ret = "OK";
                }
            }

        }catch (Exception e){
            //TODO
        }
        return  ret;
    }

    public static LinkedHashMap<String, Device> list;
    public static void initList(){
        list = new LinkedHashMap<>();
        for (int i = 0; i < numDevices; i++) {
            list.put(devNames[i], new Device(devNames[i], initialDevSFs[i]));
        }
        current = list.get(devNames[0]);
    }

}
