package com.example.swob_deku.Models.Archive;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServer.GatewayServerDAO;
import com.example.swob_deku.Models.SMS.SMS;

public class ArchiveHandler {

    public static void archiveSMS(Context context, Archive archive) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                        .fallbackToDestructiveMigration()
                        .build();
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.insert(archive);
            }
        });
        thread.start();
        thread.join();
    }

    public static boolean isArchived(Context context, long threadId) throws InterruptedException {
        final boolean[] isArchived = {false};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                                Datastore.databaseName)
                        .fallbackToDestructiveMigration()
                        .build();
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                Archive archive = archiveDAO.fetch(threadId);
                Log.d(getClass().getName(), "Is archived: " + archive);
                if(archive != null)
                    isArchived[0] = true;
            }
        });
        thread.start();
        thread.join();

        return isArchived[0];
    }

    public static void removeFromArchive(Context context, long threadId) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                                Datastore.databaseName)
                        .fallbackToDestructiveMigration()
                        .build();
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.remove(new Archive(threadId));
            }
        });
        thread.start();
        thread.join();
    }
}
