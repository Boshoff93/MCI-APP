package com.example.wiehan.mci;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.internal.view.SwipeDismissLayout;
import android.support.wearable.view.BoxInsetLayout;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;
    private TextView[] dots;
    private LinearLayout dotsLayout;
    private int[] layouts;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private ListView list;
    Chronometer timer;
    int timerSet = 0;
    ;
    ArrayList<String> colors = new ArrayList<String>();
    private AlertDialog redAlert;
    private AlertDialog yellowAlert;
    private AlertDialog orangeAlert;
    private AlertDialog describeAlert;
    private AlertDialog missionAlert;
    private SensorManager mSensorManager;
    private float mAccel; // acceleration, apart from gravity
    private float mAccelCurrent; // current acceleration + gravity
    private float mAccelLast; // last acceleration + gravity
    private int alertFlagRed = 0;
    private int alertFlagYellow = 0;
    private int alertFlagOrange = 0;
    private int alertDescribeFlag = 0;
    private int alertMissionFlag = 0;
    private long[] vibrateMessageUpdate = {0, 50, 25, 50, 25, 50};
    private long[] vibrateRed = {0, 500, 200, 500, 200, 500};
    private long[] vibrateOrange = {0, 350, 150, 350};
    private long[] vibrateYellow = {0, 200};
    private int progressPoints = 100;
    private GestureDetector gdt;
    private String alertDescription;
    private ArrayList<String> alertDescriptionBank = new ArrayList<String>();


    int counttemp = 0;

    //************************************************ Activemq Setting **************************************************

    CallbackConnection connection;
    Buffer getpayload;
    LinkedList<Buffer> payloadQueue = new LinkedList<Buffer>(); // Change to Buffer LinkedList Once android wear 2.0 availible
    int newPayloadAvailable = 0;


    int randomMessage = 0; // will delete this once android wear 2.0 availible

    //************************************************ Activemq Setting **************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);


        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.LayoutDots);

        layouts = new int[]{R.layout.screen_three, R.layout.screen_one, R.layout.screen_two};

        addBottomDots(1);
        changeStatusBarColor();
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(1);
        viewPager.addOnPageChangeListener(viewListener);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        createMissionOverviewAlert("Mission:");

        //************************************************ Activemq Setting ************************************************

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setCancelable(false);
        builder.setTitle("ActiveMQ IP");
        final EditText input = new EditText(this);
        input.setHint("Hint: 192.168.0.11");
        input.setGravity(Gravity.CENTER);
        input.setTextSize(18);
        int maxLength = 20;
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MQTT mqtt = new MQTT();
                try {
                    mqtt.setHost("tcp://" + input.getText().toString() + ":1883");

                } catch (URISyntaxException e) {
                    e.printStackTrace();

                    Context context = getApplicationContext();
                    CharSequence text = "Failed to set Host";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();


                }

                mqtt.setClientId("WADClient");
                connection = mqtt.callbackConnection();
                connection.listener(new Listener() {

                    public void onDisconnected() {
                    }

                    public void onConnected() {
                    }

                    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
                        // You can now process a received message from a topic.
                        // Once process execute the ack runnable.
                        getpayload = payload ;
                        ack.run();
                        newPayloadAvailable = 1 ;
                    }

                    public void onFailure(Throwable value) {

                    }
                });

                connection.connect(new Callback<Void>() {
                    public void onFailure(Throwable value) {

                    }

                    // Once we connect..
                    public void onSuccess(Void v) {

                        // Subscribe to a topic
                        Topic[] topics = {new Topic("MCITOPIC", QoS.AT_LEAST_ONCE)};
                        connection.subscribe(topics, new Callback<byte[]>() {
                            public void onSuccess(byte[] qoses) {
                                // The result of the subcribe request.
                            }

                            public void onFailure(Throwable value) {

                            }
                        });

                    }
                });

                //***************************NEXT STEP **************************


                final Handler getPayload = new Handler();
                new Thread()
                {
                    public void run()
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        MainActivity.this.runOnUiThread(new Runnable()
                        {
                            public void run() {

                                if(newPayloadAvailable == 1 ){
                                    if(getpayload != null) {
                                        payloadQueue.add(getpayload);
                                        newPayloadAvailable = 0;
                                    }
                                }
                                getPayload.postDelayed(this, 250);
                            }

                        });
                    }

                }.start();

                final Handler dequeing = new Handler();

                new Thread() {
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {

                                try {
                                    if (!payloadQueue.isEmpty()) {
                                        String payloadContent = payloadQueue.removeFirst().toString();
                                        payloadContent = payloadContent.substring(7, payloadContent.length());

                                        if (payloadContent.startsWith("(H)")) {

                                            colors.add("#ff0000");
                                            progressPoints -= 7;
                                            arrayList.add("High Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

                                            redVib.vibrate(vibrateRed, -1);

                                            final AlertDialog.Builder redAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            redAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagRed = 0;
                                                        }
                                                    });


                                            redAlert = redAppAlert.create();
                                            WindowManager.LayoutParams placement = redAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            redAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_red);

                                            redAlert.setMessage("High Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagRed = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                                            redAlert.show();
                                            TextView textView = (TextView) redAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_red);

                                            final Button positiveButton = redAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(M)")) {
                                            colors.add("#FF8C00"); // value used by getView in arrayAdapter
                                            progressPoints -= 5;
                                            arrayList.add("Medium Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator orangeVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            orangeVib.vibrate(vibrateOrange, -1);

                                            final AlertDialog.Builder orangeAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            orangeAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagOrange = 0;
                                                        }
                                                    });


                                            orangeAlert = orangeAppAlert.create();
                                            WindowManager.LayoutParams placement = orangeAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            orangeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_orange);

                                            orangeAlert.setMessage("Medium Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagOrange = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                                            orangeAlert.show();
                                            TextView textView = (TextView) orangeAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_orange);

                                            final Button positiveButton = orangeAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(L)")) {
                                            colors.add("#FFFF00");
                                            progressPoints -= 3;
                                            arrayList.add("Low Priority Alert\n(" + timer.getText() + ")");
                                            adapter.notifyDataSetChanged();

                                            Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            yellowVib.vibrate(vibrateYellow, -1);


                                            final AlertDialog.Builder yellowAppAlert = new AlertDialog.Builder(MainActivity.this);
                                            yellowAppAlert.setPositiveButton(
                                                    "OK",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            alertFlagYellow = 0;
                                                        }
                                                    });


                                            yellowAlert = yellowAppAlert.create();
                                            WindowManager.LayoutParams placement = yellowAlert.getWindow().getAttributes();
                                            placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

                                            yellowAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_yellow);

                                            yellowAlert.setMessage("Low Priority Alert Available\n(" + timer.getText() + ")");
                                            alertFlagYellow = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                                            yellowAlert.show();
                                            TextView textView = (TextView) yellowAlert.findViewById(android.R.id.message);
                                            textView.setTextSize(20);
                                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                            textView.setHeight(300);
                                            textView.setWidth(300);
                                            textView.setBackgroundResource(R.drawable.textview_yellow);

                                            final Button positiveButton = yellowAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                                            LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                                            posParams.weight = 1;
                                            posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                            posParams.height = 100;
                                            posParams.gravity = Gravity.CENTER_HORIZONTAL;
                                            positiveButton.setGravity(Gravity.CENTER);
                                            positiveButton.setPadding(0,0,0,0);

                                            alertDescription = payloadContent.substring(3, payloadContent.length());
                                            alertDescriptionBank.add(alertDescription);

                                        } else if (payloadContent.startsWith("(UM)")) {
                                            alertDescription = payloadContent.substring(4, payloadContent.length());
                                            createMissionOverviewAlert("Mission: " + alertDescription);
                                            alertDescription = "MISSION UPDATED";
                                            Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            yellowVib.vibrate(vibrateMessageUpdate, -1);
                                            createAlertDescription();
                                        }
                                    }
                                } catch (Exception e) {

                                    Context context = getApplicationContext();
                                    CharSequence text = "Failed to Deque";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();

                                }
                                dequeing.postDelayed(this, 1000);


                            }

                        });
                    }

                }.start();

                if (timerSet == 0) {
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.start();
                    timerSet = 1;
                }
            }
        });

        final AlertDialog dialogInput = builder.create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogInput.getWindow().getAttributes());
        lp.width = 375;
        lp.height = 375;

        dialogInput.show();
        dialogInput.getWindow().setBackgroundDrawableResource(R.drawable.textview_custom);
        dialogInput.getWindow().setAttributes(lp);

        final Button neutralButton = dialogInput.getButton(AlertDialog.BUTTON_NEUTRAL);
        LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) neutralButton.getLayoutParams();
        posParams.weight = 1;
        posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
        posParams.height = 100;
        posParams.gravity = Gravity.CENTER_HORIZONTAL;
        neutralButton.setGravity(Gravity.CENTER);
        neutralButton.setPadding(0,0,0,0);


        //************************************************ Activemq Setting ************************************************


        gdt = new GestureDetector(new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {

                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                final int SWIPE_MIN_DISTANCE = 12;
                final int SWIPE_THRESHOLD_VELOCITY = 20;
                if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    alertMissionFlag = 1;
                    missionAlert.show();
                    TextView textView = (TextView) missionAlert.findViewById(android.R.id.message);
                    textView.setTextSize(20);
                    textView.setTextColor(Color.parseColor("#000000"));
                    textView.setHeight(300);
                    textView.setWidth(300);
                    textView.setBackgroundResource(R.drawable.textview_white);

                    final Button positiveButton = missionAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    posParams.weight = 1;
                    posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    posParams.height = 100;
                    posParams.gravity = Gravity.CENTER_HORIZONTAL;
                    positiveButton.setGravity(Gravity.CENTER);
                    positiveButton.setPadding(0,0,0,0);

                }

                return false;
            }
        });


        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_list_item_1, arrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView text = (TextView) row.findViewById(android.R.id.text1);

                for (int i = 0; i < arrayList.size(); i++) {
                    if (position == i) {
                        row.setBackgroundColor(Color.parseColor(colors.get(i)));
                    }
                }

                text.setTextSize(12);
                text.setGravity(Gravity.CENTER);
                text.setPadding(0,0,0,0);
                text.setTextColor(Color.parseColor("#000000"));
                return row;

            }
        };

    }

    private void addBottomDots(int position) {

        dots = new TextView[layouts.length];
        int[] colorActive = getResources().getIntArray(R.array.dot_active);
        int[] colorInactive = getResources().getIntArray(R.array.dot_inactive);
        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorInactive[position]);
            dotsLayout.addView(dots[i]);

        }

        if (dots.length > 0) {
            dots[position].setTextColor(colorActive[position]);
        }

    }

    private ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {

        Boolean openedFirstTime = true;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (openedFirstTime && positionOffset == 0 && positionOffsetPixels == 0) {
                onPageSelected(1);
                openedFirstTime = false;
            }
        }

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            View v;
            if (position == 0) {
                v = findViewById(R.id.backGround0);
            } else if (position == 1) {
                v = findViewById(R.id.backGround1);
            } else {
                v = findViewById(R.id.backGround2);
            }
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gdt.onTouchEvent(event);
                    return true;
                }
            });

            if (position == 0) {
                ProgressBar prg = (ProgressBar) findViewById(R.id.progressBar);
                TextView pointPercentage = (TextView) findViewById(R.id.pointPercentage);
                pointPercentage.setText(progressPoints + "%");
                prg.setMax(100);
                prg.setProgress(progressPoints);
                Drawable draw = getResources().getDrawable(R.drawable.progress_custom);
                prg.setProgressDrawable(draw);
            } else if (position == 2) {
                list = (ListView) findViewById(R.id.listEvents);
                list.setClickable(true);
                list.setAdapter(adapter);
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        alertDescription = alertDescriptionBank.get(position);                           //Display alert in alert array.
                        createAlertDescription();                                                        //Depends on which cell the user clicks on in notification list
                    }
                });
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

    };

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter

            if (mAccel > 12) {
                if (alertMissionFlag == 1) {
                    missionAlert.dismiss();
                    alertMissionFlag = 0;
                    return;
                }

                if (alertFlagRed == 1) {
                    alertFlagRed = 0;
                    redAlert.dismiss();
                    createAlertDescription();
                    return;
                }

                if (alertFlagOrange == 1) {
                    alertFlagOrange = 0;
                    orangeAlert.dismiss();
                    createAlertDescription();
                    return;
                }

                if (alertFlagYellow == 1) {
                    alertFlagYellow = 0;
                    yellowAlert.dismiss();
                    createAlertDescription();
                    return;
                }

                if (alertDescribeFlag == 1) {
                    alertDescribeFlag = 0;
                    describeAlert.dismiss();
                }

            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void createAlertDescription() {
        final AlertDialog.Builder alertDescripe = new AlertDialog.Builder(MainActivity.this);
        alertDescripe.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });


        describeAlert = alertDescripe.create();
        WindowManager.LayoutParams placement = describeAlert.getWindow().getAttributes();
        placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        describeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_white);
        describeAlert.setMessage(alertDescription);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                describeAlert.show();
                TextView textView = (TextView) describeAlert.findViewById(android.R.id.message);
                textView.setTextSize(20);
                textView.setTextColor(Color.parseColor("#000000"));
                textView.setHeight(300);
                textView.setWidth(300);
                textView.setBackgroundResource(R.drawable.textview_white);

                final Button positiveButton = describeAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                posParams.weight = 1;
                posParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                posParams.height = 100;
                posParams.gravity = Gravity.CENTER_HORIZONTAL;
                positiveButton.setGravity(Gravity.CENTER);
                positiveButton.setPadding(0,0,0,0);

                alertDescribeFlag = 1; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                handler.removeCallbacksAndMessages(null);
            }
        }, 200); //Need to delay as the same threshold is used to display two different types of alert messages


    }


    public void createMissionOverviewAlert(String updatMessage) {
        final AlertDialog.Builder alertMission = new AlertDialog.Builder(MainActivity.this);
        alertMission.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });


        missionAlert = alertMission.create();
        WindowManager.LayoutParams placement = missionAlert.getWindow().getAttributes();
        placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        placement.y = 100;   //y position

        missionAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_white);
        missionAlert.setMessage(updatMessage);

    }

    private class ViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = layoutInflater.inflate(layouts[position], container, false);
            container.addView(v);

            timer = (Chronometer) findViewById(R.id.timer);
            timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer cArg) {
                    long time = SystemClock.elapsedRealtime() - cArg.getBase();
                    int h = (int) (time / 3600000);
                    int m = (int) (time - h * 3600000) / 60000;
                    int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                    String hh = h < 10 ? "0" + h : h + "";
                    String mm = m < 10 ? "0" + m : m + "";
                    String ss = s < 10 ? "0" + s : s + "";
                    cArg.setText(hh + ":" + mm + ":" + ss);
                }
            });
            /*
            if (timerSet == 0) {
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                timerSet = 1;
            }
            */

            return v;
        }


        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View v = (View) object;
            container.removeView(v);
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mClockView.setVisibility(View.GONE);
        }
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener);
    }


}
