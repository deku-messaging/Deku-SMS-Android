package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import io.getstream.avatarview.AvatarView;

public class MessageReceivedViewHandler extends RecyclerView.ViewHolder {

    final static int BOTTOM_MARGIN = 4;
    TextView receivedMessage;
    public TextView date;
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

    public void bind(Conversation conversation) {
        // TODO: implement search highlight in activity
        String date = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String timeStamp = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));

        Helpers.highlightLinks(receivedMessage, conversation.getBody(),
                itemView.getContext().getColor(R.color.primary_text_color));

        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    String.valueOf(conversation.getSubscription_id()));
            if(subscriptionName != null && !subscriptionName.isEmpty())
                timeStamp += " • " + subscriptionName;
        }
        timestamp.setText(timeStamp);

        this.date.setText(date);

        receivedMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(isHighlighted(String.valueOf(sms.getMessage_id())))
//                    resetSelectedItem(String.valueOf(sms.getMessage_id()), true);
//                else if(selectedItem.getValue() != null ){
//                    longClickHighlight(messageReceivedViewHandler, smsId);
//                } else {
////                        dateView.setVisibility(dateView.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
//                }
            }
        });

        receivedMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                return longClickHighlight(messageReceivedViewHandler, String.valueOf(sms.getMessage_id()));
                return true;
            }
        });

    }

    public void highlight() {
        constraintLayout.setBackgroundResource(R.drawable.received_messages_highlighted_drawable);
    }

    public void unHighlight() {
        constraintLayout.setBackgroundResource(R.drawable.received_messages_drawable);
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
