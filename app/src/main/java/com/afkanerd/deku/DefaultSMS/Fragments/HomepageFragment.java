package com.afkanerd.deku.DefaultSMS.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class HomepageFragment extends Fragment {
    HomepageFragmentAdapter homepageFragmentAdapter;
    ViewPager2 viewPager;
    List<String> fragmentListNames = new ArrayList<>();

    private TabListenerInterface mListener;

    public HomepageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_all));
        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_encrypted));
//        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_plain));
//        fragmentListNames.add(getContext().getString(R.string.homepage_fragment_tab_automated));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    public interface TabListenerInterface {
        void tabUnselected(int position);
        void tabSelected(int position);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Verify that the parent activity implements the interface
        if (context instanceof TabListenerInterface) {
            mListener = (TabListenerInterface) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnViewManipulationListener");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        homepageFragmentAdapter = new HomepageFragmentAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(homepageFragmentAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mListener.tabSelected(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                mListener.tabUnselected(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(fragmentListNames.get(position));
            }
        }).attach();
    }

    public static class HomepageFragmentAdapter extends FragmentStateAdapter {
        public static String[] fragmentList = new String[]{
                ThreadedConversationsFragment.ALL_MESSAGES_THREAD_FRAGMENT,
                ThreadedConversationsFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT};
//                ThreadedConversationsFragment.PLAIN_MESSAGES_THREAD_FRAGMENT };
        public HomepageFragmentAdapter(Fragment fragment) {
            super(fragment);
//            fragmentList.add(ThreadedConversationsFragment.AUTOMATED_MESSAGES_THREAD_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = new ThreadedConversationsFragment();
            Bundle args = new Bundle();
            args.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE, fragmentList[position]);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return fragmentList.length;
        }
    }
}