package com.example.swob_deku.Models.Messages;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSPaging;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.functions.Function0;

public class SingleMessageViewModel extends ViewModel {
    String threadId;

    MutableLiveData<ArrayList<SMS>> mutableLiveData;
    LiveData<ArrayList<SMS>> liveData;

    Pager<Integer, SMS> pager;
    SMSPaging smsPaging;

    public Integer currentLimit = 12;
    Integer offset = 0;

    public boolean offsetStartedFromZero = true;

    public LiveData<ArrayList<SMS>> getMessages(Context context, String threadId, int offset){
        this.threadId = threadId;

        if((offset - 1) > 0) {
            offsetStartedFromZero = false;
            this.offset = offset;
        }
        this.mutableLiveData = new MutableLiveData<>(loadSMSThreads(context, offset, currentLimit));
        return mutableLiveData;
    }

    public void informNewItemChanges(Context context, String threadId) {
        this.threadId = threadId;
        informNewItemChanges(context);
    }

    public void informNewItemChanges(Context context) {
        offset = 0;
        this.mutableLiveData.setValue(loadSMSThreads(context, offset, currentLimit));
    }

    public void refresh(Context context) {
        if(offset != null) {
            offset += currentLimit;
            offset = _updateLiveData(context, offset);
        }
    }

    public int refreshDown(Context context) {
        int newSize = 0;
        if(!offsetStartedFromZero && offset != null) {
            int calculatedOffset = offset - currentLimit;
            int newLimit = currentLimit;
            if(calculatedOffset < 0) {
                newLimit = offset;
                offset = 0;
            }
            else offset = calculatedOffset;
            newSize = _updateLiveDataDown(context, offset, newLimit);
        }
        return newSize;
    }

    private Integer _updateLiveData(Context context, int offset) {
        ArrayList<SMS> newSMS = loadSMSThreads(context, offset, currentLimit);

        if (!newSMS.isEmpty()) {
            ArrayList<SMS> sms = (ArrayList<SMS>) mutableLiveData.getValue();
            sms.addAll(newSMS);
            mutableLiveData.setValue(sms);

            return offset;
        }

        return null;
    }

    private Integer _updateLiveDataDown(Context context, int offset, int limit) {
        ArrayList<SMS> newSMS = loadSMSThreads(context, offset, limit);

        if (!newSMS.isEmpty()) {
            ArrayList<SMS> sms = (ArrayList<SMS>) mutableLiveData.getValue();

            ArrayList<SMS> mergedList = new ArrayList<>();
            mergedList.addAll(newSMS);
            mergedList.addAll(sms);

            Log.d(getClass().getName(), "Updating live data...: " + newSMS.size());
            mutableLiveData.setValue(mergedList);

        }
        this.offset = offset == 0 ? null : offset;
        return newSMS.size();
    }

    private ArrayList<SMS> loadSMSThreads(Context context, Integer _offset, int limit) {
        if(_offset == null)
            _offset = 0;
        return SMSPaging.fetchSMSFromHandlers(context, threadId, limit, _offset);
    }
}
