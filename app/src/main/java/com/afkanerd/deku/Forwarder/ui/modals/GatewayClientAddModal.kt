package com.afkanerd.deku.Forwarder.ui.modals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayServerAddHttpModal(
    showBottomSheet: Boolean,
    viewModel: GatewayServerViewModel,
    gatewayServer: GatewayServer? = null,
    onDismissCallback: () -> Unit,
) {
    val context = LocalContext.current

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = false
    )

    var url: String by remember{ mutableStateOf(gatewayServer?.URL ?: "") }
    var tag: String by remember{ mutableStateOf(gatewayServer?.tag ?: "") }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissCallback,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.add_new_gateway_server),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )

                HorizontalDivider(Modifier.padding(16.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text(stringResource(R.string.enter_url))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = tag,
                    onValueChange = { tag = it },
                    label = {
                        Text(stringResource(R.string.enter_tag_optional))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                Spacer(Modifier.padding(16.dp))

                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    if(gatewayServer != null) {
                        viewModel.update(
                            context, gatewayServer.apply {
                                this.URL = url
                                this.tag = tag
                            },
                        ) {
                            onDismissCallback()
                        }
                    } else {
                        viewModel.add(
                            context, GatewayServer().apply {
                                this.URL = url
                                this.tag = tag
                            },
                        ) {
                            onDismissCallback()
                        }
                    }
                }) {
                    Text(stringResource(R.string.save))
                }

                if(gatewayServer != null || LocalInspectionMode.current) {
                    OutlinedButton(onClick = {
                        viewModel.delete(
                            context = context,
                            gatewayClient = gatewayServer!!,
                        ){
                            onDismissCallback()
                        }
                    },) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GatewayClientAddModalPreview() {
    AppTheme {
        GatewayServerAddHttpModal(
            true,
            remember{ GatewayServerViewModel() }) {
        }
    }
}