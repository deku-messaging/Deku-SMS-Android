package com.example.swob_deku.Models.Messages;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.format.DateUtils;
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

import com.example.swob_deku.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.ImageViewActivity;
import com.example.swob_deku.Models.Compression;
import com.example.swob_deku.Models.Images.ImageHandler;
import com.example.swob_deku.Models.SIMHandler;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.SMSSendActivity;

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

//public class SingleMessagesThreadRecyclerAdapter extends PagingDataAdapter<SMS, RecyclerView.ViewHolder> {
public class SingleMessagesThreadRecyclerAdapter extends RecyclerView.Adapter {
    Context context;
    Toolbar toolbar;
    String highlightedText;
    View highlightedView;

    private int selectedItemAbsPosition = RecyclerView.NO_POSITION;
    public LiveData<HashMap<String, RecyclerView.ViewHolder>> selectedItem = new MutableLiveData<>();
    MutableLiveData<HashMap<String, RecyclerView.ViewHolder>> mutableSelectedItems = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedMessage = new MutableLiveData<>();
    public MutableLiveData<String[]> retryFailedDataMessage = new MutableLiveData<>();

    public final AsyncListDiffer<SMS> mDiffer = new AsyncListDiffer(this, SMS.DIFF_CALLBACK);


    final int MESSAGE_TYPE_ALL = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

    final int TIMESTAMP_MESSAGE_TYPE_INBOX = 600;
    final int TIMESTAMP_MESSAGE_TYPE_OUTBOX = 700;

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    SecurityECDH securityECDH;
    byte[] secretKey = null;

    String address;

    public SingleMessagesThreadRecyclerAdapter(Context context, String address) throws GeneralSecurityException, IOException {
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

        View view = inflater.inflate(R.layout.messages_thread_sent_layout, parent, false);
        return new MessageSentViewHandler(view);
    }

    public void generateSecretKey() throws GeneralSecurityException, IOException {
        if(securityECDH.hasSecretKey(address))
            secretKey = Base64.decode(securityECDH.securelyFetchSecretKey(address), Base64.DEFAULT);
    }

