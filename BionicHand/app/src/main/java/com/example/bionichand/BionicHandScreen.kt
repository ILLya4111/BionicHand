package com.example.bionichand

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bionichand.ui.theme.MedicalAccent
import com.example.bionichand.ui.theme.MedicalActiveSoft
import com.example.bionichand.ui.theme.MedicalBackground
import com.example.bionichand.ui.theme.MedicalBorder
import com.example.bionichand.ui.theme.MedicalDanger
import com.example.bionichand.ui.theme.MedicalOnPrimary
import com.example.bionichand.ui.theme.MedicalPrimary
import com.example.bionichand.ui.theme.MedicalPrimaryDark
import com.example.bionichand.ui.theme.MedicalSurface
import com.example.bionichand.ui.theme.MedicalSurfaceAlt
import com.example.bionichand.ui.theme.MedicalText
import com.example.bionichand.ui.theme.MedicalTextMuted

private const val FINGER_COUNT = 4

@Composable
fun BionicHandScreen(
    isConnected: Boolean,
    batteryLevel: String,
    emgThreshold: String,
    gestureStorage: GestureStorage,
    onConnectClick: () -> Unit,
    onSendBluetoothCommand: (String) -> Unit
) {
    var libraryGestures by remember { mutableStateOf(gestureStorage.getLibrary()) }
    var activeSlots by remember { mutableStateOf(gestureStorage.getActiveSlots()) }
    var selectedTab by remember { mutableStateOf(0) }
    var showGestureDialog by remember { mutableStateOf(false) }
    var showEmgDialog by remember { mutableStateOf(false) }
    var editingGesture by remember { mutableStateOf<CustomGesture?>(null) }
    var gestureToAssign by remember { mutableStateOf<CustomGesture?>(null) }
    var activeSlotIndex by remember { mutableStateOf(-1) }

    fun refreshData() {
        libraryGestures = gestureStorage.getLibrary()
        activeSlots = gestureStorage.getActiveSlots()
    }

    Surface(color = MedicalBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Біонічний протез",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Керування жестами та EMG",
                        fontSize = 13.sp,
                        color = MedicalTextMuted
                    )
                }

                ConnectionPill(isConnected = isConnected)
            }

            Spacer(modifier = Modifier.height(14.dp))

            StatusCard(
                isConnected = isConnected,
                batteryLevel = batteryLevel,
                emgThreshold = emgThreshold,
                onConnectClick = onConnectClick,
                onOpenEmgSettings = { showEmgDialog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            AppTabs(selectedTab = selectedTab, onTabChange = { selectedTab = it })

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                LibraryTab(
                    gestures = libraryGestures,
                    isConnected = isConnected,
                    onCreateGesture = {
                        editingGesture = null
                        showGestureDialog = true
                        if (isConnected) onSendBluetoothCommand("P:0,0,0,0*")
                    },
                    onAssignGesture = { gestureToAssign = it },
                    onEditGesture = {
                        editingGesture = it
                        showGestureDialog = true
                        if (isConnected) onSendBluetoothCommand("P:${it.angles.joinToString(",")}*")
                    },
                    onDeleteGesture = {
                        gestureStorage.deleteFromLibrary(it.id)
                        refreshData()
                    }
                )
            } else {
                SlotsTab(
                    activeSlots = activeSlots,
                    activeSlotIndex = activeSlotIndex,
                    isConnected = isConnected,
                    onActivateSlot = { slotNumber ->
                        if (isConnected && activeSlotIndex != slotNumber) {
                            activeSlotIndex = slotNumber
                            onSendBluetoothCommand("A:$slotNumber*")
                        }
                    },
                    onClearSlot = { slotIndex ->
                        gestureStorage.clearActiveSlot(slotIndex)
                        if (activeSlotIndex == slotIndex + 1) activeSlotIndex = -1
                        refreshData()
                    }
                )
            }
        }
    }

    if (gestureToAssign != null) {
        AssignGestureDialog(
            gesture = gestureToAssign!!,
            activeSlots = activeSlots,
            isConnected = isConnected,
            onSendBluetoothCommand = onSendBluetoothCommand,
            onSave = { slotIndex, gesture ->
                gestureStorage.saveToActiveSlot(slotIndex, gesture)
                refreshData()
                gestureToAssign = null
            },
            onDismiss = { gestureToAssign = null }
        )
    }

    if (showGestureDialog) {
        GestureEditDialog(
            editingGesture = editingGesture,
            isConnected = isConnected,
            onSendBluetoothCommand = onSendBluetoothCommand,
            onSave = { gesture ->
                if (editingGesture == null) {
                    gestureStorage.saveToLibrary(gesture)
                } else {
                    gestureStorage.updateInLibrary(gesture)
                }
                refreshData()
                showGestureDialog = false
                if (isConnected) onSendBluetoothCommand("E*")
            },
            onDismiss = {
                showGestureDialog = false
                if (isConnected) onSendBluetoothCommand("E*")
            }
        )
    }

    if (showEmgDialog) {
        EmgSettingsDialog(
            isConnected = isConnected,
            threshold = emgThreshold,
            onSendBluetoothCommand = onSendBluetoothCommand,
            onDismiss = { showEmgDialog = false }
        )
    }
}

