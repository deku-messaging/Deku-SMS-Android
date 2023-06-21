package com.example.swob_deku.Fragments.Homepage;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.swob_deku.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class HomepageFragment extends Fragment {
    HomepageFragmentAdapter homepageFragmentAdapter;
    ViewPager2 viewPager;
    List<String> fragmentListNames = new ArrayList<>();

    public HomepageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_all));
        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_encrypted));
        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_plain));
//        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_automated));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        homepageFragmentAdapter = new HomepageFragmentAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(homepageFragmentAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(fragmentListNames.get(position));
            }
        }).attach();
    }

    public static class HomepageFragmentAdapter extends FragmentStateAdapter {
        List<String> fragmentList = new ArrayList<>();
        public HomepageFragmentAdapter(Fragment fragment) {
            super(fragment);

            fragmentList.add(MessagesThreadFragment.ALL_MESSAGES_THREAD_FRAGMENT);
            fragmentList.add(MessagesThreadFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT);
            fragmentList.add(MessagesThreadFragment.PLAIN_MESSAGES_THREAD_FRAGMENT);
//            fragmentList.add(MessagesThreadFragment.AUTOMATED_MESSAGES_THREAD_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = new MessagesThreadFragment();
            Bundle args = new Bundle();
            args.putString(MessagesThreadFragment.MESSAGES_THREAD_FRAGMENT_TYPE, fragmentList.get(position));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return fragmentList.size();
        }
    }
}