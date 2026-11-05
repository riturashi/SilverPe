package com.akaalpay.payout;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.akaalpay.R;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class BankAdapter extends RecyclerView.Adapter<BankAdapter.VH> {

    private final List<BankItem> list;
    private final OnBankClickListener listener;

    public interface OnBankClickListener {
        void onTransfer(BankItem item);
    }

    // Data Model Class inside Adapter for easy access
    public static class BankItem {
        public String bank_name, account_no, holder_name, bene_id, ifsc;
        public int bank_id, validation_status;

        public BankItem(String b_name, int b_id, String b_id_str, String acc, String ifsc_code, String h_name, int status) {
            this.bank_name = b_name;
            this.bank_id = b_id;
            this.bene_id = b_id_str;
            this.account_no = acc;
            this.ifsc = ifsc_code;
            this.holder_name = h_name;
            this.validation_status = status;
        }
    }

    public BankAdapter(List<BankItem> list, OnBankClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bank_account, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BankItem item = list.get(position);

        holder.tvBankName.setText(item.bank_name);
        holder.tvAccNo.setText("A/c: " + item.account_no);
        holder.tvIfsc.setText("IFSC: " + item.ifsc);
        holder.tvHolderName.setText(item.holder_name);

        // --- Status Logic (1=Verified, 2=Pending, 3=Rejected) ---
        if (item.validation_status == 1) {
            holder.tvStatus.setText("Status: Verified");
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Green
            holder.btnTransfer.setVisibility(View.VISIBLE); // Sirf Verified pe button dikhega
        }
        else if (item.validation_status == 2) {
            holder.tvStatus.setText("Status: Pending");
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Yellow/Amber
            holder.btnTransfer.setVisibility(View.GONE); // Pending pe hide
        }
        else if (item.validation_status == 3) {
            holder.tvStatus.setText("Status: Rejected");
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            holder.btnTransfer.setVisibility(View.GONE); // Rejected pe hide
        }
        else {
            holder.tvStatus.setText("Status: Unknown");
            holder.tvStatus.setTextColor(Color.GRAY);
            holder.btnTransfer.setVisibility(View.GONE);
        }

        holder.btnTransfer.setOnClickListener(v -> listener.onTransfer(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder class with all IDs linked
    static class VH extends RecyclerView.ViewHolder {
        TextView tvBankName, tvAccNo, tvIfsc, tvHolderName, tvStatus;
        MaterialButton btnTransfer;

        VH(View v) {
            super(v);
            tvBankName = v.findViewById(R.id.tvBankName);
            tvAccNo = v.findViewById(R.id.tvAccNo);
            tvIfsc = v.findViewById(R.id.tvIfsc);
            tvHolderName = v.findViewById(R.id.tvHolderName);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnTransfer = v.findViewById(R.id.btnTransfer);
        }
    }
}