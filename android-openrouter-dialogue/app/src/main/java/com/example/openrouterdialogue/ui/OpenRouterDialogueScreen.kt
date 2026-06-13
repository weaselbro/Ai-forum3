package com.example.openrouterdialogue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val FREE_MODELS = listOf(
    "openchat/openchat-7b:free",
    "huggingfaceh4/zephyr-7b-beta:free",
    "google/gemma-2-9b-it:free",
    "mistralai/mistral-7b-instruct:free"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenRouterDialogueScreen(
    viewModel: OpenRouterViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    var apiKey by rememberSaveable { mutableStateOf("") }
    var modelA by rememberSaveable { mutableStateOf("openchat/openchat-7b:free") }
    var modelB by rememberSaveable { mutableStateOf("huggingfaceh4/zephyr-7b-beta:free") }
    var systemPrompt by rememberSaveable {
        mutableStateOf("Engage in a candid, imaginative, and direct dialogue. Be creative, concise, and responsive to the other model.")
    }

    var maxMessagesText by rememberSaveable { mutableStateOf("100") }
    var turnDelayText by rememberSaveable { mutableStateOf("1000") }
    var temperatureText by rememberSaveable { mutableStateOf("0.8") }
    var maxTokensText by rememberSaveable { mutableStateOf("512") }

    val listState = rememberLazyListState()

    val canStart = status !is OpenRouterViewModel.ConnectionStatus.Loading &&
        status !is OpenRouterViewModel.ConnectionStatus.Running

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("OpenRouter Dialogue") }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("OpenRouter API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModelSelector(
                        label = "Model A",
                        selectedModel = modelA,
                        onModelSelected = { modelA = it },
                        modifier = Modifier.weight(1f),
                        enabled = canStart
                    )

                    ModelSelector(
                        label = "Model B",
                        selectedModel = modelB,
                        onModelSelected = { modelB = it },
                        modifier = Modifier.weight(1f),
                        enabled = canStart
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxMessagesText,
                        onValueChange = { maxMessagesText = it },
                        label = { Text("Max messages") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = turnDelayText,
                        onValueChange = { turnDelayText = it },
                        label = { Text("Delay ms") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = temperatureText,
                        onValueChange = { temperatureText = it },
                        label = { Text("Temperature") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxTokensText,
                        onValueChange = { maxTokensText = it },
                        label = { Text("Max tokens") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.start(
                                OpenRouterViewModel.DialogueConfig(
                                    apiKey = apiKey,
                                    modelA = modelA,
                                    modelB = modelB,
                                    systemPrompt = systemPrompt,
                                    maxMessages = maxMessagesText.toIntOrNull()?.coerceAtLeast(1) ?: 100,
                                    turnDelayMillis = turnDelayText.toLongOrNull()?.coerceAtLeast(250L) ?: 1_000L,
                                    temperature = temperatureText.toDoubleOrNull()?.coerceIn(0.0, 2.0) ?: 0.8,
                                    maxTokens = maxTokensText.toIntOrNull()?.coerceAtLeast(64) ?: 512
                                )
                            )
                        },
                        enabled = canStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = { viewModel.stop() },
                        enabled = status is OpenRouterViewModel.ConnectionStatus.Running,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.clear() },
                    enabled = messages.isNotEmpty() ||
                        status is OpenRouterViewModel.ConnectionStatus.Error ||
                        status is OpenRouterViewModel.ConnectionStatus.Stopped,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear")
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = status.toDisplayText(),
                    color = if (status is OpenRouterViewModel.ConnectionStatus.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Divider(Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = "Start the loop to see the conversation.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        ModelBubble(message)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedModel,
            onValueChange = {},
            label = { Text(label) },
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FREE_MODELS.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ModelBubble(
    message: OpenRouterViewModel.ChatMessageUi
) {
    val isModelA = message.role == OpenRouterViewModel.ModelRole.A

    val containerColor = if (isModelA) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = if (isModelA) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isModelA) {
            Alignment.Start
        } else {
            Alignment.End
        }
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isModelA) 4.dp else 16.dp,
                bottomEnd = if (isModelA) 16.dp else 4.dp
            ),
            color = containerColor,
            tonalElevation = 2.dp
        ) {
            Column(
                Modifier
                    .padding(12.dp)
                    .widthIn(max = 320.dp)
            ) {
                Text(
                    text = message.model,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.75f)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = message.formattedTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.65f)
                )
            }
        }
    }
}

private fun OpenRouterViewModel.ConnectionStatus.toDisplayText(): String {
    return when (this) {
        OpenRouterViewModel.ConnectionStatus.Idle -> "Idle"
        OpenRouterViewModel.ConnectionStatus.Loading -> "Preparing..."
        OpenRouterViewModel.ConnectionStatus.Running -> "Running"
        OpenRouterViewModel.ConnectionStatus.Stopped -> "Stopped"
        is OpenRouterViewModel.ConnectionStatus.Error -> "Error: $message"
    }
}

private fun OpenRouterViewModel.ChatMessageUi.formattedTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timestampMillis)
}
