package com.silverpe.recharge;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.silverpe.R;
import org.json.JSONObject;
import java.util.List;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.VH> {
    private final List<JSONObject> list;
    private final PlanBottomSheet.OnPlanSelect listener;
    private final PlanBottomSheet sheet;

    public PlanAdapter(List<JSONObject> list, PlanBottomSheet.OnPlanSelect listener, PlanBottomSheet sheet) {
        this.list = list;
        this.listener = listener;
        this.sheet = sheet;
    }

    public void updateList(List<JSONObject> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_plan, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        JSONObject obj = list.get(pos);
        String rs = obj.optString("rs", "0");
        h.tvAmount.setText("₹" + rs);
        h.tvValidity.setText(obj.optString("validity", "N/A"));
        h.tvDesc.setText(obj.optString("desc", ""));
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(rs);
            sheet.dismiss();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvValidity, tvDesc;
        VH(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvValidity = itemView.findViewById(R.id.tvValidity);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }
    }
}