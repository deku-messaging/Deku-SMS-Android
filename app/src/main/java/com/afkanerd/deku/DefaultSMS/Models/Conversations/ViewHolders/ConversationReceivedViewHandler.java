package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import io.getstream.avatarview.AvatarView;

public class ConversationReceivedViewHandler extends ConversationTemplateViewHandler {

    final static int BOTTOM_MARGIN = 4;
    TextView receivedMessage;
    public TextView date;
    TextView timestamp;

    AvatarView contactInitials;
    public ImageView imageView;
    ConstraintLayout constraintLayout, imageConstraintLayout, constraint3;

    public ConversationReceivedViewHandler(@NonNull View itemView) {
        super(itemView);
        receivedMessage = itemView.findViewById(R.id.message_received_text);
        date = itemView.findViewById(R.id.message_thread_received_date_text);
        timestamp = itemView.findViewById(R.id.received_message_date_segment);
        constraintLayout = itemView.findViewById(R.id.message_received_constraint);
        imageConstraintLayout = itemView.findViewById(R.id.message_received_image_container);
        imageView = itemView.findViewById(R.id.message_received_image_view);
        constraint3 = itemView.findViewById(R.id.conversation_received_layout_container);
    }

    @Override
    public String getMessage_id() {
        return this.message_id;
    }

    @Override
    public String getText() {
        return receivedMessage.getText().toString();
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public void bind(Conversation conversation) {
        this.id = conversation.getId();
        this.message_id = conversation.getMessage_id();
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
    }

    @Override
    public View getContainerLayout() {
        return receivedMessage;
    }

    @Override
    public void activate() {
        constraintLayout.setBackgroundResource(R.drawable.received_messages_highlighted_drawable);
    }

    @Override
    public void deactivate() {
        constraintLayout.setBackgroundResource(R.drawable.received_messages_drawable);
    }

    public static class TimestampConversationReceivedViewHandler extends ConversationReceivedViewHandler {
        public TimestampConversationReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeyReceivedViewHandler extends ConversationReceivedViewHandler {

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

    public static class TimestampKeyReceivedStartGroupViewHandler extends TimestampConversationReceivedViewHandler {
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

    public static class ConversationReceivedStartViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedStartViewHandler(@NonNull View itemView) {
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
    public static class ConversationReceivedEndViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedEndViewHandler(@NonNull View itemView) {
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

    public static class ConversationReceivedMiddleViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedMiddleViewHandler(@NonNull View itemView) {
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
