package pe.kerolabs.pozzo.features.contributions.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.features.contributions.domain.ContributionRepository
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptReader
import pe.kerolabs.pozzo.features.contributions.infrastructure.ocr.MlKitReceiptReader
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.ContributionsService
import pe.kerolabs.pozzo.features.contributions.infrastructure.repositories.ContributionRepositoryImpl
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ContributionsApiModule {

    @Provides
    @Singleton
    fun provideContributionsService(retrofit: Retrofit): ContributionsService =
        retrofit.create(ContributionsService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface ContributionsBindingsModule {

    @Binds
    fun bindContributionRepository(impl: ContributionRepositoryImpl): ContributionRepository

    @Binds
    fun bindReceiptReader(impl: MlKitReceiptReader): ReceiptReader
}
