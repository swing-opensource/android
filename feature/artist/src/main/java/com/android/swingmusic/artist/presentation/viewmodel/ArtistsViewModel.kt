package com.android.swingmusic.artist.presentation.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.android.swingmusic.artist.presentation.event.ArtistUiEvent
import com.android.swingmusic.artist.presentation.state.ArtistsUiState
import com.android.swingmusic.auth.domain.repository.AuthRepository
import com.android.swingmusic.core.data.util.Resource
import com.android.swingmusic.core.domain.util.SortBy
import com.android.swingmusic.core.domain.util.SortOrder
import com.android.swingmusic.network.domain.repository.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private var baseUrl: MutableState<String?> = mutableStateOf(null)
    private var accessToken: MutableState<String?> = mutableStateOf(null)

    val artistsUiState: MutableState<ArtistsUiState> = mutableStateOf(ArtistsUiState())

    val sortByEntries: List<Pair<SortBy, String>> = listOf(
        Pair(SortBy.DURATION, "duration"),
        Pair(SortBy.NAME, "name"),
        Pair(SortBy.NO_OF_TRACKS, "trackcount"),
        Pair(SortBy.N0_OF_ALBUMS, "albumcount"),
        Pair(SortBy.DATE_ADDED, "created_date")
    )

    private fun getArtistsCount() {
        viewModelScope.launch {
            val count = networkRepository.getArtistsCount()
            artistsUiState.value = artistsUiState.value.copy(totalArtists = count)
        }
    }

    private fun getPagingArtists(sortBy: String, sortOrder: SortOrder) {
        val order = when (sortOrder) {
            SortOrder.DESCENDING -> 1
            SortOrder.ASCENDING -> 0
        }
        viewModelScope.launch {
            artistsUiState.value = artistsUiState.value.copy(
                pagingArtists = networkRepository.getPagingArtists(
                    sortBy = sortBy,
                    sortOrder = order
                ).cachedIn(viewModelScope)
            )
        }
    }

    init {
        getBaseUrl()
        getAccessToken()
    }

    fun baseUrl() = baseUrl
    fun accessToken() = accessToken

    private fun getBaseUrl() {
        baseUrl.value = authRepository.getBaseUrl()
    }

    private fun getAccessToken() {
        accessToken.value = authRepository.getAccessToken()
    }

    init {
        getPagingArtists(
            sortBy = artistsUiState.value.sortBy.second,
            sortOrder = artistsUiState.value.sortOrder
        )
        getArtistsCount()
    }

    fun onArtistUiEvent(event: ArtistUiEvent) {
        when (event) {
            is ArtistUiEvent.OnSortBy -> {
                // Retry fetching artist count if the previous sorting resulted to Error
                if (artistsUiState.value.totalArtists is Resource.Error) {
                    getArtistsCount()
                }

                if (event.sortByPair == artistsUiState.value.sortBy) {
                    val newOrder = if (artistsUiState.value.sortOrder == SortOrder.ASCENDING)
                        SortOrder.DESCENDING else SortOrder.ASCENDING

                    artistsUiState.value = artistsUiState.value.copy(sortOrder = newOrder)
                    getPagingArtists(
                        sortBy = event.sortByPair.second,
                        sortOrder = newOrder
                    )
                } else {
                    artistsUiState.value = artistsUiState.value.copy(sortBy = event.sortByPair)
                    getPagingArtists(
                        sortBy = event.sortByPair.second,
                        sortOrder = artistsUiState.value.sortOrder
                    )
                }
            }

            is ArtistUiEvent.OnClickArtist -> {
                // TODO: Navigate from the UI (apparently not in the VM)
            }

            is ArtistUiEvent.OnUpdateGridCount -> {
                artistsUiState.value = artistsUiState.value.copy(
                    gridCount = event.newCount
                )
            }

            is ArtistUiEvent.OnRetry -> {
                if (artistsUiState.value.totalArtists is Resource.Error) {
                    getArtistsCount()
                }
            }
        }
    }
}
