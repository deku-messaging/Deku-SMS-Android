package com.example.swob_deku.Models;

import android.content.Context;
import android.provider.Telephony;
import android.text.format.DateUtils;
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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class SingleMessagesThreadRecyclerAdapter extends RecyclerView.Adapter{

    Context context;
    List<SMS> messagesList;
    int renderLayoutReceived, renderLayoutSent;

    public SingleMessagesThreadRecyclerAdapter(Context context, List<SMS> messagesList, int renderLayoutReceived, int renderLayoutSent) {
        this.context = context;
        this.messagesList = messagesList;
        this.renderLayoutReceived = renderLayoutReceived;
        this.renderLayoutSent = renderLayoutSent;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch(viewType) {
            // https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_OUTBOX
            case 1: {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                View view = inflater.inflate(this.renderLayoutReceived, parent, false);
                return new MessageReceivedViewHandler(view);
            }
            case 5:
            case 4:
            case 2: {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                View view = inflater.inflate(this.renderLayoutSent, parent, false);
                return new MessageSentViewHandler(view);
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String date = messagesList.get(position).getDate();
        if (DateUtils.isToday(Long.parseLong(date))) {
            DateFormat dateFormat = new SimpleDateFormat("h:mm a");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }
        else {
            DateFormat format = new SimpleDateFormat("MMMM dd h:mm a");

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date(Long.parseLong(date)));
            date = format.format(calendar.getTime());
        }
        SMS sms = messagesList.get(position);
        switch(sms.getType()) {
//            https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns?hl=en#TYPE
            case "1":
                ((MessageReceivedViewHandler)holder).receivedMessage.setText(sms.getBody());
                ((MessageReceivedViewHandler)holder).date.setText(date);
                break;

            case "2":
                ((MessageSentViewHandler)holder).sentMessage.setText(sms.getBody());
                ((MessageSentViewHandler) holder).date.setText(date);

                int status = sms.getStatusCode();
                String statusMessage = status == Telephony.Sms.STATUS_COMPLETE ?
                        "delivered" : "sent";
                ((MessageSentViewHandler) holder).sentMessageStatus.setText(statusMessage);
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

//        switch(sms.getType()) {
//            case "1":
//                ((MessageReceivedViewHandler) holder).layout.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                    }
//                });
//                break;
//            case "2":
//            case"4":
//            case "5":
//                ((MessageSentViewHandler) holder).layout.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                    }
//                });
//                break;
//        }

    }

    @Override
    public int getItemCount() {
        return this.messagesList.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        int messageType = Integer.parseInt(messagesList.get(position).getType());
        return (messageType > -1 )? messageType : 0;
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
