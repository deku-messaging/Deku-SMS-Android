package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

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
import kotlinx.coroutines.CoroutineScope;

public class SingleMessageViewModel extends ViewModel {
    private MutableLiveData<ArrayList<SMS>> _messagesList = new MutableLiveData<>();
    public LiveData<ArrayList<SMS>> messagesList = _messagesList;

    String threadId;
    SMSPaging smsPaging;

    public LiveData<ArrayList<SMS>> getMessages(Context context, String threadId){
        if(smsPaging == null) {
            this.threadId = threadId;
            loadSMSThreads();

            smsPaging = new SMSPaging(context, threadId);
        }
        return messagesList;
    }

    public void informChanges(Context context, String threadId) {
        Log.d(getClass().getName(), "Informing changes for: " + threadId);
        this.threadId = threadId;
        loadSMSThreads();
    }

    public void informChanges() {
        loadSMSThreads();
    }

    private void loadSMSThreads() {
        final int pageSize = 1;
        Pager<Integer, ArrayList<SMS>> pager = new Pager<>(
                new PagingConfig(pageSize),
                ()-> smsPaging);

        CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);
        PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), viewModelScope);
    }
}