    private String decryptContent(String input) {
        if(this.secretKey != null &&
                input.getBytes(StandardCharsets.UTF_8).length > 16
                        + SecurityHelpers.ENCRYPTED_WATERMARK_START.length()
                        + SecurityHelpers.ENCRYPTED_WATERMARK_END.length()
                && SecurityHelpers.containersWaterMark(input)) {
            try {
                byte[] encryptedContent = SecurityECDH.decryptAES(Base64.decode(
                        SecurityHelpers.removeEncryptedMessageWaterMark(input), Base64.DEFAULT),
                        secretKey);
                input = new String(encryptedContent, StandardCharsets.UTF_8);
            } catch(Throwable e ) {
                e.printStackTrace();
            }
        }
        return input;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final SMS sms = (SMS) mDiffer.getCurrentList().get(position);
//        final SMS sms = (SMS) mDiffer.getCurrentList().get(holder.getAbsoluteAdapterPosition());
        final String smsId = sms.getId();

        String date = sms.getDate();
        if (DateUtils.isToday(Long.parseLong(date))) {
            DateFormat dateFormat = new SimpleDateFormat("h:mm a");
            date = context.getString(R.string.sms_sent_date_today) + " " + dateFormat.format(new Date(Long.parseLong(date)));
        }
        else {
            DateFormat dateFormat = new SimpleDateFormat("EE h:mm a");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }

        final String text = decryptContent(sms.getBody());

        boolean isEncryptionKey = SecurityHelpers.isKeyExchange(sms.getBody());
        if(holder instanceof MessageReceivedViewHandler) {
            MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
            if(holder instanceof TimestampMessageReceivedViewHandler)
                messageReceivedViewHandler.timestamp.setText(date);
            else
                messageReceivedViewHandler.timestamp.setVisibility(View.GONE);

            TextView receivedMessage = messageReceivedViewHandler.receivedMessage;

            TextView dateView = messageReceivedViewHandler.date;
            dateView.setVisibility(View.INVISIBLE);

            if(text.contains(ImageHandler.IMAGE_HEADER)) {
                ConstraintLayout imageConstraint = messageReceivedViewHandler.imageConstraintLayout;
                try {
                    byte[] body = Base64.decode(text
                            .replace(ImageHandler.IMAGE_HEADER, ""), Base64.DEFAULT);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(body, 0, body.length);
                    messageReceivedViewHandler.imageView.setImageBitmap(bitmap);

                    imageConstraint.setVisibility(View.VISIBLE);
                    receivedMessage.setVisibility(View.GONE);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            else if(isEncryptionKey) {
                ConstraintLayout imageConstraint = messageReceivedViewHandler.imageConstraintLayout;

                messageReceivedViewHandler.imageView.setImageDrawable(context.getDrawable(R.drawable.round_key_24));
                imageConstraint.setVisibility(View.VISIBLE);
                receivedMessage.setVisibility(View.GONE);
            }
            else {
//                receivedMessage.setText(text);
                Helpers.highlightLinks(receivedMessage, text, context.getColor(R.color.primary_text_color));
            }

//            Log.d(getClass().getName(), "Subscription id: " + sms.getSubscriptionId());
//            String dateStatus = date + " • " + SIMHandler.getSubscriptionName(context, sms.getSubscriptionId());
//            String dateStatus = date + " • " + sms.getSubscriptionId();
//            dateView.setText(dateStatus);
            dateView.setText(date);

            messageReceivedViewHandler.receivedMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isHighlighted(sms.getId()))
                        resetSelectedItem(sms.id, true);
                    else if(selectedItem.getValue() != null ){
                        longClickHighlight(messageReceivedViewHandler, smsId);
                    } else {
                        dateView.setVisibility(dateView.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    }
                }
            });

            messageReceivedViewHandler.imageConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ImageViewActivity.class);
                    intent.putExtra(ImageViewActivity.IMAGE_INTENT_EXTRA, sms.getId());
                    intent.putExtra(SMSSendActivity.THREAD_ID, sms.getThreadId());
                    intent.putExtra(SMSSendActivity.ADDRESS, sms.getAddress());
                    intent.putExtra(SMSSendActivity.ID, sms.getId());
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
            });

            messageReceivedViewHandler.receivedMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return longClickHighlight(messageReceivedViewHandler, sms.getId());
                }
            });

        }
        else {
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;
            if(position != 0) {
                messageSentViewHandler.date.setVisibility(View.INVISIBLE);
                messageSentViewHandler.sentMessageStatus.setVisibility(View.INVISIBLE);
            }
            messageSentViewHandler.date.setText(date);

            if(holder instanceof TimestampMessageSentViewHandler)
                messageSentViewHandler.timestamp.setText(date);
            else
                messageSentViewHandler.timestamp.setVisibility(View.GONE);

            final int status = sms.getStatusCode();
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
                statusMessage = "• " + statusMessage;
                messageSentViewHandler.sentMessageStatus.invalidate();
                messageSentViewHandler.date.invalidate();
            }


            messageSentViewHandler.sentMessageStatus.setText(statusMessage);

            if(sms.getBody().contains(ImageHandler.IMAGE_HEADER)) {
                byte[] body = Base64.decode(sms.getBody()
                        .replace(ImageHandler.IMAGE_HEADER, ""), Base64.DEFAULT);

                Bitmap bitmap = BitmapFactory.decodeByteArray(body, 0, body.length);
                messageSentViewHandler.imageView.setImageBitmap(bitmap);

                messageSentViewHandler.imageConstraintLayout.setVisibility(View.VISIBLE);
                messageSentViewHandler.sentMessage.setVisibility(View.GONE);
            }
            else if(isEncryptionKey) {
                ConstraintLayout imageConstraint = messageSentViewHandler.imageConstraintLayout;

                messageSentViewHandler.imageView.setImageDrawable(context.getDrawable(R.drawable.round_key_24));
                imageConstraint.setVisibility(View.VISIBLE);
                messageSentViewHandler.sentMessage.setVisibility(View.GONE);
            }
            else {
//                messageSentViewHandler.sentMessage.setText(text);
//                messageSentViewHandler.highlightLinks(messageSentViewHandler.sentMessage, text);
                Helpers.highlightLinks(messageSentViewHandler.sentMessage, text,
                        context.getColor(R.color.primary_background_color));
            }

            messageSentViewHandler.sentMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isHighlighted(sms.getId()))
                        resetSelectedItem(sms.id, true);
                    else if(selectedItem.getValue() != null) {
                        longClickHighlight(messageSentViewHandler, smsId);
                    }
                    else if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                        Log.d(getClass().getName(), "Resending message...");
                        String[] messageValues = new String[2];
                        messageValues[0] = sms.id;
