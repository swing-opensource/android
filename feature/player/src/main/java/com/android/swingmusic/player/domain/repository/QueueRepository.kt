package com.android.swingmusic.player.domain.repository

import com.android.swingmusic.core.domain.model.Track
import com.android.swingmusic.database.domain.model.LastPlayedTrack

interface QueueRepository {
    suspend fun insertTracks(track: List<Track>)

    suspend fun getAllTracks(): List<Track>

    suspend fun clearQueue()

    suspend fun updateLastPlayedTrack(trackHash: String, indexInQueue: Int)

    suspend fun getLastPlayedTrack(): LastPlayedTrack?
}
