package com.example.swob_deku.Models;

import android.content.Context;
import android.provider.Telephony;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.R;
import com.google.android.material.card.MaterialCardView;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public class SingleMessagesThreadRecyclerAdapter extends RecyclerView.Adapter{

    Context context;
    List<SMS> messagesList;
    int renderLayoutReceived, renderLayoutSent, renderLayoutTimestamp;
    int focusPosition = -1;
    long focusId;
    RecyclerView view;
    String searchString;

    public SingleMessagesThreadRecyclerAdapter(Context context, List<SMS> messagesList, int renderLayoutReceived, int renderLayoutSent, int renderLayoutTimestamp) {
        this.context = context;
        this.messagesList = messagesList;
        this.renderLayoutReceived = renderLayoutReceived;
        this.renderLayoutSent = renderLayoutSent;
        this.renderLayoutTimestamp = renderLayoutTimestamp;
    }

    public SingleMessagesThreadRecyclerAdapter(Context context, List<SMS> messagesList, int renderLayoutReceived, int renderLayoutSent, int renderLayoutTimestamp, long focusId, String searchString) {
        this.context = context;
        this.messagesList = messagesList;
        this.renderLayoutReceived = renderLayoutReceived;
        this.renderLayoutSent = renderLayoutSent;
        this.renderLayoutTimestamp = renderLayoutTimestamp;
        this.focusId = focusId;
        this.searchString = searchString;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        switch(viewType) {
            // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
            case 100: {
                View view = inflater.inflate(this.renderLayoutTimestamp, parent, false);
                return new MessageTimestampViewerHandler(view);
            }
            case 1: {
                View view = inflater.inflate(this.renderLayoutReceived, parent, false);
                return new MessageReceivedViewHandler(view);
            }
            case 5:
            case 4:
            case 2: {
                View view = inflater.inflate(this.renderLayoutSent, parent, false);
                return new MessageSentViewHandler(view);
            }
        }

        return null;
    }

    public void setView(RecyclerView view) {
        this.view = view;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);


        if(focusPosition != -1 && holder.getAdapterPosition() == focusPosition) {
            if(!searchString.isEmpty()) {
                Log.d("", "Focus not empty..");
                String text = messagesList.get(focusPosition).getBody();
                Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);

                for (int index = text.indexOf(searchString); index >= 0; index = text.indexOf(searchString, index + 1)) {
                    spannable.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.highlight_yellow)),
                            index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.black)),
                            index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                switch (holder.getItemViewType()) {
                    case 1: {
                        ((MessageReceivedViewHandler) holder).receivedMessage.setText(spannable);
                        break;
                    }
                    case 5:
                    case 4:
                    case 2: {
                        ((MessageSentViewHandler) holder).sentMessage.setText(spannable);
                        break;
                    }
                }
            }
            this.view.scrollToPosition(focusPosition);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        SMS sms = messagesList.get(position);

        if(focusId != -1 && sms.getId() != null && Long.valueOf(sms.getId()) == focusId) {
            final int finalPosition = position;
            this.focusPosition = finalPosition;
        }

        String date = sms.getDate();
        if(sms.isDatesOnly()) {
            if (DateUtils.isToday(Long.parseLong(date))) {
                DateFormat dateFormat = new SimpleDateFormat("h:mm a");
                date = "Today " + dateFormat.format(new Date(Long.parseLong(date)));
            }
            else {
                DateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d h:mm a");
                date = dateFormat.format(new Date(Long.parseLong(date)));
            }
            ((MessageTimestampViewerHandler)holder).date.setText(date);
            return;
        }

        if (DateUtils.isToday(Long.parseLong(date))) {
            DateFormat dateFormat = new SimpleDateFormat("h:mm a");
            date = "Today " + dateFormat.format(new Date(Long.parseLong(date)));
        }
        else {
            DateFormat dateFormat = new SimpleDateFormat("EE h:mm a");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }

        switch(sms.getType()) {
//            https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns?hl=en#TYPE
            case "1":
                TextView receivedMessage = ((MessageReceivedViewHandler)holder).receivedMessage;
                receivedMessage.setText(sms.getBody());

                TextView dateView = ((MessageReceivedViewHandler)holder).date;
                dateView.setVisibility(View.INVISIBLE);
                dateView.setText(date);

                ((MessageReceivedViewHandler)holder).receivedMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(((MessageReceivedViewHandler)holder).date.getVisibility() == View.VISIBLE) {
                            dateView.setVisibility(View.INVISIBLE);
                        }
                        else {
                            dateView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;

            case "2":
                ((MessageSentViewHandler)holder).sentMessage.setText(sms.getBody());
                ((MessageSentViewHandler) holder).date.setText(date);
                ((MessageSentViewHandler)holder).date.setVisibility(View.INVISIBLE);

                int status = sms.getStatusCode();
                String statusMessage = status == Telephony.Sms.STATUS_COMPLETE ?
                        "delivered" : "sent";
                ((MessageSentViewHandler)holder).sentMessageStatus.setVisibility(View.INVISIBLE);
                ((MessageSentViewHandler) holder).sentMessageStatus.setText(statusMessage);

                ((MessageSentViewHandler) holder).sentMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(((MessageSentViewHandler)holder).date.getVisibility() == View.VISIBLE) {
                            ((MessageSentViewHandler)holder).date.setVisibility(View.INVISIBLE);
                            ((MessageSentViewHandler)holder).sentMessageStatus.setVisibility(View.INVISIBLE);
                        }
                        else {
                            ((MessageSentViewHandler)holder).date.setVisibility(View.VISIBLE);
                            ((MessageSentViewHandler)holder).sentMessageStatus.setVisibility(View.VISIBLE);
                        }
                    }
                });

                break;
            case "4":
                ((MessageSentViewHandler)holder).sentMessage.setText(messagesList.get(position).getBody());
                ((MessageSentViewHandler) holder).date.setText(date);
                ((MessageSentViewHandler) holder).sentMessageStatus.setText("sending...");
                break;
            case "5":
                ((MessageSentViewHandler)holder).sentMessage.setText(messagesList.get(position).getBody());
                ((MessageSentViewHandler) holder).date.setText(date);
                ((MessageSentViewHandler) holder).sentMessageStatus.setText("failed");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return this.messagesList.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        if(messagesList.get(position).isDatesOnly())
            return 100;

        int messageType = Integer.parseInt(messagesList.get(position).getType());
        return (messageType > -1 )? messageType : 0;
    }

    public class MessageTimestampViewerHandler extends RecyclerView.ViewHolder {
        TextView date;
        public MessageTimestampViewerHandler(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.messages_thread_timestamp_textview);
        }
    }

    public class MessageSentViewHandler extends RecyclerView.ViewHolder {
        TextView sentMessage;
        TextView sentMessageStatus;
        TextView date;
        MaterialCardView layout;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_thread_sent_card_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
            layout = itemView.findViewById(R.id.text_sent_container);
        }
    }

    public class MessageReceivedViewHandler extends RecyclerView.ViewHolder {
        TextView receivedMessage;
        TextView date;
        public MessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.message_thread_received_card_text);
            date = itemView.findViewById(R.id.message_thread_received_date_text);
        }
    }
}
