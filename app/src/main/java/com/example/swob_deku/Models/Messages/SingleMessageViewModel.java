package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.SMS.SMSPaging;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import kotlin.jvm.functions.Function0;
import kotlinx.coroutines.CoroutineScope;

public class SingleMessageViewModel extends ViewModel {
    public LiveData<PagingData<ArrayList<SMS>>> messagesList = new MutableLiveData<>();

    String threadId;
    Context context;

    Lifecycle lifecycle;

    public LiveData<PagingData<ArrayList<SMS>>> getMessages(Context context, String threadId, Lifecycle lifecycle){
        this.threadId = threadId;
        this.context = context;
        this.lifecycle = lifecycle;

        return loadSMSThreads();
    }

    public void informChanges(Context context, String threadId) {
        Log.d(getClass().getName(), "Informing changes for: " + threadId);
        this.threadId = threadId;
        loadSMSThreads();
    }

    public void informChanges() {
        loadSMSThreads();
    }

    private LiveData<PagingData<ArrayList<SMS>>> loadSMSThreads() {
        Log.d(getClass().getName(), "Paging loading data for ViewModel!");
        final int pageSize = 1;
        PagingConfig pagingConfig = new PagingConfig(1, 1, false);
        Pager<Integer, ArrayList<SMS>> pager = new Pager<>(pagingConfig, new Function0<PagingSource<Integer, ArrayList<SMS>>>() {
            @Override
            public PagingSource<Integer, ArrayList<SMS>> invoke() {
                return new SMSPaging();
            }
        });

        LiveData<PagingData<ArrayList<SMS>>> pagingDataLiveData = PagingLiveData.getLiveData(pager);
        Log.d(getClass().getName(), "Pager: " + pagingDataLiveData.getValue());
        return PagingLiveData.cachedIn(pagingDataLiveData, this.lifecycle);
    }
}
