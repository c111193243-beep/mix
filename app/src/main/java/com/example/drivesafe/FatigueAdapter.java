package com.example.drivesafe.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drivesafe.db.FatigueRecord;

import com.example.drivesafe.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 列表：疲勞記錄 */
public class FatigueAdapter extends RecyclerView.Adapter<FatigueAdapter.ViewHolder> {

    private final List<FatigueRecord> data = new ArrayList<>();
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(View v, int position, FatigueRecord record);
    }
    public interface OnItemLongClickListener {
        boolean onItemLongClick(View v, int position, FatigueRecord record);
    }

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public FatigueAdapter() {
        setHasStableIds(true);
    }

    public FatigueAdapter(@NonNull List<FatigueRecord> initial) {
        setHasStableIds(true);
        setItems(initial);
    }

    // ====== 資料操作 ======
    public void setItems(@NonNull List<FatigueRecord> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    public void addItems(@NonNull List<FatigueRecord> items) {
        int start = data.size();
        data.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    public void addItem(@NonNull FatigueRecord item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    /** 讓外部可用 adapter.getItem(pos) */
    public FatigueRecord getItem(int position) {
        if (position < 0 || position >= data.size()) return null;
        return data.get(position);
    }

    @Override public long getItemId(int position) {
        FatigueRecord r = getItem(position);
        return (r != null) ? r.getId() : RecyclerView.NO_ID;
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
    public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longClickListener = l; }

    // ====== Adapter ======
    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fatigue_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FatigueRecord r = getItem(position);
        if (r == null) return;

        h.tvScore.setText(String.format(Locale.getDefault(), "%.2f", r.getScore()));
        long t = r.effectiveTime();
        h.tvTime.setText((t > 0) ? timeFmt.format(new Date(t)) : "—");
        h.tvMeta.setText(String.format(
                Locale.getDefault(),
                "ID %d • %s",
                r.getId(),
                r.isSynced() ? "Synced" : "Not synced"));

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(v, h.getBindingAdapterPosition(), r);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(v, h.getBindingAdapterPosition(), r);
            }
            return false;
        });
    }

    @Override public int getItemCount() { return data.size(); }

    // ====== ViewHolder ======
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvScore;
        final TextView tvTime;
        final TextView tvMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvTime  = itemView.findViewById(R.id.tvTime);
            tvMeta  = itemView.findViewById(R.id.tvMeta);
        }
    }
}
