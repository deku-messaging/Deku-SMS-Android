package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

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

    MutableLiveData<List<ThreadedConversations>> liveData;

    ThreadedConversationsDao threadedConversationsDao;
    ConversationDao conversationDao;

    String threadId;

    public LiveData<List<ThreadedConversations>> get(ThreadedConversationsDao threadedConversationsDao){
        this.threadedConversationsDao = threadedConversationsDao;
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
        }
        return liveData;
    }

    public LiveData<List<ThreadedConversations>> getByThreadId(
            ThreadedConversationsDao threadedConversationsDao, String threadId){
        this.threadedConversationsDao = threadedConversationsDao;
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
            this.threadId = threadId;
        }
        return liveData;
    }

    public LiveData<List<ThreadedConversations>> get(ConversationDao conversationDao){
        this.conversationDao = conversationDao;
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
        }
        return liveData;
    }

    public void search(String input) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Conversation> conversations = new ArrayList<>();
                if(threadId == null || threadId.isEmpty())
                    conversations = threadedConversationsDao.find(input);
                else
                    conversations = threadedConversationsDao.findByThread(input,threadId);

                List<ThreadedConversations> threadedConversations = new ArrayList<>();
                for(Conversation conversation : conversations) {
                    ThreadedConversations threadedConversation =
                            ThreadedConversations.build(conversation);
                    threadedConversations.add(threadedConversation);
                }
                liveData.postValue(threadedConversations);
            }
        });
        thread.start();
    }

}