@Composable
private fun ConnectionPill(isConnected: Boolean) {
    val text = if (isConnected) "Підключено" else "Не підключено"
    val color = if (isConnected) MedicalAccent else MedicalTextMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusCard(
    isConnected: Boolean,
    batteryLevel: String,
    emgThreshold: String,
    onConnectClick: () -> Unit,
    onOpenEmgSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MedicalSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MedicalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MedicalDanger else MedicalPrimary
                    )
                ) {
                    Text(if (isConnected) "Відключити" else "Підключити протез", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenEmgSettings,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MedicalPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalPrimary)
                ) {
                    Text("EMG")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallInfoBox(
                    title = "Заряд",
                    value = if (isConnected) "$batteryLevel%" else "--",
                    modifier = Modifier.weight(1f)
                )
                SmallInfoBox(
                    title = "Поріг EMG",
                    value = emgThreshold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SmallInfoBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MedicalSurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(title, color = MedicalTextMuted, fontSize = 12.sp)
        Text(value, color = MedicalText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppTabs(selectedTab: Int, onTabChange: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MedicalBackground,
        contentColor = MedicalPrimary,
        divider = {},
        indicator = {}
    ) {
        TabButton("Бібліотека", selectedTab == 0) { onTabChange(0) }
        TabButton("Пам'ять протеза", selectedTab == 1) { onTabChange(1) }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Tab(selected = selected, onClick = onClick) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) MedicalPrimary else MedicalSurfaceAlt)
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) MedicalOnPrimary else MedicalTextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LibraryTab(
    gestures: List<CustomGesture>,
    isConnected: Boolean,
    onCreateGesture: () -> Unit,
    onAssignGesture: (CustomGesture) -> Unit,
    onEditGesture: (CustomGesture) -> Unit,
    onDeleteGesture: (CustomGesture) -> Unit
) {
    Button(
        onClick = onCreateGesture,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryDark)
    ) { Text("Створити новий жест") }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(gestures) { gesture ->
            GestureCard(
                gesture = gesture,
                isConnected = isConnected,
                onAssign = { onAssignGesture(gesture) },
                onEdit = { onEditGesture(gesture) },
                onDelete = { onDeleteGesture(gesture) }
            )
        }
    }
}

@Composable
private fun GestureCard(
    gesture: CustomGesture,
    isConnected: Boolean,
    onAssign: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalSurface),
        border = BorderStroke(1.dp, MedicalBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                gesture.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MedicalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (gesture.isStandard) "Стандартний жест" else "Користувацький жест",
                color = MedicalTextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAssign,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
                ) { Text("У слот", maxLines = 1) }

                if (!gesture.isStandard) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MedicalBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalPrimaryDark)
                    ) { Text("Змінити", maxLines = 1) }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(0.9f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MedicalDanger.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalDanger)
                    ) { Text("Видалити", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun SlotsTab(
    activeSlots: List<CustomGesture?>,
    activeSlotIndex: Int,
    isConnected: Boolean,
    onActivateSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            SlotCard(
                title = "Слот 0",
                subtitle = "Апаратний режим",
                gestureName = "Базовий кулак",
                isActive = activeSlotIndex == 0,
                canClear = false,
                isConnected = isConnected,
                onActivate = { onActivateSlot(0) },
                onClear = {}
            )
        }

        items(3) { index ->
            val slotNumber = index + 1
            SlotCard(
                title = "Слот $slotNumber",
                subtitle = "Користувацький режим",
                gestureName = activeSlots.getOrNull(index)?.name ?: "Пусто",
                isActive = activeSlotIndex == slotNumber,
                canClear = activeSlots.getOrNull(index) != null,
                isConnected = isConnected,
                onActivate = { onActivateSlot(slotNumber) },
                onClear = { onClearSlot(index) }
            )
        }
    }
}

