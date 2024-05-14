package com.afkanerd.deku.QueueListener.GatewayClients;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;

import androidx.startup.AppInitializer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.ThreadedConversationsActivity;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.QueueListener.RMQ.RMQWorkManager;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.WorkManagerInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GatewayClientHandler {

    public Datastore databaseConnector;

    public GatewayClientHandler(Context context) {
        databaseConnector = Datastore.getDatastore(context);
    }

    public long add(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        final long[] id = {-1};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                id[0] = gatewayClientDAO.insert(gatewayClient);
            }
        });
        thread.start();
        thread.join();

        return id[0];
    }

    public void delete(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.delete(gatewayClient);
            }
        });
        thread.start();
        thread.join();
    }

    public void update(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.update(gatewayClient);
            }
        });
        thread.start();
        thread.join();
    }

    public GatewayClient fetch(long id) throws InterruptedException {
        final GatewayClient[] gatewayClient = {new GatewayClient()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClient[0] = gatewayClientDAO.fetch(id);
            }
        });
        thread.start();
        thread.join();

        return gatewayClient[0];
    }

    public List<GatewayClient> fetchAll() throws InterruptedException {
        final List<GatewayClient>[] gatewayClientList = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientList[0] = gatewayClientDAO.getAll();
            }
        });

        thread.start();
        thread.join();

        return gatewayClientList[0];
    }

    public final static String MIGRATIONS = "MIGRATIONS";
    public final static String MIGRATIONS_TO_11 = "MIGRATIONS_TO_11";
    public void startServices(Context context) throws InterruptedException {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        try {
            OneTimeWorkRequest gatewayClientListenerWorker = new OneTimeWorkRequest.Builder(RMQWorkManager.class)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS
                    )
                    .addTag(GatewayClient.class.getName())
                    .build();

            WorkManager workManager = WorkManager.getInstance(context);
            workManager.enqueueUniqueWork(ThreadedConversationsActivity.UNIQUE_WORK_MANAGER_NAME,
                    ExistingWorkPolicy.KEEP,
                    gatewayClientListenerWorker);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static String getConnectionStatus(Context context, String gatewayClientId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                GatewayClientListingActivity.Companion.getGATEWAY_CLIENT_LISTENERS(), Context.MODE_PRIVATE);

        if(sharedPreferences.contains(gatewayClientId)) {
            if(sharedPreferences.getBoolean(gatewayClientId, false)) {
                return context.getString(R.string.gateway_client_customization_connected);
            } else {
                return context.getString(R.string.gateway_client_customization_reconnecting);
            }
        }
        return context.getString(R.string.gateway_client_customization_deactivated);
    }

    public static List<String> getPublisherDetails(Context context, String projectName) {
        List<SubscriptionInfo> simcards = SIMHandler.getSimCardInformation(context);

        final String operatorCountry = Helpers.getUserCountry(context);

        List<String> operatorDetails = new ArrayList<>();
        for(int i=0;i<simcards.size(); ++i) {
            String mcc = String.valueOf(simcards.get(i).getMcc());
            int _mnc = simcards.get(i).getMnc();
            String mnc = _mnc < 10 ? "0" + _mnc : String.valueOf(_mnc);
            String carrierId = mcc + mnc;

            String publisherName = projectName + "." + operatorCountry + "." + carrierId;
            operatorDetails.add(publisherName);
        }

        return operatorDetails;
    }


    public static void startListening(Context context, GatewayClient gatewayClient) throws InterruptedException {
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                Datastore.getDatastore(context).gatewayClientDAO().update(gatewayClient);
                if(gatewayClient.getActivated())
                    AppInitializer.getInstance(context)
                            .initializeComponent(WorkManagerInitializer.class);
            }
        });
    }

}
