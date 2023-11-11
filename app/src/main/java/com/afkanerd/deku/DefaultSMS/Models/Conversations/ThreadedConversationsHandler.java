package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;

public class ThreadedConversationsHandler {

    public static ThreadedConversations get(Context context, ThreadedConversations threadedConversations) throws InterruptedException {
        ThreadedConversationsDao threadedConversationsDao = ThreadedConversations.getDao(context);
        final ThreadedConversations[] threadedConversations1 = {threadedConversations};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(threadedConversations.getThread_id() != null &&
                        !threadedConversations.getThread_id().isEmpty())
                    threadedConversations1[0] =
                            threadedConversationsDao.get(threadedConversations.getThread_id());
                else if(threadedConversations.getAddress() != null &&
                        !threadedConversations.getAddress().isEmpty()) {
//                    threadedConversations1[0] =
//                            threadedConversationsDao.getByAddress(threadedConversations.getAddress());
                    ThreadedConversations threadedConversation =
                            threadedConversationsDao.getByAddress(threadedConversations.getAddress());
                    if(threadedConversation != null )
                        threadedConversations1[0] = threadedConversation;
                }
            }
        });
        thread.start();
        thread.join();

        return threadedConversations1[0];
    }

    public static void call(Context context, ThreadedConversations threadedConversations) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.setData(Uri.parse("tel:" + threadedConversations.getAddress()));

        context.startActivity(callIntent);
    }
}
