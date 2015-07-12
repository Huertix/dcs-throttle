package com.dev.huertix.throttle_dcs_beta;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huertix on 06.07.15.
 * To collect and store user network configuration
 */


public class Start extends Activity{

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";


    private EditText editText_hostIp;
    private EditText editText_hostPort;
    private EditText editText_receiverPort;
    private FileManager fmanager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        // Set portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        fmanager = new FileManager(getBaseContext());

        editText_hostIp  = (EditText) findViewById(R.id.hostIp);
        editText_hostPort = (EditText) findViewById(R.id.hostPort);
        editText_receiverPort = (EditText) findViewById(R.id.receiverPort);

        String[] netCfgAux = fmanager.readFromFile();

        if(netCfgAux!=null){

            editText_hostIp.setText((String)netCfgAux[0]);
            editText_hostPort.setText((String)netCfgAux[1]);
            editText_receiverPort.setText((String)netCfgAux[2]);

        }


        editText_receiverPort.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {

                    if(checkText()){

                        String[] netCfg = new String[3];

                        netCfg[0] = editText_hostIp.getText().toString();
                        netCfg[1] = editText_hostPort.getText().toString();
                        netCfg[2] = editText_receiverPort.getText().toString();

                        fmanager.writeToFile(netCfg);

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        Bundle extras = new Bundle();
                        extras.putString("ip",netCfg[0]);
                        extras.putString("senderPort", netCfg[1]);
                        extras.putString("receiverPort", netCfg[2]);

                        Intent intent = new Intent(Start.this, Main.class);
                        intent.putExtras(extras);
                        startActivity(intent);
                        finish();



                    }

                    return true;
                }
                return false;
            }
        });


    }



    public boolean checkText() {

        boolean isCorrect = true;

        String hostIpString = editText_hostIp.getText().toString();
        String hostPortString = editText_hostPort.getText().toString();
        String receiverPort = editText_receiverPort.getText().toString();

        if(hostIpString.trim().equals("") || hostPortString.trim().equals("")  || receiverPort.trim().equals("") ) {

            Toast.makeText(getApplicationContext(), "ALL FIELDS MUST BE FILLED", Toast.LENGTH_LONG).show();
            return false;
        }

        Pattern p1 = Pattern.compile(IPADDRESS_PATTERN);
        Pattern p2 = Pattern.compile("[0-9]+");

        Matcher matcher = p1.matcher(hostIpString);

        if(!matcher.matches()){
            Toast.makeText(getApplicationContext(), " BAD IP  FORMAT\nEx: 192.168.1.4", Toast.LENGTH_LONG).show();
            isCorrect = false;
        }

        matcher = p2.matcher(hostPortString);

        if(!matcher.matches()){
            Toast.makeText(getApplicationContext(), "BAD PORT FORMAT\nEx: 10000", Toast.LENGTH_LONG).show();
            isCorrect = false;
        }

        matcher = p2.matcher(receiverPort);

        if(!matcher.matches()){
            Toast.makeText(getApplicationContext(), "BAD PORT FORMAT\nEx: 10001", Toast.LENGTH_LONG).show();
            isCorrect = false;
        }



        int a = Integer.parseInt(hostPortString);
        int b = Integer.parseInt(receiverPort);

        if((a < 1025 || a > 65535) || (b < 1025 || b > 65535)){
            Toast.makeText(getApplicationContext(), "BAD PORT RANGE\n" +
                    "Choose Range [1025-65535] ", Toast.LENGTH_LONG).show();
            isCorrect = false;
        }

        else if(a==b){
            Toast.makeText(getApplicationContext(), "PORTS MUST BE DIFFERENT", Toast.LENGTH_LONG).show();
            isCorrect = false;
        }


    return isCorrect;


    }
}
