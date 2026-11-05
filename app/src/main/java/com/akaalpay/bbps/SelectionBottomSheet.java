package com.akaalpay.bbps;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akaalpay.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class SelectionBottomSheet extends BottomSheetDialogFragment {

    public interface OnItemSelectListener{
        void onSelect(String name, int position);
    }

    private String title = "";
    private ArrayList<String> list = new ArrayList<>();
    private ArrayList<String> filtered = new ArrayList<>();
    private OnItemSelectListener listener;

    public static SelectionBottomSheet newInstance(
            String title,
            ArrayList<String> list,
            OnItemSelectListener listener
    ){
        SelectionBottomSheet sheet = new SelectionBottomSheet();
        sheet.title = title;
        sheet.list = list;
        sheet.filtered = new ArrayList<>(list);
        sheet.listener = listener;
        return sheet;
    }

    RecyclerView recyclerView;
    ItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(
                R.layout.bottom_sheet_selection,
                container,
                false
        );

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        EditText etSearch = v.findViewById(R.id.etSearch);
        ImageView btnClose = v.findViewById(R.id.btnClose);
        recyclerView = v.findViewById(R.id.recyclerView);

        tvTitle.setText(title);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        adapter = new ItemAdapter();
        recyclerView.setAdapter(adapter);

        btnClose.setOnClickListener(view -> dismiss());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}

            @Override
            public void afterTextChanged(Editable s) {

                filtered.clear();

                for(String item : list){
                    if(item.toLowerCase()
                            .contains(s.toString().toLowerCase())){
                        filtered.add(item);
                    }
                }

                adapter.notifyDataSetChanged();
            }
        });

        return v;
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Holder>{

        class Holder extends RecyclerView.ViewHolder{
            TextView tvName;
            Holder(View v){
                super(v);
                tvName = v.findViewById(R.id.tvName);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.row_selection,
                            parent,
                            false
                    );

            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(
                @NonNull Holder holder,
                int position) {

            String name = filtered.get(position);

            holder.tvName.setText(name);

            holder.itemView.setOnClickListener(v -> {

                int originalPos = list.indexOf(name);

                if(listener != null){
                    listener.onSelect(name, originalPos);
                }

                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }
    }
}