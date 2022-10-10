package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.example.swob_deku.Models.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS;
import com.example.swob_deku.Models.SMSHandler;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class RouterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.routed_messages_toolbar);
        myToolbar.setTitle(R.string.router_name);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        populateRouteJobs();
    }

    List<SMS> getThreadsFromCursor(Cursor cursor) {
        List<SMS> threadsInCursor = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do{
                SMS sms = new SMS(cursor);
                threadsInCursor.add(sms);
            }
            while(cursor.moveToNext());
        }
        else {
            Log.i(this.getLocalClassName(), "No threads in cursor");
        }

        return threadsInCursor;
    }

    void populateRouteJobs() {
        ArrayList<Long> workerIds = listRouteJobs();

        Cursor cursor = SMSHandler.fetchSMSMessageForAllIds(getApplicationContext(), workerIds);
        List<SMS> messagesForThread = getThreadsFromCursor(cursor);
        // TODO: append the status to this list somehow

        messagesForThread = SMSHandler.getAddressForThreads(getApplicationContext(), messagesForThread);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, messagesForThread, R.layout.messages_threads_layout);

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }

    public static String[] convertSetToStringArray(Set<String> setOfString)
    {

        // Create String[] of size of setOfString
        String[] arrayOfString = new String[setOfString.size()];

        // Copy elements from set to string array
        // using advanced for loop
        int index = 0;
        for (String str : setOfString)
            arrayOfString[index++] = str;

        // return the formed String[]
        return arrayOfString;
    }

    public ArrayList<Long> listRouteJobs() {

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Arrays.asList(SMSReceiverActivity.TAG_NAME))
                .addStates(Arrays.asList(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED))
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfos(workQuery);

        ArrayList<Long> workerIds = new ArrayList<>();
        try {
            List<WorkInfo> workInfoList = workInfos.get();

            String messageId = new String();
            for(WorkInfo workInfo : workInfoList) {
                String[] tags = convertSetToStringArray(workInfo.getTags());
                for(int i = 0; i< tags.length; ++i) {
                    if (tags[i].contains("swob.work.id")) {
                        tags = tags[i].split("\\.");
                        messageId = tags[tags.length - 1];
                        break;
                    }
                }
                Log.d("", "Work info: " + messageId + " : " + workInfo.getState().name());
                workerIds.add(Long.valueOf(messageId));
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return workerIds;
    }
}