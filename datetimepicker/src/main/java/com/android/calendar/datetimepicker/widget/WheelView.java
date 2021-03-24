package com.android.calendar.datetimepicker.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.android.calendar.datetimepicker.R;
import com.android.calendar.datetimepicker.adapter.WheelAdapter;
import com.android.calendar.datetimepicker.common.InertiaTimerTask;
import com.android.calendar.datetimepicker.common.LineConfig;
import com.android.calendar.datetimepicker.common.MessageHandler;
import com.android.calendar.datetimepicker.common.OnItemPickedRunnable;
import com.android.calendar.datetimepicker.common.SmoothScrollTimerTask;
import com.android.calendar.datetimepicker.listeners.OnItemPickListener;
import com.android.calendar.datetimepicker.listeners.WheelViewGestureListener;
import com.android.calendar.datetimepicker.model.IPickerViewData;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 3D滚轮控件
 */
public class WheelView extends View {
    private String TAG = this.getClass().getSimpleName();

    private Context context;

    /**
     * 点击，滑翔(滑到尽头)，拖拽事件
     */
    public enum ACTION {
        CLICK, FLING, DRAG
    }

    /**
     * 分割线配置
     */
    private LineConfig lineConfig = null;

    /**
     * 分隔线类型
     */
    private LineConfig.DividerType dividerType;


    public Handler handler;
    /**
     * 控制滑动
     */
    private GestureDetector gestureDetector;
    private OnItemPickListener onItemPickListener;

    private boolean isOptions = false;
    //private boolean isCenterLabel = true;

    /**
     * 附加单位是否仅仅只显示在选中项后面  是否只中间选中显示label   中间显示只有一个label   否则 和item个数一样
     */
    private boolean onlyShowCenterLabel = true;

    /**
     * label与文本的间距
     *
     * @param labelSpace
     */
    public void setLabelSpace(int labelSpace) {
        this.labelSpace = labelSpace;
    }

    private int labelSpace = 0;

    /**
     * 线程相关
     */
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;

    /**
     * 画笔相关
     */
    Paint paintOuterText;
    Paint paintCenterText;
    Paint paintLine;

    private WheelAdapter<String> adapter;

    /**
     * 附加单位
     */
    private String label;

    /**
     * 选项的文字大小
     */
    public int textSize;
    public int maxTextWidth;
    public int maxTextHeight;

    /**
     * 每行高度
     */
    public float itemHeight;

    /**
     * 字体样式，默认是等宽字体
     */
    private Typeface typeface = Typeface.MONOSPACE;

    private int textColorOut = 0xFFa8a8a8;
    private int textColorCenter = 0xFF2a2a2a;
    private int dividerColor = 0xFFd5d5d5;
    private int dividerWidth;

    /**
     * 条目间距倍数
     */
    private float lineSpacingMultiplier = 2.5F;
    public boolean isLoop;

    /**
     * 第一条线Y坐标值
     */
    private float firstLineY;

    /**
     * 第二条线Y坐标
     */
    private float secondLineY;

    /**
     * 中间label绘制的Y坐标
     */
    private float centerY;

    /**
     * 滚动总高度y值
     */
    public float totalScrollY;

    /**
     * 初始化默认选中项
     */
    public int initPosition;

    /**
     * 选中的Item
     */
    private String selectedItem;

    /**
     * 选中的Item是第几个
     */
    private int selectedPosition;
    private int preCurrentIndex;

    /**
     * 滚动偏移值,用于记录滚动了多少个item
     */
    private int change;

    /**
     * 绘制几个条目，实际上第一项和最后一项Y轴压缩成0%了，所以可见的数目实际为9
     */
    private int itemsVisible = 5;

    /**
     * WheelView 控件高度
     */
    private int measuredHeight;

    /**
     * WheelView 控件宽度
     */
    private int measuredWidth;

    /**
     * 半圆周长
     */
    private int halfCircumference;

    /**
     * 半径
     */
    private int radius;

    private int mOffset = 0;
    private float previousY = 0;
    private long startTime = 0;

    /**
     * 修改这个值可以改变滑行速度
     */
    private static final int VELOCITY_FLING = 10;
    private int widthMeasureSpec, heightMeasureSpec;

