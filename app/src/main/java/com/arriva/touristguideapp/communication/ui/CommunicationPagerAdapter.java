package com.arriva.touristguideapp.communication.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CommunicationPagerAdapter extends FragmentStateAdapter {

    public CommunicationPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new CommunicatorFragment();
            case 0:
            default:
                return new PhrasebookFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
