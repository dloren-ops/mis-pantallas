package com.dloren.mispantallas.data.repository

import com.dloren.mispantallas.data.remote.GithubReleaseDataSource
import com.dloren.mispantallas.domain.model.AppRelease
import com.dloren.mispantallas.domain.repository.UpdateRepository

class UpdateRepositoryImpl(
    private val dataSource: GithubReleaseDataSource
) : UpdateRepository {
    override suspend fun getLatestRelease(): AppRelease? = dataSource.fetchLatestRelease()
}
