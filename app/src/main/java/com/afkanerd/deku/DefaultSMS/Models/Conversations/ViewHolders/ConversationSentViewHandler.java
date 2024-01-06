package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.graphics.Color;
import android.provider.Telephony;
import android.text.Spannable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
//import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.E2EE.E2EEHandler;

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

    LinearLayoutCompat.LayoutParams layoutParams;

    boolean lastKnownStateIsFailed = false;

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

        layoutParams = (LinearLayoutCompat.LayoutParams) linearLayoutCompat.getLayoutParams();
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

    public void bind(Conversation conversation, String searchString) {
        this.id = conversation.getId();
        this.message_id = conversation.getMessage_id();

        String timestamp = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String txDate = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));

        this.timestamp.setText(timestamp);
        this.date.setText(txDate);

        int status = conversation.getStatus();
        String statusMessage = status == Telephony.TextBasedSmsColumns.STATUS_COMPLETE ?
                itemView.getContext().getString(R.string.sms_status_delivered) :
                itemView.getContext().getString(R.string.sms_status_sent);

        if(status == Telephony.TextBasedSmsColumns.STATUS_PENDING )
            statusMessage = itemView.getContext().getString(R.string.sms_status_sending);
        else if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED ) {
            statusMessage = itemView.getContext().getString(R.string.sms_status_failed);
            sentMessageStatus.setVisibility(View.VISIBLE);
            this.date.setVisibility(View.VISIBLE);
            sentMessageStatus.setTextAppearance(R.style.conversation_failed);
            this.date.setTextAppearance(R.style.conversation_failed);
            lastKnownStateIsFailed = true;
        }
        if(lastKnownStateIsFailed && status != Telephony.TextBasedSmsColumns.STATUS_FAILED) {
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            lastKnownStateIsFailed = false;
        }
        statusMessage = " • " + statusMessage;

        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    conversation.getSubscription_id());
            if(!subscriptionName.isEmpty())
                statusMessage += " • " + subscriptionName;
        }

        sentMessageStatus.setText(statusMessage);

        final String[] text = {conversation.getText()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String keystoreAlias = E2EEHandler.deriveKeystoreAlias(conversation.getAddress(), 0);
                    if(E2EEHandler.canCommunicateSecurely(itemView.getContext(), keystoreAlias) &&
                            E2EEHandler.isValidDekuText(text[0])) {
                        byte[] extractedText = E2EEHandler.extractTransmissionText(text[0]);
                        text[0] = new String(E2EEHandler.decryptText(itemView.getContext(), keystoreAlias, extractedText));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(searchString != null && !searchString.isEmpty() && text[0] != null) {
            Spannable spannable = Helpers.highlightSubstringYellow(itemView.getContext(),
                    text[0], searchString, true);
            sentMessage.setText(spannable);
        }
        else
//            Helpers.highlightLinks(sentMessage, text[0],
//                    itemView.getContext().getColor(R.color.primary_text_color));
            Helpers.highlightLinks(sentMessage, text[0], Color.BLACK);
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

    @Override
    public void toggleDetails() {
        int visibility = this.messageStatusLinearLayoutCompact.getVisibility() == View.VISIBLE ?
                View.GONE : View.VISIBLE;
        this.messageStatusLinearLayoutCompact.setVisibility(visibility);
    }

    @Override
    public void hideDetails() {
        this.messageStatusLinearLayoutCompact.setVisibility(View.GONE);
    }

    @Override
    public void showDetails() {
        this.messageStatusLinearLayoutCompact.setVisibility(View.VISIBLE);
    }

    public static class TimestampConversationSentViewHandler extends ConversationSentViewHandler {
        public TimestampConversationSentViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeySentViewHandler extends ConversationSentViewHandler {
        public KeySentViewHandler(@NonNull View itemView) {
            super(itemView);
        }

        public void highlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
        }

        public void unHighlight() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_drawable);
        }

        @Override
        public void bind(Conversation conversation, String searchString) {
            super.bind(conversation, searchString);
            sentMessage.setText(itemView.getContext().getString(R.string.conversation_key_title_requested));
            sentMessage.setTextAppearance(R.style.key_request_initiated);
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

    public static class ConversationSentStartViewHandler extends ConversationSentViewHandler {
        public ConversationSentStartViewHandler(@NonNull View itemView) {
            super(itemView);
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

