package com.dev.huertix.throttle_dcs_beta;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.*;

/**
 * Created by huertix on 06.07.15.
 * With this code we can connect to the Export.lua script from DCS.
 * Make sure PORTs config are the same in Export.lua and Statics variables in t
 */




public class Main extends Activity implements OnTouchListener {
    private static final String   MSG_HEAD = "THRDCS"; //identification for incoming messages
    private static final int ID_MENU = 0;
    private static String SENDER_ADDRESS;// = "192.168.2.3";//public ip of my server
    private static int SENDER_PORT;// = 14081;
    private static int LISTENING_PORT;// = 14080;
    private static final int COLOR = Integer.parseInt( "f80",16);

    private TextView textView;
    private TextView textView_conStatus;
    private ImageView imageView;
    private Button gearUP_button;
    private Button gearDown_button;
    private Button flaps0_button;
    private Button flaps10_button;
    private Button flaps20_button;
    private Button brakeOn_button;
    private Button brakeOff_button;

    private Canvas canvas;
    private Bitmap bitmap;

    private int sleeptime = 1500;

    private float dw, dh;
    private float y;

    private String gear = "0";
    private String flaps = "0";
    private String flapsNow;
    private String brakeOn = "0";
    private String brakeOff = "0";
    private String userThrottle = "10";
    private String messageReceived ="10";

    private Thread receiverThread;
    private Thread senderThread;
    private Thread UIupdateThread;

