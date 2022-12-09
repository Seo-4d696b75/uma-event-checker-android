package jp.seo.uma.eventchecker

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.uma.eventchecker.data.repository.DataRepository
import jp.seo.uma.eventchecker.data.repository.ScreenRepository
import jp.seo.uma.eventchecker.data.repository.SearchRepository
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import jp.seo.uma.eventchecker.data.repository.impl.DataRepositoryImpl
import jp.seo.uma.eventchecker.data.repository.impl.ScreenCaptureImpl
import jp.seo.uma.eventchecker.data.repository.impl.SearchRepositoryImpl
import jp.seo.uma.eventchecker.img.ImageProcess
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindDataRepository(impl: DataRepositoryImpl): DataRepository

    @Binds
    fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    fun bindScreenRepository(impl: ScreenCaptureImpl): ScreenRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SettingModule {
    @Provides
    @Singleton
    fun provideSettingRepository(
        @ApplicationContext context: Context
    ) = SettingRepository(context)
}

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun provideImageProcess() = ImageProcess()
}