    private int mGravity = Gravity.CENTER;
    /**
     * 中间选中文字开始绘制位置
     */
    private int drawCenterContentStart = 0;

    /**
     * 非中间文字开始绘制位置
     */
    private int drawOutContentStart = 0;

    /**
     * 非中间文字则用此控制高度，压扁形成3d错觉
     */
    private static final float SCALE_CONTENT = 1F;

    /**
     * 偏移量
     */
    private float centerContentOffset;

    public WheelView(Context context) {
        this(context, null);
    }

    @SuppressLint("NewApi")
    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        textSize = getResources().getDimensionPixelSize(R.dimen.view_text_size);//默认大小

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（0.75/1.0/1.5/2.0/3.0）

        if (density < 1) {//根据密度不同进行适配
            centerContentOffset = 2.4F;
        } else if (1 <= density && density < 2) {
            centerContentOffset = 3.6F;
        } else if (2 <= density && density < 3) {
            centerContentOffset = 6.0F;
        } else if (density >= 3) {
            centerContentOffset = density * 2.5F;
        }
        if (attrs != null) {
            @SuppressLint("CustomViewStyleable")
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.wheelview, 0, 0);
            mGravity = a.getInt(R.styleable.wheelview_wheelview_gravity, Gravity.CENTER);
            textColorOut = a.getColor(R.styleable.wheelview_wheelview_textColorOut, 0xFFa8a8a8);
            textColorCenter = a.getColor(R.styleable.wheelview_wheelview_textColorCenter, 0xFF2a2a2a);
            dividerColor = a.getColor(R.styleable.wheelview_wheelview_dividerColor, 0xFFd5d5d5);
            dividerWidth = a.getDimensionPixelSize(R.styleable.wheelview_wheelview_dividerWidth, 2);
            textSize = a.getDimensionPixelOffset(R.styleable.wheelview_wheelview_textSize, textSize);
            lineSpacingMultiplier = a.getFloat(R.styleable.wheelview_wheelview_lineSpacingMultiplier, lineSpacingMultiplier);
            a.recycle();//回收内存
        }
        judgeLineSpace();

        initWheelView(context);
    }

    /**
     * 判断间距是否在1.0-2.0之间
     */
    private void judgeLineSpace() {
        if (lineSpacingMultiplier < 1.2f) {
            lineSpacingMultiplier = 1.2f;
        } else if (lineSpacingMultiplier > 2.0f) {
            lineSpacingMultiplier = 2.5f;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private void initWheelView(Context context) {
        this.context = context;
        handler = new MessageHandler(this);
        gestureDetector = new GestureDetector(context, new WheelViewGestureListener(this));
        gestureDetector.setIsLongpressEnabled(false);

        isLoop = true;

        totalScrollY = 0;
        initPosition = -1;

        initPaints();

    }

    private void initPaints() {
        paintOuterText = new Paint();
        paintOuterText.setColor(textColorOut);
        paintOuterText.setAntiAlias(true);
        paintOuterText.setTypeface(typeface);
        paintOuterText.setTextSize(textSize);

        paintCenterText = new Paint();
        paintCenterText.setColor(textColorCenter);
        paintCenterText.setAntiAlias(true);
        paintCenterText.setTextScaleX(1.1F);
        paintCenterText.setTypeface(typeface);
        paintCenterText.setTextSize(textSize);

        paintLine = new Paint();
        paintLine.setColor(dividerColor);
        paintLine.setAntiAlias(true);
        paintLine.setStyle(Paint.Style.STROKE);
        if (lineConfig != null) {
            paintLine.setColor(lineConfig.getColor());
            paintLine.setAlpha(lineConfig.getAlpha());
            paintLine.setStrokeWidth(lineConfig.getThick());
        }

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void remeasure() {//重新测量
        if (adapter == null) {
            return;
        }
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        measureTextWidthHeight();

        //半圆的周长 = item高度乘以item数目-1
        halfCircumference = (int) (itemHeight * (itemsVisible - 1));
        //整个圆的周长除以PI得到直径，这个直径用作控件的总高度
        measuredHeight = (int) ((halfCircumference * 2) / Math.PI);
        //求出半径
        radius = (int) (halfCircumference / Math.PI);
        //控件宽度，这里支持weight
        //控件宽度，这里支持weight
        measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        //计算两条横线 和 选中项画笔的基线Y位置
        firstLineY = (measuredHeight - itemHeight) / 2.0F + (itemHeight / 11);
        secondLineY = (measuredHeight + itemHeight) / 2.0F - (itemHeight / 11);
        centerY = secondLineY - (itemHeight - maxTextHeight) / 2.0f - centerContentOffset;

        //初始化显示的item的position
        if (initPosition == -1) {
            if (isLoop) {
                initPosition = (adapter.getItemsCount() + 1) / 2;
            } else {
                initPosition = 0;
            }
        }
        preCurrentIndex = initPosition;
    }

    private int measureSize(int mode, int sizeExpect, int sizeActual) {
//        MeasureSpec.getMode()方法返回的结果有三种：
//        UNSPECIFIED：父节点对子节点的大小没有任何要求。
//        EXACTLY: 父节点要求其子节点的大小指定为某个确切的值。其子节点以及其他子孙节点都需要适应该大小。
//        AT MOST：父节点要求其子节点的大小不能超过某个最大值，其子节点以及其他子孙节点的大小都需要小于这个值
        int realSize;
        if (mode == MeasureSpec.EXACTLY) {
            realSize = sizeExpect;
        } else {
            realSize = sizeActual;
            if (mode == MeasureSpec.AT_MOST)
                realSize = Math.min(realSize, sizeExpect);
        }
        return realSize;
    }

    /**
     * 计算最大length的Text的宽高度
     */
    private void measureTextWidthHeight() {
        Rect rect = new Rect();
        for (int i = 0; i < adapter.getItemsCount(); i++) {
            String s1 = getContentText(adapter.getItem(i));
            paintCenterText.getTextBounds(s1, 0, s1.length(), rect);

            int textWidth = rect.width();

            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
            paintCenterText.getTextBounds("\u661F\u671F", 0, 2, rect); // 星期的字符编码（以它为标准高度）

            maxTextHeight = rect.height() + 2;

        }
        itemHeight = lineSpacingMultiplier * maxTextHeight;
        Log.d(TAG, "measureTextWidthHeight:: itemHeight:" + itemHeight + ",maxTextHeight:" + maxTextHeight + ",lineSpacingMultiplier:" + lineSpacingMultiplier);
    }

    public void smoothScroll(ACTION action) {//平滑滚动的实现
        cancelFuture();
        if (action == ACTION.FLING || action == ACTION.DRAG) {
            mOffset = (int) ((totalScrollY % itemHeight + itemHeight) % itemHeight);
            if ((float) mOffset > itemHeight / 2.0F) {//如果超过Item高度的一半，滚动到下一个Item去
                mOffset = (int) (itemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }
        //停止的时候，位置有偏移，不是全部都能正确停止到中间位置的，这里把文字位置挪回中间去
        mFuture = mExecutor.scheduleWithFixedDelay(new SmoothScrollTimerTask(this, mOffset), 0, 10, TimeUnit.MILLISECONDS);
    }

    public final void scrollBy(float velocityY) {//滚动惯性的实现
        cancelFuture();
        mFuture = mExecutor.scheduleWithFixedDelay(new InertiaTimerTask(this, velocityY), 0, VELOCITY_FLING, TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    /**
     * 设置是否循环滚动
     *
     * @param canLoop 是否循环
     */
    public final void setCanLoop(boolean canLoop) {
        isLoop = canLoop;
    }

    public final void setTypeface(Typeface font) {
        typeface = font;
        paintOuterText.setTypeface(typeface);
        paintCenterText.setTypeface(typeface);
    }

    public final void setTextSize(float size) {
        if (size > 0.0F) {
            textSize = (int) (context.getResources().getDisplayMetrics().density * size);
            paintOuterText.setTextSize(textSize);
            paintCenterText.setTextSize(textSize);
        }
    }

    public final void setCurrentItem(int currentItem) {
        this.initPosition = currentItem;
        totalScrollY = 0;//回归顶部，不然重设setCurrentItem的话位置会偏移的，就会显示出不对位置的数据
        invalidate();
    }

    public final void setOnItemPickListener(OnItemPickListener onItemPickListener) {
        this.onItemPickListener = onItemPickListener;
    }

    public final void setAdapter(WheelAdapter adapter) {
        this.adapter = adapter;
        remeasure();
        invalidate();
    }

    public final WheelAdapter getAdapter() {
        return adapter;
    }

    public final int getCurrentPosition() {
        return selectedPosition;
    }

    public final String getCurrentItem() {
        selectedItem = (String) adapter.getItem(selectedPosition);
        return selectedItem;
    }

    //handler 里调用
    public final void onItemPicked() {
        if (onItemPickListener != null) {
            postDelayed(new OnItemPickedRunnable(this, onItemPickListener), 200L);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (adapter == null) {
            return;
        }
        //可见的item数组
        @SuppressLint("DrawAllocation")
        String[] drawItemCount = new String[itemsVisible];
        //滚动的Y值高度除去每行Item的高度，得到滚动了多少个item，即change数
        change = (int) (totalScrollY / itemHeight);
        try {
            //滚动中实际的预选中的item(即经过了中间位置的item) ＝ 滑动前的位置 ＋ 滑动相对位置
            preCurrentIndex = initPosition + change % adapter.getItemsCount();
        } catch (ArithmeticException e) {
            Log.e("WheelView", "出错了！adapter.getItemsCount() == 0，联动数据不匹配");
        }
        if (!isLoop) { //不循环的情况
            if (preCurrentIndex < 0) {
                preCurrentIndex = 0;
            }
            if (preCurrentIndex > adapter.getItemsCount() - 1) {
                preCurrentIndex = adapter.getItemsCount() - 1;
            }
        } else {//循环
            if (preCurrentIndex < 0) {//举个例子：如果总数是5，preCurrentIndex ＝ －1，那么preCurrentIndex按循环来说，其实是0的上面，也就是4的位置
                preCurrentIndex = adapter.getItemsCount() + preCurrentIndex;
            }
            if (preCurrentIndex > adapter.getItemsCount() - 1) {//同理上面,自己脑补一下
                preCurrentIndex = preCurrentIndex - adapter.getItemsCount();
            }
        }
        //跟滚动流畅度有关，总滑动距离与每个item高度取余，即并不是一格格的滚动，每个item不一定滚到对应Rect里的，这个item对应格子的偏移值
        float itemHeightOffset = (totalScrollY % itemHeight);

        // 设置数组中每个元素的值
        int counter = 0;
        while (counter < itemsVisible) {
            int index = preCurrentIndex - (itemsVisible / 2 - counter);//索引值，即当前在控件中间的item看作数据源的中间，计算出相对源数据源的index值
            //判断是否循环，如果是循环数据源也使用相对循环的position获取对应的item值，如果不是循环则超出数据源范围使用""空白字符串填充，在界面上形成空白无数据的item项
            if (isLoop) {
                index = getLoopMappingIndex(index);
                drawItemCount[counter] = adapter.getItem(index);
            } else if (index < 0) {
                drawItemCount[counter] = "";
            } else if (index > adapter.getItemsCount() - 1) {
                drawItemCount[counter] = "";
            } else {
                drawItemCount[counter] = adapter.getItem(index);
            }

            counter++;

        }
        //设置线可见时绘制两条线
        if (lineConfig != null && lineConfig.isVisible()) {
            //float ratio  = lineConfig.getRatio();
            //绘制中间两条横线
            if (dividerType == LineConfig.DividerType.WRAP) {//横线长度仅包裹内容
                float startX;
                float endX;

                if (TextUtils.isEmpty(label)) {//隐藏Label的情况
                    startX = (measuredWidth - maxTextWidth) / 2 - 12;
                } else {
                    startX = (measuredWidth - maxTextWidth) / 4 - 12;
                }

                if (startX <= 0) {//如果超过了WheelView的边缘
                    startX = 10;
                }
                endX = measuredWidth - startX;
                canvas.drawLine(startX, firstLineY, endX, firstLineY, paintLine);
                canvas.drawLine(startX, secondLineY, endX, secondLineY, paintLine);
            } else if (dividerType == LineConfig.DividerType.CIRCLE) {
                //分割线为圆圈形状
                paintLine.setStyle(Paint.Style.STROKE);
                paintLine.setStrokeWidth(dividerWidth);
                float startX;
                float endX;
                if (TextUtils.isEmpty(label)) {//隐藏Label的情况
                    startX = (measuredWidth - maxTextWidth) / 2f - 12;
                } else {
                    startX = (measuredWidth - maxTextWidth) / 4f - 12;
                }
                if (startX <= 0) {//如果超过了WheelView的边缘
                    startX = 10;
                }
                endX = measuredWidth - startX;
                //半径始终以宽高中最大的来算
                float radius = Math.max((endX - startX), itemHeight) / 1.8f;
                canvas.drawCircle(measuredWidth / 2f, measuredHeight / 2f, radius, paintLine);
            } else if (dividerType == LineConfig.DividerType.LEFT_ROUND) {
                float startX;
                float endX;

                if (TextUtils.isEmpty(label)) {//隐藏Label的情况
                    startX = (measuredWidth - maxTextWidth) / 2 - 12;
                } else {
                    startX = (measuredWidth - maxTextWidth) / 4 - 12;
                }

                if (startX <= 0) {//如果超过了WheelView的边缘
                    startX = 10;
                }
                endX = measuredWidth - startX;
                canvas.drawLine(startX, firstLineY, measuredWidth, firstLineY, paintLine);
                canvas.drawLine(startX, secondLineY, measuredWidth, secondLineY, paintLine);

                //绘制左边圆弧
                RectF rectF = new RectF(startX - itemHeight / 2, firstLineY, startX + itemHeight / 2, secondLineY);
                canvas.drawArc(rectF, -90, -180, false, paintLine);
            } else if (dividerType == LineConfig.DividerType.RIGHT_ROUND) {
                float startX;
                float endX;

                if (TextUtils.isEmpty(label)) {//隐藏Label的情况
                    startX = (measuredWidth - maxTextWidth) / 2 - 12;
                } else {
                    startX = (measuredWidth - maxTextWidth) / 4 - 12;
                }

                if (startX <= 0) {//如果超过了WheelView的边缘
                    startX = 10;
                }
                endX = measuredWidth - startX;
                canvas.drawLine(0, firstLineY, endX, firstLineY, paintLine);
                canvas.drawLine(0, secondLineY, endX, secondLineY, paintLine);

                //绘制右边圆弧
                RectF rectF = new RectF(endX - itemHeight / 2, firstLineY, endX + itemHeight / 2, secondLineY);
                canvas.drawArc(rectF, -90, 180, false, paintLine);
            } else if (dividerType == LineConfig.DividerType.ROUND) {
                float startX;
                float endX;

                if (TextUtils.isEmpty(label)) {//隐藏Label的情况
                    startX = (measuredWidth - maxTextWidth) / 2 - 12;
                } else {
                    startX = (measuredWidth - maxTextWidth) / 4 - 12;
                }

                if (startX <= 0) {//如果超过了WheelView的边缘
                    startX = 10;
                }
                endX = measuredWidth - startX;
                canvas.drawLine(startX, firstLineY, endX, firstLineY, paintLine);
                canvas.drawLine(startX, secondLineY, endX, secondLineY, paintLine);

                //绘制左边圆弧
                RectF lRectF = new RectF(startX - itemHeight / 2, firstLineY, startX + itemHeight / 2, secondLineY);
                canvas.drawArc(lRectF, -90, -180, false, paintLine);

                //绘制右边圆弧
                RectF rRectF = new RectF(endX - itemHeight / 2, firstLineY, endX + itemHeight / 2, secondLineY);
                canvas.drawArc(rRectF, -90, 180, false, paintLine);
            } else {
                canvas.drawLine(0.0F, firstLineY, measuredWidth, firstLineY, paintLine);
                canvas.drawLine(0.0F, secondLineY, measuredWidth, secondLineY, paintLine);
            }
        }
        //只显示选中项Label文字的模式，并且Label文字不为空，则进行绘制
        if (onlyShowCenterLabel && !TextUtils.isEmpty(label)) {
            //绘制文字，靠右并留出空隙
            int drawRightContentStart = measuredWidth - getTextWidth(paintCenterText, label);
            canvas.drawText(label, drawRightContentStart - centerContentOffset, centerY, paintCenterText);
        }

        counter = 0;
        while (counter < itemsVisible) {
            canvas.save();
            // 弧长 L = itemHeight * counter - itemHeightOffset
            // 求弧度 α = L / r  (弧长/半径) [0,π]
            double radian = ((itemHeight * counter - itemHeightOffset)) / radius;
            // 弧度转换成角度(把半圆以Y轴为轴心向右转90度，使其处于第一象限及第四象限
            // angle [-90°,90°]
            float angle = (float) (90D - (radian / Math.PI) * 180D);//item第一项,从90度开始，逐渐递减到 -90度

            // 计算取值可能有细微偏差，保证负90°到90°以外的不绘制
            if (angle >= 90F || angle <= -90F) {
                canvas.restore();
            } else {
                //获取内容文字
                String contentText;
                //如果是label每项都显示的模式，并且item内容不为空、label 也不为空
                if (!onlyShowCenterLabel && !TextUtils.isEmpty(label) && !TextUtils.isEmpty(getContentText(drawItemCount[counter]))) {
                    //每个item 加label
                    contentText = getContentText(drawItemCount[counter]) + label;
                } else {
                    contentText = getContentText(drawItemCount[counter]);
                }

                reMeasureTextSize(contentText);
                //计算开始绘制的位置
                //drawCenterContentStart = measureContentStart(paintCenterText, contentText);
                //drawOutContentStart = measureContentStart(paintOuterText, contentText);

                measuredCenterContentStart(paintCenterText, contentText);
                measuredOutContentStart(paintOuterText, contentText);

                float translateY = (float) (radius - Math.cos(radian) * radius - (Math.sin(radian) * maxTextHeight) / 2D);
                //根据Math.sin(radian)来更改canvas坐标系原点，然后缩放画布，使得文字高度进行缩放，形成弧形3d视觉差
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                if (translateY <= firstLineY && maxTextHeight + translateY >= firstLineY) {
                    // 条目经过第一条线
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, firstLineY - translateY);
                    canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, firstLineY - translateY, measuredWidth, (int) (itemHeight));
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, drawCenterContentStart, maxTextHeight - centerContentOffset, paintCenterText);
                    canvas.restore();
                } else if (translateY <= secondLineY && maxTextHeight + translateY >= secondLineY) {
                    // 条目经过第二条线
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, secondLineY - translateY);
                    canvas.scale(1.0F, (float) Math.sin(radian) * 1.0F);
                    canvas.drawText(contentText, drawCenterContentStart, maxTextHeight - centerContentOffset, paintCenterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, secondLineY - translateY, measuredWidth, (int) (itemHeight));
                    canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                } else if (translateY >= firstLineY && maxTextHeight + translateY <= secondLineY) {
                    // 中间条目
                    //canvas.clipRect(0, 0, measuredWidth,   maxTextHeight);
                    //让文字居中
                    float Y = maxTextHeight - centerContentOffset;//因为圆弧角换算的向下取值，导致角度稍微有点偏差，加上画笔的基线会偏上，因此需要偏移量修正一下
                    //if (onlyShowCenterLabel && !TextUtils.isEmpty(label)) {
                    //    contentText += label;
                    //}
                    canvas.drawText(contentText, drawCenterContentStart, Y, paintCenterText);
                    selectedPosition = adapter.indexOf(drawItemCount[counter]);

                } else {
                    // 其他条目
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight * 3));
                    canvas.scale(1.0F, (float) Math.sin(radian) * SCALE_CONTENT);
                    Log.d(TAG, "onDraw:: drawOutContentStart:" + drawOutContentStart + ",contentText:" + contentText + ",itemHeight:" + itemHeight + ",radian:" + radian);
                    //canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.drawText(contentText, drawOutContentStart, maxTextHeight, paintOuterText);
                    canvas.restore();
                }
                canvas.restore();
                paintCenterText.setTextSize(textSize);
            }
            counter++;
        }

    }

    /**
     * 根据文字的长度 重新设置文字的大小 让其能完全显示
     *
     * @param contentText
     */
    private void reMeasureTextSize(String contentText) {
        Rect rect = new Rect();
        paintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
        int width = rect.width();
        int size = textSize;
        while (width > measuredWidth) {
            size--;
            //设置2条横线中间的文字大小
            paintCenterText.setTextSize(size);
            paintCenterText.getTextBounds(contentText, 0, contentText.length(), rect);
            width = rect.width();
        }
        //设置2条横线外面的文字大小
        paintOuterText.setTextSize(size);
    }


    //递归计算出对应的index
    private int getLoopMappingIndex(int index) {
        if (index < 0) {
            index = index + adapter.getItemsCount();
            index = getLoopMappingIndex(index);
        } else if (index > adapter.getItemsCount() - 1) {
            index = index - adapter.getItemsCount();
            index = getLoopMappingIndex(index);
        }
        return index;
    }

    /**
     * 根据传进来的对象获取getPickerViewText()方法，来获取需要显示的值
     *
     * @param item 数据源的item
     * @return 对应显示的字符串
     */
    private String getContentText(Object item) {
        if (item == null) {
            return "";
        } else if (item instanceof IPickerViewData) {
            return ((IPickerViewData) item).getPickerViewText();
        } else if (item instanceof Integer) {
            //如果为整形则最少保留两位数.
            return String.format(Locale.getDefault(), "%02d", (int) item);
        }
        return item.toString();
    }

    /**
     * 动态获取文本width
     */
    private int measureTextWidth(Paint paint, String text) {
        Rect rect = getRect(paint, text);
        int textWidth = rect.width();
        if (textWidth > maxTextWidth) {
            maxTextWidth = textWidth;
            return maxTextWidth;
        }
        return textWidth;
    }

    private Rect getRect(Paint paint, String text) {
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * 根据文本width动态画出文本的左右padding
     */
    private int measureContentStart(Paint paint, String text) {
        int baselineX = (measuredWidth - measureTextWidth(paint, text)) / 2;
        return baselineX - 4;
    }

    private void measuredCenterContentStart(Paint paint, String content) {
        Rect rect = new Rect();
        paintCenterText.getTextBounds(content, 0, content.length(), rect);
        switch (mGravity) {
            case Gravity.CENTER://显示内容居中
                if (isOptions || label == null || label.equals("") || !onlyShowCenterLabel) {
                    drawCenterContentStart = (int) ((measuredWidth - rect.width()) * 0.5) - 4;
                } else {//只显示中间label时，时间选择器内容偏左一点，留出空间绘制单位标签
                    drawCenterContentStart = (int) ((measuredWidth - rect.width()) * 0.25) - 4;
                }
                break;
            case Gravity.LEFT:
                //drawCenterContentStart = 0;
                drawCenterContentStart = measuredWidth / 4;
                break;
            case Gravity.RIGHT://添加偏移量
                //drawCenterContentStart = measuredWidth - rect.width() - (int) centerContentOffset;
                drawCenterContentStart = measuredWidth - measuredWidth / 2;
                break;
        }
    }

    private void measuredOutContentStart(Paint paint, String content) {
        Rect rect = new Rect();
        paintOuterText.getTextBounds(content, 0, content.length(), rect);

        switch (mGravity) {
            case Gravity.CENTER:
                if (isOptions || label == null || label.equals("") || !onlyShowCenterLabel) {
                    drawOutContentStart = (int) ((measuredWidth - rect.width()) * 0.5);
                } else {//只显示中间label时，时间选择器内容偏左一点，留出空间绘制单位标签
                    drawOutContentStart = (int) ((measuredWidth - rect.width()) * 0.25);
                }
                break;
            case Gravity.LEFT:
                //drawOutContentStart = 0;
                drawOutContentStart = measuredWidth / 4;
                break;
            case Gravity.RIGHT:
                //drawOutContentStart = measuredWidth - rect.width() - (int) centerContentOffset;
                drawOutContentStart = measuredWidth - measuredWidth / 2;
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
        Log.d(TAG, "onMeasure:: ");
        remeasure();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = gestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            //按下
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                cancelFuture();
                previousY = event.getRawY();
                break;
            //滑动中
            case MotionEvent.ACTION_MOVE:

                float dy = previousY - event.getRawY();
                previousY = event.getRawY();
                totalScrollY = totalScrollY + dy;

                // 边界处理。
                if (!isLoop) {
                    float top = -initPosition * itemHeight;
                    float bottom = (adapter.getItemsCount() - 1 - initPosition) * itemHeight;


                    if (totalScrollY - itemHeight * 0.25 < top) {
                        top = totalScrollY - dy;
                    } else if (totalScrollY + itemHeight * 0.25 > bottom) {
                        bottom = totalScrollY - dy;
                    }

                    if (totalScrollY < top) {
                        totalScrollY = (int) top;
                    } else if (totalScrollY > bottom) {
                        totalScrollY = (int) bottom;
                    }
                }
                break;
            //完成滑动，手指离开屏幕
            case MotionEvent.ACTION_UP:

            default:
                if (!eventConsumed) {//未消费掉事件

                    /**
                     * TODO<关于弧长的计算>
                     *
                     * 弧长公式： L = α*R
                     * 反余弦公式：arccos(cosα) = α
                     * 由于之前是有顺时针偏移90度，
                     * 所以实际弧度范围α2的值 ：α2 = π/2-α    （α=[0,π] α2 = [-π/2,π/2]）
                     * 根据正弦余弦转换公式 cosα = sin(π/2-α)
                     * 代入，得： cosα = sin(π/2-α) = sinα2 = (R - y) / R
                     * 所以弧长 L = arccos(cosα)*R = arccos((R - y) / R)*R
                     */

                    float y = event.getY();
                    double L = Math.acos((radius - y) / radius) * radius;
                    //item0 有一半是在不可见区域，所以需要加上 itemHeight / 2
                    int circlePosition = (int) ((L + itemHeight / 2) / itemHeight);
                    float extraOffset = (totalScrollY % itemHeight + itemHeight) % itemHeight;
                    //已滑动的弧长值
                    mOffset = (int) ((circlePosition - itemsVisible / 2) * itemHeight - extraOffset);

                    if ((System.currentTimeMillis() - startTime) > 120) {
                        // 处理拖拽事件
                        smoothScroll(ACTION.DRAG);
                    } else {
                        // 处理条目点击事件
                        smoothScroll(ACTION.CLICK);
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    /**
     * 获取Item个数
     *
     * @return item个数
     */
    public int getItemsCount() {
        return adapter != null ? adapter.getItemsCount() : 0;
    }

    /**
     * 附加在右边的单位字符串
     *
     * @param label 单位
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 附加在右边的单位字符串
     */
    public final void setLabel(String label, boolean onlyShowCenterLabel) {
        this.label = label;
        this.onlyShowCenterLabel = onlyShowCenterLabel;
    }

    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    public int getTextWidth(Paint paint, String str) {//计算文字宽度
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    private void setIsOptions(boolean options) {
        isOptions = options;
    }

    public void setLineConfig(LineConfig lineConfig) {
        if (null != lineConfig) {
            paintLine.setColor(lineConfig.getColor());
            paintLine.setAlpha(lineConfig.getAlpha());
            paintLine.setStrokeWidth(lineConfig.getThick());
            this.lineConfig = lineConfig;
        }

    }

    public void setUnSelectedTextColor(int textColorOut) {
        if (textColorOut != 0) {
            this.textColorOut = textColorOut;
            paintOuterText.setColor(this.textColorOut);
        }
    }

    public void setSelectedTextColor(int textColorCenter) {
        if (textColorCenter != 0) {
            this.textColorCenter = textColorCenter;
            paintCenterText.setColor(this.textColorCenter);
        }
    }

    public void setDividerColor(int dividerColor) {
        if (dividerColor != 0) {
            this.dividerColor = dividerColor;
            paintLine.setColor(this.dividerColor);
        }
    }

    public void setDividerType(LineConfig.DividerType dividerType) {
        this.dividerType = dividerType;
    }

    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        if (lineSpacingMultiplier != 0) {
            this.lineSpacingMultiplier = lineSpacingMultiplier;
            judgeLineSpace();
        }
    }
}