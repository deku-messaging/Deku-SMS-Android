package com.afkanerd.deku

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameTable
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.afkanerd.deku.Datastore.Migrate16To17
import com.afkanerd.deku.Datastore.Migrate19To20
import com.afkanerd.deku.Datastore.Migrate20To21
import com.afkanerd.deku.Datastore.Migrate21To22
import com.afkanerd.deku.Datastore.Migrate22To23
import com.afkanerd.deku.Datastore.Migrate28To29
import com.afkanerd.deku.RemoteListeners.Models.RemoteListener.RemoteListenersQueuesDao
import com.afkanerd.deku.RemoteListeners.Models.RemoteListenerDAO
import com.afkanerd.deku.RemoteListeners.Models.RemoteListeners
import com.afkanerd.deku.RemoteListeners.Models.RemoteListenersQueues
import com.afkanerd.deku.Forwarder.data.dao.GatewayServerDAO
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import com.afkanerd.smswithoutborders_libsmsmms.data.Cryptography.getDatabasePassword
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import kotlin.concurrent.Volatile


@Database(
    entities = [GatewayServer::class, RemoteListenersQueues::class, RemoteListeners::class],
    version = 31,
    autoMigrations = [AutoMigration(from = 9, to = 10), AutoMigration(
        from = 10,
        to = 11
    ), AutoMigration(from = 11, to = 12), AutoMigration(
        from = 12,
        to = 13
    ), AutoMigration(from = 13, to = 14), AutoMigration(
        from = 14,
        to = 15
    ), AutoMigration(from = 15, to = 16), AutoMigration(
        from = 16,
        to = 17,
        spec = Migrate16To17::class
    ), AutoMigration(from = 17, to = 18), AutoMigration(
        from = 18,
        to = 19
    ), AutoMigration(from = 19, to = 20, spec = Migrate19To20::class), AutoMigration(
        from = 20,
        to = 21,
        spec = Migrate20To21::class
    ), AutoMigration(from = 21, to = 22, spec = Migrate21To22::class), AutoMigration(
        from = 22,
        to = 23,
        spec = Migrate22To23::class
    ), AutoMigration(from = 23, to = 24), AutoMigration(
        from = 24,
        to = 25
    ), AutoMigration(from = 25, to = 26), AutoMigration(
        from = 26,
        to = 27
    ), AutoMigration(from = 27, to = 28), AutoMigration(
        from = 28,
        to = 29,
        spec = Migrate28To29::class
    ), AutoMigration(from = 29, to = 30), AutoMigration(from = 30, to = 31)]
)
abstract class Datastore : RoomDatabase() {
    abstract fun gatewayServerDAO(): GatewayServerDAO

    abstract fun remoteListenerDAO(): RemoteListenerDAO
    abstract fun remoteListenersQueuesDao(): RemoteListenersQueuesDao

    init {
        System.loadLibrary("sqlcipher")
    }

    @DeleteTable(tableName = "CustomKeyStore")
    internal class Migrate16To17 : AutoMigrationSpec

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = "Conversation",
            columnName = "isBlocked"
        ),
        DeleteColumn(
            tableName = "Conversation",
            columnName = "isMute"
        ),
        DeleteColumn(tableName = "Conversation", columnName = "isSecured")
    )
    @DeleteTable.Entries(DeleteTable(tableName = "ThreadedConversations"))
    internal class Migrate19To20 : AutoMigrationSpec

    @DeleteTable.Entries(DeleteTable(tableName = "ConversationsThreadsEncryption"))
    internal class Migrate20To21 : AutoMigrationSpec

    @RenameTable.Entries(
        RenameTable(
            fromTableName = "GatewayClientProjects",
            toTableName = "RemoteListenersQueues"
        )
    )
    internal class Migrate21To22 : AutoMigrationSpec

    @RenameTable.Entries(
        RenameTable(
            fromTableName = "GatewayClient",
            toTableName = "RemoteListeners"
        )
    )
    internal class Migrate22To23 : AutoMigrationSpec

    @DeleteTable.Entries(
        DeleteTable(tableName = "Archive"),
        DeleteTable(tableName = "Conversation"),
        DeleteTable(
            tableName = "ThreadsConfigurations"
        )
    )
    internal class Migrate28To29 : AutoMigrationSpec

    companion object {
        @Volatile
        private var datastore: Datastore? = null
        private const val dbKeystoreAlias = "afkanerd.smswithoutborders.deku.sms_mms_keystore_alias"
        @JvmField
        var databaseName: String = "afkanerd.smswithoutborders.dekusms.db"

        @JvmStatic
        @Synchronized
        fun getDatastore(context: Context): Datastore {
            if (datastore == null) {
                create(context)
            }
            return datastore!!
        }

        private fun create(context: Context) {
            getDatabasePassword(context, dbKeystoreAlias).use { password ->
                val databaseFile = context.getDatabasePath(databaseName)

                password.useRaw { rawBytes ->
                    datastore = databaseBuilder(
                        context = context.applicationContext,
                        klass = Datastore::class.java,
                        databaseFile.absolutePath,
                    )
                        .openHelperFactory(SupportOpenHelperFactory(rawBytes))
                        .fallbackToDestructiveMigration(false)
                        .build()
                }
            }
        }
    }
}
