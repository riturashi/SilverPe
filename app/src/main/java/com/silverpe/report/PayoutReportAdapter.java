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

public class PayoutReportAdapter extends RecyclerView.Adapter<PayoutReportAdapter.ViewHolder> {

    Context context;
    ArrayList<PayoutReportModel> list;

    public PayoutReportAdapter(Context context, ArrayList<PayoutReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_payout_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        PayoutReportModel model = list.get(position);

        holder.txtMobile.setText(model.account_no);
        holder.txtAadhar.setText(model.account_holder);
        holder.txtRRN.setText(model.ifsc_code);
        holder.txtAck.setText(model.ackno);
        holder.txtRef.setText(model.reference_id);

        holder.txtType.setText("₹ " + model.txn_amount);
        holder.txtDate.setText(model.date);

        // Status
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
            Intent intent = new Intent(context, PayoutReceiptActivity.class);
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
        TextView txtMobile, txtAadhar;
        TextView txtRRN, txtAck, txtType, txtRef;

        public ViewHolder(View itemView) {
            super(itemView);

            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtDate = itemView.findViewById(R.id.txtDate);

            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtAadhar = itemView.findViewById(R.id.txtAadhar);

            txtRRN = itemView.findViewById(R.id.txtRRN);
            txtAck = itemView.findViewById(R.id.txtAck);
            txtType = itemView.findViewById(R.id.txtType);
            txtRef = itemView.findViewById(R.id.txtRef);
        }
    }
}