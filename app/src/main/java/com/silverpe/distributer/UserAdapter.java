package com.silverpe.distributer;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.silverpe.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Shows only the user fields requested from users/all response: data.users.data. */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<JSONObject> users = new ArrayList<>();
    private final LayoutInflater inflater;

    public UserAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void submitUsers(List<JSONObject> newUsers) {
        users.clear();
        if (newUsers != null) users.addAll(newUsers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(inflater.inflate(R.layout.item_user_all, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder h, int position) {
        JSONObject u = users.get(position);

        String name = value(u, "name", "Name not provided");

        h.tvAvatar.setText(name.substring(0, 1).toUpperCase());
        h.tvName.setText(name);
        if ("1".equals(value(u, "status", ""))) {
            h.tvIndex.setText("Active");
            h.tvIndex.setTextColor(Color.parseColor("#15803D"));
            h.tvIndex.setBackgroundResource(R.drawable.bg_user_badge);

        } else if ("0".equals(value(u, "status", ""))) {
            h.tvIndex.setText("Inactive");
            h.tvIndex.setTextColor(Color.parseColor("#DC2626"));
            h.tvIndex.setBackgroundResource(R.drawable.bg_user_badge_inactive);
        }

        h.tvParentUserId.setText(value(u, "parent_user_id", "—"));
        h.tvMobile.setText(value(u, "mobile", "—"));
        h.tvRegMobile.setText(value(u, "registered_mobile", "—"));
        h.tvEmail.setText(value(u, "email", "—"));
        h.tvDob.setText(value(u, "dob", "—"));
        h.tvPan.setText(value(u, "profile_pan", "—"));
        h.tvAadhaar.setText(value(u, "profile_aadhar", "—"));
        h.tvAddress.setText(value(u, "address", "—"));
        h.tvState.setText(value(u, "state_name", value(u, "state", "—")));
        h.tvCity.setText(value(u, "city_name", value(u, "city", "—")));
        h.tvPincode.setText(value(u, "pincode", "—"));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private String value(JSONObject o, String key, String fallback) {
        String r = o.optString(key, "");
        if (TextUtils.isEmpty(r) || "null".equalsIgnoreCase(r.trim())) return fallback;
        return r.trim();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar, tvName, tvMobile, tvRegMobile, tvEmail,tvIndex,tvParentUserId,
                tvDob, tvPan, tvAadhaar, tvAddress, tvState, tvCity, tvPincode;

        UserViewHolder(@NonNull View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            tvParentUserId = v.findViewById(R.id.tvParentUserId);
            tvName = v.findViewById(R.id.tvUserName);
            tvIndex = v.findViewById(R.id.tvIndex);
            tvMobile = v.findViewById(R.id.tvMobile);
            tvRegMobile = v.findViewById(R.id.tvRegMobile);
            tvEmail = v.findViewById(R.id.tvEmail);
            tvDob = v.findViewById(R.id.tvDob);
            tvPan = v.findViewById(R.id.tvPan);
            tvAadhaar = v.findViewById(R.id.tvAadhaar);
            tvAddress = v.findViewById(R.id.tvAddress);
            tvState = v.findViewById(R.id.tvState);
            tvCity = v.findViewById(R.id.tvCity);
            tvPincode = v.findViewById(R.id.tvPincode);
        }
    }
}