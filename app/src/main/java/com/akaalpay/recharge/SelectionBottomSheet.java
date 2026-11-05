package com.akaalpay.recharge;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.akaalpay.R;
import java.util.List;

public class SelectionBottomSheet extends BottomSheetDialogFragment {

    private String title;
    private List<String> items;
    private OnSelect listener;

    public interface OnSelect {
        void onSelected(String name, int position);
    }

    public static SelectionBottomSheet newInstance(String title, List<String> items, OnSelect listener) {
        SelectionBottomSheet sheet = new SelectionBottomSheet();
        sheet.title = title;
        sheet.items = items;
        sheet.listener = listener;
        return sheet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottom_sheet_operator, container, false);

        TextView tvTitle = v.findViewById(R.id.title);
        EditText search = v.findViewById(R.id.search);
        ListView list = v.findViewById(R.id.list);

        tvTitle.setText(title);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.getFilter().filter(s);
            }
            public void afterTextChanged(Editable s) {}
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            String name = adapter.getItem(position);
            int originalIndex = items.indexOf(name);

            listener.onSelected(name, originalIndex);
            dismiss();
        });

        return v;
    }
}