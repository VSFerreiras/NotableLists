package ucne.edu.notablelists.presentation.Notes.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isMenuExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickerContext by remember { mutableStateOf(PickerContext.REMINDER) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler {
        viewModel.onEvent(NoteEditEvent.OnBackClick)
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is NoteEditUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.DismissDeleteDialog) },
            title = { Text("¿Estás seguro?") },
            text = { Text("Eliminar una nota es permanente y no se puede deshacer") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(NoteEditEvent.DeleteNote) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NoteEditEvent.DismissDeleteDialog) }) { Text("Cancelar") }
            }
        )
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
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        DateTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                val date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val hour = timePickerState.hour
                val minute = timePickerState.minute

                if (pickerContext == PickerContext.REMINDER) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.onEvent(NoteEditEvent.SetReminder(date, hour, minute))
                }
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (state.isTagSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(NoteEditEvent.HideTagSheet) },
            sheetState = sheetState
        ) {
            TagSelectionSheetContent(
                availableTags = state.availableTags,
                currentTag = state.tag,
                onTagSelected = { viewModel.onEvent(NoteEditEvent.SelectTag(it)) },
                onTagCreated = { viewModel.onEvent(NoteEditEvent.CreateNewTag(it)) },
                onTagDelete = { viewModel.onEvent(NoteEditEvent.DeleteAvailableTag(it)) }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(NoteEditEvent.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FabMenu(
                expanded = isMenuExpanded,
                onToggle = { isMenuExpanded = !isMenuExpanded },
                onPriorityClick = {
                    val nextPriority = (state.priority + 1) % 3
                    viewModel.onEvent(NoteEditEvent.ChangePriority(nextPriority))
                    isMenuExpanded = false
                },
                onReminderClick = {
                    pickerContext = PickerContext.REMINDER
                    showDatePicker = true
                    isMenuExpanded = false
                },
                onChecklistClick = {
                    viewModel.onEvent(NoteEditEvent.AddChecklistItem)
                    isMenuExpanded = false
                },
                onTagClick = {
                    viewModel.onEvent(NoteEditEvent.ShowTagSheet)
                    isMenuExpanded = false
                },
                onDeleteClick = {
                    viewModel.onEvent(NoteEditEvent.ShowDeleteDialog)
                    isMenuExpanded = false
                }
            )
        }
    ) { paddingValues ->
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
                onValueChange = { viewModel.onEvent(NoteEditEvent.EnteredTitle(it)) },
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRowChips(
                state = state,
                onRemoveReminder = { viewModel.onEvent(NoteEditEvent.ClearReminder) },
                onRemoveTag = { viewModel.onEvent(NoteEditEvent.EnteredTag("")) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.checklist.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.checklist.forEachIndexed { index, item ->
                        ChecklistItemRow(
                            item = item,
                            onTextChange = { viewModel.onEvent(NoteEditEvent.UpdateChecklistItem(index, it)) },
                            onToggle = { viewModel.onEvent(NoteEditEvent.ToggleChecklistItem(index)) },
                            onRemove = { viewModel.onEvent(NoteEditEvent.RemoveChecklistItem(index)) }
                        )
                    }
                    TextButton(onClick = { viewModel.onEvent(NoteEditEvent.AddChecklistItem) }) {
                        Text("+ Añadir")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            TransparentHintTextField(
                text = state.description,
                hint = "Escribe algo...",
                onValueChange = { viewModel.onEvent(NoteEditEvent.EnteredDescription(it)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                ),
                singleLine = false,
                modifier = Modifier.fillMaxSize()
            )

            Spacer(modifier = Modifier.height(100.dp))
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Etiquetas",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableTags) { tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationDrawerItem(
                        label = { Text(tag) },
                        selected = tag == currentTag,
                        onClick = { onTagSelected(tag) },
                        icon = { Icon(Icons.Default.Label, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onTagDelete(tag) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Tag",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

enum class PickerContext { REMINDER, AUTO_DELETE }

@Composable
fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        },
        text = { content() }
    )
}

@Composable
fun FlowRowChips(
    state: NoteEditState,
    onRemoveReminder: () -> Unit,
    onRemoveTag: () -> Unit
) {
    if (state.priority > 0 || state.tag.isNotBlank() || state.reminder != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.priority > 0) {
                val priorityText = when(state.priority) {
                    1 -> "Prioridad media"
                    2 -> "Prioridad alta"
                    else -> "P: ${state.priority}"
                }
                ChipInfo(text = priorityText, icon = Icons.Default.Flag)
            }
            if (state.tag.isNotBlank()) {
                ChipInfo(text = state.tag, icon = Icons.Default.Label, onDelete = onRemoveTag)
            }
            state.reminder?.let {
                ChipInfo(text = it, icon = Icons.Default.Alarm, onDelete = onRemoveReminder)
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
        if(item.text.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = item.isDone,
            onCheckedChange = { onToggle() }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            if (item.text.isEmpty()) {
                Text(
                    text = "Elemento de lista",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
            }
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = if (item.isDone)
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                else MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
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
    onDeleteClick: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 135f else 0f, label = "fab_rotation")

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FabMenuItem(Icons.Default.Flag, "Prioridad", onPriorityClick)
                FabMenuItem(Icons.Default.Alarm, "Recordatorio", onReminderClick)
                FabMenuItem(Icons.Default.Label, "Etiqueta", onTagClick)
                FabMenuItem(Icons.Default.Checklist, "Lista", onChecklistClick)
                FabMenuItem(Icons.Default.Delete, "Eliminar", onDeleteClick, MaterialTheme.colorScheme.errorContainer)
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier.rotate(rotation)
            )
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
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
            Text(
                text = hint,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun ChipInfo(
    text: String,
    icon: ImageVector,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).clickable { onDelete?.invoke() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onDelete != null) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
            }
        }
    }
}