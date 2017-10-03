package com.example.wiehan.mci;

import android.app.AlertDialog;
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
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
    int timerSet = 0 ;
    String[] colors = new String[]{"#D3D3D3","#FFFFFF"};
    private AlertDialog redAlert;
    private AlertDialog yellowAlert;
    private AlertDialog orangeAlert ;

    private SensorManager mSensorManager;
    private float mAccel; // acceleration, apart from gravity
    private float mAccelCurrent; // current acceleration + gravity
    private float mAccelLast; // last acceleration + gravity
    private int alertFlag = 0 ;



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

        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_list_item_1, arrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                int color = position % colors.length;
                view.setBackgroundColor(Color.parseColor(colors[color]));
                text.setTextColor(Color.parseColor("#000000"));
                text.setTextSize(10);

                return view;
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

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            if (position == 0) {
                ProgressBar prg = (ProgressBar) findViewById(R.id.progressBar);
                prg.setMax(100);
                prg.setProgress(70);
                Drawable draw = getResources().getDrawable(R.drawable.progress_custom);
                prg.setProgressDrawable(draw);
            } else if (position == 2) {
                list = (ListView) findViewById(R.id.listEvents);
                list.setAdapter(adapter);
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
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter

            if (mAccel > 12) {
                if(alertFlag == 1) {
                    alertFlag = 0;
                    if(redAlert.isShowing()){
                        redAlert.dismiss();
                    } else if (yellowAlert.isShowing()) {
                        yellowAlert.dismiss();
                    } else if (orangeAlert.isShowing()){
                        orangeAlert.dismiss();
                    }
                    Toast toast = Toast.makeText(getApplicationContext(), "Shake Occured.", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

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
            if(timerSet == 0) {
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                timerSet =1;
            }

            final Button red = (Button) findViewById(R.id.red);
            Button orange = (Button) findViewById(R.id.orange);
            Button yellow = (Button) findViewById(R.id.yellow);


            red.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    arrayList.add("Notification Red\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    redVib.vibrate(500);

                    final AlertDialog.Builder redAppAlert = new AlertDialog.Builder(MainActivity.this);
                    redAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    redAlert = redAppAlert.create();
                    WindowManager.LayoutParams placement = redAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.CENTER | Gravity.CENTER_HORIZONTAL;
                    placement.y = 100;   //y position


                    redAlert.setMessage("Notification Red\n(" + timer.getText() + ")");
                    alertFlag = 1 ; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                    redAlert.show();
                    TextView textView = (TextView) redAlert.findViewById(android.R.id.message);
                    textView.setTextSize(16);
                    textView.setGravity(Gravity.CENTER);
                    textView.setHeight(200);
                    textView.setWidth(100);
                    textView.setBackgroundResource(R.drawable.textview_red);


                }
            });

            yellow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    arrayList.add("Notification Yellow\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    yellowVib.vibrate(100);


                    final AlertDialog.Builder yellowAppAlert = new AlertDialog.Builder(MainActivity.this);
                    yellowAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    yellowAlert = yellowAppAlert.create();
                    WindowManager.LayoutParams placement = yellowAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    placement.y = 150;   //y position

                    yellowAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_yellow);

                    yellowAlert.setMessage("Notification Yellow\n(" + timer.getText() + ")");
                    alertFlag = 1 ; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                    yellowAlert.show();
                    TextView textView = (TextView) yellowAlert.findViewById(android.R.id.message);
                    textView.setTextSize(16);
                    textView.setGravity(Gravity.CENTER);
                    textView.setHeight(200);
                    textView.setWidth(100);
                    textView.setBackgroundResource(R.drawable.textview_yellow);


                }
            });

            orange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    arrayList.add("Notification Orange\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator orangeVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    orangeVib.vibrate(300);

                    final AlertDialog.Builder orangeAppAlert = new AlertDialog.Builder(MainActivity.this);
                    orangeAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    orangeAlert = orangeAppAlert.create();
                    WindowManager.LayoutParams placement = orangeAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    placement.y = 150;   //y position

                    orangeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_orange);

                    orangeAlert.setMessage("Notification Orange\n(" + timer.getText() + ")");
                    alertFlag = 1 ; // Alert is shown, set flag so that onSensorChange event knows to remove alert
                    orangeAlert.show();
                    TextView textView = (TextView) orangeAlert.findViewById(android.R.id.message);
                    textView.setTextSize(16);
                    textView.setGravity(Gravity.CENTER);
                    textView.setHeight(200);
                    textView.setWidth(100);
                    textView.setBackgroundResource(R.drawable.textview_orange);


                }
            });

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
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
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
