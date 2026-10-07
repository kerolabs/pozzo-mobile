package pe.kerolabs.pozzo.features.savingsgroups.presentation.mygroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.iam.application.ObserveDisplayNameUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.ObserveMyGroupsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.application.RefreshMyGroupsUseCase
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup

data class MyGroupsUiState(
    val displayName: String = "",
    val groups: List<SavingsGroup> = emptyList(),
    val isRefreshing: Boolean = true,
    val errorMessage: String? = null,
    val loadedOnce: Boolean = false,
) {
    val firstName: String get() = displayName.trim().substringBefore(' ')
    val showEmptyState: Boolean get() = loadedOnce && groups.isEmpty()
}

private data class RefreshState(val isRefreshing: Boolean, val errorMessage: String?, val loadedOnce: Boolean)

@HiltViewModel
class MyGroupsViewModel @Inject constructor(
    observeMyGroups: ObserveMyGroupsUseCase,
    observeDisplayName: ObserveDisplayNameUseCase,
    private val refreshMyGroups: RefreshMyGroupsUseCase,
) : ViewModel() {

    private val refresh = MutableStateFlow(RefreshState(isRefreshing = true, errorMessage = null, loadedOnce = false))

    val state: StateFlow<MyGroupsUiState> = combine(
        observeDisplayName(),
        observeMyGroups(),
        refresh,
    ) { name, groups, refreshState ->
        MyGroupsUiState(
            displayName = name.orEmpty(),
            groups = groups,
            isRefreshing = refreshState.isRefreshing,
            errorMessage = refreshState.errorMessage,
            loadedOnce = refreshState.loadedOnce || groups.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyGroupsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refresh.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshMyGroups()
            refresh.update {
                RefreshState(
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.userMessage(),
                    loadedOnce = it.loadedOnce || result.isSuccess,
                )
            }
        }
    }

    fun dismissError() {
        refresh.update { it.copy(errorMessage = null) }
    }
}
