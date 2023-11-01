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

    LiveData<List<Conversation>> liveData;

    ConversationDao conversationDao;

    public LiveData<List<Conversation>> get(ConversationDao conversationDao, String threadId)
            throws InterruptedException {
        this.conversationDao = conversationDao;
        this.threadId = threadId;

        loadRoom();
        return this.liveData;
    }

    private void loadRoom() throws InterruptedException {
        if(this.liveData == null) {
            Thread loadRoom = new Thread(new Runnable() {
                @Override
                public void run() {
                    liveData = conversationDao.get(threadId);
                }
            });
            loadRoom.setName("load ROOM thread");
            loadRoom.start();
            loadRoom.join();
        }
    }

    public void loadNative(Context context) {
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
}
