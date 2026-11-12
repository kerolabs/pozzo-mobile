package pe.kerolabs.pozzo.features.contributions.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.features.contributions.domain.AuctionRepository
import pe.kerolabs.pozzo.features.contributions.infrastructure.remote.AuctionsService
import pe.kerolabs.pozzo.features.contributions.infrastructure.repositories.AuctionRepositoryImpl
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AuctionsApiModule {

    @Provides
    @Singleton
    fun provideAuctionsService(retrofit: Retrofit): AuctionsService = retrofit.create(AuctionsService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface AuctionsBindingsModule {

    @Binds
    fun bindAuctionRepository(impl: AuctionRepositoryImpl): AuctionRepository
}
