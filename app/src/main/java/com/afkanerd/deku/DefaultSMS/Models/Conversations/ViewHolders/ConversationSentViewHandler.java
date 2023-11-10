package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.provider.Telephony;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
//import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ConversationSentViewHandler extends ConversationTemplateViewHandler {

    final static int BOTTOM_MARGIN = 4;
    TextView sentMessage;
    TextView sentMessageStatus;
    TextView date;
    TextView timestamp;
    ImageView imageView;
    ConstraintLayout imageConstraintLayout;

    LinearLayoutCompat linearLayoutCompat;
    public LinearLayoutCompat messageStatusLinearLayoutCompact;

    public ConversationSentViewHandler(@NonNull View itemView) {
        super(itemView);
        sentMessage = itemView.findViewById(R.id.message_sent_text);
        sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
        date = itemView.findViewById(R.id.message_thread_sent_date_text);
        timestamp = itemView.findViewById(R.id.sent_message_date_segment);
        linearLayoutCompat = itemView.findViewById(R.id.conversation_linear_layout);
        messageStatusLinearLayoutCompact = itemView.findViewById(R.id.conversation_status_linear_layout);

//        constraintLayout = itemView.findViewById(R.id.message_sent_constraint);
//        imageConstraintLayout = itemView.findViewById(R.id.message_sent_image_container);
//        imageView = itemView.findViewById(R.id.message_sent_image_view);
//        constraint4 = itemView.findViewById(R.id.conversation_sent_layout_container);

        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) linearLayoutCompat.getLayoutParams();
        layoutParams.bottomMargin = Helpers.dpToPixel(16);
        linearLayoutCompat.setLayoutParams(layoutParams);
    }

    @Override
    public String getMessage_id() {
        return this.message_id;
    }

    @Override
    public String getText() {
        return sentMessage.getText().toString();
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

        String timestamp = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String txDate = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));

        this.timestamp.setText(timestamp);
        this.date.setText(txDate);

        final int status = conversation.getStatus();
        String statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_COMPLETE ?
                itemView.getContext().getString(R.string.sms_status_delivered) :
                itemView.getContext().getString(R.string.sms_status_sent);

        if(status == Telephony.TextBasedSmsColumns.STATUS_PENDING )
            statusMessage = itemView.getContext().getString(R.string.sms_status_sending);
        if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED ) {
            statusMessage = itemView.getContext().getString(R.string.sms_status_failed);
            sentMessageStatus.setVisibility(View.VISIBLE);
            this.date.setVisibility(View.VISIBLE);
            sentMessageStatus.setTextColor(
                    itemView.getContext().getResources().getColor(R.color.failed_red,
                            itemView.getContext().getTheme()));
            this.date.setTextColor(itemView.getContext().getResources().getColor(R.color.failed_red,
                    itemView.getContext().getTheme()));
        } else {
            statusMessage = " • " + statusMessage;
        }
        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    String.valueOf(conversation.getSubscription_id()));
            if(subscriptionName != null && !subscriptionName.isEmpty())
                statusMessage += " • " + subscriptionName;
        }

        sentMessageStatus.setText(statusMessage);

        Helpers.highlightLinks(sentMessage, conversation.getBody(),
                itemView.getContext().getColor(R.color.primary_background_color));
    }

    @Override
    public View getContainerLayout() {
        return sentMessage;
    }

    @Override
    public void activate() {
        sentMessage.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
    }

    @Override
    public void deactivate() {
        sentMessage.setBackgroundResource(R.drawable.sent_messages_drawable);
    }

    public static class TimestampConversationSentViewHandler extends ConversationSentViewHandler {
        public TimestampConversationSentViewHandler(@NonNull View itemView) {
            super(itemView);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) linearLayoutCompat.getLayoutParams();
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeySentViewHandler extends ConversationSentViewHandler {
        public KeySentViewHandler(@NonNull View itemView) {
            super(itemView);
            imageView.setImageDrawable(itemView.getContext().getDrawable(R.drawable.round_key_24));
            imageConstraintLayout.setVisibility(View.VISIBLE);
            sentMessage.setVisibility(View.GONE);
        }

        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
        }

        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_drawable);
        }
    }

    public static class TimestampKeySentViewHandler extends KeySentViewHandler {
        public TimestampKeySentViewHandler(@NonNull View itemView) {
            super(itemView);
        }
    }


    public static class TimestampKeySentStartGroupViewHandler extends TimestampConversationSentViewHandler {
        public TimestampKeySentStartGroupViewHandler(@NonNull View itemView) {
            super(itemView);
            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));

        }

        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }

    public static class ConversationSentStartViewHandler extends ConversationSentViewHandler {
        public ConversationSentStartViewHandler(@NonNull View itemView) {
            super(itemView);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) linearLayoutCompat.getLayoutParams();
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));
        }

        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }
    public static class ConversationSentEndViewHandler extends ConversationSentViewHandler {
        public ConversationSentEndViewHandler(@NonNull View itemView) {
            super(itemView);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) linearLayoutCompat.getLayoutParams();
            layoutParams.bottomMargin = Helpers.dpToPixel(16);
            linearLayoutCompat.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_end_view_drawable));
        }
        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_end_highlight_drawable);
        }
        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_end_view_drawable);
        }
    }

    public static class ConversationSentMiddleViewHandler extends ConversationSentViewHandler {
        public ConversationSentMiddleViewHandler(@NonNull View itemView) {
            super(itemView);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) linearLayoutCompat.getLayoutParams();
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_middle_view_drawable));
        }

        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_middle_hightlight_drawable);
        }
        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_middle_view_drawable);
        }
    }

}

