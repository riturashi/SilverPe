package com.akaalpay.report;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;

import java.util.ArrayList;

public class UatmReportAdapter extends RecyclerView.Adapter<UatmReportAdapter.ViewHolder> {

    Context context;
    ArrayList<UatmReportModel> list;

    public UatmReportAdapter(Context context, ArrayList<UatmReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_uatm_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        UatmReportModel model = list.get(position);

        holder.txtMobile.setText(model.mobile);
        holder.txtAmount.setText("₹ " + model.amount);
        holder.txtRef.setText(model.externalRef);
        holder.txtIpayId.setText(model.ipayId);

        holder.txtQrStatus.setText(
                TextUtils.isEmpty(model.qrStatus) ? "-" : model.qrStatus
        );

        holder.txtPayerName.setText(
                TextUtils.isEmpty(model.payerName) ? "-" : model.payerName
        );

        holder.txtPayerVpa.setText(
                TextUtils.isEmpty(model.payerVpa) ? "-" : model.payerVpa
        );

        // Date Format
        holder.txtDate.setText(model.date);

        // Status Logic - same convention as AEPS (1=Success, 0=Failed, else Pending)
        if (model.status == 1) {

            holder.txtStatus.setText("Success");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_success);

        } else if (model.status == 0) {

            holder.txtStatus.setText("Failed");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);

        } else {

            holder.txtStatus.setText("Pending");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);

        }

        // Open Receipt
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, UpiAtmReceiptActivity.class);
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
        TextView txtMobile, txtAmount;
        TextView txtRef, txtIpayId, txtQrStatus;
        TextView txtPayerName, txtPayerVpa;

        public ViewHolder(View itemView) {
            super(itemView);

            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtDate = itemView.findViewById(R.id.txtDate);

            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtAmount = itemView.findViewById(R.id.txtAmount);

            txtRef = itemView.findViewById(R.id.txtRef);
            txtIpayId = itemView.findViewById(R.id.txtIpayId);
            txtQrStatus = itemView.findViewById(R.id.txtQrStatus);

            txtPayerName = itemView.findViewById(R.id.txtPayerName);
            txtPayerVpa = itemView.findViewById(R.id.txtPayerVpa);
        }
    }
}