package com.afkanerd.deku.DefaultSMS;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

public class DualSIMConversationActivity extends CustomAppCompactActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
