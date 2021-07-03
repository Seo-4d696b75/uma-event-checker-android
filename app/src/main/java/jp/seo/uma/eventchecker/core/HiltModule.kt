package jp.seo.uma.eventchecker.core

import androidx.lifecycle.ViewModelStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }
}
