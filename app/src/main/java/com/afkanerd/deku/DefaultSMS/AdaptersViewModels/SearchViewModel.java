package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {

    MutableLiveData<Pair<List<ThreadedConversations>, Integer>> liveData;

    ThreadedConversationsDao threadedConversationsDao;

    String threadId;

    public LiveData<Pair<List<ThreadedConversations>,Integer>> get(ThreadedConversationsDao threadedConversationsDao){
        this.threadedConversationsDao = threadedConversationsDao;
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
        }
        return liveData;
    }

    public LiveData<Pair<List<ThreadedConversations>,Integer>> getByThreadId(
            ThreadedConversationsDao threadedConversationsDao, String threadId){
        this.threadedConversationsDao = threadedConversationsDao;
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
            this.threadId = threadId;
        }
        return liveData;
    }

    public void search(Context context, String input) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversation> conversations = new ArrayList<>();
                Integer index = null;
                if(threadId == null || threadId.isEmpty())
                    conversations = threadedConversationsDao.findAddresses(input);
                else {
                    conversations = threadedConversationsDao.findByThread(input, threadId);
                    ConversationDao conversationDao = new Conversation().getDaoInstance(context);
                    List<Conversation> conversationList = conversationDao.getAll(threadId);
                    if(!conversationList.isEmpty()) {
                        index = conversationList.indexOf(conversationList.get(0));
                    }
                }

                List<ThreadedConversations> threadedConversations =
                        ThreadedConversations.buildRaw(context, conversations);
                liveData.postValue(new Pair<>(threadedConversations, index));
            }
        });
        thread.start();
    }

}