    private boolean doubleBackToExitPressedOnce=false;
    private boolean continueReceiver = true;
    private boolean continueSender = true;
    private boolean isFirstConnection;
    private boolean isConnected;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) this.findViewById(R.id.imageView1);
        textView = (TextView) this.findViewById(R.id.textView1);
        textView_conStatus = (TextView) this.findViewById(R.id.con_status);
        gearUP_button = (Button) this.findViewById(R.id.button_gear_up);
        gearDown_button = (Button) this.findViewById(R.id.button_gear_down);
        flaps0_button = (Button) this.findViewById(R.id.button_flap_0);
        flaps10_button = (Button) this.findViewById(R.id.button_flap_10);
        flaps20_button = (Button) this.findViewById(R.id.button_flap_20);
        brakeOn_button = (Button) this.findViewById(R.id.button_brake_on);
        brakeOff_button = (Button) this.findViewById(R.id.button_brake_off);

        gearUP_button.setOnTouchListener(this);
        gearDown_button.setOnTouchListener(this);
        flaps0_button.setOnTouchListener(this);
        flaps10_button.setOnTouchListener(this);
        flaps20_button.setOnTouchListener(this);
        brakeOn_button.setOnTouchListener(this);
        brakeOff_button.setOnTouchListener(this);

        isFirstConnection = true;

        Display currentDisplay = getWindowManager().getDefaultDisplay();
        dw = currentDisplay.getWidth() - imageView.getWidth();
        dh = currentDisplay.getHeight() - imageView.getHeight();

        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = (int) imageView.getWidth();
                int height = (int) imageView.getHeight();

                bitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bitmap);
                imageView.setImageBitmap(bitmap);
            }
        });


        imageView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                float imageHeight = imageView.getHeight();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        y = event.getY();
                        userThrottle = touch2thr(imageHeight, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }


                return true;
            }
        });

        Bundle extras = getIntent().getExtras();
        SENDER_ADDRESS = extras.getString("ip");
        SENDER_PORT = Integer.parseInt(extras.getString("senderPort"));
        LISTENING_PORT = Integer.parseInt(extras.getString("receiverPort"));


        updateThread();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        MenuItem itemExit = menu.add(Menu.NONE,ID_MENU,Menu.NONE,"EXIT");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //check selected menu item
        if(item.getItemId() == ID_MENU){
            //close the Activity
            this.finish();
            return true;
        }
        return false;
    }



    @Override
    protected void onStop() {
        super.onStop();

        continueReceiver = false;
        continueSender = false;
        receiverThread.interrupt();
        senderThread.interrupt();
        UIupdateThread.interrupt();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        continueReceiver = false;
        continueSender = false;
        receiverThread.interrupt();
        senderThread.interrupt();
        UIupdateThread.interrupt();


    }



    protected void onResume() {
        super.onResume();

        continueReceiver = true;
        receiverThread = new Thread(new Receiver());
        receiverThread.start();

        continueSender = true;
        senderThread = new Thread(new Sender());
        senderThread.start();



    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }



    private String messageBuilder(){
        String st = MSG_HEAD;

        st = st + ",";
        st = st + userThrottle;
        st = st + ",";
        st = st + (gear);
        st = st + ",";
        st = st + (flaps);
        st = st + ",";
        st = st + (brakeOn);
        st = st + ",";
        st = st + (brakeOff);
        return st;
    }


    private void updateThread() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        public void run() {

                           // textView.setText(messageReceived + " %");

                            String[] msgArray = messageReceived.split(",");

                            //Log.d("UDP", "Length: " + msgArray.length);

                            if (msgArray[0].equals(MSG_HEAD)) {

                                if (!isConnected) {
                                    textView_conStatus.setText("CONNECTED");
                                    textView_conStatus.setTextColor(Color.GREEN);
                                    isConnected = true;
                                    textView.setText(msgArray[1] + " %");
                                    isFirstConnection = true;
                                    sleeptime = 100;

                                }

                                textView.setText(msgArray[1] + " %");
                                flapsNow = msgArray[3];


                                if (isFirstConnection) {

                                    if (msgArray[2].equals("0")) {
                                        gearUP_button.setBackgroundColor(Color.BLACK);
                                        gearDown_button.setBackgroundColor(COLOR);

                                    } else {
                                        gearUP_button.setBackgroundColor(COLOR);
                                        gearDown_button.setBackgroundColor(Color.BLACK);
                                    }

                                    if (flapsNow.equals("0")) {
                                        flaps0_button.setBackgroundColor(Color.BLACK);
                                        flaps10_button.setBackgroundColor(COLOR);
                                        flaps20_button.setBackgroundColor(COLOR);
                                    } else if (flapsNow.equals("10")) {
                                        flaps0_button.setBackgroundColor(COLOR);
                                        flaps10_button.setBackgroundColor(Color.BLACK);
                                        flaps20_button.setBackgroundColor(COLOR);
                                    } else {
                                        flaps0_button.setBackgroundColor(COLOR);
                                        flaps10_button.setBackgroundColor(COLOR);
                                        flaps20_button.setBackgroundColor(Color.BLACK);
                                    }

                                    isFirstConnection = false;

                                }
                                messageReceived = "";

                            } else {
                                textView_conStatus.setText("DISCONNECTED");
                                textView_conStatus.setTextColor(Color.RED);
                                isConnected = false;
                                sleeptime = 1500;

                            }

                            float aux = (float) Integer.parseInt(userThrottle);
                            drawThr(imageView, bitmap, convertValue_to_Yaxe(imageView.getHeight(), aux));


                        }
                    });

                }
            }
        };




        UIupdateThread = new Thread(runnable);
        UIupdateThread.start();



    }




    /*private void drawThr(ImageView v, Bitmap bmp, float y){
        v.invalidate();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        //canvas.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
        canvas.drawRect((int) v.getWidth() - v.getWidth() + 20, (int) y, (int) v.getWidth() - 20, (int) dh, paint);
    }*/


    /*
    *   Function to draw the throttle bar indicator
     */
    private void drawThr(ImageView v, Bitmap bmp, float y) {
        v.invalidate();
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);
        //canvas.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
        canvas.drawRect((int) v.getWidth() - v.getWidth() + 20, (int) y, (int) v.getWidth() - 20, (int) dh, paint);
    }


    /*
    * it returns the Throttle value to Y imageview coordinate
     */
    private float convertValue_to_Yaxe(float layoutY, float value) {
        float aux = (layoutY * value) / 100;
        return layoutY - aux;
    }


    private String touch2thr(float layoutY, float touchY) {

        if(touchY > layoutY){
            touchY = layoutY;
        }

        float value = (100 * touchY) / layoutY;
        value = value - 100;
        value = Math.abs(value);

        if (value > 100)
            value = 100;

        if (value < 0)
            value = 0;

        return String.valueOf(Math.round(value));
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN) {

            if (v.getId() == gearUP_button.getId()) {
                gear = "0";
                gearUP_button.setBackgroundColor(Color.BLACK);
                gearDown_button.setBackgroundColor(COLOR);

            }
            if (v.getId() == gearDown_button.getId()) {
                gear = "1";
                gearUP_button.setBackgroundColor(COLOR);
                gearDown_button.setBackgroundColor(Color.BLACK);

            }
            if (v.getId() == flaps0_button.getId()) {
                flaps = "0";
                flaps0_button.setBackgroundColor(Color.BLACK);
                flaps10_button.setBackgroundColor(COLOR);
                flaps20_button.setBackgroundColor(COLOR);
            }
            if (v.getId() == flaps10_button.getId()) {
                flaps = "10";
                flaps0_button.setBackgroundColor(COLOR);
                flaps10_button.setBackgroundColor(Color.BLACK);
                flaps20_button.setBackgroundColor(COLOR);
            }
            if (v.getId() == flaps20_button.getId()) {
                flaps = "20";
                flaps0_button.setBackgroundColor(COLOR);
                flaps10_button.setBackgroundColor(COLOR);
                flaps20_button.setBackgroundColor(Color.BLACK);
            }
            if (v.getId() == brakeOn_button.getId()) {
                brakeOn = "1";
                brakeOff = "0";

            }
            if (v.getId() == brakeOff_button.getId()) {
                brakeOn = "0";
                brakeOff = "1";
            }
        }

        if(event.getAction()==MotionEvent.ACTION_UP) {

            if (v.getId() == brakeOn_button.getId()
                    || v.getId() == brakeOff_button.getId()) {
                brakeOn = "0";
                brakeOff = "0";

            }

        }

        return false;
    }


    /*
    * Communication with Export.lua script
     */
    class Receiver implements Runnable {

        @Override
        public void run() {
            try {
                //Opening listening socket
                Log.d("UDP", "Opening listening socket on port " + LISTENING_PORT + "...");
                DatagramSocket socket = new DatagramSocket(LISTENING_PORT);
                socket.setBroadcast(true);
                socket.setReuseAddress(true);

                while (continueReceiver) {
                    //Listening on socket
                    Log.d("UDP", "Listening...");
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    messageReceived = new String(packet.getData()).trim();
                    Log.d("UDP", "Received: '" + messageReceived +"'");

                }
                socket.close();
            } catch (Exception e) {
                Log.e("UDP", "Receiver error", e);
            }
        }


    }

    class Sender implements Runnable {

        @Override
        public void run() {
            try {
                //Preparing the socket
                InetAddress serverAddr = InetAddress.getByName(SENDER_ADDRESS);
                DatagramSocket socket = new DatagramSocket();
                while (continueSender) {
                    //Preparing the packet
                    //byte[] buf = (userThrottle).getBytes();
                    byte[] buf = (messageBuilder()).getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SENDER_PORT);

                    //Sending the packet
                    // Log.d("UDP", String.format("Sending: '%s' to %s:%s", new String(buf), SERVER_ADDRESS, SERVER_PORT));
                    socket.send(packet);
                    // Log.d("UDP", "Packet sent.");

                    senderThread.sleep(sleeptime);

                }
                socket.close();
            } catch (Exception e) {
                Log.e("UDP", "Sender error", e);
            }
        }
    }

}





/*
view.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            @Override
            public void onClick() {
                super.onClick();
                // your on click here
            }

            @Override
            public void onDoubleClick() {
                super.onDoubleClick();
                // your on onDoubleClick here
            }

            @Override
            public void onLongClick() {
                super.onLongClick();
                // your on onLongClick here
            }

            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                // your swipe up here
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                // your swipe down here.
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                // your swipe left here.
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                // your swipe right here.
            }
        });

}
 */

