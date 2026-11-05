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

public class ExpressDmtReportAdapter extends RecyclerView.Adapter<ExpressDmtReportAdapter.ViewHolder> {

    Context context;
    ArrayList<ExpressDmtReportModel> list;

    public ExpressDmtReportAdapter(Context context, ArrayList<ExpressDmtReportModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_express_dmt_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ExpressDmtReportModel model = list.get(position);

        holder.txtMobile.setText(model.mobile);
        holder.txtBeneName.setText(model.beneName != null ? model.beneName : "--");
        holder.txtIfsc.setText(model.ifsc != null ? model.ifsc : "--");
        holder.txtAccount.setText(model.accountNo);
        holder.txtUtr.setText(model.utr != null ? model.utr : "--");
        holder.txtAmount.setText("₹" + model.amount);
        holder.txtDate.setText(model.date);
        holder.txtTxnType.setText(model.txnType);

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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ExpressDmtReceiptActivity.class);
            intent.putExtra("id", model.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStatus, txtDate, txtMobile, txtBeneName;
        TextView txtIfsc, txtAccount, txtUtr, txtAmount, txtTxnType;

        public ViewHolder(View itemView) {
            super(itemView);
            txtStatus   = itemView.findViewById(R.id.txtStatus);
            txtDate     = itemView.findViewById(R.id.txtDate);
            txtMobile   = itemView.findViewById(R.id.txtMobile);
            txtBeneName = itemView.findViewById(R.id.txtBeneName);
            txtIfsc     = itemView.findViewById(R.id.txtIfsc);
            txtAccount  = itemView.findViewById(R.id.txtAccount);
            txtUtr      = itemView.findViewById(R.id.txtUtr);
            txtAmount   = itemView.findViewById(R.id.txtAmount);
            txtTxnType  = itemView.findViewById(R.id.txtTxnType);
        }
    }
}
