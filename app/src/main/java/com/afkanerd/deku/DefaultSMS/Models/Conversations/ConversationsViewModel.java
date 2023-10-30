package com.afkanerd.deku.DefaultSMS.Models.Conversations;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;

import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSPaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

public class ConversationsViewModel extends ViewModel {
    public String threadId;

    MutableLiveData<ArrayList<SMS>> mutableLiveData;
    LiveData<ArrayList<SMS>> liveData;

    Pager<Integer, SMS> pager;
    SMSPaging smsPaging;

    public Integer currentLimit = 15;
    Integer offset = 0;

    public boolean offsetStartedFromZero = true;

    public LiveData<ArrayList<SMS>> getMessages(Context context, String threadId, int offset){
        this.threadId = threadId;
        if((offset - 1) > 0) {
            offsetStartedFromZero = false;
            this.offset = offset;
        }

        if(this.mutableLiveData == null) {
            ArrayList<SMS> loadedSMS = this.threadId != null ?
                    loadSMSThreads(context, offset, currentLimit) :
                    new ArrayList<>();
            this.mutableLiveData = new MutableLiveData<>(loadedSMS);
        }
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
        Log.d(getClass().getName(), "Refreshing recyclerview");
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<SMS> newSMS = loadSMSThreads(context, offset, currentLimit);
                if (!newSMS.isEmpty()) {
                    ArrayList<SMS> sms = (ArrayList<SMS>) mutableLiveData.getValue();
                    sms.addAll(newSMS);

                    TreeSet<SMS> smsTreeSet = new TreeSet<>(sms);
                    smsTreeSet = (TreeSet<SMS>) smsTreeSet.descendingSet();
                    mutableLiveData.postValue(new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0]))));
                }

            }
        }).start();
        return offset;
    }

    private Integer _updateLiveDataDown(Context context, int offset, int limit) {
        ArrayList<SMS> newSMS = loadSMSThreads(context, offset, limit);

        if (!newSMS.isEmpty()) {
            ArrayList<SMS> sms = mutableLiveData.getValue();
            sms.addAll(newSMS);

            TreeSet<SMS> smsTreeSet = new TreeSet<>(sms);
            smsTreeSet = (TreeSet<SMS>) smsTreeSet.descendingSet();
            mutableLiveData.setValue(new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0]))));
        }
        this.offset = offset == 0 ? null : offset;
        return newSMS.size();
    }

    public void _updateLiveData(ArrayList<SMS> newSMS) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!newSMS.isEmpty()) {
                    ArrayList<SMS> sms = (ArrayList<SMS>) mutableLiveData.getValue();
                    sms.addAll(newSMS);

                    TreeSet<SMS> smsTreeSet = new TreeSet<>(sms);
                    smsTreeSet = (TreeSet<SMS>) smsTreeSet.descendingSet();
                    mutableLiveData.postValue(new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0]))));
                }
            }
        }).start();
    }

    public void loadFromPosition(Context context, int position) {
        // if top = load down
        // if down = load up
        // if middle = load up and down
        int mid_count = currentLimit / 2;
        if((position - mid_count) < 0) {
            // BOTTOM
            this.offset = 0;
            _updateLiveData(loadSMSThreads(context, this.offset, currentLimit));
        }
        else {
            this.offset = position - mid_count;
            _updateLiveData(loadSMSThreads(context, this.offset, currentLimit));
        }
        offsetStartedFromZero = false;
    }

    private ArrayList<SMS> loadSMSThreads(Context context, Integer _offset, int limit) {
        if(_offset == null)
            _offset = 0;
        ArrayList<SMS> smsArray =  SMSPaging.fetchMessages_advanced(context, threadId, limit, _offset);
        TreeSet<SMS> smsTreeSet = new TreeSet<>(smsArray);
        smsTreeSet = (TreeSet<SMS>) smsTreeSet.descendingSet();

        return new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0])));
    }
}
