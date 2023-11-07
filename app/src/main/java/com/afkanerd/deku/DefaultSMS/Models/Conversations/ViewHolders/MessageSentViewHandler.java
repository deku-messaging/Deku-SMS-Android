package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.provider.Telephony;
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

public class MessageSentViewHandler extends RecyclerView.ViewHolder {

    final static int BOTTOM_MARGIN = 4;
    TextView sentMessage;
    public TextView sentMessageStatus;
    public TextView date;
    TextView timestamp;
    ImageView imageView;
    ConstraintLayout constraintLayout, imageConstraintLayout, constraint4;

    public MessageSentViewHandler(@NonNull View itemView) {
        super(itemView);
        sentMessage = itemView.findViewById(R.id.message_sent_text);
        sentMessageStatus = itemView.findViewById(R.id.message_thread_sent_status_text);
        date = itemView.findViewById(R.id.message_thread_sent_date_text);
        timestamp = itemView.findViewById(R.id.sent_message_date_segment);
        constraintLayout = itemView.findViewById(R.id.message_sent_constraint);
        imageConstraintLayout = itemView.findViewById(R.id.message_sent_image_container);
        imageView = itemView.findViewById(R.id.message_sent_image_view);
        constraint4 = itemView.findViewById(R.id.constraintLayout4);
    }

    public void bind(Conversation conversation) {
        String date = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String timeStamp = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));
        timestamp.setText(timeStamp);

        this.date.setText(date);

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

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(isHighlighted(String.valueOf(sms.getMessage_id())))
//                    resetSelectedItem(String.valueOf(sms.getMessage_id()), true);
//                else if(selectedItem.getValue() != null) {
//                    longClickHighlight(messageSentViewHandler, smsId);
//                }
//                else if(status == Telephony.TextBasedSmsColumns.STATUS_FAILED) {
//                    String[] messageValues = new String[2];
//                    messageValues[0] = String.valueOf(sms.getMessage_id());
//
//                    String _text = text;
//                    if(holder instanceof KeySentViewHandler) {
////                            _text = SecurityHelpers.removeKeyWaterMark(text);
//                        messageValues[1] = _text;
//                        retryFailedDataMessage.setValue(messageValues);
//                    }
//                    else {
//                        messageValues[1] = _text;
//                        retryFailedMessage.setValue(messageValues);
//                    }
//                }
//                else {
//                    int visibility = messageSentViewHandler.date.getVisibility() == View.VISIBLE ?
//                            View.GONE : View.VISIBLE;
//                    messageSentViewHandler.date.setVisibility(visibility);
//                    messageSentViewHandler.sentMessageStatus.setVisibility(visibility);
//                }
            }
        };

        sentMessage.setOnClickListener(onClickListener);
        sentMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
    }

    public void highlight() {
        constraintLayout.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
    }

    public void unHighlight() {
        constraintLayout.setBackgroundResource(R.drawable.sent_messages_drawable);
    }


    public static class TimestampMessageSentViewHandler extends MessageSentViewHandler {
        public TimestampMessageSentViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeySentViewHandler extends MessageSentViewHandler {
        public KeySentViewHandler(@NonNull View itemView) {
            super(itemView);
            imageView.setImageDrawable(itemView.getContext().getDrawable(R.drawable.round_key_24));
            imageConstraintLayout.setVisibility(View.VISIBLE);
            constraintLayout.setVisibility(View.GONE);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_highlighted_drawable);
        }

        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_drawable);
        }
    }

    public static class TimestampKeySentViewHandler extends KeySentViewHandler {
        public TimestampKeySentViewHandler(@NonNull View itemView) {
            super(itemView);
        }
    }


    public static class TimestampKeySentStartGroupViewHandler extends TimestampMessageSentViewHandler {
        public TimestampKeySentStartGroupViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }

    public static class MessageSentStartViewHandler extends MessageSentViewHandler {
        public MessageSentStartViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_start_view_drawable));

            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_start_view_drawable);
        }
    }
    public static class MessageSentEndViewHandler extends MessageSentViewHandler {
        public MessageSentEndViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_end_view_drawable));
        }
        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_end_highlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_end_view_drawable);
        }
    }

    public static class MessageSentMiddleViewHandler extends MessageSentViewHandler {
        public MessageSentMiddleViewHandler(@NonNull View itemView) {
            super(itemView);

            constraintLayout.setBackground(
                    itemView.getContext().getDrawable(R.drawable.sent_messages_middle_view_drawable));
            ConstraintLayout.LayoutParams params= (ConstraintLayout.LayoutParams)
                    constraint4.getLayoutParams();
            params.bottomMargin= BOTTOM_MARGIN;
            constraint4.setLayoutParams(params);
        }

        public void highlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_middle_hightlight_drawable);
        }
        public void unHighlight() {
            constraintLayout.setBackgroundResource(R.drawable.sent_messages_middle_view_drawable);
        }
    }

}

