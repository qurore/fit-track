package com.github.qurore.fittrack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TabContentFragment extends Fragment {
    private static final String ARG_TITLE = "title";

    public static TabContentFragment newInstance(String title) {
        TabContentFragment fragment = new TabContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE) : "Tab";
        
        TextView titleTextView = view.findViewById(R.id.tabContentTitle);
        titleTextView.setText(title + " Training");
    }
} 