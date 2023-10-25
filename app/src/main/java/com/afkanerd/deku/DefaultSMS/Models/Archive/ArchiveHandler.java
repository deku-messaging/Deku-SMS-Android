package com.afkanerd.deku.DefaultSMS.Models.Archive;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Migrations;
import com.afkanerd.deku.DefaultSMS.Models.Datastore;

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
                .addMigrations(new Migrations.Migration4To5())
                .addMigrations(new Migrations.Migration5To6())
                .addMigrations(new Migrations.Migration6To7())
                .addMigrations(new Migrations.Migration7To8())
                .build();
        archiveDAO = databaseConnector.archiveDAO();
    }

    public void archiveSMS(Context context, Archive archive) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.insert(archive);
            }
        });
        thread.start();
        thread.join();
    }

    public void archiveMultipleSMS(String[] threadId) throws InterruptedException {
        Log.d(getClass().getName(), "Running to archive: " + threadId.length);
        Archive[] archives = new Archive[threadId.length];

        for(int i=0;i<threadId.length;++i)
            archives[i] = new Archive(Long.parseLong(threadId[i]));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.insert(archives);
            }
        });
        thread.start();
        thread.join();
    }

    public boolean isArchived(long threadId) throws InterruptedException {
        return archiveDAO.fetch(threadId) != null;
    }

    public void removeFromArchive(Context context, long threadId) throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.remove(new Archive(threadId));
            }
        });
        thread.start();
        thread.join();
    }

    public void removeMultipleFromArchive(Context context, long[] threadId) throws InterruptedException {
        Archive[] archives = new Archive[threadId.length];

        for(int i=0;i<threadId.length;++i)
            archives[i] = new Archive(threadId[i]);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                archiveDAO.remove(archives);
            }
        });
        thread.start();
        thread.join();
    }

    public List<Archive> loadAllMessages(Context context) throws InterruptedException {
        final List<Archive>[] fetchedData = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArchiveDAO archiveDAO = databaseConnector.archiveDAO();
                fetchedData[0] = archiveDAO.fetchAll();
                databaseConnector.close();
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
