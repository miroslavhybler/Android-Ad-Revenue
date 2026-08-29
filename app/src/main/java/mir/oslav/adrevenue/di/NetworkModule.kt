package mir.oslav.adrevenue.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mir.oslav.adrevenue.core.network.ConnectivityNetworkMonitor
import mir.oslav.adrevenue.core.network.NetworkMonitor

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor,
    ): NetworkMonitor
}
