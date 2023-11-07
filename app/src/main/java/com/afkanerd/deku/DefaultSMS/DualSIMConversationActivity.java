package com.afkanerd.deku.DefaultSMS;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;

import java.util.List;

public class DualSIMConversationActivity extends CustomAppCompactActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    private void showMultiDualSimAlert(Runnable runnable) {
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
//                defaultSubscriptionId = subscriptionInfos.get(0).getSubscriptionId();
                runnable.run();
                dialog.dismiss();
            }
        });
        sim1TextView.setText(subscriptionInfos.get(0).getDisplayName());

        sim2ImageView.setImageBitmap(sim2Bitmap);
        sim2ImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                defaultSubscriptionId = subscriptionInfos.get(1).getSubscriptionId();
                runnable.run();
                dialog.dismiss();
            }
        });
        sim2TextView.setText(subscriptionInfos.get(1).getDisplayName());

        dialog.show();
    }


    public void onLongClickSendButton(View view) {
//        List<SubscriptionInfo> simcards = SIMHandler.getSimCardInformation(getApplicationContext());
//
//        TextView simcard1 = findViewById(R.id.simcard_select_operator_1_name);
//        TextView simcard2 = findViewById(R.id.simcard_select_operator_2_name);
//
//        ImageButton simcard1Img = findViewById(R.id.simcard_select_operator_1);
//        ImageButton simcard2Img = findViewById(R.id.simcard_select_operator_2);
//
//        ArrayList<TextView> views = new ArrayList();
//        views.add(simcard1);
//        views.add(simcard2);
//
//        ArrayList<ImageButton> buttons = new ArrayList();
//        buttons.add(simcard1Img);
//        buttons.add(simcard2Img);
//
//        for (int i = 0; i < simcards.size(); ++i) {
//            CharSequence carrierName = simcards.get(i).getCarrierName();
//            views.get(i).setText(carrierName);
//            buttons.get(i).setImageBitmap(simcards.get(i).createIconBitmap(getApplicationContext()));
//
//            final int subscriptionId = simcards.get(i).getSubscriptionId();
//            buttons.get(i).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    defaultSubscriptionId = subscriptionId;
//                    findViewById(R.id.simcard_select_constraint).setVisibility(View.INVISIBLE);
//                    String subscriptionText = getString(R.string.default_subscription_id_changed) +
//                            carrierName;
//                    Toast.makeText(getApplicationContext(), subscriptionText, Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        multiSimcardConstraint.setVisibility(View.VISIBLE);
    }

}
