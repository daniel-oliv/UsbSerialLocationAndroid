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

//    public static String[] devNames = {"2", "4", "12", "16", "17", "19"};
//    public static int initialDevSFs[] = {7, 8, 9, 11, 12, 10};

    public static String[] devNames = {"2", "4", "12", "16", "17"};
//    public static String[] devNames = {"19", "13", "6", "5", "9"}; //public static String[] devNames = {"14", "13", "6", "5", "9"};

    public static int initialDevSFs[] = {7, 8, 9, 11, 12};

//    public static String[] devNames = {"4"};
//    public static int initialDevSFs[] = {8};

    /// ESTÁ EM OUTRA ORDEM
    /// para 70 bytes - ou seja, string
    /// SFs - 7 a 12
    /// Modo 1 4000 de offset e 1.2 de fator - (colocar 100 ms a mais para cada)
    // SF7: 4256 - disp 4 - 4100 de offset
    // SF8: 4276 - disp 4 - 4000 de offset
    // SF9: 5504 - disp 12 5000 de offset
    // SF10:
    // SF11: 5480 - disp 16 3800 de offset
    // SF12: 7268 - 4100 de off 3100 tb funcionou

    public static int allSFS[] = {7, 8, 9, 10, 11, 12};
//    public static int[] packetTime_ms = {130, 230, 420, 790, 1400, 2640};
    public static int[] packetTime_ms = {4356, 4376, 4476, 5504, 5480, 7268};
//    public static int[] packetTime_ms = {4300, 4300, 4400, 5500, 5400, 7200};
    public static int timeToSumInPacketTime = 0;
    public static double packetTimeFactor = 1.0;

///////// configurações que usei para tentar utilizar modo zero de maneira simplificada.
//    public static int[] packetTime_ms = {1000, 1000, 1000, 1000, 1000, 1000};
//    public static int timeToSumInPacketTime = 0;
//    public static double packetTimeFactor = 1.0;

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
    public static String pendentStCmds = "";
    public String pendentCmds = "";
    int sentTime;
    int SF;
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
        sentTime = 0;
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

    //   >19>Lat:-18.8787814,Long:-42.787898798;
    //   >4>SF:8|Lat:-18.9168416,Long:-48.2607992;
    public byte[] cmdSendLatLong(){
        return (  this.strLatLong() ).getBytes();
    }
    public String padStr(String inputString, int finalLength){
        return String.format("%1$" + finalLength + "s", inputString);
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
                //return  (4000-790+packetTime_ms[ind])/4;
            }
        }
        return MAX_PACKET_TIME;
    }

    public static int[] getWaitTimeArray(){
        int waitArr [] = new int[devNames.length];
        for (int m=0; m < devNames.length; m++){
            waitArr[m] = list.get(devNames[m]).getWaitTime();
        }
        return waitArr;
    }
    public static String printArray(int [] array){
        String ret ="[";
        for (int m=0; m < array.length; m++){
            ret += array[m] + ", ";
        }
        ret += "]";

        return ret;
    }
    public int getWaitTime(){
       return ((int) (this.getPackTime() * packetTimeFactor) )+ timeToSumInPacketTime;
    }

    public static String scheduleCommand(String str){
        String ret = "";

        try {
            /// Set mode to send -> Ex.: MO,0 -> 0: espera o current terminar de mandar (tempo teórico, guardado em packetTime_ms) ;
            // 1: envia assim que o packTime for passar, dadndo priorodade para os dispositivos que estão a mais tempo sem mandar
            if(str.contains("MO")){
                String params [] = str.split(",");
                if(params.length >= 2){
                    int newMode = Integer.parseInt(params[1]);
                    if(MainActivity.isValidMode(newMode)) {
                        if(params.length == 3){
                            int offset = Integer.parseInt(params[2]);
                        }
                        Device.pendentStCmds = str;
                        ret = "OK";
                    }
                }
            }

            /// Set SF -> Ex.: SF,4,12 -> SF, DISP, SF_NUM
            else if(str.contains("SF")){
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

            if(Device.pendentStCmds.contains("MO")){
                String params [] = Device.pendentStCmds.split(",");
                if(params.length >= 2){
                    int newMode = Integer.parseInt(params[1]);
                    if(MainActivity.setModeToSend(newMode)) {
                        Device.pendentStCmds = "";
                        if(params.length == 3){
                            timeToSumInPacketTime = Integer.parseInt(params[2]);
                        }
                        ret = "OK";
                    }
                }
            }

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
