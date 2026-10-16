package br.com.semesperanca.frontendleads.presentation.teams

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
import br.com.semesperanca.frontendleads.data.dto.TeamDto
import br.com.semesperanca.frontendleads.presentation.ui.AddButton
import br.com.semesperanca.frontendleads.presentation.ui.AppColors
import br.com.semesperanca.frontendleads.presentation.ui.SearchField
import br.com.semesperanca.frontendleads.presentation.ui.SubHeader
import br.com.semesperanca.frontendleads.presentation.ui.form.FormActions
import br.com.semesperanca.frontendleads.presentation.ui.form.ReusableForm
import br.com.semesperanca.frontendleads.resources.Res
import br.com.semesperanca.frontendleads.resources.groups
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private sealed interface TeamOverlay {
    data object None : TeamOverlay
    data object Create : TeamOverlay
    data class Edit(val team: TeamDto) : TeamOverlay
}

@Composable
fun TeamsScreen() {
    val viewModel: TeamsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var overlay by remember { mutableStateOf<TeamOverlay>(TeamOverlay.None) }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Gray50)) {
        when (val o = overlay) {
            is TeamOverlay.Create -> {
                val formValues = remember { mutableStateMapOf<String, String>().apply { putAll(teamFormDefaults()) } }
                TeamFormScreen(
                    title = "Novo Time",
                    values = formValues,
                    onValueChange = { k, v -> formValues[k] = v },
                    onClear = { teamFormFields.forEach { formValues[it.key] = "" } },
                    isLoading = state.isLoading,
                    onSave = { viewModel.create(toCreateTeamRequest(formValues)); overlay = TeamOverlay.None },
                    onBack = { overlay = TeamOverlay.None },
                )
            }
            is TeamOverlay.Edit -> {
                val formValues = remember(o.team) { mutableStateMapOf<String, String>().apply { putAll(teamFormDefaults(o.team)) } }
                TeamFormScreen(
                    title = "Editar Time",
                    values = formValues,
                    onValueChange = { k, v -> formValues[k] = v },
                    onClear = { teamFormFields.forEach { formValues[it.key] = "" } },
                    isLoading = state.isLoading,
                    onSave = { o.team.id?.let { viewModel.update(it, toUpdateTeamRequest(formValues)) }; overlay = TeamOverlay.None },
                    onBack = { overlay = TeamOverlay.None },
                )
            }
            TeamOverlay.None -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TeamsListContent(
                        state = state,
                        onSearch = { viewModel.search(it) },
                        onClearSearch = { viewModel.loadTeams() },
                        onEdit = { overlay = TeamOverlay.Edit(it) },
                        onDelete = { it.id?.let { id -> viewModel.delete(id) } },
                    )
                }
            }
        }

        if (overlay is TeamOverlay.None) {
            AddButton(
                onClick = { overlay = TeamOverlay.Create },
                contentDescription = "Novo Time",
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
    }
}

@Composable
private fun TeamsListContent(
    state: TeamsUiState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onEdit: (TeamDto) -> Unit,
    onDelete: (TeamDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchField(
            value = "",
            onValueChange = { if (it.isBlank()) onClearSearch() else onSearch(it) },
            placeholder = "Buscar por nome...",
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (state.teams.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.groups),
                    contentDescription = null,
                    tint = AppColors.Gray300,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum time cadastrado",
                    color = AppColors.Gray400,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(state.teams) { team ->
                    TeamRow(team = team, onEdit = { onEdit(team) }, onDelete = { onDelete(team) })
                    HorizontalDivider(color = AppColors.Gray200)
                }
            }
        }
    }
}

@Composable
private fun TeamRow(team: TeamDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = team.name ?: "Sem nome",
                color = AppColors.Gray900,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            team.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = it, color = AppColors.Gray500, fontSize = 13.sp)
            }
            team.maxAgents?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Máx. $it corretores", color = AppColors.Gray400, fontSize = 12.sp)
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
private fun TeamFormScreen(
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
                fields = teamFormFields,
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
