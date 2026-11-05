package com.akaalpay.report;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;

import java.util.ArrayList;

public class FinoAepsReportAdapter extends RecyclerView.Adapter<FinoAepsReportAdapter.ViewHolder> {

    Context context;
    ArrayList<FinoAepsReportModel> list;

    public FinoAepsReportAdapter(Context context, ArrayList<FinoAepsReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_fino_aeps_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        FinoAepsReportModel model = list.get(position);

        holder.txtMobile.setText(model.mobile);
        holder.txtAadhar.setText(model.aadhar);
        holder.txtBank.setText(model.bank);
        holder.txtRRN.setText(model.rrn);
        holder.txtAck.setText(model.ackno);
        String txnType;

        switch (model.type) {

            case "BE":
                txnType = "Balance Enquiry";
                break;

            case "CW":
                txnType = "Cash Withdrawal";
                break;

            case "MS":
                txnType = "Mini Statement";
                break;

            case "CD":
                txnType = "Cash Deposit";
                break;

            default:
                txnType = "Not Defined";
                break;
        }

        holder.txtType.setText(txnType);
        holder.txtRef.setText(model.reference);

        // Date Format
        holder.txtDate.setText(model.date);

        // Status Logic
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


        // Open Receipt
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, FinoAepsReceiptActivity.class);
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