package pe.kerolabs.pozzo.features.savingsgroups.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.features.savingsgroups.application.GetMemberPhotosUseCase

/** Loads, once per screen, the photos of the members of a group. */
@HiltViewModel
class MemberPhotosViewModel @Inject constructor(private val getMemberPhotos: GetMemberPhotosUseCase) : ViewModel() {

    private val _photos = MutableStateFlow<Map<String, String>>(emptyMap())
    val photos: StateFlow<Map<String, String>> = _photos.asStateFlow()

    private var loadedFor: String? = null

    fun load(groupId: String) {
        if (loadedFor == groupId) return
        loadedFor = groupId
        viewModelScope.launch { _photos.value = getMemberPhotos(groupId) }
    }
}

/**
 * The profile photo of each member of a group, by membership. Every context names the members of a group
 * with the same membership ids, so the pot, the reviews and the history use it to show the photos.
 * Members without the application or without a photo are not in the map.
 */
@Composable
fun rememberMemberPhotos(groupId: String?): Map<String, String> {
    if (groupId == null) return emptyMap()
    val viewModel: MemberPhotosViewModel = hiltViewModel(key = "member-photos-$groupId")
    LaunchedEffect(groupId) { viewModel.load(groupId) }
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    return photos
}
