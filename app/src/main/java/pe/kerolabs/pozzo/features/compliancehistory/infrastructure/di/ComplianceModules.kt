package pe.kerolabs.pozzo.features.compliancehistory.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceRepository
import pe.kerolabs.pozzo.features.compliancehistory.infrastructure.remote.ComplianceService
import pe.kerolabs.pozzo.features.compliancehistory.infrastructure.repositories.ComplianceRepositoryImpl
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ComplianceApiModule {

    @Provides
    @Singleton
    fun provideComplianceService(retrofit: Retrofit): ComplianceService = retrofit.create(ComplianceService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface ComplianceBindingsModule {

    @Binds
    fun bindComplianceRepository(impl: ComplianceRepositoryImpl): ComplianceRepository
}
