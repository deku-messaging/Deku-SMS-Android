package com.example.swob_server.Models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_server.R;

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
            case 1: {
                LayoutInflater inflater = LayoutInflater.from(this.context);
                View view = inflater.inflate(this.renderLayoutReceived, parent, false);
                return new MessageReceivedViewHandler(view);
            }
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
        switch(messagesList.get(position).getType()) {
            case "1":
                ((MessageReceivedViewHandler)holder).receivedMessage.setText(messagesList.get(position).getBody());
                ((MessageReceivedViewHandler)holder).date.setText(messagesList.get(position).getDate());
                break;

            case "2":
                ((MessageSentViewHandler)holder).sentMessage.setText(messagesList.get(position).getBody());
                ((MessageSentViewHandler)holder).date.setText(messagesList.get(position).getDate());
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
        return Integer.parseInt(messagesList.get(position).getType());
    }

    public class MessageSentViewHandler extends RecyclerView.ViewHolder {
        TextView sentMessage;
        TextView sentMessageStatus;
        TextView date;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_thread_sent_card_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
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
