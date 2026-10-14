package com.afkanerd.deku.DefaultSMS.ui

import com.afkanerd.deku.DefaultSMS.R
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.compose.AppTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import com.afkanerd.deku.DefaultSMS.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.about_deku))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = BuildConfig.VERSION_NAME,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = stringResource(R.string.about_deku_motto),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = 8.dp,
                    top = 18.dp,
                    end = 8.dp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        top = 50.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(R.drawable.github_mark),
                    contentDescription = stringResource(
                        R.string.about_deku_github_titile
                    ),
                    modifier = Modifier.size(80.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.about_deku_github_titile
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(
                            R.string.about_deku_github_description
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.about_deku_github_btn
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                                val url = context.getString(
                                    R.string.about_deku_github_url
                                )

                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                    )
                                )
                            }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutPreview() {
    AppTheme {
        About()
    }
}