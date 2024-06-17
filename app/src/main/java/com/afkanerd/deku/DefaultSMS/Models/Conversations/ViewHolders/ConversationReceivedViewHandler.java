package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;


import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.E2EE.E2EEHandler;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;

public class ConversationReceivedViewHandler extends ConversationTemplateViewHandler {
    TextView receivedMessage;
    public TextView date;
    TextView timestamp;

    LinearLayoutCompat linearLayoutCompat;

    LinearLayoutCompat.LayoutParams layoutParams;


    public ConversationReceivedViewHandler(@NonNull View itemView) {
        super(itemView);
        receivedMessage = itemView.findViewById(R.id.message_received_text);
        date = itemView.findViewById(R.id.message_thread_received_date_text);
        timestamp = itemView.findViewById(R.id.received_message_date_segment);
        linearLayoutCompat = itemView.findViewById(R.id.conversation_received_linear_layout);

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

    public void bind(Conversation conversation, String searchString) {
        this.id = conversation.getId();
        this.message_id = conversation.getMessage_id();
        String timestamp = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String txDate = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));

        final String text = conversation.getText();
        if(searchString != null && !searchString.isEmpty() && text != null) {
            Spannable spannable = Helpers.highlightSubstringYellow(itemView.getContext(),
                    text, searchString, false);
            receivedMessage.setText(spannable);
        }
        else
            Helpers.highlightLinks(receivedMessage, text,
                    itemView.getContext().getColor(R.color.md_theme_onPrimary));

        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    conversation.getSubscription_id());
            if(subscriptionName != null && !subscriptionName.isEmpty())
                txDate += " • " + subscriptionName;
        }
        this.timestamp.setText(timestamp);

        this.date.setText(txDate);
    }

    @Override
    public View getContainerLayout() {
        return receivedMessage;
    }

    @Override
    public void activate() {
        Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                R.drawable.received_messages_drawable);
        drawable.setColorFilter(
                new PorterDuffColorFilter(ContextCompat
                        .getColor(itemView.getContext(),
                                R.color.md_theme_onSecondary),
                        PorterDuff.Mode.SRC_IN));
        receivedMessage.setBackground(drawable);
    }

    @Override
    public void deactivate() {
        receivedMessage.setBackgroundResource(R.drawable.received_messages_drawable);
    }

    @Override
    public void toggleDetails() {
        int visibility = this.date.getVisibility() == View.VISIBLE ?
                View.GONE : View.VISIBLE;
        this.date.setVisibility(visibility);
    }

    @Override
    public void hideDetails() {
        this.date.setVisibility(View.GONE);
    }

    @Override
    public void showDetails() {
        this.date.setVisibility(View.VISIBLE);
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
        }

        @Override
        public void bind(Conversation conversation, String searchString) {
            super.bind(conversation, searchString);
            receivedMessage.setTextAppearance(R.style.key_request_initiated);
            receivedMessage.setText(
                    itemView.getContext().getString(R.string.conversation_threads_secured_content));
        }
    }
    public static class TimestampKeyReceivedViewHandler extends KeyReceivedViewHandler {
        public TimestampKeyReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class TimestampKeyReceivedStartViewHandler extends TimestampConversationReceivedViewHandler {
        public TimestampKeyReceivedStartViewHandler(@NonNull View itemView) {
            super(itemView);

            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);
            timestamp.setVisibility(View.VISIBLE);

            receivedMessage.setBackground(
                    itemView.getContext()
                            .getDrawable(R.drawable.received_mesages_start_view_drawable));
        }

        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.received_mesages_start_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(),
                                    R.color.md_theme_onSecondary),
                            PorterDuff.Mode.SRC_IN));
            receivedMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            receivedMessage.setBackgroundResource(R.drawable.received_mesages_start_view_drawable);
        }
    }

    public static class ConversationReceivedStartViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedStartViewHandler(@NonNull View itemView) {
            super(itemView);

            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_mesages_start_view_drawable));
        }
        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.received_mesages_start_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(),
                                    R.color.md_theme_onSecondary),
                            PorterDuff.Mode.SRC_IN));
            receivedMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            receivedMessage.setBackgroundResource(R.drawable.received_mesages_start_view_drawable);
        }
    }
    public static class ConversationReceivedEndViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedEndViewHandler(@NonNull View itemView) {
            super(itemView);
            layoutParams.bottomMargin = Helpers.dpToPixel(16);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_end_view_drawable));
        }
        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.received_messages_end_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(),
                                    R.color.md_theme_onSecondary),
                            PorterDuff.Mode.SRC_IN));
            receivedMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            receivedMessage.setBackgroundResource(R.drawable.received_messages_end_view_drawable);
        }
    }

    public static class ConversationReceivedMiddleViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedMiddleViewHandler(@NonNull View itemView) {
            super(itemView);
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_middle_view_drawable));
        }

        @Override
        public void activate() {
            Drawable drawable = ContextCompat.getDrawable(itemView.getContext(),
                    R.drawable.received_messages_middle_view_drawable);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(ContextCompat
                            .getColor(itemView.getContext(),
                                    R.color.md_theme_onSecondary),
                            PorterDuff.Mode.SRC_IN));
            receivedMessage.setBackground(drawable);
        }

        @Override
        public void deactivate() {
            receivedMessage.setBackgroundResource(R.drawable.received_messages_middle_view_drawable);
        }
    }

}
