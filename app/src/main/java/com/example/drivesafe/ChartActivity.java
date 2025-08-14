package com.example.drivesafe;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.DashPathEffect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    // UI
    private MaterialToolbar toolbar;
    private MaterialButton btnPickDate, btnExport, btnShare;
    private LineChart lineChart;
    private TextView tvAvg, tvMax, tvMin, tvEmptyHint, tvSelectedDate;

    // Colors
    @ColorInt private int colorLine;
    @ColorInt private int colorFillTop;
    @ColorInt private int colorFillBottom;
    @ColorInt private int colorTextDark;
    @ColorInt private int colorGrid;
    @ColorInt private int colorHighlight;

    // Formats
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        bindViews();
        bindColors();
        setupToolbar();
        setupChartAppearance();
        setupButtons();

        // 預設顯示「今天」
        Calendar today = Calendar.getInstance();
        setDataForDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
    }

    private void bindViews() {
        toolbar        = findViewById(R.id.chartToolbar);
        btnPickDate    = findViewById(R.id.btnPickDate);
        lineChart      = findViewById(R.id.lineChart);
        tvAvg          = findViewById(R.id.tvAvg);
        tvMax          = findViewById(R.id.tvMax);
        tvMin          = findViewById(R.id.tvMin);
        tvEmptyHint    = findViewById(R.id.tvEmptyHint);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnExport      = findViewById(R.id.btnExport);
        btnShare       = findViewById(R.id.btnShare);
    }

    private void bindColors() {
        colorLine       = getColorCompat(this, R.color.titaniumBlueGray);
        colorFillTop    = getColorCompat(this, R.color.titaniumBlueGray);
        colorFillBottom = getColorCompat(this, R.color.silverGray);
        colorTextDark   = getColorCompat(this, R.color.textDark);
        colorGrid       = getColorCompat(this, R.color.inputStrokeGray);
        colorHighlight  = getColorCompat(this, R.color.accentRed);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupChartAppearance() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setNoDataText("尚無圖表資料");
        lineChart.setNoDataTextColor(colorGrid);
        lineChart.setExtraTopOffset(8f);
        lineChart.setExtraBottomOffset(10f);
        lineChart.setExtraLeftOffset(10f);
        lineChart.setExtraRightOffset(16f);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setHardwareAccelerationEnabled(true);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        // X 軸樣式（當日用 HH:mm）
        XAxis x = lineChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(colorTextDark);
        x.setTextSize(11f);
        x.setDrawAxisLine(false);
        x.setDrawGridLines(true);
        x.setGridColor(colorGrid);
        x.setGridDashedLine(new DashPathEffect(new float[]{6f, 6f}, 0));
        setXAxisForDay(); // 當日模式

        // Y 軸
        YAxis yL = lineChart.getAxisLeft();
        yL.setAxisMinimum(0f);
        yL.setAxisMaximum(10f);
        yL.setLabelCount(6, true);
        yL.setTextColor(colorTextDark);
        yL.setTextSize(11f);
        yL.setDrawAxisLine(false);
        yL.setDrawGridLines(true);
        yL.setGridColor(colorGrid);
        yL.setGridDashedLine(new DashPathEffect(new float[]{6f, 6f}, 0));
        lineChart.getAxisRight().setEnabled(false);
    }

    /** 當日模式：X 軸顯示 HH:mm */
    private void setXAxisForDay() {
        XAxis x = lineChart.getXAxis();
        x.setGranularity(15f * 60f * 1000f); // 15 分鐘
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis((long) value);
                return timeFmt.format(c.getTime());
            }
        });
    }

    private void setupButtons() {
        btnPickDate.setOnClickListener(v -> openDatePicker());
        btnExport.setOnClickListener(v -> exportChartImage());
        btnShare.setOnClickListener(v -> shareChartImage());
    }

    /* ===== 日期選擇 & 當日資料 ===== */

    private void openDatePicker() {
        final Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (DatePicker view, int y, int m, int d) -> setDataForDate(y, m, d),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /** 載入某一日的資料並重繪（X 軸 HH:mm） */
    private void setDataForDate(int year, int month /*0-11*/, int day) {
        // 計算當天起訖毫秒
        Calendar start = Calendar.getInstance();
        start.set(year, month, day, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startMs = start.getTimeInMillis();

        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        long endMs = end.getTimeInMillis();

        // 這裡先用假資料：每 30 分鐘一筆；之後換成你 Room/Firestore 的真資料
        List<Entry> entries = loadDayEntries(startMs, endMs);

        setXAxisForDay(); // 確保 X 軸用 HH:mm

        if (entries == null || entries.isEmpty()) {
            showEmpty(true);
            lineChart.clear();
            tvAvg.setText("—");
            tvMax.setText("—");
            tvMin.setText("—");
        } else {
            showEmpty(false);
            LineDataSet ds = new LineDataSet(entries, "疲勞指數");
            styleDataSet(ds);
            LineData data = new LineData(ds);
            data.setDrawValues(false);
            lineChart.setData(data);
            lineChart.animateX(400);
            lineChart.invalidate();
            computeAndBindStats(entries);
        }

        // 顯示所選日期
        if (tvSelectedDate != null) {
            tvSelectedDate.setText("當日：" + dateFmt.format(start.getTime()));
        }
        TextView title = findViewById(R.id.tvChartTitle);
        if (title != null) {
            title.setText(String.format(Locale.getDefault(), "疲勞指數（%s）", dateFmt.format(start.getTime())));
        }
        if (btnPickDate != null) {
            btnPickDate.setText(dateFmt.format(start.getTime()));
        }
    }

    /** 當日假資料：每 30 分鐘生成一筆（1~10）*/
    private List<Entry> loadDayEntries(long startMs, long endMs) {
        List<Entry> list = new ArrayList<>();
        long step = 30L * 60L * 1000L; // 30 分鐘
        for (long t = startMs + step; t <= endMs - step; t += step) {
            float v = (float) (3.0 + Math.sin((t - startMs) / 2.5e6) * 2.0 + Math.random() * 2.0);
            if (v < 1f) v = 1f; if (v > 10f) v = 10f;
            list.add(new Entry(t, v));
        }
        return list;
    }

    /* ===== DataSet 外觀 ===== */

    private void styleDataSet(LineDataSet set) {
        set.setColor(colorLine);
        set.setLineWidth(2.3f);
        set.setDrawCircles(true);
        set.setCircleColor(colorLine);
        set.setCircleRadius(3.3f);
        set.setDrawCircleHole(false);

        // 平滑曲線
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.18f);

        // 高亮
        set.setHighlightEnabled(true);
        set.setHighLightColor(colorHighlight);
        set.setHighlightLineWidth(1.2f);
        set.setDrawHorizontalHighlightIndicator(false);

        // 陰影填充
        set.setDrawFilled(true);
        set.setFillDrawable(makeFillGradient());
    }

    private GradientDrawable makeFillGradient() {
        int top = adjustAlpha(colorFillTop, 60);
        int bottom = adjustAlpha(colorFillBottom, 0);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ top, bottom }
        );
        gd.setCornerRadius(0);
        return gd;
    }

    private void computeAndBindStats(List<Entry> entries) {
        float sum = 0f, max = -Float.MAX_VALUE, min = Float.MAX_VALUE;
        for (Entry e : entries) {
            float v = e.getY();
            sum += v;
            if (v > max) max = v;
            if (v < min) min = v;
        }
        float avg = sum / entries.size();
        tvAvg.setText(String.format(Locale.getDefault(), "%.1f", avg));
        tvMax.setText(String.format(Locale.getDefault(), "%.1f", max));
        tvMin.setText(String.format(Locale.getDefault(), "%.1f", min));
    }

    private void showEmpty(boolean show) {
        if (tvEmptyHint != null) tvEmptyHint.setVisibility(show ? View.VISIBLE : View.GONE);
        if (lineChart != null) lineChart.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    /* ===== 匯出 / 分享 ===== */

    private void exportChartImage() {
        Bitmap bmp = lineChart.getChartBitmap();
        if (bmp == null) {
            Toast.makeText(this, "沒有可匯出的圖表", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = saveBitmapToGallery(this, bmp, "fatigue_chart_" + System.currentTimeMillis() + ".png");
        Toast.makeText(this, uri != null ? "已匯出到相簿" : "匯出失敗", Toast.LENGTH_SHORT).show();
    }

    private void shareChartImage() {
        Bitmap bmp = lineChart.getChartBitmap();
        if (bmp == null) {
            Toast.makeText(this, "沒有可分享的圖表", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = saveBitmapToGallery(this, bmp, "share_fatigue_chart.png");
        if (uri == null) {
            Toast.makeText(this, "分享失敗", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "分享圖表"));
    }

    /* ===== Utils ===== */

    @ColorInt
    private static int getColorCompat(Context ctx, int resId) {
        return ContextCompat.getColor(ctx, resId);
    }

    @ColorInt
    private static int adjustAlpha(@ColorInt int color, int alpha /*0-255*/) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Nullable
    private static Uri saveBitmapToGallery(Context ctx, Bitmap bitmap, String displayName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DriveSafe");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
            Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                if (os == null) return null;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                ctx.getContentResolver().update(uri, values, null, null);
            }
            return uri;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
