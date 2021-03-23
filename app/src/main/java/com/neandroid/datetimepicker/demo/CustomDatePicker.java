package com.neandroid.datetimepicker.demo;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.calendar.datetimepicker.picker.DatePicker;


/**
 * 自定义布局日期选择器
 */
public class CustomDatePicker extends DatePicker {
    private View topView;
    private View footerView;
    private TextView tvTitle;
    private TextView tvDone;
    private String title;
    private String confirm;

    public CustomDatePicker(Activity activity) {
        super(activity);
        initHeaderView();
        initFooterView();
    }

    @Nullable
    @Override
    protected View makeFooterView() {
        if (null == footerView) {
            return super.makeFooterView();
        } else {
            return footerView;
        }
    }

    @NonNull
    @Override
    protected View makeCenterView() {
        return super.makeCenterView();
    }

    @Nullable
    @Override
    protected View makeHeaderView() {
        if (null == topView) {
            return super.makeHeaderView();
        } else {
            return topView;
        }

    }

    private void initHeaderView() {
        topView = LayoutInflater.from(getContext()).inflate(R.layout.layout_header, null);
        tvTitle = topView.findViewById(R.id.tv_title);
    }

    private void initFooterView() {
        footerView = LayoutInflater.from(getContext()).inflate(R.layout.layout_footer, null);
        tvDone = footerView.findViewById(R.id.tv_done);
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit();
            }
        });
    }

    @Override
    protected void onSubmit() {
        super.onSubmit();
        this.dismiss();
    }

    public void setTitle(String title) {
        this.title = title;
        if (null != tvTitle)
            tvTitle.setText(title);
    }

    public String getTitle() {
        return title;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
        if (tvDone != null) {
            tvDone.setText(confirm);
        }
    }

    public String getConfirm() {
        return confirm;
    }
}
