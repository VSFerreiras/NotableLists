package ucne.edu.notablelists.presentation.Notes.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ucne.edu.notablelists.domain.friends.model.Friend

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteEditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToFriends: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isProcessingClick by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    val safeOnBack = {
        if (!isProcessingClick && !state.isLoading) {
            isProcessingClick = true
            viewModel.onEvent(NoteEditEvent.BackClicked)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.navigationEvent) {
        state.navigationEvent?.let { effect ->
            when (effect) {
                is NoteEditSideEffect.NavigateBack -> onNavigateBack()
                is NoteEditSideEffect.NavigateToLogin -> onNavigateToLogin()
                is NoteEditSideEffect.NavigateToFriends -> onNavigateToFriends()
            }
            viewModel.onEvent(NoteEditEvent.NavigationHandled)
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(NoteEditEvent.DialogDismissed)
            isProcessingClick = false
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(NoteEditEvent.DialogDismissed)
        }
    }

    BackHandler(enabled = !state.isLoading && !isProcessingClick) {
        safeOnBack()
    }

    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.DialogDismissed) },
            title = { Text(if (state.isOwner) "Eliminar nota" else "Salir de nota") },
            text = { Text("¿Estás seguro de que deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(NoteEditEvent.DeleteConfirmed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditEvent.DialogDismissed) }) { Text("Cancelar") }
            }
        )
    }

    if (state.collaboratorPendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.DialogDismissed) },
            title = { Text("Eliminar acceso") },
            text = { Text("¿Eliminar a ${state.collaboratorPendingRemoval?.username}?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(NoteEditEvent.RemoveCollaboratorConfirmed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditEvent.DialogDismissed) }) { Text("Cancelar") }
            }
        )
    }

    if (state.isLoginRequiredDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.DialogDismissed) },
            title = { Text("Iniciar Sesión") },
            text = { Text("Necesitas estar conectado para compartir.") },
            confirmButton = {
                Button(onClick = { viewModel.onEvent(NoteEditEvent.LoginClicked) }) { Text("Iniciar Sesión") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditEvent.DialogDismissed) }) { Text("Cancelar") }
            }
        )
    }

    if (state.isNoFriendsDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.DialogDismissed) },
            title = { Text("¡Necesitas Amigos!") },
            text = { Text("Agrega amigos para compartir notas.") },
            confirmButton = {
                Button(onClick = { viewModel.onEvent(NoteEditEvent.FriendsClicked) }) { Text("Ir a Amigos") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditEvent.DialogDismissed) }) { Text("Cancelar") }
            }
        )
    }

    if (state.isShareSheetVisible) {
        ModalBottomSheet(onDismissRequest = { viewModel.onEvent(NoteEditEvent.DialogDismissed) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Compartir con...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.friends) { friend ->
                        FriendSelectionItem(
                            friend,
                            onClick = { viewModel.onEvent(NoteEditEvent.ShareWithFriend(friend.id)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        DateTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                val date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                viewModel.onEvent(NoteEditEvent.ReminderSet(date, timePickerState.hour, timePickerState.minute))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (state.isTagSheetVisible) {
        ModalBottomSheet(onDismissRequest = { viewModel.onEvent(NoteEditEvent.TagSelected(state.tag)) }) {
            TagSelectionSheetContent(
                availableTags = state.availableTags,
                currentTag = state.tag,
                onTagSelected = { viewModel.onEvent(NoteEditEvent.TagSelected(it)) },
                onTagCreated = { viewModel.onEvent(NoteEditEvent.TagCreated(it)) },
                onTagDelete = { viewModel.onEvent(NoteEditEvent.TagDeleted(it)) }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = safeOnBack,
                        enabled = !state.isLoading && !isProcessingClick
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.remoteId != null) {
                        Box {
                            IconButton(onClick = { viewModel.onEvent(NoteEditEvent.CollaboratorMenuClicked) }) {
                                Icon(Icons.Outlined.Group, "Collaborators", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = state.isCollaboratorMenuExpanded,
                                onDismissRequest = { viewModel.onEvent(NoteEditEvent.CollaboratorMenuClicked) },
                                modifier = Modifier.width(280.dp).background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Text("En esta nota", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                state.collaborators.forEach { collaborator ->
                                    DropdownMenuItem(
                                        text = { Text(collaborator.username, fontWeight = if (collaborator.isOwner) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            Surface(shape = CircleShape, color = if (collaborator.isOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(32.dp)) {
                                                Box(contentAlignment = Alignment.Center) { Text(collaborator.username.take(1).uppercase()) }
                                            }
                                        },
                                        onClick = {},
                                        trailingIcon = {
                                            if (state.isOwner && !collaborator.isOwner) {
                                                IconButton(onClick = { viewModel.onEvent(NoteEditEvent.RemoveCollaboratorRequested(collaborator)) }) {
                                                    Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!state.isLoading) {
                FabMenu(
                    expanded = isFabMenuExpanded,
                    onToggle = { isFabMenuExpanded = !isFabMenuExpanded },
                    onPriorityClick = { viewModel.onEvent(NoteEditEvent.PriorityChanged((state.priority + 1) % 3)); isFabMenuExpanded = false },
                    onReminderClick = { showDatePicker = true; isFabMenuExpanded = false },
                    onChecklistClick = { viewModel.onEvent(NoteEditEvent.ChecklistItemAdded); isFabMenuExpanded = false },
                    onTagClick = { viewModel.onEvent(NoteEditEvent.TagMenuClicked); isFabMenuExpanded = false },
                    onShareClick = { viewModel.onEvent(NoteEditEvent.ShareClicked); isFabMenuExpanded = false },
                    onDeleteClick = { viewModel.onEvent(NoteEditEvent.DeleteClicked); isFabMenuExpanded = false },
                    isOwner = state.isOwner
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TransparentHintTextField(
                    text = state.title,
                    hint = "Título",
                    onValueChange = { viewModel.onEvent(NoteEditEvent.TitleChanged(it)) },
                    textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRowChips(
                    state = state,
                    onRemoveReminder = { viewModel.onEvent(NoteEditEvent.ReminderCleared) },
                    onRemoveTag = { viewModel.onEvent(NoteEditEvent.TagChanged("")) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (state.checklist.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.checklist.forEachIndexed { index, item ->
                            ChecklistItemRow(
                                item = item,
                                onTextChange = { viewModel.onEvent(NoteEditEvent.ChecklistItemUpdated(index, it)) },
                                onToggle = { viewModel.onEvent(NoteEditEvent.ChecklistItemToggled(index)) },
                                onRemove = { viewModel.onEvent(NoteEditEvent.ChecklistItemRemoved(index)) }
                            )
                        }
                        TextButton(onClick = { viewModel.onEvent(NoteEditEvent.ChecklistItemAdded) }) { Text("+ Añadir") }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                TransparentHintTextField(
                    text = state.description,
                    hint = "Escribe algo...",
                    onValueChange = { viewModel.onEvent(NoteEditEvent.DescriptionChanged(it)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    ),
                    singleLine = false,
                    modifier = Modifier.fillMaxSize()
                )
                Spacer(modifier = Modifier.height(100.dp))
            }

            if (state.isLoading || isProcessingClick) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.01f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) CircularWavyProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun FriendSelectionItem(
    friend: Friend,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = friend.username, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TagSelectionSheetContent(
    availableTags: List<String>,
    currentTag: String,
    onTagSelected: (String) -> Unit,
    onTagCreated: (String) -> Unit,
    onTagDelete: (String) -> Unit
) {
    var newTagText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Etiquetas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                label = { Text("Nueva etiqueta") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newTagText.isNotBlank()) {
                        onTagCreated(newTagText)
                        newTagText = ""
                        keyboardController?.hide()
                    }
                })
            )
            IconButton(onClick = {
                if (newTagText.isNotBlank()) {
                    onTagCreated(newTagText)
                    newTagText = ""
                    keyboardController?.hide()
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Add Tag") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(availableTags) { tag ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    NavigationDrawerItem(
                        label = { Text(tag) },
                        selected = tag == currentTag,
                        onClick = { onTagSelected(tag) },
                        icon = { Icon(Icons.Default.Label, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onTagDelete(tag) }) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Tag", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } },
        text = { content() }
    )
}

@Composable
fun FlowRowChips(
    state: NoteEditState,
    onRemoveReminder: () -> Unit,
    onRemoveTag: () -> Unit
) {
    if (state.priority > 0 || state.tag.isNotBlank() || !state.reminder.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.priority > 0) {
                val priorityText = when (state.priority) {
                    1 -> "Prioridad media"
                    2 -> "Prioridad alta"
                    else -> "P: ${state.priority}"
                }
                ChipInfo(text = priorityText, icon = Icons.Default.Flag)
            }
            if (state.tag.isNotBlank()) {
                ChipInfo(text = state.tag, icon = Icons.Default.Label, onDelete = onRemoveTag)
            }
            if (!state.reminder.isNullOrBlank()) {
                ChipInfo(text = state.reminder, icon = Icons.Default.Alarm, onDelete = onRemoveReminder)
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (item.text.isEmpty()) focusRequester.requestFocus()
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = item.isDone, onCheckedChange = { onToggle() })
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            if (item.text.isEmpty()) {
                Text(
                    text = "Elemento de lista",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )
            }
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = if (item.isDone)
                    MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                else MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove item", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onPriorityClick: () -> Unit,
    onReminderClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onTagClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isOwner: Boolean
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 135f else 0f, label = "fab_rotation")

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FabMenuItem(Icons.Default.Flag, "Prioridad", onPriorityClick)
                FabMenuItem(Icons.Default.Alarm, "Recordatorio", onReminderClick)
                FabMenuItem(Icons.Default.Label, "Etiqueta", onTagClick)
                FabMenuItem(Icons.Default.Share, "Compartir", onShareClick)
                FabMenuItem(Icons.Default.Checklist, "Lista", onChecklistClick)

                if (isOwner) {
                    FabMenuItem(Icons.Default.Delete, "Eliminar", onDeleteClick, MaterialTheme.colorScheme.errorContainer)
                } else {
                    FabMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Salir", onDeleteClick, MaterialTheme.colorScheme.errorContainer)
                }
            }
        }
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Menu", modifier = Modifier.rotate(rotation))
        }
    }
}

@Composable
fun FabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = containerColor, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
            Icon(imageVector = icon, contentDescription = label)
        }
    }
}

@Composable
fun TransparentHintTextField(
    text: String,
    hint: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    singleLine: Boolean = false
) {
    Box(modifier = modifier) {
        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
        if (text.isEmpty()) {
            Text(text = hint, style = textStyle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun ChipInfo(
    text: String,
    icon: ImageVector,
    onDelete: (() -> Unit)? = null
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).clickable { onDelete?.invoke() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onDelete != null) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
            }
        }
    }
}