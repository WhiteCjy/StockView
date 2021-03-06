package ysn.com.stock.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

import java.util.List;
import java.util.Map;

import ysn.com.stock.R;
import ysn.com.stock.bean.IFenShi;
import ysn.com.stock.manager.FenShiDataManager;
import ysn.com.stock.manager.FiveDayFenShiDataManager;
import ysn.com.stock.utils.TimeUtils;

/**
 * @Author yangsanning
 * @ClassName FiveDayFiveDayFenShiView
 * @Description 五日分时
 * @Date 2020/5/7
 */
public class FiveDayFenShiView extends StockView {

    /**
     * 价格线宽度
     */
    private int priceStrokeWidth;

    /**
     * heartRadius: 心脏半径
     * heartDiameter: 心脏直径
     * HEART_INIT_ALPHA: 初始透明度
     * HEART_BEAT_RATE: 心率
     * HEART_BEAT_FRACTION_RATE: 心跳动画时间
     */
    private int heartRadius;
    private int heartDiameter;
    private int heartInitAlpha;
    private long heartBeatRate;
    private long heartBeatFractionRate;

    /**
     * isBeat: 是否跳动
     * beatFraction: 变化率
     */
    private boolean isBeat = false;
    private float beatFraction;
    private Paint heartPaint;
    private ValueAnimator beatAnimator;
    private Handler beatHandler = new Handler();
    private Runnable beatRunnable = new Runnable() {
        @Override
        public void run() {
            beatAnimator.start();
            invalidate();
            beatHandler.postDelayed(this, heartBeatRate);
        }
    };

    private boolean isEnabledBottomTable;
    private boolean isEnabledSlide;

    private Path pricePath;
    private Paint pricePaint;

    /**
     * 每个数据格的宽(5份)
     */
    private int dataWidth;

    FiveDayFenShiDataManager dataManager;

    public FiveDayFenShiView(Context context) {
        super(context);
    }

