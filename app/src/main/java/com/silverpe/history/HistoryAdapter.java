package com.silverpe.history;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverpe.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final Context context;
    private final List<HistoryModel> list;

    public HistoryAdapter(Context context, List<HistoryModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {

        HistoryModel m = list.get(i);

        // Service name & narration
        h.tvService.setText(m.serviceName);
        h.tvNarration.setText(m.narration);
        h.tvDate.setText(m.date);

        // 🔼 BEFORE BALANCE (amount ke upar)
        h.tvBeforeBalance.setText("Before Balance : ₹ " + m.beforeBalance);

        // 💰 AMOUNT (credit / debit)
        // OnBindViewHolder mein ye line add karein color professional dikhane ke liye
        if ("C".equalsIgnoreCase(m.type)) {
            h.tvAmount.setText("+ ₹ " + m.amount);
            h.tvAmount.setTextColor(Color.parseColor("#2E7D32")); // Bold Green
        } else {
            h.tvAmount.setText("- ₹ " + m.amount);
            h.tvAmount.setTextColor(Color.parseColor("#C62828")); // Bold Red
        }

        // 🔽 UPDATED BALANCE (amount ke niche)
        h.tvUpdatedBalance.setText("Updated Balance : ₹ " + m.updatedBalance);

        // Service logo
        Glide.with(context)
                .load(m.serviceImage)
                .placeholder(R.drawable.ic_wallet)
                .error(R.drawable.ic_wallet)
                .into(h.imgService);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= VIEW HOLDER =================
    static class VH extends RecyclerView.ViewHolder {

        ImageView imgService;
        TextView tvService, tvNarration, tvDate;
        TextView tvBeforeBalance, tvAmount, tvUpdatedBalance;

        VH(@NonNull View v) {
            super(v);

            imgService = v.findViewById(R.id.imgService);
            tvService = v.findViewById(R.id.tvService);
            tvNarration = v.findViewById(R.id.tvNarration);
            tvDate = v.findViewById(R.id.tvDate);

            tvBeforeBalance = v.findViewById(R.id.tvBeforeBalance);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvUpdatedBalance = v.findViewById(R.id.tvUpdatedBalance);
        }
    }
}