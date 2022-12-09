package jp.seo.uma.eventchecker

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.seo.uma.eventchecker.capture.ScreenCapture
import jp.seo.uma.eventchecker.capture.ScreenSetting
import jp.seo.uma.eventchecker.data.repository.DataRepository
import jp.seo.uma.eventchecker.data.repository.ScreenRepository
import jp.seo.uma.eventchecker.data.repository.SearchRepository
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import jp.seo.uma.eventchecker.data.repository.impl.DataRepositoryImpl
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
    fun bindScreenRepository(impl: ScreenCapture): ScreenRepository

    @Binds
    fun bindSettingRepository(impl: ScreenSetting): SettingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun provideImageProcess() = ImageProcess()
}