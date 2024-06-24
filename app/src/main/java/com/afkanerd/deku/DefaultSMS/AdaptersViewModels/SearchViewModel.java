package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {

    MutableLiveData<Pair<List<ThreadedConversations>, Integer>> liveData;

    String threadId;

    public Datastore databaseConnector;

    public LiveData<Pair<List<ThreadedConversations>,Integer>> get(){
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
        }
        return liveData;
    }

    public LiveData<Pair<List<ThreadedConversations>,Integer>> getByThreadId(String threadId){
        if(this.liveData == null) {
            liveData = new MutableLiveData<>();
            this.threadId = threadId;
        }
        return liveData;
    }

    public void search(Context context, String input) throws InterruptedException {
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<Conversation> conversations = new ArrayList<>();
                Integer index = null;
                if(threadId == null || threadId.isEmpty())
                    conversations = databaseConnector.threadedConversationsDao().findAddresses(input);
                else {
                    conversations = databaseConnector.threadedConversationsDao()
                            .findByThread(input, threadId);
                    List<Conversation> conversationList = databaseConnector.conversationDao()
                            .getAll(threadId);
                    if(!conversationList.isEmpty()) {
                        index = conversationList.indexOf(conversationList.get(0));
                    }
                }

                List<ThreadedConversations> threadedConversations =
                        ThreadedConversations.buildRaw(context, conversations);
                liveData.postValue(new Pair<>(threadedConversations, index));
            }
        });
    }

}
