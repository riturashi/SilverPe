package com.silverpe.recharge;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.silverpe.R;

import java.util.ArrayList;

public class RechargeReportAdapter extends RecyclerView.Adapter<RechargeReportAdapter.ViewHolder> {

    Context context;
    ArrayList<RechargeReportModel> list;

    public RechargeReportAdapter(Context context, ArrayList<RechargeReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_recharge_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RechargeReportModel model = list.get(position);

        holder.txtOperator.setText(model.operatorName != null && !model.operatorName.equals("null")
                ? model.operatorName : "Unknown");
        holder.txtMobile.setText(model.mobileNo);
        holder.txtAmount.setText("₹" + model.amount);
        holder.txtDate.setText(model.date);
        holder.txtMessage.setText(model.responseMessage);

        // Refunded Badge
        if (model.refunded != null && model.refunded.equals("1")) {
            holder.txtRefunded.setVisibility(View.VISIBLE);
        } else {
            holder.txtRefunded.setVisibility(View.GONE);
        }

        // Status
        if (model.rechargeStatus == 1) {
            holder.txtStatus.setText("Success");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_success);
        } else if (model.rechargeStatus == 0) {
            holder.txtStatus.setText("Failed");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
        } else {
            holder.txtStatus.setText("Pending");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // Open Receipt
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RechargeReceiptActivity.class);
            intent.putExtra("id", model.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStatus, txtDate, txtOperator, txtMobile;
        TextView txtAmount, txtMessage, txtRefunded;

        public ViewHolder(View itemView) {
            super(itemView);
            txtStatus   = itemView.findViewById(R.id.txtStatus);
            txtDate     = itemView.findViewById(R.id.txtDate);
            txtOperator = itemView.findViewById(R.id.txtOperator);
            txtMobile   = itemView.findViewById(R.id.txtMobile);
            txtAmount   = itemView.findViewById(R.id.txtAmount);
            txtMessage  = itemView.findViewById(R.id.txtMessage);
            txtRefunded = itemView.findViewById(R.id.txtRefunded);
        }
    }
}