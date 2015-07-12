package com.dev.huertix.throttle_dcs_beta;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by huertix on 06.07.15.
 * Just a simple code to store and recover user network information
 */
public class FileManager extends Activity {

    Context context;
    File file;

    public FileManager(Context c){
        super();
        context = c;
    }


    public  void writeToFile(String[] data) {
        try {
            FileOutputStream stream = new FileOutputStream(file);

            for(String a : data){
                stream.write((a+"\n").getBytes());
            }

            stream.close();
        }
        catch (IOException e) {
            Log.d("UDP", "File write failed: " + e.toString());
        }
    }


    public String[] readFromFile() {

        String[] ret = new String[3];

        String path="";

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if(isSDPresent)
        {
            path = context.getExternalFilesDir(null).getAbsolutePath();
        } else
        {
            path = context.getFilesDir().getPath();
        }





        file = new File(path + "/dcs-net.cfg");



        try {
            InputStream inputStream = new FileInputStream(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                int count = 0;

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    ret[count] = receiveString;
                    count++;
                }

                inputStream.close();

                if(count==0)
                    return null;

            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            ret = null;

        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}
