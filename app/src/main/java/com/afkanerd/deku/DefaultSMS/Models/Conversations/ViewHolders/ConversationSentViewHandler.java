package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.provider.Telephony;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
//import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.textview.MaterialTextView;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class ConversationSentViewHandler extends ConversationTemplateViewHandler {
    public final static int TYPE_KEY = 1;
    public final static int TYPE_CONVERSATION = 0;

    final static int BOTTOM_MARGIN = 4;
    public ImageView messageFailedIcon;
    MaterialTextView sentMessage;
    TextView sentMessageStatus;
    TextView date;
    TextView timestamp;
    LinearLayoutCompat linearLayoutCompat;
    LinearLayoutCompat.LayoutParams layoutParams;
    LinearLayoutCompat messageStatusTimestampLayout;

    public int type = TYPE_CONVERSATION;

    public ConversationSentViewHandler(@NonNull View itemView, int type) {
        super(itemView);
        this.type = type;
        if(type == TYPE_CONVERSATION) {
            sentMessage = itemView.findViewById(R.id.message_sent_text);
            sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
            date = itemView.findViewById(R.id.message_thread_sent_date_text);
            timestamp = itemView.findViewById(R.id.sent_message_date_segment);
            messageStatusTimestampLayout = itemView.findViewById(R.id.message_status_timestamp);
            messageFailedIcon = itemView.findViewById(R.id.message_failed_indicator_img);
            linearLayoutCompat = itemView.findViewById(R.id.sent_message_linear_layout);
            layoutParams = (LinearLayoutCompat.LayoutParams) linearLayoutCompat.getLayoutParams();
            layoutParams.bottomMargin = Helpers.dpToPixel(16);
            linearLayoutCompat.setLayoutParams(layoutParams);
        }
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
            statusMessage = itemView.getContext().getString(R.string.sms_status_failed_only);

            sentMessageStatus.setTextAppearance(R.style.conversation_failed);
            this.date.setTextAppearance(R.style.conversation_failed);
            this.messageFailedIcon.setVisibility(View.VISIBLE);

            LinearLayoutCompat.LayoutParams linearLayoutCompat1 = (LinearLayoutCompat.LayoutParams)
                    this.messageStatusTimestampLayout.getLayoutParams();
            linearLayoutCompat1.setMarginEnd(Helpers.dpToPixel(32));
            this.messageStatusTimestampLayout.setLayoutParams(linearLayoutCompat1);
            showDetails();
        }
        else {
            sentMessageStatus.setTextAppearance(R.style.conversation_default);
            this.date.setTextAppearance(R.style.conversation_default);
            this.messageFailedIcon.setVisibility(View.GONE);
        }

        statusMessage = " • " + statusMessage;

        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    conversation.getSubscription_id());
            if(!subscriptionName.isEmpty())
                statusMessage += " • " + subscriptionName;
        }

        sentMessageStatus.setText(statusMessage);

        final String text = conversation.getText();
        if(searchString != null && !searchString.isEmpty() && text != null) {
            Spannable spannable = Helpers.highlightSubstringYellow(itemView.getContext(),
                    text, searchString, true);
            sentMessage.setText(spannable);
        }
        else
            Helpers.highlightLinks(sentMessage, text, Color.BLACK);
    }

    @Override
    public View getContainerLayout() {
        return sentMessage;
    }

    @Override
    public void activate() {
        Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                R.drawable.sent_messages_drawable);
        drawable.setColorFilter(
                new PorterDuffColorFilter(ContextCompat
                        .getColor(itemView.getContext(), R.color.md_theme_outline),
                        PorterDuff.Mode.SRC_IN));
        sentMessage.setBackground(drawable);
    }

    @Override
    public void deactivate() {
        sentMessage.setBackgroundResource(R.drawable.sent_messages_drawable);
    }

    @Override
    public void toggleDetails() {
        int visibility = this.messageStatusTimestampLayout.getVisibility() == View.VISIBLE ?
                View.GONE : View.VISIBLE;
        this.messageStatusTimestampLayout.setVisibility(visibility);
    }

    @Override
    public void hideDetails() {
        this.messageStatusTimestampLayout.setVisibility(View.GONE);
    }

    @Override
    public void showDetails() {
        this.messageStatusTimestampLayout.setVisibility(View.VISIBLE);
    }

    public static class TimestampConversationSentViewHandler extends ConversationSentViewHandler {
        public TimestampConversationSentViewHandler(@NonNull View itemView) {
            super(itemView, TYPE_CONVERSATION);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeySentViewHandler extends ConversationSentViewHandler {
        public KeySentViewHandler(@NonNull View itemView) {
            super(itemView, TYPE_KEY);
        }

        @Override
        public void bind(Conversation conversation, String searchString) {
//            super.bind(conversation, searchString);
//            sentMessage.setText(itemView.getContext().getString(R.string.conversation_key_title_requested));
//            sentMessage.setTextAppearance(R.style.key_request_initiated);
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

        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.sent_messages_start_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(), R.color.md_theme_outline),
                            PorterDuff.Mode.SRC_IN));
            sentMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }

    public static class ConversationSentStartViewHandler extends ConversationSentViewHandler {
        public ConversationSentStartViewHandler(@NonNull View itemView) {
            super(itemView, TYPE_CONVERSATION);
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));
        }

        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.sent_messages_start_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(), R.color.md_theme_outline),
                            PorterDuff.Mode.SRC_IN));
            sentMessage.setBackground(drawable);
        }
        @Override
        public void deactivate() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }
    public static class ConversationSentEndViewHandler extends ConversationSentViewHandler {
        public ConversationSentEndViewHandler(@NonNull View itemView) {
            super(itemView, TYPE_CONVERSATION);
            layoutParams.bottomMargin = Helpers.dpToPixel(16);
            linearLayoutCompat.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_end_view_drawable));
        }
        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.sent_messages_end_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(), R.color.md_theme_outline),
                            PorterDuff.Mode.SRC_IN));
            sentMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_end_view_drawable);
        }
    }

    public static class ConversationSentMiddleViewHandler extends ConversationSentViewHandler {
        public ConversationSentMiddleViewHandler(@NonNull View itemView) {
            super(itemView, TYPE_CONVERSATION);
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
//            linearLayoutCompat.setLayoutParams(layoutParams);
            itemView.setLayoutParams(layoutParams);

            sentMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_middle_view_drawable));
        }

        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.sent_messages_middle_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(), R.color.md_theme_outline),
                            PorterDuff.Mode.SRC_IN));
            sentMessage.setBackground(drawable);
        }
        @Override
        public void deactivate() {
            sentMessage.setBackgroundResource(R.drawable.sent_messages_middle_view_drawable);
        }
    }

}

