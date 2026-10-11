package com.afkanerd.deku.Forwarder.ui.modals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Forwarder.data.models.GatewayServer
import com.afkanerd.deku.Forwarder.data.models.SMTP
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayServerAddSmtpModal(
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

    var host by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_host ?: "") }
    var username by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_username ?: "") }
    var password by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_password ?: "") }
    var from by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_from ?: "") }
    var recipient by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_recipient ?: "") }
    var subject: String by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_subject ?: "") }
    var port by remember{ mutableStateOf(gatewayServer?.smtp?.smtp_port.toString()) }
    var passwordVisibility: Boolean by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissCallback,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
                    .fillMaxSize(),
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
                    value = host,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_url))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = username,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_username))
                    },
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    visualTransformation = if (passwordVisibility)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_password))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = recipient,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_recipients_separated_by_comma))
                    },
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = from,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.from_label_optional))
                    },
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = subject,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_subject))
                    },
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = port,
                    onValueChange = {},
                    label = {
                        Text(stringResource(R.string.enter_port_number))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Spacer(Modifier.padding(16.dp))

                Button(onClick = {
                    if(gatewayServer != null) {
                        viewModel.update(
                            context, gatewayServer.apply {
                                this.smtp = SMTP(
                                    host,
                                    username,
                                    password,
                                    from,
                                    recipient,
                                    subject,
                                    port.toInt()
                                )
                            },
                        ) {
                            onDismissCallback()
                        }
                    }
                    else {
                        viewModel.add(
                            context, GatewayServer().apply {
                                this.smtp = SMTP(
                                    host,
                                    username,
                                    password,
                                    from,
                                    recipient,
                                    subject,
                                    port.toInt()
                                )
                            },
                        ) {
                            onDismissCallback()
                        }
                    }
                }) {
                    Text(stringResource(R.string.add))
                }

                Spacer(Modifier.padding(16.dp))

                if(gatewayServer != null || LocalInspectionMode.current) {
                    Button(onClick = {
                        viewModel.delete(
                            context = context,
                            gatewayClient = gatewayServer!!,
                        ){
                            onDismissCallback()
                        }
                    }, colors = ButtonDefaults
                        .buttonColors(MaterialTheme.colorScheme.error) ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GatewayServerAddSmtpModalPreview() {
    AppTheme {
        GatewayServerAddSmtpModal(
            true,
            remember{ GatewayServerViewModel() }) {
        }
    }
}
