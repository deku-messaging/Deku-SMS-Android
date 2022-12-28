package com.example.swob_deku.Models.SMS;

import android.database.Cursor;
import android.provider.Telephony;

public class SMSThreads {

    int messageCount = 0;
    String body = "";
    String threadId;

    public SMSThreads(Cursor cursor) {
        // msg_count, snippet, thread_id
        int messageCountIndex = cursor.getColumnIndexOrThrow(
                Telephony.Sms.Conversations.MESSAGE_COUNT);
        this.messageCount = cursor.getInt(messageCountIndex);

        int bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);
        this.body = String.valueOf(cursor.getString(bodyIndex));

        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID);
        this.threadId = String.valueOf(cursor.getString(threadIdIndex));
    }
}
