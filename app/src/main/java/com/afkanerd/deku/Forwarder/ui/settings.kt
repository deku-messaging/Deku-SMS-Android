package com.afkanerd.deku.Forwarder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Forwarder.data.GatewayClientsSettingsManager
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayClientsSettingsView(
    navController: NavController,
    gatewayServerViewModel: GatewayServerViewModel,
) {
    val context = LocalContext.current
    val gatewayClientsSettingsManager = GatewayClientsSettingsManager(context)

    val batteryOptimization by gatewayClientsSettingsManager.getRouteOnLowBattery()
        .collectAsStateWithLifecycle(false)

    val clearRoutedCache by gatewayClientsSettingsManager.getClearRouteCache()
        .collectAsStateWithLifecycle(false)

    val hashIncomingAddress by gatewayClientsSettingsManager.getHashIncomingAddress()
        .collectAsStateWithLifecycle(false)

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.afkanerd.deku.DefaultSMS.R.string.gateway_client_settings))},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            "Navigate back"
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Column( Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(stringResource(com.afkanerd.deku.DefaultSMS.R.string.route_on_low_battery)) },
                supportingContent = {
                    Text(stringResource(com.afkanerd.deku.DefaultSMS.R.string.when_activated_would_ignore_low_battery_constraint_and_keep_routing))
                },
                trailingContent = {
                    Switch(
                        checked = batteryOptimization,
                        onCheckedChange = {
                            gatewayServerViewModel.setRouteOnLowBattery(context, it)
                        }
                    )
                },
                modifier = Modifier.clickable(
                    onClick = {
                        gatewayServerViewModel.setRouteOnLowBattery(context, !batteryOptimization)
                    }
                ),
            )

//            ListItem(
//                headlineContent = { Text(stringResource(com.afkanerd.deku.DefaultSMS.R.string.clear_routed_cache)) },
//                supportingContent = {
//                    Text(stringResource(com.afkanerd.deku.DefaultSMS.R.string.this_would_automatically_remove_the_routed_history_after_24_hours))
//                },
//                trailingContent = {
//                    Switch(
//                        checked = clearRoutedCache,
//                        onCheckedChange = {
//                            gatewayServerViewModel.setClearRouteCache(context, it)
//                        }
//                    )
//                },
//                modifier = Modifier.clickable(
//                    onClick = {
//                        gatewayServerViewModel.setClearRouteCache(context, !clearRoutedCache)
//                    }
//                ),
//            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.hash_incoming_address)) },
                supportingContent = {
                    Text(stringResource(R.string.the_incoming_address_would_be_hashed_using_sha256_before_forwarding))
                },
                trailingContent = {
                    Switch(
                        checked = hashIncomingAddress,
                        onCheckedChange = {
                            gatewayServerViewModel.setHashIncomingAddress(context, it)
                        }
                    )
                },
                modifier = Modifier.clickable(
                    onClick = {
                        gatewayServerViewModel.setHashIncomingAddress(context, !hashIncomingAddress)
                    }
                ),
            )
        }
    }

}


@Preview
@Composable
private fun PermissionsScreen_Preview() {
    AppTheme {
        GatewayClientsSettingsView(
            rememberNavController(),
            remember{ GatewayServerViewModel() }
        )
    }
}
