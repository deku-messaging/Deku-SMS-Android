package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ConversationTemplateViewHandler extends RecyclerView.ViewHolder {
    Long id;
    public ConversationTemplateViewHandler(@NonNull View itemView) {
        super(itemView);
    }

    public abstract String getText();

    public abstract void setId(Long id);

    public abstract long getId();

    public abstract View getContainerLayout();
    public abstract void activate();
    public abstract void deactivate();
}