//                        messageValues[1] = sms.getBody();
                        messageValues[1] = text;
                        retryFailedMessage.setValue(messageValues);
                    }
                    else {
                        int visibility = messageSentViewHandler.date.getVisibility() == View.VISIBLE ?
                                View.INVISIBLE : View.VISIBLE;
                        messageSentViewHandler.date.setVisibility(visibility);
                        messageSentViewHandler.sentMessageStatus.setVisibility(visibility);
                    }
                }
            });

            messageSentViewHandler.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
                        String plainText = SecurityHelpers.removeKeyWaterMark(text);
                        retryFailedDataMessage.setValue(new String[]{sms.id, plainText});
                    }
                }
            });

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
                List<String> newItems = new ArrayList<>();
                newItems.add(smsId);
                mutableSelectedItems.setValue(new HashMap<String, RecyclerView.ViewHolder>() {{
                    put(smsId, messageReceivedViewHandler);
                }});
                messageReceivedViewHandler.highlight();
                messageReceivedViewHandler.setIsRecyclable(false);
                return true;
            } else if (!selectedItem.getValue().containsKey(smsId)) {
                HashMap<String, RecyclerView.ViewHolder> previousItems = selectedItem.getValue();
                previousItems.put(smsId, messageReceivedViewHandler);
                mutableSelectedItems.setValue(previousItems);
                messageReceivedViewHandler.highlight();
                messageReceivedViewHandler.setIsRecyclable(false);
                return true;
            }
        }
        else {
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;
            if(selectedItem.getValue() == null || selectedItem.getValue().isEmpty()) {
                List<String> newItems = new ArrayList<>();
                newItems.add(smsId);
                mutableSelectedItems.setValue(new HashMap<String, RecyclerView.ViewHolder>(){{put(smsId, messageSentViewHandler);}});
                messageSentViewHandler.highlight();
                messageSentViewHandler.setIsRecyclable(false);
                return true;
            }
            else if(!selectedItem.getValue().containsKey(smsId)) {
                HashMap<String, RecyclerView.ViewHolder> previousItems = selectedItem.getValue();
                previousItems.put(smsId, messageSentViewHandler);
                mutableSelectedItems.setValue(previousItems);
                messageSentViewHandler.highlight();
                messageSentViewHandler.setIsRecyclable(false);
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
            Log.d(getClass().getName(), "Content should be highlighted now!");

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

        for(String key: items.keySet())
            resetSelectedItem(key, false);

        mutableSelectedItems.setValue(new HashMap<>());
    }

    public boolean hasSelectedItems() {
        return !(mutableSelectedItems.getValue() == null || mutableSelectedItems.getValue().isEmpty());
    }

    @Override
    public int getItemViewType(int position) {
//        ItemSnapshotList snapshotList = this.snapshot();
        List snapshotList = mDiffer.getCurrentList();
        SMS sms = (SMS) snapshotList.get(position);
//        if (position != 0 && (position == snapshotList.size() - 1 ||
        if (position == snapshotList.size() - 1 ||
                !SMSHandler.isSameHour(sms, (SMS) snapshotList.get(position + 1))) {
            return (sms.getType() == MESSAGE_TYPE_INBOX) ?
                    TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
        } else {
//            int messageType = mDiffer.getCurrentList().get(position).getType();
//            return (messageType > -1 )? messageType : 0;
            return sms.getType();
        }
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void removeAllItems(String[] _keys) {
        List<String> keys = new ArrayList<>(Arrays.asList(_keys));
        List<SMS> sms = new ArrayList<>(mDiffer.getCurrentList());
        List<SMS> smsNew = new ArrayList<>();
        for(SMS sms1 : sms)
            if(!keys.contains(sms1.getId()))
                smsNew.add(sms1);

        mDiffer.submitList(smsNew);
    }

    public void removeItem(String keys) {
        List<SMS> sms = new ArrayList<>(mDiffer.getCurrentList());
        for(int i=0; i< sms.size(); ++i) {
            if(sms.get(i).getId().equals(keys)) {
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

         ConstraintLayout constraintLayout, imageConstraintLayout;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_sent_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
            timestamp = itemView.findViewById(R.id.sent_message_date_segment);
            constraintLayout = itemView.findViewById(R.id.message_sent_constraint);
            imageConstraintLayout = itemView.findViewById(R.id.message_sent_image_container);
            imageView = itemView.findViewById(R.id.message_sent_image_view);
        }

        static class ClickableURLSpan extends URLSpan {
            ClickableURLSpan(String url) {
                super(url);
            }

            @Override
            public void onClick(View widget) {
                Log.d(getClass().getName(), "Link clicked!");
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
        }
    }

    public static class MessageReceivedViewHandler extends RecyclerView.ViewHolder {
        TextView receivedMessage;
        TextView date;
        TextView timestamp;
        ImageView imageView;
        ConstraintLayout constraintLayout, imageConstraintLayout;

        public MessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.message_received_text);
            date = itemView.findViewById(R.id.message_thread_received_date_text);
            timestamp = itemView.findViewById(R.id.received_message_date_segment);
            constraintLayout = itemView.findViewById(R.id.message_received_constraint);
            imageConstraintLayout = itemView.findViewById(R.id.message_received_image_container);
            imageView = itemView.findViewById(R.id.message_received_image_view);

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
        }
    }

}
