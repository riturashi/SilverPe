package com.akaalpay.report;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;

import java.util.ArrayList;

public class LeadReportAdapter extends RecyclerView.Adapter<LeadReportAdapter.ViewHolder> {

    Context context;
    ArrayList<LeadReportModel> list;

    public LeadReportAdapter(Context context, ArrayList<LeadReportModel> list) {
        this.context = context;
        this.list    = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_lead_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        LeadReportModel model = list.get(position);

        holder.txtRefId.setText(model.ref_id);
        holder.txtName.setText(model.name);
        holder.txtMobile.setText(model.mobile_no);
        holder.txtEmail.setText(model.email);
        holder.txtProduct.setText(model.product);
        holder.txtExecStatus.setText(model.executive_status != null ? model.executive_status : "-");
        holder.txtRemarks.setText(model.remarks != null ? model.remarks : "-");
        holder.txtMessage.setText(model.message);
        holder.txtDate.setText(model.date);

        // Status badge
        if (model.status == 1) {
            holder.txtStatus.setText("Success");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_success);
        } else if (model.status == 2) {
            holder.txtStatus.setText("Pending");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (model.status == 3) {
            holder.txtStatus.setText("Processing");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else {
            holder.txtStatus.setText("Failed");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
        }

        // No click — no receipt
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtStatus, txtDate, txtRefId, txtName,
                txtMobile, txtEmail, txtProduct,
                txtExecStatus, txtRemarks, txtMessage;

        public ViewHolder(View itemView) {
            super(itemView);
            txtStatus     = itemView.findViewById(R.id.txtStatus);
            txtDate       = itemView.findViewById(R.id.txtDate);
            txtRefId      = itemView.findViewById(R.id.txtRefId);
            txtName       = itemView.findViewById(R.id.txtName);
            txtMobile     = itemView.findViewById(R.id.txtMobile);
            txtEmail      = itemView.findViewById(R.id.txtEmail);
            txtProduct    = itemView.findViewById(R.id.txtProduct);
            txtExecStatus = itemView.findViewById(R.id.txtExecStatus);
            txtRemarks    = itemView.findViewById(R.id.txtRemarks);
            txtMessage    = itemView.findViewById(R.id.txtMessage);
        }
    }
}