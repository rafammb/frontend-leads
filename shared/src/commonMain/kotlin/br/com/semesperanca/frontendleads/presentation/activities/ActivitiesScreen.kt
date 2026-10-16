package br.com.semesperanca.frontendleads.presentation.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.semesperanca.frontendleads.data.dto.ActivityDto
import br.com.semesperanca.frontendleads.presentation.ui.AddButton
import br.com.semesperanca.frontendleads.presentation.ui.AppColors
import br.com.semesperanca.frontendleads.presentation.ui.SearchField
import br.com.semesperanca.frontendleads.presentation.ui.SubHeader
import br.com.semesperanca.frontendleads.presentation.ui.form.FormActions
import br.com.semesperanca.frontendleads.presentation.ui.form.ReusableForm
import br.com.semesperanca.frontendleads.resources.Res
import br.com.semesperanca.frontendleads.resources.calendar_month
import br.com.semesperanca.frontendleads.resources.check_circle
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private sealed interface ActivityOverlay {
    data object None : ActivityOverlay
    data object Create : ActivityOverlay
    data class Edit(val activity: ActivityDto) : ActivityOverlay
}

@Composable
fun ActivitiesScreen() {
    val viewModel: ActivitiesViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var overlay by remember { mutableStateOf<ActivityOverlay>(ActivityOverlay.None) }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Gray50)) {
        when (val o = overlay) {
            is ActivityOverlay.Create -> {
                val formValues = remember { mutableStateMapOf<String, String>().apply { putAll(activityFormDefaults()) } }
                ActivityFormScreen(
                    title = "Nova Atividade",
                    values = formValues,
                    onValueChange = { k, v -> formValues[k] = v },
                    onClear = { activityFormFields.forEach { formValues[it.key] = "" } },
                    isLoading = state.isLoading,
                    onSave = { viewModel.create(toCreateActivityRequest(formValues)); overlay = ActivityOverlay.None },
                    onBack = { overlay = ActivityOverlay.None },
                )
            }
            is ActivityOverlay.Edit -> {
                val formValues = remember(o.activity) { mutableStateMapOf<String, String>().apply { putAll(activityFormDefaults(o.activity)) } }
                ActivityFormScreen(
                    title = "Editar Atividade",
                    values = formValues,
                    onValueChange = { k, v -> formValues[k] = v },
                    onClear = { activityFormFields.forEach { formValues[it.key] = "" } },
                    isLoading = state.isLoading,
                    onSave = { o.activity.id?.let { viewModel.update(it, toUpdateActivityRequest(formValues)) }; overlay = ActivityOverlay.None },
                    onBack = { overlay = ActivityOverlay.None },
                )
            }
            ActivityOverlay.None -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    ActivitiesListContent(
                        state = state,
                        onSearch = { viewModel.loadActivities(search = it) },
                        onClearSearch = { viewModel.loadActivities() },
                        onEdit = { overlay = ActivityOverlay.Edit(it) },
                        onDelete = { it.id?.let { id -> viewModel.delete(id) } },
                    )
                }
            }
        }

        if (overlay is ActivityOverlay.None) {
            AddButton(
                onClick = { overlay = ActivityOverlay.Create },
                contentDescription = "Nova Atividade",
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
    }
}

@Composable
private fun ActivitiesListContent(
    state: ActivitiesUiState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onEdit: (ActivityDto) -> Unit,
    onDelete: (ActivityDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            value = "",
            onValueChange = { if (it.isBlank()) onClearSearch() else onSearch(it) },
            placeholder = "Buscar por título...",
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.activities.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.calendar_month),
                    contentDescription = null,
                    tint = AppColors.Gray300,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhuma atividade cadastrada",
                    color = AppColors.Gray400,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(state.activities) { activity ->
                    ActivityRow(
                        activity = activity,
                        onEdit = { onEdit(activity) },
                        onDelete = { onDelete(activity) },
                    )
                    HorizontalDivider(color = AppColors.Gray200)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: ActivityDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(
                    if (activity.completed) Res.drawable.check_circle else Res.drawable.calendar_month,
                ),
                contentDescription = null,
                tint = if (activity.completed) AppColors.Emerald500 else AppColors.Amber500,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = activity.title ?: "Sem título",
                    color = AppColors.Gray900,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                activity.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it.take(60) + if (it.length > 60) "…" else "",
                        color = AppColors.Gray500,
                        fontSize = 13.sp,
                    )
                }
                activity.activityDate?.take(16)?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it.replace("T", " às "),
                        color = AppColors.Gray400,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Editar",
                color = AppColors.Emerald600,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(4.dp),
            )
            Text(
                text = "Excluir",
                color = AppColors.Red500,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun ActivityFormScreen(
    title: String,
    values: Map<String, String>,
    onValueChange: (String, String) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Gray50)) {
        SubHeader(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ReusableForm(
                fields = activityFormFields,
                values = values,
                onValueChange = onValueChange,
            )
            Spacer(modifier = Modifier.height(20.dp))
            FormActions(
                onSave = onSave,
                onClear = onClear,
            )
        }
    }
}
