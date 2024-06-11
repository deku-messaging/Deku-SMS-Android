package com.afkanerd.deku.Router.GatewayServers;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afkanerd.deku.DefaultSMS.Modals.ConversationsSecureRequestModalSheetFragment;
import com.afkanerd.deku.Datastore;
import com.afkanerd.deku.Modules.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.Router.FTP;
import com.afkanerd.deku.Router.SMTP;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class GatewayServerListingActivity extends AppCompatActivity {
    Toolbar toolbar;

    Datastore databaseConnector;

    View includedViewFormat;

    GatewayServerRecyclerAdapter gatewayServerRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_servers_listing_activitiy);

        databaseConnector = Datastore.getDatastore(getApplicationContext());

        toolbar = findViewById(R.id.gateway_servers_listing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.gateway_server_listing_toolbar_title));


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        RecyclerView recentsRecyclerView = findViewById(R.id.gateway_server_listing_recycler_view);
        recentsRecyclerView.setLayoutManager(linearLayoutManager);

        gatewayServerRecyclerAdapter = new GatewayServerRecyclerAdapter();
        recentsRecyclerView.setAdapter(gatewayServerRecyclerAdapter);

        GatewayServerViewModel gatewayServerViewModel = new ViewModelProvider(this).get(
                GatewayServerViewModel.class);

        gatewayServerViewModel.get(getApplicationContext()).observe(this,
                new Observer<List<GatewayServer>>() {
                    @Override
                    public void onChanged(List<GatewayServer> gatewayServerList) {
                        gatewayServerRecyclerAdapter.submitList(gatewayServerList);
                        if(gatewayServerList.isEmpty())
                            findViewById(R.id.no_gateway_server_added).setVisibility(View.VISIBLE);
                        else
                            findViewById(R.id.no_gateway_server_added).setVisibility(View.GONE);
                    }
                });

        gatewayServerRecyclerAdapter.gatewayServerClickedListener
                .observe(this, new Observer<GatewayServer>() {
                    @Override
                    public void onChanged(GatewayServer gatewayServer) {
                        if(gatewayServer != null) {
                            if(gatewayServer.getProtocol().equals(SMTP.PROTOCOL)) {
                                showSecureRequestAgreementModal(SMTP_LAYOUT, TYPE_SMTP,
                                        gatewayServer);
                            } else if(gatewayServer.getProtocol().equals(FTP.PROTOCOL)) {
                                showSecureRequestAgreementModal(FTP_LAYOUT, TYPE_FTP,
                                        gatewayServer);
                            }
                            else {
                                showSecureRequestAgreementModal(HTTP_LAYOUT, TYPE_HTTP,
                                        gatewayServer);
                            }
                            gatewayServerRecyclerAdapter.gatewayServerClickedListener.setValue(null);
                        }
                    }
                });
    }

    public void onSaveTypeFTP(View view, GatewayServer gatewayServerEdit) {
        TextInputEditText textInputHost =
                view.findViewById(R.id.gateway_server_add_ftp_host_input);
        TextInputEditText textInputUsername =
                view.findViewById(R.id.gateway_server_add_ftp_username_input);
        TextInputEditText textInputPassword =
                view.findViewById(R.id.gateway_server_add_ftp_password_input);

        GatewayServer gatewayServer = new GatewayServer();
        MaterialCheckBox materialCheckBoxBase64 =
                includedViewFormat.findViewById(R.id.add_gateway_data_format_base64);
        if(materialCheckBoxBase64.isChecked()) {
            gatewayServer.setFormat(GatewayServer.BASE64_FORMAT);
        }
        gatewayServer.setProtocol(FTP.PROTOCOL);
        gatewayServer.ftp.ftp_host = textInputHost.getText().toString();
        gatewayServer.ftp.ftp_username = textInputUsername.getText().toString();
        gatewayServer.ftp.ftp_password = textInputPassword.getText().toString();
        gatewayServer.setDate(System.currentTimeMillis());

        if(gatewayServerEdit != null) {
            gatewayServer.setId(gatewayServerEdit.getId());
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().update(gatewayServer);
                }
            });
        }
        else
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().insert(gatewayServer);
                }
            });
    }

    public void onSaveTypeSmtp(View view, GatewayServer gatewayServerEdit) {
        TextInputEditText textInputHost =
                view.findViewById(R.id.gateway_server_add_smtp_host_input);
        TextInputEditText textInputUsername =
                view.findViewById(R.id.gateway_server_add_smtp_username_input);
        TextInputEditText textInputPassword =
                view.findViewById(R.id.gateway_server_add_smtp_password_input);
        TextInputEditText textInputPort =
                view.findViewById(R.id.gateway_server_add_smtp_port_input);
        TextInputEditText textInputFrom =
                view.findViewById(R.id.gateway_server_add_smtp_from_input);
        TextInputEditText textInputRecipient =
                view.findViewById(R.id.gateway_server_add_smtp_recipient_input);
        TextInputEditText textInputSubject =
                view.findViewById(R.id.gateway_server_add_smtp_subject_input);

        GatewayServer gatewayServer = new GatewayServer();
        MaterialCheckBox materialCheckBoxBase64 =
                includedViewFormat.findViewById(R.id.add_gateway_data_format_base64);
        if(materialCheckBoxBase64.isChecked()) {
            gatewayServer.setFormat(GatewayServer.BASE64_FORMAT);
        }

        gatewayServer.setProtocol(SMTP.PROTOCOL);
        gatewayServer.smtp.smtp_host = textInputHost.getText().toString();
        gatewayServer.smtp.smtp_username = textInputUsername.getText().toString();
        gatewayServer.smtp.smtp_password = textInputPassword.getText().toString();
        if(textInputPort.getText() != null && !textInputPort.getText().toString().isEmpty())
            gatewayServer.smtp.smtp_port = Integer.parseInt(textInputPort.getText().toString());
        gatewayServer.smtp.smtp_from = textInputFrom.getText().toString();
        gatewayServer.smtp.smtp_recipient = textInputRecipient.getText().toString();
        gatewayServer.smtp.smtp_subject = textInputSubject.getText().toString();
        gatewayServer.setDate(System.currentTimeMillis());

        if(gatewayServerEdit != null) {
            gatewayServer.setId(gatewayServerEdit.getId());
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().update(gatewayServer);
                }
            });
        }
        else
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().insert(gatewayServer);
                }
            });
    }

    public void onSaveTypeHttp(View view, GatewayServer gatewayServerEdit) {
        TextInputEditText textInputEditTextUrl = view.findViewById(R.id.new_gateway_server_url_input);
        String gatewayServerUrl = textInputEditTextUrl.getText().toString();

        TextInputEditText textInputEditTextTag = view.findViewById(R.id.new_gateway_server_tag_input);
        String gatewayServerTag = textInputEditTextTag.getText().toString();

        String protocol = GatewayServer.POST_PROTOCOL;

        GatewayServer gatewayServer = new GatewayServer(gatewayServerUrl);
        MaterialCheckBox materialCheckBoxBase64 =
                includedViewFormat.findViewById(R.id.add_gateway_data_format_base64);
        if(materialCheckBoxBase64.isChecked()) {
            gatewayServer.setFormat(GatewayServer.BASE64_FORMAT);
        }

        // Important: test if valid url
        gatewayServer.setTag(gatewayServerTag);
        gatewayServer.setProtocol(protocol);
        gatewayServer.setDate(System.currentTimeMillis());

        if(gatewayServerEdit != null) {
            gatewayServer.setId(gatewayServerEdit.getId());
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().update(gatewayServer);
                }
            });
            gatewayServerRecyclerAdapter.gatewayServerClickedListener = new MutableLiveData<>();
        }
        else
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    databaseConnector.gatewayServerDAO().insert(gatewayServer);
                }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gateway_server_listing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    int HTTP_LAYOUT = R.layout.fragment_modalsheet_gateway_server_http_add_layout;
    int SMTP_LAYOUT = R.layout.fragment_modalsheet_gateway_server_smtp_add_layout;
    int FTP_LAYOUT = R.layout.fragment_modalsheet_gateway_server_ftp_add_layout;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gateway_server_menu_add_http) {
            showSecureRequestAgreementModal(HTTP_LAYOUT, TYPE_HTTP, null);
            return true;
        }
        else if (item.getItemId() == R.id.gateway_server_menu_add_smtp) {
            showSecureRequestAgreementModal(SMTP_LAYOUT, TYPE_SMTP, null);
            return true;
        }
        else if (item.getItemId() == R.id.gateway_server_menu_add_ftp) {
            showSecureRequestAgreementModal(FTP_LAYOUT, TYPE_FTP, null);
            return true;
        }
        return false;
    }

    static int TYPE_HTTP = 1;
    static int TYPE_SMTP = 2;
    static int TYPE_FTP = 3;

    public void showSecureRequestAgreementModal(int layout, final int type,
                                                GatewayServer gatewayServer) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        GatewayServerAddModalFragment gatewayServerAddModalFragment =
                new GatewayServerAddModalFragment(layout, gatewayServer);
        fragmentTransaction.add(gatewayServerAddModalFragment,
                ConversationsSecureRequestModalSheetFragment.TAG);
        fragmentTransaction.show(gatewayServerAddModalFragment);

        fragmentTransaction.commitNow();
        gatewayServerAddModalFragment.runnable = new Runnable() {
            @Override
            public void run() {
                includedViewFormat = gatewayServerAddModalFragment.getView().
                        findViewById(R.id.gateway_server_routing_include);
                if(type == TYPE_HTTP)
                    onSaveTypeHttp(gatewayServerAddModalFragment.getView(), gatewayServer);
                else if(type == TYPE_SMTP)
                    onSaveTypeSmtp(gatewayServerAddModalFragment.getView(), gatewayServer);
                else if(type == TYPE_FTP)
                    onSaveTypeFTP(gatewayServerAddModalFragment.getView(), gatewayServer);
                gatewayServerAddModalFragment.dismiss();
            }
        };
    }
}

