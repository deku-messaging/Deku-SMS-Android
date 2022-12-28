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
import android.view.View;
import android.widget.TextView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        ArrayList<ArrayList<String>> workerIdsList = listRouteJobs();
        if(workerIdsList.isEmpty())
            return;

        TextView noRoutedMessagesText = findViewById(R.id.router_no_showable_messages_text);
        noRoutedMessagesText.setVisibility(View.GONE);

        ArrayList<Long> workerIds = new ArrayList<>();

        for(ArrayList workerList : workerIdsList)
            workerIds.add(Long.valueOf(workerList.get(0).toString()));

        Cursor cursor = SMSHandler.fetchSMSMessageForAllIds(getApplicationContext(), workerIds);
        List<SMS> messagesForThread = getThreadsFromCursor(cursor);

        for(int j = 0; j < messagesForThread.size(); ++j) {
            SMS sms = messagesForThread.get(j);
            for(int i = 0;i< workerIds.size(); ++i ) {
                if(workerIds.get(i).equals(Long.valueOf(sms.getId()))) {
                    messagesForThread.get(j).setRouterStatus(workerIdsList.get(i).get(1));
                    Log.d("", "Found matches...");
                    break;
                }
            }
        }

//        messagesForThread = SMSHandler.getAddressForThreads(getApplicationContext(), messagesForThread, false);

        RecyclerView messagesThreadRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout, true, "");

        messagesThreadRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        messagesThreadRecyclerView.setLayoutManager(linearLayoutManager);
    }


    public ArrayList<ArrayList<String>> listRouteJobs() {

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Arrays.asList(SMSTextReceiverBroadcastActivity.TAG_NAME))
                .addStates(Arrays.asList(
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.CANCELLED))
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfos(workQuery);

        ArrayList<ArrayList<String>> workerIds = new ArrayList<>();
        try {
            List<WorkInfo> workInfoList = workInfos.get();

            String messageId = new String();
            for(WorkInfo workInfo : workInfoList) {
                String[] tags = Helpers.convertSetToStringArray(workInfo.getTags());
                for(int i = 0; i< tags.length; ++i) {
                    if (tags[i].contains("swob.work.id")) {
                        tags = tags[i].split("\\.");
                        messageId = tags[tags.length - 1];
                        break;
                    }
                }
                if(!messageId.isEmpty()) {
                    Log.d("", "Work info: " + messageId + " : " + workInfo.getState().name());
                    ArrayList<String> routeJobState = new ArrayList<>();
                    routeJobState.add(messageId);
                    routeJobState.add(workInfo.getState().name());
                    workerIds.add(routeJobState);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return workerIds;
    }
}