package ucne.edu.notablelists.presentation.notes_list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ucne.edu.notablelists.presentation.Notes.list.*
import ucne.edu.notablelists.presentation.users.UserEvent
import ucne.edu.notablelists.presentation.users.UserState
import ucne.edu.notablelists.presentation.users.UserViewModel

@Composable
fun NotesListRoute(
    viewModel: NotesListViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    onNavigateToDetail: (String?) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userState by userViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = true) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is NotesListSideEffect.NavigateToDetail -> onNavigateToDetail(effect.noteId)
                is NotesListSideEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(NotesListEvent.SelectionCleared)
    }

    NotesListScreen(
        state = state,
        userState = userState,
        onEvent = viewModel::onEvent,
        onUserEvent = userViewModel::onEvent,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToFriends = onNavigateToFriends
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesListScreen(
    state: NotesListState,
    userState: UserState,
    onEvent: (NotesListEvent) -> Unit,
    onUserEvent: (UserEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(NotesListEvent.LogoutDismissed) },
            icon = { Icon(Icons.Outlined.ExitToApp, contentDescription = null) },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Seguro quieres cerrar sesión en NotableLists?") },
            confirmButton = {
                Button(
                    onClick = {
                        onUserEvent(UserEvent.Logout)
                        onEvent(NotesListEvent.LogoutConfirmed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar Sesión") }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NotesListEvent.LogoutDismissed) }) { Text("Cancelar") }
            }
        )
    }

    if (state.showDeleteSelectionDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(NotesListEvent.DeleteSelectedDismissed) },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Eliminar notas") },
            text = { Text("¿Deseas eliminar las ${state.selectedNoteIds.size} notas seleccionadas?") },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(NotesListEvent.DeleteSelectedConfirmed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NotesListEvent.DeleteSelectedDismissed) }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val fabContainerColor by animateColorAsState(
                if (state.isSelectionMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                label = "fabColor"
            )
            val fabContentColor by animateColorAsState(
                if (state.isSelectionMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                label = "fabContent"
            )

            ExtendedFloatingActionButton(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > 500) {
                        lastClickTime = currentTime
                        onEvent(NotesListEvent.AddNoteClicked)
                    }
                },
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                expanded = state.isSelectionMode,
                icon = {
                    AnimatedContent(
                        targetState = state.isSelectionMode,
                        transitionSpec = { scaleIn() togetherWith scaleOut() },
                        label = "fabIcon"
                    ) { isSelectionMode ->
                        if (isSelectionMode) Icon(Icons.Default.Delete, "Eliminar") else Icon(Icons.Default.Add, "Crear")
                    }
                },
                text = { Text("Eliminar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(NotesListEvent.SearchQueryChanged(it)) },
                    modifier = Modifier.weight(1f)
                )
                UserAvatarMenu(
                    currentUser = userState.currentUser,
                    pendingRequestCount = state.pendingRequestCount,
                    onLogoutClick = { onEvent(NotesListEvent.LogoutClicked) },
                    onLoginClick = onNavigateToLogin,
                    onFriendsClick = onNavigateToFriends
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            FilterChipsSection(
                filters = state.filterChips,
                onFilterSelected = {
                    onEvent(NotesListEvent.FilterChanged(it))
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading && state.notes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularWavyProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    items(state.notes, key = { it.id }) { noteUi ->
                        NoteItemCard(
                            noteUi = noteUi,
                            isSelectionMode = state.isSelectionMode,
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) {
                                    lastClickTime = currentTime
                                    onEvent(NotesListEvent.NoteClicked(noteUi.id))
                                }
                            },
                            onLongClick = { onEvent(NotesListEvent.NoteLongClicked(noteUi.id)) }
                        )
                    }
                    if (!state.isLoading && state.notes.isEmpty()) {
                        item { EmptyStateMessage() }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isFocused) 28.dp else 16.dp,
        label = "searchBarShape"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "searchBarColor"
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(cornerRadius),
        placeholder = {
            Text(
                "Buscar notas...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
        })
    )
}

@Composable
fun UserAvatarMenu(
    currentUser: String,
    pendingRequestCount: Int,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onFriendsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser.isNotBlank()

    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 16.dp else 50.dp,
        label = "avatarShape"
    )

    val containerColor by animateColorAsState(
        targetValue = if (expanded || isLoggedIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "avatarColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (expanded || isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "avatarContentColor"
    )

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.size(56.dp),
            border = if (isLoggedIn || expanded) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoggedIn) {
                    Text(
                        text = currentUser.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (pendingRequestCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Login",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
            ) {
                if (isLoggedIn) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Hola,",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentUser,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lista de Amigos")
                                if (pendingRequestCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(pendingRequestCount.toString())
                                    }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            onFriendsClick()
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión") },
                        leadingIcon = {
                            Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            onLogoutClick()
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Iniciar Sesión") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        onClick = {
                            onLoginClick()
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsSection(
    filters: List<FilterUiItem>,
    onFilterSelected: (NoteFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(filters) { item ->
            val isSelected = item.isSelected

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(item.filter) },
                label = { Text(item.label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemCard(
    modifier: Modifier = Modifier,
    noteUi: NoteUiItem,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (baseContainerColor, contentColor) = noteUi.style.getColors()

    val scale by animateFloatAsState(
        targetValue = if (noteUi.isSelected) 0.95f else 1f,
        label = "scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (noteUi.isSelected) MaterialTheme.colorScheme.surfaceVariant else baseContainerColor,
        label = "color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (noteUi.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (noteUi.isSelected) 0.dp else 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (noteUi.isShared) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nota compartida",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = noteUi.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (noteUi.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = noteUi.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor.copy(alpha = 0.85f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    noteUi.priorityChips.forEach { priority ->
                        val (_, pContent) = priority.style.getColors()
                        MetaDataChip(
                            text = priority.label,
                            icon = Icons.Default.Flag,
                            contentColor = pContent,
                            containerColor = pContent.copy(alpha = 0.1f)
                        )
                    }

                    noteUi.tags.forEach { tag ->
                        val (_, tContent) = tag.style.getColors()
                        MetaDataChip(
                            text = tag.label,
                            icon = Icons.Default.Label,
                            contentColor = contentColor,
                            containerColor = contentColor.copy(alpha = 0.1f)
                        )
                    }

                    if (!noteUi.reminder.isNullOrBlank()) {
                        MetaDataChip(
                            text = noteUi.reminder,
                            icon = Icons.Default.Alarm,
                            contentColor = contentColor,
                            containerColor = contentColor.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            if (noteUi.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetaDataChip(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    containerColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = contentColor
            )
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NoteAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay notas aún",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Crea una nueva nota para empezar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoteStyle.getColors(): Pair<Color, Color> {
    return when (this) {
        NoteStyle.Secondary -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
        NoteStyle.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NoteStyle.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
}