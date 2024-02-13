package java.com.afkanerd.deku.QueueListener;


import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.DefaultSMS.BuildConfig;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ConversationHandler;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClient;
import com.afkanerd.deku.QueueListener.RMQ.RMQConnection;
import com.afkanerd.deku.QueueListener.RMQ.RMQMonitor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RMQConnectionTest {

    Context context;

    public RMQConnectionTest() {
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void multiThreadedTest() throws Exception {
        String address = "+237699911122";
        String body = "Hello world";
        List<SubscriptionInfo> subscriptionInfoList = SIMHandler.getSimCardInformation(context);
        SubscriptionInfo subscriptionInfo = subscriptionInfoList.get(0);

        Conversation conversation = ConversationHandler.buildConversationForSending(context,
                body, subscriptionInfo.getSubscriptionId(), address);
    }

}
