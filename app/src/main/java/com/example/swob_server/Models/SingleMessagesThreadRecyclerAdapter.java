package com.example.swob_server.Models;

import android.content.Context;
import android.util.Log;
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
                MessageReceivedViewHandler messageReceivedViewHandler = (MessageReceivedViewHandler) holder;
                messageReceivedViewHandler.receivedMessage.setText(messagesList.get(position).getBody());
                break;

            case "2":
                Log.i("", String.valueOf(holder));
                ((MessageSentViewHandler)holder).sentMessage.setText(messagesList.get(position).getBody());
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
        String messageType = messagesList.get(position).getType();
        Log.i("", "message type: " + messageType);

        return Integer.parseInt(messagesList.get(position).getType());
    }

    public class MessageSentViewHandler extends RecyclerView.ViewHolder {
        TextView sentMessage;
        public MessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.message_thread_sent_card_text);
            Log.i("", "sent Message: " + sentMessage);
        }
    }

    public class MessageReceivedViewHandler extends RecyclerView.ViewHolder {
        TextView receivedMessage;
        public MessageReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            receivedMessage = itemView.findViewById(R.id.message_thread_received_card_text);
        }
    }
}
