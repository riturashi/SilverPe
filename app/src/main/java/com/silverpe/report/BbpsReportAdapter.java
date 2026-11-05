package com.silverpe.report;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.silverpe.R;

import java.util.ArrayList;

public class BbpsReportAdapter extends RecyclerView.Adapter<BbpsReportAdapter.ViewHolder> {

    Context context;
    ArrayList<BbpsReportModel> list;

    public BbpsReportAdapter(Context context, ArrayList<BbpsReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_bbps_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        BbpsReportModel model = list.get(position);

        // 👉 Customer Number (canumber)
        holder.txtMobile.setText(model.mobile);

        // 👉 Biller Name
        holder.txtAadhar.setText(model.aadhar);

        // 👉 Reference ID
        holder.txtRRN.setText(model.rrn);

        // 👉 Ack (handle null)
        holder.txtAck.setText(model.ackno == null ? "-" : model.ackno);

        // 👉 Type (category or biller name)
        holder.txtType.setText(model.type);

        // 👉 Reference
        holder.txtRef.setText(model.reference);

        // 👉 Date
        holder.txtDate.setText(model.date);

        // ✅ Status Logic (BBPS)
        if (model.status == 1) {

            holder.txtStatus.setText("Success");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_success);

        } else if (model.status == 2) {

            holder.txtStatus.setText("Pending");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);


        } else {

            holder.txtStatus.setText("Failed");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);

        }

        // 👉 Click (same hi rakha)
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, BbpsReceiptActivity.class);
            intent.putExtra("id", model.id);
            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtStatus, txtDate;
        TextView txtMobile, txtAadhar, txtBank;
        TextView txtRRN, txtAck, txtType, txtRef;

        public ViewHolder(View itemView) {
            super(itemView);

            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtDate = itemView.findViewById(R.id.txtDate);

            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtAadhar = itemView.findViewById(R.id.txtAadhar);
            txtBank = itemView.findViewById(R.id.txtBank);

            txtRRN = itemView.findViewById(R.id.txtRRN);
            txtAck = itemView.findViewById(R.id.txtAck);
            txtType = itemView.findViewById(R.id.txtType);
            txtRef = itemView.findViewById(R.id.txtRef);
        }
    }
}