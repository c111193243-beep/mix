package com.example.drivesafe.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drivesafe.db.FatigueRecord;
import com.example.drivesafe.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminRecordAdapter extends RecyclerView.Adapter<AdminRecordAdapter.VH> {

    public interface Listener {
        void onEdit(FatigueRecord r);
        void onDelete(FatigueRecord r);
        void onRowClick(FatigueRecord r);
    }

    private final List<FatigueRecord> data = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public AdminRecordAdapter(Listener l) {
        this.listener = l;
    }

    public void submit(List<FatigueRecord> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public List<FatigueRecord> current() {
        return new ArrayList<>(data);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_record, parent, false); // ✅ 修正名稱
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        FatigueRecord r = data.get(pos);

        long ts = r.effectiveTime();
        h.tvDate.setText(fmt.format(new Date(ts)));

        // 例：ID 1 • Score 7.0 • HIGH • Note8
        String meta = String.format(
                Locale.getDefault(),
                "ID %d • Score %.1f%s%s",
                r.getId(),
                r.getScore(),
                isEmpty(r.getFatigueLevel()) ? "" : (" • " + r.getFatigueLevel()),
                isEmpty(r.getSourceDevice()) ? "" : (" • " + r.getSourceDevice())
        );
        h.tvMeta.setText(meta);

        h.btnEdit.setOnClickListener(v -> listener.onEdit(r));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        h.itemView.setOnClickListener(v -> listener.onRowClick(r));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvMeta;
        MaterialButton btnEdit, btnDelete;

        VH(@NonNull View v) {
            super(v);
            tvDate    = v.findViewById(R.id.tvDate);
            tvMeta    = v.findViewById(R.id.tvMeta);
            btnEdit   = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
