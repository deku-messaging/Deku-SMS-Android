package com.example.swob_deku.Models.Archive;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.GatewayServer.GatewayServerDAO;
import com.example.swob_deku.Models.SMS.SMS;

import java.util.ArrayList;
import java.util.List;

public class ArchiveHandler {
    Context context;
    Datastore databaseConnector;
    ArchiveDAO archiveDAO;
    public ArchiveHandler(Context context) {
        this.context = context;
        databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .fallbackToDestructiveMigration()
                .build();
        archiveDAO = databaseConnector.archiveDAO();
    }

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

    public boolean isArchived(long threadId) throws InterruptedException {
        return archiveDAO.fetch(threadId) != null;
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

    public static List<Archive> loadAllMessages(Context context) throws InterruptedException {
        final List<Archive>[] fetchedData = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                                Datastore.databaseName)
                        .fallbackToDestructiveMigration()
                        .build();
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                fetchedData[0] = archiveDAO.fetchAll();
            }
        });
        thread.start();
        thread.join();

        return fetchedData[0];
    }

    public void close() {
        databaseConnector.close();
    }

}
