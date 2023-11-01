package com.afkanerd.deku.DefaultSMS.Models.Conversations;


import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;

import com.afkanerd.deku.DefaultSMS.Models.RoomViewModel;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSPaging;

import java.util.ArrayList;
import java.util.List;

public class ConversationsViewModel extends ViewModel implements RoomViewModel {
    public String threadId;

    MutableLiveData<List<Conversation>> mutableLiveData;
    LiveData<List<Conversation>> liveData;

    Pager<Integer, SMS> pager;
    SMSPaging smsPaging;

    public Integer currentLimit = 15;
    Integer offset = 0;

    public boolean offsetStartedFromZero = true;

    ConversationDao conversationDao;

    public LiveData<List<Conversation>> get(ConversationDao conversationDao, String threadId, Context context) throws InterruptedException {
        this.conversationDao = conversationDao;

        loadNative(context, threadId);
        return this.liveData;
    }

    private void loadNative(Context context, String threadId) throws InterruptedException {
        if(this.liveData == null) {
            Thread loadRoom = new Thread(new Runnable() {
                @Override
                public void run() {
                    liveData = conversationDao.getAll();
                }
            });
            loadRoom.setName("load ROOM thread");
            loadRoom.start();
            loadRoom.join();
        }

        Thread loadNativeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = SMSHandler.fetchByThreadId(context, threadId);
                List<Conversation> conversationList = new ArrayList<>();
                if(cursor.moveToNext()) {
                    do {
                        conversationList.add(Conversation.build(cursor));
                    } while(cursor.moveToNext());
                }
                cursor.close();
                conversationDao.insertAll(conversationList);
            }
        });
        loadNativeThread.setName("load_native_thread");
        loadNativeThread.start();
    }

    @Override
    public void insert(Object entity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(entity instanceof Conversation)
                    conversationDao.insert((Conversation) entity);
            }
        }).start();
    }

//    public LiveData<List<Conversation>> get(Context context, String threadId, int offset){
//        this.threadId = threadId;
//        if((offset - 1) > 0) {
//            offsetStartedFromZero = false;
//            this.offset = offset;
//        }
//
//        if(this.mutableLiveData == null) {
//            ArrayList<SMS> loadedSMS = this.threadId != null ?
//                    loadSMSThreads(context, offset, currentLimit) :
//                    new ArrayList<>();
//            this.mutableLiveData = new MutableLiveData<>(loadedSMS);
//        }
//        return mutableLiveData;
//    }
//
//    public void informNewItemChanges(Context context, String threadId) {
//        this.threadId = threadId;
//        informNewItemChanges(context);
//    }
//
//    public void informNewItemChanges(Context context) {
//        offset = 0;
//        this.mutableLiveData.setValue(loadSMSThreads(context, offset, currentLimit));
//    }
//
//    public void refresh(Context context) {
//        Log.d(getClass().getName(), "Refreshing recyclerview");
//        if(offset != null) {
//            offset += currentLimit;
//            offset = _updateLiveData(context, offset);
//        }
//    }
//
//    private Integer _updateLiveData(Context context, int offset) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final ArrayList<SMS> newSMS = loadSMSThreads(context, offset, currentLimit);
//                if (!newSMS.isEmpty()) {
//                    ArrayList<SMS> sms =  mutableLiveData.getValue();
//                    if(sms != null)
//                        sms.addAll(newSMS);
//                    else
//                        sms = newSMS;
//                    final ArrayList<SMS> f_sms = new ArrayList<>();
//                    f_sms.addAll(sms);
//
//                    NavigableSet<SMS> smsTreeSet = new ConcurrentSkipListSet<>(f_sms);
//                    smsTreeSet = smsTreeSet.descendingSet();
//                    mutableLiveData.postValue(new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0]))));
//                }
//
//            }
//        }).start();
//        return offset;
//    }
//
//    public void _updateLiveData(ArrayList<SMS> newSMS) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (!newSMS.isEmpty()) {
//                    ArrayList<SMS> sms =  mutableLiveData.getValue();
//                    if(sms != null)
//                        sms.addAll(newSMS);
//                    else
//                        sms = newSMS;
//                    final ArrayList<SMS> f_sms = new ArrayList<>();
//                    f_sms.addAll(sms);
//
//                    NavigableSet<SMS> smsTreeSet = new ConcurrentSkipListSet<>(f_sms);
//                    smsTreeSet = smsTreeSet.descendingSet();
//                    mutableLiveData.postValue(new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0]))));
//                }
//            }
//        }).start();
//    }
//
//    public int loadFromPosition(Context context, int position) {
//        // if top = load down
//        // if down = load up
//        // if middle = load up and down
//        int mid_count = currentLimit / 2;
//        int size = mutableLiveData.getValue().size();
//        if((position - mid_count) < 0) {
//            // BOTTOM
//            this.offset = 0;
//            _updateLiveData(loadSMSThreads(context, this.offset, currentLimit));
//        }
//        else {
//            this.offset = position - mid_count;
//            _updateLiveData(loadSMSThreads(context, this.offset, currentLimit));
//        }
//        offsetStartedFromZero = false;
////        return size + this.offset;
//        return currentLimit + offset;
//    }
//
//    private ArrayList<SMS> loadSMSThreads(Context context, Integer _offset, int limit) {
//        if(_offset == null)
//            _offset = 0;
//        ArrayList<SMS> smsArray =  SMSPaging.fetchMessages_advanced(context, threadId, limit, _offset);
//        TreeSet<SMS> smsTreeSet = new TreeSet<>(smsArray);
//        smsTreeSet = (TreeSet<SMS>) smsTreeSet.descendingSet();
//
//        return new ArrayList<>(Arrays.asList(smsTreeSet.toArray(new SMS[0])));
//    }
}
