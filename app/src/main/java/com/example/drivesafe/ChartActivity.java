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
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChartActivity extends AppCompatActivity {

    // 自訂選單ID（不靠 R.id）
    private static final int ID_PICK_DATE = 10001;

    // UI
    private MaterialToolbar toolbar;
    private ChipGroup chipGroupRange;
    private Chip chip1w, chip1m, chip3m;
    private LineChart lineChart;
    private TextView tvAvg, tvMax, tvMin, tvEmptyHint, tvSelectedDate;
    private MaterialButton btnExport, btnShare;

    // Colors
    @ColorInt private int colorLine;
    @ColorInt private int colorFillTop;
    @ColorInt private int colorFillBottom;
    @ColorInt private int colorTextDark;
    @ColorInt private int colorGrid;
    @ColorInt private int colorHighlight;

    // Formats
    private final SimpleDateFormat dayFmt  = new SimpleDateFormat("MM/dd", Locale.getDefault());
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
        setupChips();
        setupButtons();

        // 預設載入 1 週
        setDataForRangeDays(7);

        // 若是從外部帶日期進來，可直接顯示當天（可選）
        if (getIntent() != null && getIntent().hasExtra("year")) {
            setDataForDate(
                    getIntent().getIntExtra("year", 1970),
                    getIntent().getIntExtra("month", 0),
                    getIntent().getIntExtra("day", 1)
            );
        }
    }

    private void bindViews() {
        toolbar        = findViewById(R.id.chartToolbar);
        chipGroupRange = findViewById(R.id.chipGroupRange);
        chip1w         = findViewById(R.id.chip_1w);
        chip1m         = findViewById(R.id.chip_1m);
        chip3m         = findViewById(R.id.chip_3m);

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

    /** 只負責把 Toolbar 設成 ActionBar + 返回鍵 */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 顯示左上返回
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        // 不在這裡加 menu；改由 onCreateOptionsMenu 負責
    }

    /** 在這裡建立右上角「選日期」按鈕（不需要任何 menu_chart.xml） */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem pick = menu.add(0, ID_PICK_DATE, 0, "選日期");
        pick.setIcon(android.R.drawable.ic_menu_my_calendar); // 系統日曆圖示
        pick.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    /** 處理返回鍵與「選日期」點擊 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == ID_PICK_DATE) {
            openDatePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        XAxis x = lineChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(colorTextDark);
        x.setTextSize(11f);
        x.setDrawAxisLine(false);
        x.setDrawGridLines(true);
        x.setGridColor(colorGrid);
        x.setGridDashedLine(new DashPathEffect(new float[]{6f, 6f}, 0));

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

    /** 週/月/季模式：X 軸顯示 MM/dd */
    private void setXAxisForRange() {
        XAxis x = lineChart.getXAxis();
        x.setGranularity(24f * 60f * 60f * 1000f); // 天
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis((long) value);
                return dayFmt.format(c.getTime());
            }
        });
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

    private void setupChips() {
        chipGroupRange.setSingleSelection(true);
        chipGroupRange.setOnCheckedStateChangeListener((group, list) -> {
            if (list == null || list.isEmpty()) return;
            int id = list.get(0);
            // 切回範圍模式 → 隱藏「當日」標籤
            if (tvSelectedDate != null) tvSelectedDate.setVisibility(View.GONE);
            if (id == R.id.chip_1w) setDataForRangeDays(7);
            else if (id == R.id.chip_1m) setDataForRangeDays(30);
            else if (id == R.id.chip_3m) setDataForRangeDays(90);
        });
    }

    private void setupButtons() {
        btnExport.setOnClickListener(v -> exportChartImage());
        btnShare.setOnClickListener(v -> shareChartImage());
    }

    /* ===== 範圍資料 ===== */

    private void setDataForRangeDays(int days) {
        setXAxisForRange();
        List<Entry> entries = loadFatigueEntries(days);

        if (entries == null || entries.isEmpty()) {
            showEmpty(true);
            lineChart.clear();
            tvAvg.setText("—");
            tvMax.setText("—");
            tvMin.setText("—");
            return;
        }

        showEmpty(false);
        LineDataSet ds = new LineDataSet(entries, "疲勞指數");
        styleDataSet(ds);
        LineData data = new LineData(ds);
        data.setDrawValues(false);
        lineChart.setData(data);
        lineChart.animateX(500);
        lineChart.invalidate();

        computeAndBindStats(entries);
    }

    /** 示範範圍資料（請換成你的資料來源） */
    private List<Entry> loadFatigueEntries(int days) {
        List<Entry> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = days - 1; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            float v = (float) (3.5 + Math.sin(i * 0.35f) * 2.0 + Math.random() * 2.5);
            v = Math.max(1f, Math.min(10f, v));
            list.add(new Entry(c.getTimeInMillis(), v));
        }
        return list;
    }

    /* ===== 當日資料 ===== */

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
        // 清掉範圍 chips 的選取狀態
        if (chipGroupRange != null) chipGroupRange.clearCheck();

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

        // 撈資料（這裡先用假資料：每 30 分鐘一筆）
        List<Entry> entries = loadDayEntries(startMs, endMs);

        // X 軸改成 HH:mm
        setXAxisForDay();

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
            tvSelectedDate.setVisibility(View.VISIBLE);
        }
        TextView title = findViewById(R.id.tvChartTitle);
        if (title != null) {
            title.setText(String.format(Locale.getDefault(), "疲勞指數（%s）", dateFmt.format(start.getTime())));
        }
    }

    /** 當日假資料：每 30 分鐘生成一筆 */
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
