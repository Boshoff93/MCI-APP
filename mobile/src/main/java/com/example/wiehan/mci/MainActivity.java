package com.example.wiehan.mci;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.constraint.solver.ArrayLinkedVariables;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import org.w3c.dom.Text;

import java.util.ArrayList;



public class MainActivity extends AppCompatActivity {

    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;
    private TextView[] dots;
    private LinearLayout dotsLayout;
    private int[] layouts;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private ListView list;
    private int pickColor = 0;
    Chronometer timer;
    String[] colors = new String[]{"#D3D3D3","#FFFFFF"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.LayoutDots);

        layouts = new int[]{R.layout.screen_one, R.layout.screen_two};

        addBottomDots(0);
        changeStatusBarColor();
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewListener);

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
            if (position == 1) {
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
            timer.setBase(SystemClock.elapsedRealtime());
            timer.start();

            ProgressBar prg = (ProgressBar) findViewById(R.id.progressBar);
            prg.setMax(100);
            prg.setProgress(70);

            Drawable draw = getResources().getDrawable(R.drawable.progress_custom);
            prg.setProgressDrawable(draw);

            Button red = (Button) findViewById(R.id.red);
            Button orange = (Button) findViewById(R.id.orange);
            Button yellow = (Button) findViewById(R.id.yellow);


            red.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    arrayList.add("Notification Red\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator redVib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    redVib.vibrate(500);

                    final AlertDialog.Builder redAppAlert = new AlertDialog.Builder(MainActivity.this, R.style.customDialog);
                    redAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    final AlertDialog redAlert = redAppAlert.create();
                    WindowManager.LayoutParams placement = redAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    placement.y = 150;   //y position

                    redAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_red);

                    redAlert.setMessage("Notification Red\n(" + timer.getText() + ")");
                    redAlert.show();
                    TextView textView = (TextView) redAlert.findViewById(android.R.id.message);
                    textView.setTextSize(20);


                }
            });

            yellow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    arrayList.add("Notification Yellow\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator yellowVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    yellowVib.vibrate(100);


                    final AlertDialog.Builder yellowAppAlert = new AlertDialog.Builder(MainActivity.this, R.style.customDialog);
                    yellowAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    final AlertDialog yellowAlert = yellowAppAlert.create();
                    WindowManager.LayoutParams placement = yellowAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    placement.y = 150;   //y position

                    yellowAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_yellow);

                    yellowAlert.setMessage("Notification Yellow\n(" + timer.getText() + ")");
                    yellowAlert.show();
                    TextView textView = (TextView) yellowAlert.findViewById(android.R.id.message);
                    textView.setTextSize(20);


                }
            });

            orange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    arrayList.add("Notification Orange\n(" + timer.getText() + ")");
                    adapter.notifyDataSetChanged();

                    Vibrator orangeVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    orangeVib.vibrate(300);

                    final AlertDialog.Builder orangeAppAlert = new AlertDialog.Builder(MainActivity.this, R.style.customDialog);
                    orangeAppAlert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });


                    final AlertDialog orangeAlert = orangeAppAlert.create();
                    WindowManager.LayoutParams placement = orangeAlert.getWindow().getAttributes();
                    placement.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    placement.y = 150;   //y position

                    orangeAlert.getWindow().setBackgroundDrawableResource(R.drawable.textview_orange);

                    orangeAlert.setMessage("Notification Orange (" + timer.getText() + ")");
                    orangeAlert.show();
                    TextView textView = (TextView) orangeAlert.findViewById(android.R.id.message);
                    textView.setTextSize(20);


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
}

