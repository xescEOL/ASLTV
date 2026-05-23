package com.xesc.asltv.ui

import androidx.lifecycle.ViewModel
import com.xesc.asltv.data.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val epgRepository: EpgRepository
) : ViewModel() {
    val isEpgSyncing = epgRepository.isSyncing
    val epgSyncProgress = epgRepository.syncProgress
}