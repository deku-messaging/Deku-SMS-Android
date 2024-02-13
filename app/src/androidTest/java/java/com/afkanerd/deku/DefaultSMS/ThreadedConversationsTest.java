package java.com.afkanerd.deku.DefaultSMS;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ThreadedConversationsTest {

    Context context;

   public ThreadedConversationsTest() {
       context = InstrumentationRegistry.getInstrumentation().getTargetContext();
   }
    @Test
    public void testThreadedConversationsBuildMethods() {
       ThreadedConversations threadedConversation = new ThreadedConversations();
        ThreadedConversationsDao threadedConversationsDao = threadedConversation.getDaoInstance(context);
        List<ThreadedConversations> threadedConversations = threadedConversationsDao.getAll();
        threadedConversation.close();

        Conversation conversation = new Conversation();
        ConversationDao conversationDao = conversation.getDaoInstance(context);
        List<Conversation> conversations = conversationDao.getComplete();

        assertEquals(conversations.get(0).getText(), threadedConversations.get(0).getSnippet());
    }
}
