package com.afkanerd.deku.Forwarder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import com.afkanerd.deku.Forwarder.ui.modals.GatewayServerAddHttpModal
import com.afkanerd.deku.Forwarder.ui.modals.GatewayServerAddSmtpModal
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayClientsMainView(
    navController: NavController,
    viewModel: GatewayServerViewModel
) {
    val context = LocalContext.current
    val gatewayClients by viewModel.fetch(context)
        .collectAsStateWithLifecycle(emptyList())

    var rememberMenuExpanded by remember { mutableStateOf( false)}

    var showAddHttpModal by remember { mutableStateOf( false)}
    var showAddSmtpModal by remember { mutableStateOf( false)}

    var selectedGatewayServer: GatewayServer? by remember { mutableStateOf(null)}

    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentSize(Alignment.TopEnd)
    ) {
        DropdownMenu(
            expanded = LocalInspectionMode.current || rememberMenuExpanded,
            onDismissRequest = { rememberMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.add_http_forwarders),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    showAddHttpModal = true
                    rememberMenuExpanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Add SMTP forwarder",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    showAddSmtpModal = true
                    rememberMenuExpanded = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "go back"
                        )
                    }
                },

                title = {Text(stringResource(R.string.gateway_clients))},
                actions = {
                    IconButton(onClick = {
                        rememberMenuExpanded = !rememberMenuExpanded
                    }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(com.afkanerd.lib_smsmms_android.R.string.open_menu)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier
            .padding(innerPadding)
            .padding(8.dp)
        ) {
            LazyColumn {
                items(
                    items = gatewayClients,
                    key = {it.id},
                ) { gatewayClient ->
                    Column {
                        GatewayServerCard( gatewayClient ) {
                            showAddHttpModal = true
                            selectedGatewayServer = it
                        }
                    }
                }
            }

            if(showAddHttpModal) {
                GatewayServerAddHttpModal(
                    showBottomSheet = showAddHttpModal,
                    viewModel = viewModel,
                    gatewayServer = selectedGatewayServer
                ) {
                    showAddHttpModal = false
                }
            }

            if(showAddSmtpModal) {
                GatewayServerAddSmtpModal(
                    showBottomSheet = showAddSmtpModal,
                    viewModel = viewModel,
                    gatewayServer = selectedGatewayServer,
                ) {
                    showAddSmtpModal = false
                }
            }
        }
    }
}

@Composable
fun GatewayServerCard(
    gatewayClient: GatewayServer,
    onClickCallback: (GatewayServer) -> Unit,
) {
    Card(
        onClick = {onClickCallback(gatewayClient)},
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onPrimary),
        modifier = Modifier.padding(8.dp)
    ) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    Modifier.padding(16.dp)
                ) {
                    Text(
                        gatewayClient.URL.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        gatewayClient.date.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }


            Column(Modifier.padding(16.dp)) {
                Row {
                    Text(
                        stringResource(R.string.protocols),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        gatewayClient.protocol.toString(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                HorizontalDivider(Modifier.padding(16.dp))

                Row {
                    Text(
                        stringResource(R.string.encoding),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        gatewayClient.format.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(Modifier.padding(16.dp))

                Row {
                    Text(
                        stringResource(R.string.tag),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        gatewayClient.tag,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                HorizontalDivider(Modifier.padding(16.dp))

                Row {
                    Text(
                        stringResource(R.string.status1),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "N/A",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GatewayServerCardPreview() {
    AppTheme {
        val gatewayServer = GatewayServer().apply {
            this.URL = "https://example.com/gateway-clients/sms"
            this.date = System.currentTimeMillis()
            this.tag = "tags_here"
        }
        GatewayServerCard(gatewayServer){}
    }
}