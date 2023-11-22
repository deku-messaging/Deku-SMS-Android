package com.afkanerd.deku.DefaultSMS;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;

import java.util.List;

public class DualSIMConversationActivity extends AppCompatActivity {

    protected MutableLiveData<Integer> defaultSubscriptionId = new MutableLiveData<>();
    protected int simCount = 0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    ImageButton sendImageButton;
    TextView currentSimcardTextView;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        sendImageButton = findViewById(R.id.conversation_send_btn);
        currentSimcardTextView = findViewById(R.id.conversation_compose_dual_sim_send_sim_name);
        simCount = SIMHandler.getActiveSimcardCount(getApplicationContext());

        if(sendImageButton != null) {
            try {
                defaultSubscriptionId.setValue(SIMHandler.getDefaultSimSubscription(getApplicationContext()));
            } catch(Exception e ) {
                e.printStackTrace();
            }

            sendImageButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (simCount > 1) {
                        showMultiDualSimAlert();
                        return true;
                    }
                    return false;
                }
            });
        }

        defaultSubscriptionId.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(simCount > 1) {
                    String subscriptionName = SIMHandler.getSubscriptionName(getApplicationContext(), integer);
                    currentSimcardTextView.setText(subscriptionName);
                }
            }
        });
    }

    private void showMultiDualSimAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.sim_chooser_layout_text));
//        builder.setMessage(getString(R.string.messages_thread_delete_confirmation_text));

        View simChooserView = View.inflate(getApplicationContext(), R.layout.sim_chooser_layout, null);
        builder.setView(simChooserView);

        List<SubscriptionInfo> subscriptionInfos = SIMHandler.getSimCardInformation(getApplicationContext());

        Bitmap sim1Bitmap = subscriptionInfos.get(0).createIconBitmap(getApplicationContext());
        Bitmap sim2Bitmap = subscriptionInfos.get(1).createIconBitmap(getApplicationContext());

        ImageView sim1ImageView = simChooserView.findViewById(R.id.sim_layout_simcard_1_img);
        TextView sim1TextView = simChooserView.findViewById(R.id.sim_layout_simcard_1_name);

        ImageView sim2ImageView = simChooserView.findViewById(R.id.sim_layout_simcard_2_img);
        TextView sim2TextView = simChooserView.findViewById(R.id.sim_layout_simcard_2_name);

        sim1ImageView.setImageBitmap(sim1Bitmap);
        AlertDialog dialog = builder.create();

        sim1ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defaultSubscriptionId.setValue(subscriptionInfos.get(0).getSubscriptionId());
                dialog.dismiss();
            }
        });
        sim1TextView.setText(subscriptionInfos.get(0).getDisplayName());

        sim2ImageView.setImageBitmap(sim2Bitmap);
        sim2ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defaultSubscriptionId.setValue(subscriptionInfos.get(1).getSubscriptionId());
                dialog.dismiss();
            }
        });
        sim2TextView.setText(subscriptionInfos.get(1).getDisplayName());

        dialog.show();
    }

}
