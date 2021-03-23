package com.neandroid.datetimepicker.demo;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.calendar.datetimepicker.common.LineConfig;
import com.android.calendar.datetimepicker.listeners.OnItemPickListener;
import com.android.calendar.datetimepicker.listeners.OnSingleWheelListener;
import com.android.calendar.datetimepicker.picker.DatePicker;
import com.android.calendar.datetimepicker.picker.SinglePicker;
import com.android.calendar.datetimepicker.picker.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    /**
     * 自定义分割线
     *
     * @param view
     */
    public void onCustomSplitLine(View view) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String s = "";
            if (i < 10) {
                s = "0" + i;
            } else {
                s = i + "";
            }
            list.add(s);
        }
//        String[] ss = (String[]) list.toArray();
        SinglePicker<String> picker = new SinglePicker<>(this, list);
        picker.setCanLoop(false);//不禁用循环
        picker.setLineVisible(true);
        picker.setTBLineVisible(true);
        picker.setTextSize(18);
        picker.setSelectedIndex(2);
        //启用权重 setWeightWidth 才起作用
        picker.setLabel("分");
        //picker.setItemWidth(100);
//        picker.setWeightEnable(true);
//        picker.setWeightWidth(1);
        picker.setOuterLabelEnable(true);
        picker.setCanceledOnTouchOutside(true);
        picker.setSelectedTextColor(Color.GREEN);//前四位值是透明度
        picker.setUnSelectedTextColor(Color.BLACK);
        //picker.setDividerType(LineConfig.DividerType.CIRCLE);
        //picker.setDividerType(LineConfig.DividerType.LEFT_ROUND);
        //picker.setDividerType(LineConfig.DividerType.RIGHT_ROUND);
        picker.setDividerType(LineConfig.DividerType.ROUND);
        picker.setOnSingleWheelListener(new OnSingleWheelListener() {
            @Override
            public void onWheeled(int index, String item) {
                Toast.makeText(MainActivity.this, "index=" + index + ", item=" + item, Toast.LENGTH_SHORT).show();
            }
        });
        picker.setOnItemPickListener(new OnItemPickListener<String>() {
            @Override
            public void onItemPicked(int index, String item) {
                Toast.makeText(MainActivity.this, "index=" + index + ", item=" + item, Toast.LENGTH_SHORT).show();
            }
        });
        picker.show();
    }

    /**
     * 年月日选择
     */
    public void onCustomYearMonthDayPicker(View view) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        Log.d(TAG, "onCustomYearMonthDayPicker:: " + year + "-" + month + "-" + day);
        final CustomDatePicker picker = new CustomDatePicker(this);
        picker.setTopPadding(15);
        picker.setRangeStart(2016, 8, 29);
        picker.setRangeEnd(2111, 1, 11);
        picker.setSelectedItem(year, month, day);
        picker.setWeightEnable(true);
        picker.setLineVisible(true);
        picker.setCanLinkage(true);
        picker.setLineColor(Color.BLACK);
        picker.setBackgroundRes(R.drawable.shape_picker_bg);
        picker.setLeftDividerType(LineConfig.DividerType.LEFT_ROUND);
        picker.setRightDividerType(LineConfig.DividerType.RIGHT_ROUND);
        picker.setLabel("", "", "");
        picker.setLineColor(Color.WHITE);
        picker.setCanceledOnTouchOutside(true);
        picker.setOnDatePickListener(new DatePicker.OnYearMonthDayPickListener() {
            @Override
            public void onDatePicked(String year, String month, String day) {
                Toast.makeText(MainActivity.this, year + "-" + month + "-" + day, Toast.LENGTH_SHORT).show();
            }
        });
        picker.setOnWheelListener(new DatePicker.OnWheelListener() {
            @Override
            public void onYearWheeled(int index, String year) {
                picker.setTitleText(year + "-" + picker.getSelectedMonth() + "-" + picker.getSelectedDay());
            }

            @Override
            public void onMonthWheeled(int index, String month) {
                picker.setTitleText(picker.getSelectedYear() + "-" + month + "-" + picker.getSelectedDay());
            }

            @Override
            public void onDayWheeled(int index, String day) {
                picker.setTitleText(picker.getSelectedYear() + "-" + picker.getSelectedMonth() + "-" + day);
            }
        });
        picker.show();
    }


    public void onTimePicker(View view) {
        //TimePicker picker = new TimePicker(this, TimePicker.HOUR_24);
        CustomTimePicker picker = new CustomTimePicker(this, TimePicker.HOUR_24);
        picker.setRangeStart(0, 0);//09:00
        picker.setRangeEnd(23, 59);//18:30
        picker.setLineVisible(true);
        picker.setOuterLabelEnable(false);
        picker.setCanceledOnTouchOutside(true);
        picker.setBackgroundRes(R.drawable.shape_picker_bg);
        picker.setLeftDividerType(LineConfig.DividerType.LEFT_ROUND);
        picker.setRightDividerType(LineConfig.DividerType.RIGHT_ROUND);
        picker.setLabel("", "");
        picker.setOnTimePickListener(new TimePicker.OnTimePickListener() {
            @Override
            public void onTimePicked(String hour, String minute) {
                Toast.makeText(MainActivity.this, hour + ":" + minute, Toast.LENGTH_SHORT).show();
            }
        });
        picker.show();
    }
}
