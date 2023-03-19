package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSPaging;

import java.util.ArrayList;

import kotlin.jvm.functions.Function0;

public class SingleMessageViewModel extends ViewModel {
    String threadId;
    Context context;

    Lifecycle lifecycle;

    LiveData liveData;
    Pager<Integer, SMS> pager;
    SMSPaging smsPaging;

    public LiveData<PagingData<SMS>> getMessages(Context context, String threadId, Lifecycle lifecycle){
        this.threadId = threadId;
        this.context = context;
        this.lifecycle = lifecycle;

        loadSMSThreads();
        return liveData;
    }

    public void informNewItemChanges(String threadId) {
        this.threadId = threadId;
        this.smsPaging.invalidate();
        liveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
    }

    public void informNewItemChanges() {
        this.smsPaging.invalidate();
        liveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
    }

    public void refresh() {
        loadSMSThreads();
    }
    private void loadSMSThreads() {
        Log.d(getClass().getName(), "ViewModel loading..");
        // TODO: make 20
        final int pageSize = 20;
        // TODO: make 40
        final int prefetchDistance = 0;
        // TODO: make 30
        final int initialLoad = 15;

//        PagingConfig pagingConfig = new PagingConfig(pageSize);
        PagingConfig pagingConfig = new PagingConfig(pageSize, prefetchDistance,
                true, initialLoad);
        pager = new Pager<>(pagingConfig, new Function0<PagingSource<Integer, SMS>>() {
            @Override
            public PagingSource<Integer, SMS> invoke() {
                smsPaging = smsPaging == null ?
                        new SMSPaging(context, threadId) : new SMSPaging(context, threadId, smsPaging.fetchedMessages);
                return smsPaging;
            }
        });

        liveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
    }

}
