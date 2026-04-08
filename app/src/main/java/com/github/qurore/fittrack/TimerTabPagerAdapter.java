package com.github.qurore.fittrack;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TimerTabPagerAdapter extends FragmentStateAdapter {
    private final String[] tabTitles = {"Stopwatch", "Timer"};

    public TimerTabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new StopwatchFragment();
        } else {
            return new CountdownTimerFragment();
        }
    }

    @Override
    public int getItemCount() {
        return tabTitles.length;
    }

    public String getTabTitle(int position) {
        return tabTitles[position];
    }
}
