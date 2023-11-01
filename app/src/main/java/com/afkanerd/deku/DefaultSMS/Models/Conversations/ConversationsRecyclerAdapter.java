package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMS;
import com.afkanerd.deku.DefaultSMS.Models.SMS.SMSHandler;
import com.afkanerd.deku.E2EE.Security.SecurityAES;
import com.afkanerd.deku.E2EE.Security.SecurityECDH;
import com.afkanerd.deku.E2EE.Security.SecurityHelpers;
import com.afkanerd.deku.DefaultSMS.Models.Contacts.Contacts;
import com.afkanerd.deku.DefaultSMS.R;

import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.getstream.avatarview.AvatarView;

//public class ConversationsRecyclerAdapter extends PagingDataAdapter<SMS, RecyclerView.ViewHolder> {
public class ConversationsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    Toolbar toolbar;
    String highlightedText;
    View highlightedView;

    private int selectedItemAbsPosition = RecyclerView.NO_POSITION;
    final static int BOTTOM_MARGIN = 4;
    public LiveData<HashMap<String, RecyclerView.ViewHolder>> selectedItem = new MutableLiveData<>();
    MutableLiveData<HashMap<String, RecyclerView.ViewHolder>> mutableSelectedItems = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedMessage = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedDataMessage = new MutableLiveData<>();

    public final AsyncListDiffer<Conversation> mDiffer = new AsyncListDiffer(this, Conversation.DIFF_CALLBACK);


    final int MESSAGE_TYPE_ALL = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;

    final int MESSAGE_KEY_INBOX = 400;
    final int MESSAGE_START_TYPE_INBOX = 401;
    final int MESSAGE_MIDDLE_TYPE_INBOX = 402;
    final int MESSAGE_END_TYPE_INBOX = 403;

    final int TIMESTAMP_MESSAGE_START_TYPE_INBOX = 601;
    final int TIMESTAMP_MESSAGE_TYPE_INBOX = 600;
    final int TIMESTAMP_KEY_TYPE_INBOX = 800;

    final int MESSAGE_KEY_OUTBOX = 500;
    final int TIMESTAMP_MESSAGE_TYPE_OUTBOX = 700;
    final int TIMESTAMP_KEY_TYPE_OUTBOX = 900;

    final int TIMESTAMP_MESSAGE_START_TYPE_OUTBOX = 504;
    final int MESSAGE_START_TYPE_OUTBOX = 501;
    final int MESSAGE_MIDDLE_TYPE_OUTBOX = 502;
    final int MESSAGE_END_TYPE_OUTBOX = 503;

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    SecurityECDH securityECDH;
    byte[] secretKey = null;

    String address;

    public String searchString;

    private boolean animation = false;
    public ConversationsRecyclerAdapter(Context context, String address) throws GeneralSecurityException, IOException {
//        super(SMS.DIFF_CALLBACK);
        this.context = context;
        this.selectedItem = mutableSelectedItems;

        this.securityECDH = new SecurityECDH(context);

        this.address = address;

        if(securityECDH.hasSecretKey(address))
            secretKey = Base64.decode(securityECDH.securelyFetchSecretKey(address), Base64.DEFAULT);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
        LayoutInflater inflater = LayoutInflater.from(this.context);

        if( viewType == TIMESTAMP_MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new TimestampMessageReceivedViewHandler(view);
        }
        else if( viewType == MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new TimestampMessageSentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new KeySentViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new TimestampKeySentViewHandler(view);
        }
        else if( viewType == MESSAGE_KEY_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new KeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_KEY_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new TimestampKeyReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new TimestampKeyReceivedStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_INBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_received_layout, parent, false);
            return new MessageReceivedMiddleViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new TimestampKeySentStartGroupViewHandler(view);
        }
        else if( viewType == MESSAGE_START_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentStartViewHandler(view);
        }
        else if( viewType == MESSAGE_END_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentEndViewHandler(view);
        }
        else if( viewType == MESSAGE_MIDDLE_TYPE_OUTBOX ) {
            View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
            return new MessageSentMiddleViewHandler(view);
        }

        View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
        return new MessageSentViewHandler(view);
    }

    private String decryptContent(String input) {
        if(this.secretKey != null &&
                input.getBytes(StandardCharsets.UTF_8).length > 16
                        + SecurityHelpers.ENCRYPTED_WATERMARK_START.length()
                        + SecurityHelpers.ENCRYPTED_WATERMARK_END.length()
                && SecurityHelpers.containersWaterMark(input)) {
            try {
                byte[] encryptedContent = SecurityAES.decrypt_256_cbc(Base64.decode(
                        SecurityHelpers.removeEncryptedMessageWaterMark(input), Base64.DEFAULT),
                        secretKey);
                input = new String(encryptedContent, StandardCharsets.UTF_8);
            } catch(Throwable e ) {
                e.printStackTrace();
            }
        }
        return input;
    }

    public Spannable highlightSubstringYellow(String text) {
        // Find all occurrences of the substring in the text.
        List<Integer> startIndices = new ArrayList<>();
        int index = text.indexOf(searchString);
        while (index >= 0) {
            startIndices.add(index);
            index = text.indexOf(searchString, index + searchString.length());
        }

        // Create a SpannableString object.
        SpannableString spannableString = new SpannableString(text);

        // Set the foreground color of the substring to yellow.
        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.YELLOW);
        for (int startIndex : startIndices) {
            spannableString.setSpan(backgroundColorSpan, startIndex, startIndex + searchString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Conversation sms = mDiffer.getCurrentList().get(position);
        final String smsId = String.valueOf(sms.getMessage_id());

        if(animation) {
            AnimatorSet animatorSet = new AnimatorSet();
            // Translation animation: Move the item up from the bottom
            ObjectAnimator translationAnim = ObjectAnimator.ofFloat(holder.itemView,
                    "translationY", 200f, 0f);
            translationAnim.setDuration(200);

            // Fade-in animation: Make the item gradually appear
            ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(holder.itemView,
                    "alpha", 0f, 1f);
            fadeAnim.setDuration(200);

            // Play both animations together
            animatorSet.playTogether(translationAnim, fadeAnim);
            animatorSet.start();
            animation = false;
        }

        String date = Helpers.formatDateExtended(context, sms.date);

        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String timeStamp = dateFormat.format(new Date(sms.date));

        String _text = sms.getBody();

        final String text = decryptContent(_text);
        if(holder instanceof MessageReceivedViewHandler) {
            MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
            if(holder instanceof TimestampMessageReceivedViewHandler || holder instanceof TimestampKeyReceivedViewHandler)
                messageReceivedViewHandler.timestamp.setText(date);

            TextView receivedMessage = messageReceivedViewHandler.receivedMessage;

            TextView dateView = messageReceivedViewHandler.date;

            Helpers.highlightLinks(receivedMessage, text, context.getColor(R.color.primary_text_color));
            if(searchString != null) {
                String sText = receivedMessage.getText().toString();
                receivedMessage.setText(highlightSubstringYellow(sText));
            }

            if(sms.getSubscription_id() > 0) {
                String subscriptionName = SIMHandler.getSubscriptionName(context,
                        String.valueOf(sms.getSubscription_id()));
                if(subscriptionName != null && !subscriptionName.isEmpty())
                    timeStamp += " • " + subscriptionName;
            }

            dateView.setText(timeStamp);

            messageReceivedViewHandler.receivedMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isHighlighted(String.valueOf(sms.getMessage_id())))
                        resetSelectedItem(String.valueOf(sms.getMessage_id()), true);
                    else if(selectedItem.getValue() != null ){
                        longClickHighlight(messageReceivedViewHandler, smsId);
                    } else {
//                        dateView.setVisibility(dateView.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                    }
                }
            });

            messageReceivedViewHandler.receivedMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return longClickHighlight(messageReceivedViewHandler, String.valueOf(sms.getMessage_id()));
                }
            });

        }
        else if(holder instanceof MessageSentViewHandler){
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;

            messageSentViewHandler.date.setText(timeStamp);

            if(holder instanceof TimestampMessageSentViewHandler || holder instanceof  TimestampKeySentViewHandler)
                messageSentViewHandler.timestamp.setText(date);

            final int status = sms.getStatus();
            String statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_COMPLETE ?
                    context.getString(R.string.sms_status_delivered) : context.getString(R.string.sms_status_sent);

            if(status == Telephony.TextBasedSmsColumns.STATUS_PENDING )
                statusMessage = context.getString(R.string.sms_status_sending);
            if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED ) {
                statusMessage = context.getString(R.string.sms_status_failed);
                messageSentViewHandler.sentMessageStatus.setVisibility(View.VISIBLE);
                messageSentViewHandler.date.setVisibility(View.VISIBLE);
                messageSentViewHandler.sentMessageStatus.setTextColor(
                        context.getResources().getColor(R.color.failed_red, context.getTheme()));
                messageSentViewHandler.date.setTextColor(
                        context.getResources().getColor(R.color.failed_red, context.getTheme()));
            } else {
                statusMessage = " • " + statusMessage;
                messageSentViewHandler.sentMessageStatus.invalidate();
                messageSentViewHandler.date.invalidate();
            }
            if(sms.getSubscription_id() > 0) {
                String subscriptionName = SIMHandler.getSubscriptionName(context,
                        String.valueOf(sms.getSubscription_id()));
                if(subscriptionName != null && !subscriptionName.isEmpty())
                    statusMessage += " • " + subscriptionName;
            }

            messageSentViewHandler.sentMessageStatus.setText(statusMessage);

            Helpers.highlightLinks(messageSentViewHandler.sentMessage, text,
                    context.getColor(R.color.primary_background_color));
            if(searchString != null) {
                String sText = messageSentViewHandler.sentMessage.getText().toString();
                messageSentViewHandler.sentMessage.setText(highlightSubstringYellow(sText));
            }

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isHighlighted(String.valueOf(sms.getMessage_id())))
                        resetSelectedItem(String.valueOf(sms.getMessage_id()), true);
                    else if(selectedItem.getValue() != null) {
                        longClickHighlight(messageSentViewHandler, smsId);
                    }
                    else if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                        String[] messageValues = new String[2];
                        messageValues[0] = String.valueOf(sms.getMessage_id());

                        String _text = text;
                        if(holder instanceof KeySentViewHandler) {
                            _text = SecurityHelpers.removeKeyWaterMark(text);
                            messageValues[1] = _text;
                            retryFailedDataMessage.setValue(messageValues);
                        }
                        else {
                            messageValues[1] = _text;
                            retryFailedMessage.setValue(messageValues);
                        }
                    }
                    else {
                        int visibility = messageSentViewHandler.date.getVisibility() == View.VISIBLE ?
                                View.GONE : View.VISIBLE;
                        messageSentViewHandler.date.setVisibility(visibility);
                        messageSentViewHandler.sentMessageStatus.setVisibility(visibility);
                    }
                }
            };

            messageSentViewHandler.imageView.setOnClickListener(onClickListener);
            messageSentViewHandler.sentMessage.setOnClickListener(onClickListener);
            messageSentViewHandler.sentMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return longClickHighlight(messageSentViewHandler, smsId);
                }
            });
        }

        checkForAbsPositioning(smsId, holder);
    }

    private boolean longClickHighlight(RecyclerView.ViewHolder holder, String smsId) {
        if(holder instanceof MessageReceivedViewHandler) {
            MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
            if (selectedItem.getValue() == null || selectedItem.getValue().isEmpty()) {
                mutableSelectedItems.setValue(new HashMap<String, RecyclerView.ViewHolder>() {{
                    put(smsId, messageReceivedViewHandler);
                }});
                messageReceivedViewHandler.highlight();
                return true;
            } else if (!selectedItem.getValue().containsKey(smsId)) {
                HashMap<String, RecyclerView.ViewHolder> previousItems = selectedItem.getValue();
                previousItems.put(smsId, messageReceivedViewHandler);
                mutableSelectedItems.setValue(previousItems);
                messageReceivedViewHandler.highlight();
                return true;
            }
        }
        else {
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;
            if(selectedItem.getValue() == null || selectedItem.getValue().isEmpty()) {
                mutableSelectedItems.setValue(new HashMap<String, RecyclerView.ViewHolder>(){{put(smsId, messageSentViewHandler);}});
                messageSentViewHandler.highlight();
                return true;
            }
            else if(!selectedItem.getValue().containsKey(smsId)) {
                HashMap<String, RecyclerView.ViewHolder> previousItems = selectedItem.getValue();
                previousItems.put(smsId, messageSentViewHandler);
                mutableSelectedItems.setValue(previousItems);
                messageSentViewHandler.highlight();
                return true;
            }
        }
        return false;
    }

    public boolean isHighlighted(String smsId){
        if(selectedItem.getValue() == null)
            return false;
        return selectedItem.getValue().containsKey(smsId);
    }

    public void checkForAbsPositioning(String smsId, RecyclerView.ViewHolder holder) {
        if(selectedItem.getValue() != null && selectedItem.getValue().containsKey(smsId)) {

            if (holder instanceof MessageReceivedViewHandler)
                ((MessageReceivedViewHandler) holder).highlight();

            else if (holder instanceof MessageSentViewHandler)
                ((MessageSentViewHandler) holder).highlight();
            holder.setIsRecyclable(false);
        }
    }

    public void resetSelectedItem(String key, boolean removeList) {
        HashMap<String, RecyclerView.ViewHolder> items = mutableSelectedItems.getValue();
        if(items != null) {
            RecyclerView.ViewHolder view = items.get(key);

            if(view != null) {
                view.setIsRecyclable(true);
                if (view instanceof MessageReceivedViewHandler)
                    ((MessageReceivedViewHandler) view).unHighlight();

                else if (view instanceof MessageSentViewHandler)
                    ((MessageSentViewHandler) view).unHighlight();
            }
            if(removeList) {
                items.remove(key);
                mutableSelectedItems.setValue(items);
            }
        }
    }

    public void resetAllSelectedItems() {
        HashMap<String, RecyclerView.ViewHolder> items = mutableSelectedItems.getValue();

        for(String key: items.keySet()) {
            items.get(key).setIsRecyclable(true);
            resetSelectedItem(key, false);
        }

        mutableSelectedItems.setValue(new HashMap<>());
    }

    public boolean hasSelectedItems() {
        return !(mutableSelectedItems.getValue() == null || mutableSelectedItems.getValue().isEmpty());
    }

    @Override
    public int getItemViewType(int position) {
        List<Conversation> snapshotList = mDiffer.getCurrentList();
        Conversation sms = snapshotList.get(position);

        boolean isEncryptionKey = SecurityHelpers.isKeyExchange(sms.getBody());

        int oldestItemPos = snapshotList.size() - 1;
        int newestItemPos = 0;

/*
        if(isEncryptionKey) {
            if(position == oldestItemPos || snapshotList.size() < 2 ||
                    (position > (oldestItemPos -1) && SMSHandler.isSameMinute(sms, (Conversation) snapshotList.get(position -1)) )) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_KEY_TYPE_INBOX : TIMESTAMP_KEY_TYPE_OUTBOX;
            }
            else {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        MESSAGE_KEY_INBOX : MESSAGE_KEY_OUTBOX;
            }
        }

        if(snapshotList.size() < 2) {
            return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                    TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
        }

        */
/**
         * start message - with timestamp
         * start message - without timestamp
         *//*


        if(position == oldestItemPos) { // - minus
            SMS secondMessage = (SMS) snapshotList.get(position - 1);
            if(sms.getType() == secondMessage.getType() && SMSHandler.isSameMinute(sms, secondMessage)) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
            }
            else {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
        }

        if(position > 0) {
            SMS secondMessage = (SMS) snapshotList.get(position + 1);
            SMS thirdMessage = (SMS) snapshotList.get(position - 1);
            if(!SMSHandler.isSameHour(sms, secondMessage)) {
                if(sms.getType() == thirdMessage.getType() && SMSHandler.isSameMinute(sms, thirdMessage))
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_START_TYPE_INBOX : TIMESTAMP_MESSAGE_START_TYPE_OUTBOX;
                else
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }
            if(sms.getType() == thirdMessage.getType() && SMSHandler.isSameMinute(sms, thirdMessage)) {
                if(sms.getType() != secondMessage.getType() || !SMSHandler.isSameMinute(sms, secondMessage)) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_START_TYPE_INBOX : MESSAGE_START_TYPE_OUTBOX;
                }
            }
            if(sms.getType() == secondMessage.getType() && sms.getType() == thirdMessage.getType()) {
                if(SMSHandler.isSameMinute(sms, secondMessage) && SMSHandler.isSameMinute(sms, thirdMessage)) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_MIDDLE_TYPE_INBOX : MESSAGE_MIDDLE_TYPE_OUTBOX;
                }
            }
            if(sms.getType() == secondMessage.getType() && SMSHandler.isSameMinute(sms, secondMessage)) {
                if(sms.getType() != thirdMessage.getType() || !SMSHandler.isSameMinute(sms, thirdMessage)) {
                    return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                            MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
                }
            }
        }
        if(position == newestItemPos) { // - minus
            SMS secondMessage = (SMS) snapshotList.get(position + 1);
            if(!SMSHandler.isSameHour(sms, secondMessage)) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
            }

            if(sms.getType() == secondMessage.getType() && SMSHandler.isSameMinute(sms, secondMessage)) {
                return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                        MESSAGE_END_TYPE_INBOX : MESSAGE_END_TYPE_OUTBOX;
            }
        }
*/

        return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                MESSAGE_TYPE_INBOX : MESSAGE_TYPE_OUTBOX;
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void removeAllItems(String[] _keys) {
        List<String> keys = new ArrayList<>(Arrays.asList(_keys));
        List<Conversation> sms = new ArrayList<>(mDiffer.getCurrentList());
        List<Conversation> smsNew = new ArrayList<>();
        for(Conversation sms1 : sms)
            if(!keys.contains(String.valueOf(sms1.getMessage_id())))
                smsNew.add(sms1);

        mDiffer.submitList(smsNew);
    }

    public void removeItem(String keys) {
        List<Conversation> sms = new ArrayList<>(mDiffer.getCurrentList());
        for(int i=0; i< sms.size(); ++i) {
            if(String.valueOf(sms.get(i).getMessage_id()).equals(keys)) {
                sms.remove(i);
                break;
            }
        }
        mDiffer.submitList(sms);
    }

    public static class MessageSentViewHandler extends RecyclerView.ViewHolder {
         TextView sentMessage;
         TextView sentMessageStatus;
         TextView date;
         TextView timestamp;
        ImageView imageView;
        ConstraintLayout constraintLayout, imageConstraintLayout, constraint4;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_sent_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
            timestamp = itemView.findViewById(R.id.sent_message_date_segment);
            constraintLayout = itemView.findViewById(R.id.message_sent_constraint);
            imageConstraintLayout = itemView.findViewById(R.id.message_sent_image_container);
            imageView = itemView.findViewById(R.id.message_sent_image_view);
            constraint4 = itemView.findViewById(R.id.constraintLayout4);
        }

        static class ClickableURLSpan extends URLSpan {
            ClickableURLSpan(String url) {
                super(url);
            }

            @Override
            public void onClick(View widget) {
                Uri uri = Uri.parse(getURL());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                widget.getContext().startActivity(intent);
            }
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_drawable);
        }
    }

    public static class TimestampMessageSentViewHandler extends MessageSentViewHandler {
        public TimestampMessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeySentViewHandler extends MessageSentViewHandler {
        public KeySentViewHandler(@NonNull View itemView) {
            super(itemView);
            imageView.setImageDrawable(itemView.getContext().getDrawable(R.drawable.round_key_24));
            imageConstraintLayout.setVisibility(View.VISIBLE);
            constraintLayout.setVisibility(View.GONE);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_drawable);
        }
    }

    public static class TimestampKeySentViewHandler extends KeySentViewHandler {
        public TimestampKeySentViewHandler(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class MessageReceivedViewHandler extends RecyclerView.ViewHolder {
        TextView receivedMessage;
        TextView date;
        TextView timestamp;

        AvatarView contactInitials;
        public ImageView imageView;
        ConstraintLayout constraintLayout, imageConstraintLayout, constraint3;

        public MessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.message_received_text);
            date = itemView.findViewById(R.id.message_thread_received_date_text);
            timestamp = itemView.findViewById(R.id.received_message_date_segment);
            constraintLayout = itemView.findViewById(R.id.message_received_constraint);
            imageConstraintLayout = itemView.findViewById(R.id.message_received_image_container);
            imageView = itemView.findViewById(R.id.message_received_image_view);
            constraint3 = itemView.findViewById(R.id.constraintLayout3);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_highlighted_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_drawable);
        }
    }
    public static class TimestampMessageReceivedViewHandler extends MessageReceivedViewHandler {
        public TimestampMessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeyReceivedViewHandler extends MessageReceivedViewHandler {

        public KeyReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.message_received_image_view);

            imageView.setImageDrawable(itemView.getContext().getDrawable(R.drawable.round_key_24));
            imageConstraintLayout.setVisibility(View.VISIBLE);
            constraintLayout.setVisibility(View.GONE);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_highlighted_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_drawable);
        }
    }
    public static class TimestampKeyReceivedViewHandler extends KeyReceivedViewHandler {
        public TimestampKeyReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }


    public static class TimestampKeySentStartGroupViewHandler extends MessageSentViewHandler {
        public TimestampKeySentStartGroupViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }

    public static class MessageSentStartViewHandler extends MessageSentViewHandler {
        public MessageSentStartViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));

            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }
    public static class MessageSentEndViewHandler extends MessageSentViewHandler {
        public MessageSentEndViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_end_view_drawable));
        }
        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_end_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_end_view_drawable);
        }
    }

    public static class MessageSentMiddleViewHandler extends MessageSentViewHandler {
        public MessageSentMiddleViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_middle_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_middle_hightlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_middle_view_drawable);
        }
    }
    public static class TimestampKeyReceivedStartGroupViewHandler extends TimestampMessageReceivedViewHandler {
        public TimestampKeyReceivedStartGroupViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_mesages_start_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint3.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint3.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_start_view_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_mesages_start_view_drawable);
        }
    }

    public static class MessageReceivedStartViewHandler extends MessageReceivedViewHandler {
        public MessageReceivedStartViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_mesages_start_view_drawable));

            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint3.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint3.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_start_view_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_mesages_start_view_drawable);
        }
    }
    public static class MessageReceivedEndViewHandler extends MessageReceivedViewHandler {
        public MessageReceivedEndViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_end_view_drawable));
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_end_view_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_end_view_drawable);
        }
    }

    public static class MessageReceivedMiddleViewHandler extends MessageReceivedViewHandler {
        public MessageReceivedMiddleViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_middle_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint3.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint3.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_middle_view_highlight_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.received_messages_middle_view_drawable);
        }
    }
}
