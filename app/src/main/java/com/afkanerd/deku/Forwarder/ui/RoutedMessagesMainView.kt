package com.afkanerd.deku.Forwarder.ui

import android.provider.Telephony
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.GatewayClientsListScreen
import com.afkanerd.deku.Forwarder.ui.viewModels.GatewayServerViewModel
import com.afkanerd.deku.GatewayClientsSettingsScreen
import com.afkanerd.smswithoutborders_libsmsmms.data.data.models.DateTimeUtils
import com.afkanerd.smswithoutborders_libsmsmms.data.data.models.SmsMmsNatives
import com.afkanerd.smswithoutborders_libsmsmms.data.entities.Conversations
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.isDefault
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.retrieveContactName
import com.afkanerd.smswithoutborders_libsmsmms.ui.components.ThreadConversationCard
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutedMessagesMainView(
    navController: NavController,
    viewModel: GatewayServerViewModel,
) {
    val context = LocalContext.current
    val inPreviewMode = LocalInspectionMode.current
    val routedMessages by viewModel.workFlowItems
        .collectAsStateWithLifecycle(emptyList())
    viewModel.getActiveWorkManagerItems(context)

    val isDefault by remember { mutableStateOf(inPreviewMode || context.isDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(R.string.routed_messages),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearAllRouted(context)
                    }) {
                        Icon(Icons.Filled.ClearAll,
                            stringResource(R.string.list_gateway_clients)
                        )
                    }

                    IconButton(onClick = {
                        navController.navigate(GatewayClientsListScreen)
                    }) {
                        Icon(Icons.AutoMirrored.Default.List,
                            stringResource(R.string.list_gateway_clients)
                        )
                    }

                    IconButton(onClick = {
                        navController.navigate(GatewayClientsSettingsScreen)
                    }) {
                        Icon(Icons.Outlined.Settings,
                            stringResource(R.string.list_gateway_clients)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            LazyColumn {
                items(
                    items = routedMessages,
                    key =  { it.conversation.id }
                ) { routedItemsConversations ->
                    RouterItemCard(
                        routedItemsConversations.conversation,
                        isDefault = isDefault,
                        status = routedItemsConversations.workInfo.state.name.lowercase(),
                    )
                }
            }
        }
    }
}

@Composable
fun RouterItemCard(
    conversation: Conversations,
    isDefault: Boolean,
    status: String,
) {
    val inPreviewMode = LocalInspectionMode.current
    val context = LocalContext.current

    val address = conversation.sms?.address!!
    val contactName = if(isDefault)
        context.retrieveContactName(address) ?: address
    else address

    val date = if(!inPreviewMode) DateTimeUtils.formatDate(
        context,
        conversation.sms?.date!!
    ) ?: "" else "Tues"

    OutlinedCard(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = {}
            ),
        onClick = {},
    ) {
        Column {
            ThreadConversationCard(
                id = conversation.sms?.thread_id!!,
                content = conversation.sms?.body!!,
                date = date,
                isRead = true, // TODO: get actual later
                isContact = isDefault && !contactName.isNullOrBlank(),
                unreadCount = 0,
                isSelected = false,
                isMuted = false,
                isBlocked = false,
                type = conversation.sms?.type!!,
                mms = conversation.mms?.thread_id != null,
                modifier = Modifier,
                contactPhotoUri = "",
                name = contactName,
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text( status,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

    }

}

@Preview
@Composable
fun RouterItemCardPreview() {
    AppTheme {
        RouterItemCard(
            conversation = Conversations(
                sms = SmsMmsNatives.Sms(
                    thread_id = 1,
                    address = "+123456789",
                    date = System.currentTimeMillis(),
                    date_sent = System.currentTimeMillis(),
                    read = 0,
                    status = Telephony.Sms.STATUS_COMPLETE,
                    type = Telephony.Sms.MESSAGE_TYPE_SENT,
                    body = "Hello world",
                    sub_id = 1
                )
            ),
            true,
            status = "success"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutedMessagesMainViewPreview() {
    AppTheme {
        RoutedMessagesMainView(
            rememberNavController(),
            remember{ GatewayServerViewModel() }
        )
    }
}
