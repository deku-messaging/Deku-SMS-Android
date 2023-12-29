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
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryption;
import com.afkanerd.deku.E2EE.ConversationsThreadsEncryptionDao;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomepageFragment extends Fragment {
    HomepageFragmentAdapter homepageFragmentAdapter;
    ViewPager2 viewPager;

    private TabListenerInterface mListener;

    public HomepageFragment() {
        // Required empty public constructor
    }
    public static final List<String> fragmentList = new ArrayList<>(
            Arrays.asList(ThreadedConversationsFragment.ALL_MESSAGES_THREAD_FRAGMENT,
                    ThreadedConversationsFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT ));

    static boolean encryptedEnabled = false;

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }



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
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConversationsThreadsEncryption conversationsThreadsEncryption = new ConversationsThreadsEncryption();
                ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
                        conversationsThreadsEncryption.getDaoInstance(getContext());
                if(conversationsThreadsEncryptionDao.getAll().size() < 1) {
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                view.findViewById(R.id.tab_layout).setVisibility(View.GONE);
                            }
                        });
                    }
                } else {
                    encryptedEnabled = true;
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
                            List<String> fragmentListNames = new ArrayList<>(Arrays.asList(
                                    getContext().getString(R.string.homepage_fragment_tab_all),
                                    getContext().getString(R.string.homepage_fragment_tab_encrypted)));
                            tab.setText(position >= fragmentListNames.size() ? fragmentListNames.get(0)
                                    : fragmentListNames.get(position));
                        }
                    }).attach();
                }
                conversationsThreadsEncryption.close();

            }
        }).start();

    }

    public static class HomepageFragmentAdapter extends FragmentStateAdapter {
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
            args.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE, fragmentList.get(position));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return encryptedEnabled ? 2 : 1;
        }
    }
}