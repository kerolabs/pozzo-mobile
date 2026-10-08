package pe.kerolabs.pozzo.features.notifications.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kerolabs.pozzo.features.notifications.domain.NotificationRepository
import pe.kerolabs.pozzo.features.notifications.infrastructure.remote.NotificationsService
import pe.kerolabs.pozzo.features.notifications.infrastructure.repositories.NotificationRepositoryImpl
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NotificationsApiModule {

    @Provides
    @Singleton
    fun provideNotificationsService(retrofit: Retrofit): NotificationsService =
        retrofit.create(NotificationsService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface NotificationsBindingsModule {

    @Binds
    fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
