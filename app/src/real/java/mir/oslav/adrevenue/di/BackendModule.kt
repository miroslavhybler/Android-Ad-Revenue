package mir.oslav.adrevenue.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mir.oslav.adrevenue.data.AdMobBackend
import mir.oslav.adrevenue.data.RealAdMobBackend

@Module
@InstallIn(SingletonComponent::class)
/** Binds the production backend for real variants. */
abstract class BackendModule {
    @Binds @Singleton abstract fun bindBackend(backend: RealAdMobBackend): AdMobBackend
}