    public FiveDayFenShiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FiveDayFenShiView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FiveDayFenShiView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        dataManager = new FiveDayFenShiDataManager(getColumnCount(), decimalFormat);
    }

    @Override
    protected void initAttr(AttributeSet attrs) {
        super.initAttr(attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FiveDayFenShiView);

        priceStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.FiveDayFenShiView_fdfsv_price_stroke_width, 2);

        heartRadius = typedArray.getDimensionPixelSize(R.styleable.FiveDayFenShiView_fdfsv_heart_radius, 5);
        heartDiameter = typedArray.getDimensionPixelSize(R.styleable.FiveDayFenShiView_fdfsv_heart_diameter, 40);
        heartInitAlpha = typedArray.getInteger(R.styleable.FiveDayFenShiView_fdfsv_heart_init_alpha, 255);
        heartBeatRate = typedArray.getInteger(R.styleable.FiveDayFenShiView_fdfsv_heart_beat_rate, 2000);
        heartBeatFractionRate = typedArray.getInteger(R.styleable.FiveDayFenShiView_fdfsv_heart_beat_fraction_rate, 2000);

        isEnabledBottomTable = typedArray.getBoolean(R.styleable.FiveDayFenShiView_fdfsv_is_enabled_bottom_tab, Boolean.FALSE);
        isEnabledSlide = typedArray.getBoolean(R.styleable.FiveDayFenShiView_fdfsv_is_enabled_slide, Boolean.FALSE);

        typedArray.recycle();
    }

    @Override
    protected void initPaint() {
        super.initPaint();
        pricePath = new Path();
        pricePaint = new Paint();
        pricePaint.setColor(getColor(R.color.stock_price_line));
        pricePaint.setAntiAlias(true);
        pricePaint.setStyle(Paint.Style.STROKE);
        pricePaint.setStrokeWidth(priceStrokeWidth);

        heartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heartPaint.setAntiAlias(true);
        beatAnimator = ValueAnimator.ofFloat(0, 1f).setDuration(heartBeatFractionRate);
        beatAnimator.addUpdateListener(animation -> {
            beatFraction = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public boolean hasBottomTable() {
        return isEnabledBottomTable;
    }

    @Override
    protected int getColumnCount() {
        return 5;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        dataWidth = getViewWidth() / getColumnCount();
    }

    @Override
    protected void onTimeTextDraw(Canvas canvas) {
        super.onTimeTextDraw(canvas);
        textPaint.setColor(getColor(R.color.stock_text_title));

        // 绘制时间坐标
        for (int i = 0; i < dataManager.dateList.size(); i++) {
            String text = TimeUtils.formatDay(dataManager.dateList.get(i).getTime());
            textPaint.getTextBounds(text, (0), text.length(), textRect);
            canvas.drawText(text, (dataWidth * i + (dataWidth - textRect.width()) / 2f), getTimeTextY(), textPaint);
        }
    }

    @Override
    protected void onChildDraw(Canvas canvas) {
        super.onChildDraw(canvas);

        if (dataManager.dataManagerList.isEmpty()) {
            return;
        }

        // 绘制坐标峰值
        drawXYText(canvas);

        // 绘制价格曲线、闪烁点
        for (Map.Entry<Integer, FenShiDataManager> entry : dataManager.dataManagerMap.entrySet()) {
            drawPriceLine(canvas, entry.getValue(), entry.getKey());
        }

        if (hasBottomTable()) {
            // 绘制下表格坐标
            drawBottomXYText(canvas);

            // 绘制柱形
            for (Map.Entry<Integer, FenShiDataManager> entry : dataManager.dataManagerMap.entrySet()) {
                drawPillar(canvas, entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * 绘制坐标峰值
     */
    private void drawXYText(Canvas canvas) {
        // 价格最大值
        String text = decimalFormat.format(dataManager.maxPrice);
        textPaint.setColor(getColor(R.color.stock_red));
        textPaint.getTextBounds(text, (0), text.length(), textRect);
        float textMargin = getTextMargin();
        float y = (getTopTableMaxY() + textRect.height() + textMargin);
        canvas.drawText(text, textMargin, y, textPaint);

        // 增幅
        text = "+" + dataManager.percent;
        textPaint.getTextBounds(text, 0, text.length(), textRect);
        canvas.drawText(text, (viewWidth - textRect.width() - textMargin), y, textPaint);

        // 价格最小值
        textPaint.setColor(getColor(R.color.stock_green));
        y = getTopTableMinY() - textMargin;
        canvas.drawText(decimalFormat.format(dataManager.minPrice), textMargin, y, textPaint);

        // 减幅
        text = "-" + dataManager.percent;
        textPaint.setColor(getColor(R.color.stock_green));
        textPaint.getTextBounds(text, 0, text.length(), textRect);
        canvas.drawText(text, (viewWidth - textRect.width() - textMargin), y, textPaint);

        // 中间坐标
        textPaint.setColor(getColor(R.color.stock_text_title));
        text = decimalFormat.format(dataManager.lastClose);
        canvas.drawText(text, textMargin, (-(topTableHeight - textRect.height()) / 2f), textPaint);
    }

    /**
     * 绘制价格曲线、闪烁点
     */
    private void drawPriceLine(Canvas canvas, FenShiDataManager dataManager, int position) {
        float x = tableMargin + dataWidth * position;
        float y = getY(dataManager.getPrice(0));
        pricePath.moveTo(x, y);
        for (int i = 1; i < dataManager.priceSize(); i++) {
            x = getX(i) + dataWidth * position;
            y = getY(dataManager.getPrice(i));
            pricePath.lineTo(x, y);

            if (position == getColumnCount() - 1 && isBeat && dataManager.isLastPrice(i)) {
                //绘制扩散圆
                heartPaint.setColor(getColor(R.color.stock_price_line));
                heartPaint.setAlpha((int) (heartInitAlpha - heartInitAlpha * beatFraction));
                canvas.drawCircle(x, y, (heartRadius + heartDiameter * beatFraction), heartPaint);
                // 绘制中心圆
                heartPaint.setAlpha(255);
                heartPaint.setColor(getColor(R.color.stock_price_line));
                canvas.drawCircle(x, y, heartRadius, heartPaint);
            }
        }

        canvas.drawPath(pricePath, pricePaint);

        pricePath.reset();
    }

    /**
     * 获取价格线的y轴坐标
     *
     * @param price 当前价格
     * @return 价格线的y轴坐标
     */
    private float getY(float price) {
        return getY(price, dataManager.minPrice, dataManager.maxPrice);
    }

    /**
     * 获取x轴坐标
     *
     * @param position 当前position
     * @return x轴坐标
     */
    @Override
    public float getX(int position) {
        return getColumnX(((dataWidth) / (float) totalCount), position);
    }


    /**
     * 绘制下表格坐标
     */
    private void drawBottomXYText(Canvas canvas) {
        // 下表格最大量
        textPaint.getTextBounds(dataManager.maxVolumeString, 0, dataManager.maxVolumeString.length(), textRect);
        float x = viewWidth - tableMargin - xYTextMargin - textRect.width();
        canvas.drawText(dataManager.maxVolumeString, x, (getBottomTableMinY() + textRect.height() + xYTextMargin), textPaint);

        // 下表格中间值
        textPaint.getTextBounds(dataManager.centreVolumeString, 0, dataManager.centreVolumeString.length(), textRect);
        canvas.drawText(dataManager.centreVolumeString, x, (getBottomTableMinY() + (bottomTableHeight + textRect.height()) / 2), textPaint);
    }

    /**
     * 绘制柱形
     * columnSpace: 宽 - 边距 - 柱子间距(1f)
     */
    private void drawPillar(Canvas canvas, FenShiDataManager dataManager, int position) {
        float columnSpace = (dataWidth - (totalCount * 1f)) / totalCount;
        Paint paint = new Paint();
        paint.setStrokeWidth(columnSpace);
        for (int i = 0; i < dataManager.priceSize() - 1; i++) {
            if (i == 0) {
                paint.setColor(getColor(dataManager.getPrice(i) >= dataManager.lastClose ? R.color.stock_red : R.color.stock_green));
            } else {
                paint.setColor(getColor(dataManager.getPrice(i) >= dataManager.getPrice(i - 1) ? R.color.stock_red : R.color.stock_green));
            }
            float lineX = tableMargin + (columnSpace * i) + (i * 1f) + 1 + dataWidth * position;
            float maxHeight = (bottomTableHeight - 1) * 0.95f;
            float stopY = getBottomTableMaxY() - (dataManager.getVolume(i) * maxHeight) / dataManager.maxVolume;
            canvas.drawLine(lineX, getBottomTableMaxY(), lineX, stopY, paint);
        }
        paint.reset();
    }

    public <T extends IFenShi> void seftData(List<T> fenShiList) {
        dataManager.setData(fenShiList);
        invalidate();
        startBeat();
    }

    public void startBeat() {
        stopBeat();
        if (dataManager.getLastDataManager().isTimeNotEmpty() && isBeatTime()) {
            isBeat = true;
            beatHandler.post(beatRunnable);
        }
    }

    private boolean isBeatTime() {
        String lastTime = dataManager.getLastDataManager().getLastTime();
        return !"11:30".equals(lastTime) && !"15:00".equals(lastTime);
    }

    public void stopBeat() {
        isBeat = false;
        beatHandler.removeCallbacks(beatRunnable);
    }
}
