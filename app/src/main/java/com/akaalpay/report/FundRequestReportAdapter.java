package com.akaalpay.report;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;

import java.util.ArrayList;

public class FundRequestReportAdapter extends RecyclerView.Adapter<FundRequestReportAdapter.ViewHolder> {

    Context context;
    ArrayList<FundRequestReportModel> list;

    public FundRequestReportAdapter(Context context, ArrayList<FundRequestReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_fund_request_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FundRequestReportModel model = list.get(position);

        holder.txtRequestId.setText(model.requestId);
        holder.txtAmount.setText("₹" + model.amount);
        holder.txtPaymentMode.setText(model.paymentMode);
        holder.txtBankName.setText("Bank : " + (model.bankName != null && !model.bankName.isEmpty() ? model.bankName : "--"));
        holder.txtAccount.setText("A/C : " + (model.accountNo != null && !model.accountNo.isEmpty() ? model.accountNo : "--"));
        holder.txtIfsc.setText("IFSC : " + (model.ifscCode != null && !model.ifscCode.isEmpty() ? model.ifscCode : "--"));
        holder.txtReceiptNo.setText("Receipt No : " + (model.receiptNo != null && !model.receiptNo.isEmpty() ? model.receiptNo : "--"));
        holder.txtRemark.setText("Remark : " + (model.remark != null && !model.remark.isEmpty() ? model.remark : "--"));
        holder.txtDate.setText(model.date);

        // Admin remark shown only if present (usually on rejection)
        if (model.adminRemark != null && !model.adminRemark.isEmpty()) {
            holder.txtAdminRemark.setVisibility(View.VISIBLE);
            holder.txtAdminRemark.setText("Admin Remark : " + model.adminRemark);
        } else {
            holder.txtAdminRemark.setVisibility(View.GONE);
        }

        // Status badge
        if (model.status == 1) {
            holder.txtStatus.setText("Approved");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_success);
        } else if (model.status == 2) {
            holder.txtStatus.setText("Rejected");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
        } else {
            holder.txtStatus.setText("Pending");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }


    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRequestId, txtStatus, txtAmount, txtPaymentMode;
        TextView txtBankName, txtAccount, txtIfsc, txtReceiptNo, txtRemark, txtAdminRemark;
        TextView txtDate;

        public ViewHolder(View itemView) {
            super(itemView);
            txtRequestId   = itemView.findViewById(R.id.txtRequestId);
            txtStatus      = itemView.findViewById(R.id.txtStatus);
            txtAmount      = itemView.findViewById(R.id.txtAmount);
            txtPaymentMode = itemView.findViewById(R.id.txtPaymentMode);
            txtBankName    = itemView.findViewById(R.id.txtBankName);
            txtAccount     = itemView.findViewById(R.id.txtAccount);
            txtIfsc        = itemView.findViewById(R.id.txtIfsc);
            txtReceiptNo   = itemView.findViewById(R.id.txtReceiptNo);
            txtRemark      = itemView.findViewById(R.id.txtRemark);
            txtAdminRemark = itemView.findViewById(R.id.txtAdminRemark);
            txtDate        = itemView.findViewById(R.id.txtDate);
//            txtViewReceipt = itemView.findViewById(R.id.txtViewReceipt);
        }
    }
}