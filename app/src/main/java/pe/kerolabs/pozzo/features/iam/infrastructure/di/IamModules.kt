package pe.kerolabs.pozzo.features.iam.infrastructure.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.core.network.AccessTokenProvider
import pe.kerolabs.pozzo.features.iam.domain.AuthRepository
import pe.kerolabs.pozzo.features.iam.infrastructure.local.SessionManager
import pe.kerolabs.pozzo.features.iam.infrastructure.remote.AuthService
import pe.kerolabs.pozzo.features.iam.infrastructure.repositories.AuthRepositoryImpl
import retrofit2.Retrofit

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Module
@InstallIn(SingletonComponent::class)
object IamApiModule {

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object IamDataStoreModule {

    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore
}

@Module
@InstallIn(SingletonComponent::class)
interface IamBindingsModule {

    @Binds
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindAccessTokenProvider(impl: SessionManager): AccessTokenProvider
}
