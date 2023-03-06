package com.example.swob_deku.Models.Messages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.Telephony;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.DataHelper;
import com.example.swob_deku.ImageViewActivity;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.google.android.material.card.MaterialCardView;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class SingleMessagesThreadRecyclerAdapter extends RecyclerView.Adapter{
    Context context;
    int renderLayoutReceived, renderLayoutSent, renderLayoutTimestamp;
    Long focusId;
    RecyclerView view;
    String searchString;
    Toolbar toolbar;
    String highlightedText;
    View highlightedView;

    private final AsyncListDiffer<SMS> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    final int MESSAGE_TYPE_ALL = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

    final int TIMESTAMP_MESSAGE_TYPE_INBOX = 600;
    final int TIMESTAMP_MESSAGE_TYPE_OUTBOX = 700;

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    public SingleMessagesThreadRecyclerAdapter(Context context, int renderLayoutReceived,
                                               int renderLayoutSent,
                                               int renderLayoutTimestamp,
                                               Long focusId,
                                               String searchString,
                                               RecyclerView view, Toolbar toolbar) {
        this.context = context;
        this.renderLayoutReceived = renderLayoutReceived;
        this.renderLayoutSent = renderLayoutSent;
        this.renderLayoutTimestamp = renderLayoutTimestamp;
        this.focusId = focusId;
        this.searchString = searchString;
        this.view = view;
        this.toolbar = toolbar;


        enableToolbar();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
        LayoutInflater inflater = LayoutInflater.from(this.context);

        if( viewType == TIMESTAMP_MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(this.renderLayoutReceived, parent, false);
            return new TimestampMessageReceivedViewHandler(view);
        }
        else if( viewType == MESSAGE_TYPE_INBOX ) {
            View view = inflater.inflate(this.renderLayoutReceived, parent, false);
            return new MessageReceivedViewHandler(view);
        }
        else if( viewType == TIMESTAMP_MESSAGE_TYPE_OUTBOX ) {
            View view = inflater.inflate(this.renderLayoutSent, parent, false);
            return new TimestampMessageSentViewHandler(view);
        }

        View view = inflater.inflate(this.renderLayoutSent, parent, false);
        return new MessageSentViewHandler(view);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

//        for(int i=0;i<mDiffer.getCurrentList().size();++i) {
//            SMS sms = mDiffer.getCurrentList().get(i);
//            if (focusId!=null
//                    && searchString!=null
//                    && sms.id.equals(Long.toString(focusId))
//                    && !searchString.isEmpty()) {
//                String text = sms.getBody();
//                Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);
//
//                for (int index = text.indexOf(searchString); index >= 0; index = text.indexOf(searchString, index + 1)) {
//                    spannable.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.highlight_yellow)),
//                            index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.black)),
//                            index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                }
//
//                // TODO: not working
////                switch (holder.getItemViewType()) {
////                    case 1: {
////                        ((MessageReceivedViewHandler) holder).receivedMessage.setText(spannable);
////                        break;
////                    }
////                    case 5:
////                    case 4:
////                    case 2: {
////                        ((MessageSentViewHandler) holder).sentMessage.setText(spannable);
////                        break;
////                    }
////                }
////                break;
//                this.view.smoothScrollToPosition(i);
//            }
//        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final SMS sms = mDiffer.getCurrentList().get(position);
        // TODO: for search
//        if(focusId != -1 && sms.getId() != null && Long.valueOf(sms.getId()) == focusId) {
//            final int finalPosition = position;
//            this.focusPosition = finalPosition;
//        }

        String date = sms.getDate();
        if (DateUtils.isToday(Long.parseLong(date))) {
            DateFormat dateFormat = new SimpleDateFormat("h:mm a");
            date = "Today " + dateFormat.format(new Date(Long.parseLong(date)));
        }
        else {
            DateFormat dateFormat = new SimpleDateFormat("EE h:mm a");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }

        if(holder instanceof MessageReceivedViewHandler) {
            MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
            if(holder instanceof TimestampMessageReceivedViewHandler)
                messageReceivedViewHandler.timestamp.setText(date);
            else
                messageReceivedViewHandler.timestamp.setVisibility(View.GONE);

            TextView receivedMessage = messageReceivedViewHandler.receivedMessage;
            receivedMessage.setText(sms.getBody());

            TextView dateView = messageReceivedViewHandler.date;
            dateView.setVisibility(View.INVISIBLE);
            dateView.setText(date);
        }
        else {
            MessageSentViewHandler messageSentViewHandler = (MessageSentViewHandler) holder;
            messageSentViewHandler.sentMessage.setText(sms.getBody());
            messageSentViewHandler.date.setText(date);

            if(holder instanceof TimestampMessageSentViewHandler)
                messageSentViewHandler.timestamp.setText(date);
            else
                messageSentViewHandler.timestamp.setVisibility(View.GONE);

            messageSentViewHandler.date.setVisibility(View.INVISIBLE);

            final int status = sms.getStatusCode();
            String statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_COMPLETE ?
                    "delivered" : "sent";

            statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_PENDING ?
                    "sending..." : statusMessage;

            statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_FAILED ?
                    "failed!" : statusMessage;

            statusMessage = "• " + statusMessage;

            messageSentViewHandler.sentMessageStatus.setText(statusMessage);

            if(mDiffer.getCurrentList().size() -1 != position ) {
                messageSentViewHandler.sentMessageStatus.setVisibility(View.INVISIBLE);
            }

        }
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<SMS> list) {
        mDiffer.submitList(list);
    }

    @Override
    public int getItemViewType(int position) {
//        if(mDiffer.getCurrentList().get(position).isDatesOnly())
//            return 100;
//
//        int messageType = mDiffer.getCurrentList().get(position).getType();
//        return (messageType > -1 )? messageType : 0;

        if (position != 0 && (position == mDiffer.getCurrentList().size() - 1 ||
                !SMSHandler.isSameHour(mDiffer.getCurrentList().get(position),
                        mDiffer.getCurrentList().get(position + 1)))) {
            return (mDiffer.getCurrentList().get(position).getType() == MESSAGE_TYPE_INBOX) ?
                    TIMESTAMP_MESSAGE_TYPE_INBOX : TIMESTAMP_MESSAGE_TYPE_OUTBOX;
        } else {
            int messageType = mDiffer.getCurrentList().get(position).getType();
            return (messageType > -1 )? messageType : 0;
        }
    }


    public class MessageSentViewHandler extends RecyclerView.ViewHolder {
         TextView sentMessage;
         TextView sentMessageStatus;
         TextView date;
         TextView timestamp;
         MaterialCardView layout;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_thread_sent_card_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
            layout = itemView.findViewById(R.id.text_sent_container);
            timestamp = itemView.findViewById(R.id.sent_message_date_segment);
        }
    }

    public class TimestampMessageSentViewHandler extends MessageSentViewHandler {
        public TimestampMessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class MessageReceivedViewHandler extends RecyclerView.ViewHolder {
         TextView receivedMessage;
         TextView date;
        TextView timestamp;

        public MessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.message_thread_received_card_text);
            date = itemView.findViewById(R.id.message_thread_received_date_text);
            timestamp = itemView.findViewById(R.id.received_message_date_segment);
        }
    }
    public class TimestampMessageReceivedViewHandler extends MessageReceivedViewHandler {
        public TimestampMessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static final DiffUtil.ItemCallback<SMS> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SMS>() {
                @Override
                public boolean areItemsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public void enableToolbar(){
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.copy_text:

                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

                        // TODO: use an actual label
                        if(!highlightedText.equals("null") && !highlightedText.isEmpty()) {
                            ClipData clip = ClipData.newPlainText("label", highlightedText);
                                clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case R.id.close_toolbar:

                        toolbar.setBackgroundColor(Color.TRANSPARENT);
                        Menu menu = toolbar.getMenu();
                        menu.clear();
                        toolbar.inflateMenu(R.menu.default_menu);

                        highlightedView.setBackgroundResource(R.drawable.sent_blue);

                        break;

                }
                return false;
            }
        });
    }
}
