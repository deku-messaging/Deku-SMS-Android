package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;
import androidx.paging.TransformablePage;
import androidx.paging.rxjava3.PagingRx;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.SMS.SMSPaging;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.rxjava3.core.Flowable;
import kotlinx.coroutines.CoroutineScope;

public class SingleMessageViewModel extends ViewModel {
    String threadId;
    Context context;

    Lifecycle lifecycle;

    MutableLiveData mutableLiveData;

    public LiveData<PagingData<SMS>> getMessages(Context context, String threadId, Lifecycle lifecycle){
        this.threadId = threadId;
        this.context = context;
        this.lifecycle = lifecycle;

        mutableLiveData = new MutableLiveData();

        mutableLiveData.setValue(loadSMSThreads());

        return mutableLiveData;
    }

    public void informChanges(String threadId) {
        Log.d(getClass().getName(), "Informing changes for: " + threadId);
        this.threadId = threadId;

        loadSMSThreads();
    }


    private LiveData<PagingData<SMS>> loadSMSThreads() {
        // TODO: make 20
        final int pageSize = 10;
        // TODO: make 40
        final int prefetchDistance = 0;
        // TODO: make 30
        final int initialLoad = 15;

//        PagingConfig pagingConfig = new PagingConfig(pageSize);
        PagingConfig pagingConfig = new PagingConfig(pageSize, prefetchDistance,
                true, initialLoad);

        Pager<Integer, SMS> pager = new Pager<>(pagingConfig,
                () -> new SMSPaging(context, threadId));

        LiveData liveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
        return liveData;
    }

}
