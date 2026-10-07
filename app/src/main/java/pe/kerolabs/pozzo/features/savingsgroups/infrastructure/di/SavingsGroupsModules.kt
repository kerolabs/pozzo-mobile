package pe.kerolabs.pozzo.features.savingsgroups.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.core.database.PozzoDatabase
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroupRepository
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.local.GroupDao
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.remote.GroupsService
import pe.kerolabs.pozzo.features.savingsgroups.infrastructure.repositories.SavingsGroupRepositoryImpl
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object SavingsGroupsApiModule {

    @Provides
    @Singleton
    fun provideGroupsService(retrofit: Retrofit): GroupsService = retrofit.create(GroupsService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object SavingsGroupsLocalModule {

    @Provides
    @Singleton
    fun provideGroupDao(database: PozzoDatabase): GroupDao = database.groupDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface SavingsGroupsRepositoryModule {

    @Binds
    fun bindSavingsGroupRepository(impl: SavingsGroupRepositoryImpl): SavingsGroupRepository
}
