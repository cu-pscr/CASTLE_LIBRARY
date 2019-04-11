package com.example.castle_scheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import static java.lang.Thread.sleep;

public class castle_scheduler {
    private static int decision;
    private static long limit;
    private static int A_star;
    private static int b_t;
    private static long total_wait=0;
    private static SharedPreferences lib_sharedpreference;
    private static HashMap<String, String> list = new HashMap<>();
    private static Context castle;
    private static int rsrp=0,rsrq=0,snr=0;
    public castle_scheduler(Context mContext) {
        this.castle = mContext;//castle_scheduler cs = new castle_scheduler(this); //Here the context is passing  from main function
        this.rsrp=0;
        this.rsrq=0;
        this.snr=0;
    }


    /*init function which will check if external storage device is accessible and make sure that it is readable and create the hash entry
    for the file*/
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        else{
            Log.d("castle_library","external storage is not present in your cell phone");
        }
        return false;
    }

    private static String MD5( String md5){
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    private static void castle_train_data() {
        //Log.d("castle_library","In Train Data function");
        String file_path = Environment.getExternalStorageDirectory() + "/optimised_2.csv";
        //Log.d("Path", "" + file_path);
        if ((!isExternalStorageAvailable()) && (!isExternalStorageReadOnly())) {
            Log.d("castle_library", "Sorry cannot read the file");
            System.exit(0);
        } else {
            File file = new File(file_path);
            if(file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file_path));
                    String line;
                    int count = 1;
                    while ((line = reader.readLine()) != null) {
                        String str[] = line.split(",");
                        String md5 = "";
                        int limit = str.length;
                        for (int i = 0; i < limit - 1; i++) {
                            //System.out.println("*****"+str[i]+"*****");
                            md5 = md5 + str[i].trim();
                        }
                        //Log.d("String comb","\n"+md5);
                        String hash_key = MD5(md5);
                        //Log.d("Hash value",""+hash_key+"\n"+count);
                        list.put(hash_key, str[limit - 1]);
                        count++;
                    }
                    reader.close();
                    //Log.d("Total entry", "" + count + "Hash map size:" + list.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                Log.d("CASTLE LIBRARY:","Data File doesn't exists and please load it");
                System.exit(0);
            }
        }
    }

    private static void castle_listner() {
        Log.d("castle_library","Listnening started");
        while(true) {
            snr=lib_sharedpreference.getInt("snr_value",0);
            Log.d("castle_library","app SNR:\t"+snr);
            TelephonyManager tm = (TelephonyManager) castle.getSystemService(castle.TELEPHONY_SERVICE);
            try {
                List<CellInfo> cellInfoList = tm.getAllCellInfo();
                if (cellInfoList != null && cellInfoList.size() != 0) {
                    CellInfo cellInfo = cellInfoList.get(0);
                    if (cellInfo instanceof CellInfoLte) {
                        String result = ((CellInfoLte) cellInfo).getCellSignalStrength().toString();
                        String result_log[] = result.split(" ");
                        //System.out.println("*********************");
                        for (String temp : result_log) {
                            if (temp.startsWith("rsrp=")) {
                                String rsrp_str = temp.substring(5);
                                rsrp = Integer.valueOf(rsrp_str);
                                if(rsrp>0){
                                    rsrp*=-1;
                                }
                                Log.d("catle_library","Sensed rsrp\t"+rsrp+"\n");
                            } else if (temp.startsWith("rsrq")) {
                                String rsrq_str = temp.substring(5);
                                rsrq = Integer.valueOf(rsrq_str);
                                if(rsrq>0){
                                    rsrq*=-1;
                                }
                                Log.d("catle_library","Sensed rsrq\t"+rsrq+"\n");
                            }
                        }
                     }
                }else{
                    Log.d("castle_library","Sensing did not happen,make sure your" +
                            "acess and connection of Network is good");
                }
            } catch (Exception e) {
                e.getCause();
            }
            try {
                Log.d("Castle_library","Sleeping one sec");
                sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void init(SharedPreferences sharedPreferences,int A_star_rec,int b_t_rec,long total_limit){
        A_star=A_star_rec;
        limit=total_limit;
        b_t=b_t_rec;
        lib_sharedpreference=sharedPreferences;
        Log.d("castle_library","In Init function");
        isExternalStorageAvailable();
        isExternalStorageReadOnly();
        castle_train_data();
        castle_listner();
    }
    /*end of init function*/


    /*
    Function that predict the load class to which the cell environment belongs to?
     */
    private static int castle_get_best_class(){
        int encoded_value;
        int SINR_class;
        int count;
        //Log.d("castle_library","in predict class\n SNR:\t"+snr+"\nRSRP\t"+rsrp+"\nRSRQ" +"\t"+rsrq+"\n");
        //Log.d("castle_library","in predict class SNR from shared:\t"+lib_sharedpreference.getInt("snr_value",0));
        // decide SINR_class = {0, 1, 2}
        if (snr < 110) {
            SINR_class = 0;
            count = snr / 10;
        } else if (snr < 220) {
            SINR_class = 1;
            count = (snr / 10) - 11;
        } else {
            SINR_class = 2;
            count = (snr / 10) - 22;
        }
        String find1 = Integer.toString(rsrp);
        String find2 = Integer.toString(rsrq);
        String find3 = Integer.toString(SINR_class);

        String find = find1 + find2 + find3;
        String hash1 = MD5(find);
        //System.out.println("get hash code"+hash1);
        if(list.containsKey(hash1)) {
            String load_class = list.get(hash1).trim();
            encoded_value=Integer.valueOf(load_class);
            //Log.d("library","got the key\n"+encoded_value+"count value\t"+count);
            // decode
            while (count > 0)
            {
                encoded_value = encoded_value / 4;
                count--;
            }
            encoded_value = encoded_value % 4;
            //Log.d("library","class by encoding\n"+encoded_value);
            if (encoded_value == 0) {
                return 4;
            }
            else {
                return encoded_value;   // class 1, 2, 3
            }
        }
        else {
            //System.out.println("Sorry class entry is not present in the given hash keys");
            return 0;
        }
    }


    private static int castle_frequent_finder(int arr[], int n)
    {
        Arrays.sort(arr);
        int max_count = 1, res = arr[0];
        int curr_count = 1;
        for (int i = 1; i < n; i++)
        {
            if (arr[i] == arr[i - 1])
                curr_count++;
            else
            {
                if (curr_count > max_count)
                {
                    max_count = curr_count;
                    res = arr[i - 1];
                }
                curr_count = 1;
            }
        }
        if (curr_count > max_count) {
            max_count = curr_count;
            res = arr[n - 1];
        }
        Log.d("castle_library","Best class choosen:\t"+res);
        return res;
    }

    public static int castle_predict_class_long( ){
        int array[]=new int[10];
        int predicted_class=0;
        for(int i=0;i<10;i++){
            array[i]=castle_get_best_class();
            try{
                sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        predicted_class=castle_frequent_finder(array,10);
        return predicted_class;
    }

    public static int castle_predict_class_short( ){
        int predicted_class=0;
        predicted_class=castle_get_best_class();
        return predicted_class;
    }
    /*
    End of predict class function.
     */

    /*
    Function that computes the load percentage of cell environment
     */
    public static int castle_compute_load_percentage(){//computed percentage class,ML class
        if(rsrq>0)  rsrq*=-1;
        int rsrq_abs=Math.abs(rsrq);
        Log.d("castle","In compute load percentage function\nRSRQ\t"+rsrq_abs);
        int load=((rsrq_abs-6)*100)/(13-6);
        int class_load=(load/25)+1;
        int load_percentage=((rsrq_abs-6)*100)/(13-6);
        int cell_load=castle_predict_class_long( );
        if(class_load<cell_load){
            if ((cell_load - class_load) > 1)
                load_percentage+= 20;
            else
                load_percentage+= 10;

            if (load_percentage > 100)
                load_percentage = 100;
        }else{
            if(cell_load - class_load > 1)
                load_percentage-= 20;
            else
                load_percentage-= 10;

            if (load_percentage < 0)
                load_percentage = 0;
        }
        System.out.println("********************Final load percentage in Library*************\n:"+load_percentage);
        return load_percentage;
    }
    /*
    End of cell load percentage compute function.
     */

    /*
    Function that schedules the download of file
     */

    private static int castle_random_wait( ){
        int bound=60;
        Random rand=new Random();
        int rand_num = ThreadLocalRandom.current().nextInt(0, bound );
        Log.d("Main service","Random number is "+rand_num+"\n");
        int random_num=rand_num;
        return rand_num;
    }

    private static boolean castle_decide( ){
        double A_t=0;
        boolean decide_flag=false;
        int decide_factor=castle_predict_class_long( );
        Log.d("decide_new","SNR value:"+snr+"\n"+"Class:"+decide_factor);
        if(snr>0)
            switch (decide_factor){
                case 1://class 1
                    A_t=(b_t*snr*1)/300;
                    break;
                case 2://class 2
                    A_t=(b_t*snr*0.75)/300;
                    break;
                case 3://class 3
                    A_t=(b_t*snr*0.5)/300;
                    break;
                case 4://class 4
                    A_t=(b_t*snr*0.25)/300;
                    break;
            }
        //Log.d("decide_new","A_t:"+A_t+"\tinteger equivelent"+(int)A_t+"\tA*:"+A_star);
        if((int)A_t>A_star){
            //Log.d("download","decide flag set to true");
            decide_flag=true;
        }
        else
        {
            //Log.d("download?","decide flag set to false");
            decide_flag=false;
        }
        return decide_flag;
    }

    private static int castle_download_decision(){
            boolean decide_flag = false;
            decide_flag = castle_decide();
            if (decide_flag) {
                Log.d("First decision", "Wait for random time");
                int wait_delay = castle_random_wait();
                try {
                    sleep(wait_delay * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                decide_flag = false;
                decide_flag = castle_decide();
                if (decide_flag) {
                    Log.d("Second decision", "decided to download and set the flag\n");
                    return 1;
                }
            }
            return 0;
    }
    public static int castle_schedule( ) {
        total_wait=0;
        while(true){
            long start_time = System.nanoTime();
            int decision=castle_download_decision();
            long end_time = System.nanoTime();
            long diff_time = end_time - start_time;
            total_wait+=diff_time;
            if(decision==1){
                return 1;
            }else{
                //limit = 8000 * 1000;
                //long check_count=8;
                Log.d("castle_library","Total waited now:\t"+total_wait/1000000+"\tshould be greater than\t"+limit+"\n");
                total_wait /= 1000000;
                if (total_wait > limit) {//80000
                    Log.d("Failed to download", "waited too long now direct download\n");
                    return 1;
                } else {
                    start_time = System.nanoTime();
                    try{
                        Log.d("castle_library","Lets sleep for a second");
                        sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    end_time = System.nanoTime();
                    diff_time = end_time - start_time;
                    total_wait+=diff_time;
                    Log.d("castle_library","Waiting is not done enough\t"+total_wait/1000000);
                }
            }
        }
    }
}
