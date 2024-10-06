package com.afkanerd.deku.DefaultSMS.AdaptersViewModels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;

import java.util.ArrayList;
import java.util.List;

import kotlin.coroutines.Continuation;


public class ConversationPagingSource extends PagingSource<Integer, Conversation> {

    String threadId;
    ConversationDao conversationDao;

    Integer initialKey;

    Context context;

    public ConversationPagingSource(Context context, ConversationDao conversationDao, String threadId,
                                    Integer initialKey) {
        this.conversationDao = conversationDao;
        this.threadId = threadId;
        this.initialKey = initialKey;
        this.context = context;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Conversation> state) {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
//        Integer anchorPosition = state.getAnchorPosition();
        Integer anchorPosition = initialKey;
        if (anchorPosition == null) {
            anchorPosition = state.getAnchorPosition();
            if(anchorPosition == null)
                return null;
        }

        LoadResult.Page<Integer, Conversation> anchorPage = state.closestPageToPosition(anchorPosition);
        if (anchorPage == null) {
            return null;
        }

        Integer prevKey = anchorPage.getPrevKey();
        if (prevKey != null) {
            return prevKey + 1;
        }

        Integer nextKey = anchorPage.getNextKey();
        if (nextKey != null) {
            return nextKey - 1;
        }
        return null;

    }

    @Nullable
    @Override
    public LoadResult<Integer, Conversation> load(
            @NonNull LoadParams<Integer> loadParams,
            @NonNull Continuation<? super LoadResult<Integer, Conversation>> continuation) {
        final List<Conversation>[] list = new List[]{new ArrayList<>()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                list[0] = conversationDao.getDefault(threadId);
//                /**
//                 * Decrypt encrypted messages using their key
//                 */
//                String address = "";
//                if(list[0].size() > 0)
//                    address = list[0].get(0).getAddress();
//                for(int i=0;i<list[0].size(); ++i) {
//                    try {
//                        if ((list[0].get(i).getType() ==
//                                Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT  ||
//                                list[0].get(i).getType() ==
//                                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED ) &&
//                                E2EEHandler.isValidDefaultText(list[0].get(i).getText())) {
//                            try {
//                                byte[] cipherText =
//                                        E2EEHandler.extractTransmissionText(list[0].get(i).getText());
//                                byte[] mk = Base64.decode(list[0].get(i).get_mk(), Base64.NO_WRAP);
//
//                                String keystoreAlias = E2EEHandler.deriveKeystoreAlias( address,
//                                        0);
//
//                                ConversationsThreadsEncryption conversationsThreadsEncryption =
//                                        new ConversationsThreadsEncryption();
//                                ConversationsThreadsEncryptionDao conversationsThreadsEncryptionDao =
//                                        conversationsThreadsEncryption.getDaoInstance(context);
//                                conversationsThreadsEncryption =
//                                        conversationsThreadsEncryptionDao.findByKeystoreAlias(keystoreAlias);
//
//                                byte[] AD = Base64.decode(conversationsThreadsEncryption.getPublicKey(),
//                                        Base64.NO_WRAP);
//
//                                list[0].get(i).setText(new String(E2EEHandler.decrypt(context,
//                                        keystoreAlias, cipherText, mk, AD)));
//                            } catch (Throwable e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    } catch(Exception e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new LoadResult.Page<>(list[0],
                null,
                null,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }
}
