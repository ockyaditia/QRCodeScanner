package com.example.ockyaditiasaputra.qrcodescanner;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Ocky Aditia Saputra on 29/10/2015.
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                return new LoginFragment();
            case 1:
                return new RegisterFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