@Composable
private fun SlotCard(
    title: String,
    subtitle: String,
    gestureName: String,
    isActive: Boolean,
    canClear: Boolean,
    isConnected: Boolean,
    onActivate: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MedicalActiveSoft else MedicalSurface),
        border = BorderStroke(1.dp, if (isActive) MedicalAccent else MedicalBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MedicalPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = MedicalTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(gestureName, color = MedicalText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onActivate,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isActive) MedicalAccent else MedicalPrimary)
                ) { Text(if (isActive) "Активний" else "Активувати", maxLines = 1) }

                if (canClear) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(0.85f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MedicalBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalTextMuted)
                    ) { Text("Очистити", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun AssignGestureDialog(
    gesture: CustomGesture,
    activeSlots: List<CustomGesture?>,
    isConnected: Boolean,
    onSendBluetoothCommand: (String) -> Unit,
    onSave: (Int, CustomGesture) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зберегти в пам'ять") },
        text = {
            Column {
                Text("Оберіть слот для жесту «${gesture.name}»:")
                Spacer(modifier = Modifier.height(12.dp))
                activeSlots.forEachIndexed { index, current ->
                    val slotNumber = index + 1
                    OutlinedButton(
                        onClick = {
                            onSave(index, gesture)
                            if (isConnected) onSendBluetoothCommand("W:$slotNumber:${gesture.angles.joinToString(",")}*")
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) { Text("Слот $slotNumber: ${current?.name ?: "вільний"}") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
private fun GestureEditDialog(
    editingGesture: CustomGesture?,
    isConnected: Boolean,
    onSendBluetoothCommand: (String) -> Unit,
    onSave: (CustomGesture) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editingGesture?.name ?: "") }
    var angles by remember { mutableStateOf(editingGesture?.angles?.normalizedAngles() ?: List(FINGER_COUNT) { 0 }) }
    val fingerNames = listOf("Великий", "Вказівний", "Середній", "Безіменний + мізинець")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingGesture == null) "Новий жест" else "Редагування жесту") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("EMG тимчасово вимкнено. Рух пальців можна перевіряти з повзунків.", color = MedicalTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Назва жесту") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                fingerNames.forEachIndexed { index, fingerName ->
                    Text("$fingerName: ${angles[index]}°", color = MedicalText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = angles[index].toFloat(),
                        onValueChange = { newValue ->
                            val angle = newValue.toInt()
                            angles = angles.toMutableList().also { it[index] = angle }
                            if (isConnected) onSendBluetoothCommand("P:$index:$angle*")
                        },
                        valueRange = 0f..180f
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val gesture = if (editingGesture == null) {
                            CustomGesture(name = name.trim(), angles = angles)
                        } else {
                            editingGesture.copy(name = name.trim(), angles = angles)
                        }
                        onSave(gesture)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
            ) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
private fun EmgSettingsDialog(
    isConnected: Boolean,
    threshold: String,
    onSendBluetoothCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualValue by remember(threshold) {
        mutableStateOf(threshold.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 1023) ?: 700)
    }
    var instructionStep by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштування EMG") },
        text = {
            Column {
                Text("Поточний поріг: $threshold", color = MedicalText, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (instructionStep) {
                        1 -> "Калібрування запущено. Спочатку тримайте руку розслабленою, потім напружте м'яз за підказкою."
                        else -> "Автокалібрування підбере поріг за сигналом датчика. Перед запуском підключіть протез."
                    },
                    color = MedicalTextMuted,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        instructionStep = 1
                        if (isConnected) onSendBluetoothCommand("C*")
                    },
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
                ) { Text("Запустити автокалібрування") }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Поріг вручну: $manualValue",
                    color = MedicalText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Slider(
                    value = manualValue.toFloat(),
                    onValueChange = { manualValue = it.toInt().coerceIn(0, 1023) },
                    valueRange = 0f..1023f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0", color = MedicalTextMuted, fontSize = 12.sp)
                    Text("1023", color = MedicalTextMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (isConnected) onSendBluetoothCommand("T:$manualValue*")
                    },
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalPrimary)
                ) { Text("Застосувати вручну") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } }
    )
}

private fun List<Int>.normalizedAngles(): List<Int> {
    return when {
        size == FINGER_COUNT -> this.map { it.coerceIn(0, 180) }
        size >= 5 -> listOf(this[0], this[1], this[2], maxOf(this[3], this[4])).map { it.coerceIn(0, 180) }
        else -> (this + List(FINGER_COUNT - size) { 0 }).map { it.coerceIn(0, 180) }
    }
}
