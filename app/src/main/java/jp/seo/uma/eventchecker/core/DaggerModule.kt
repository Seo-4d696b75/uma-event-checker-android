package jp.seo.uma.eventchecker.core

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.uma.eventchecker.R
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/08/08.
 */
@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {
    @ExperimentalSerializationApi
    @Singleton
    @Provides
    fun provideDataNetwork(
        @ApplicationContext ctx: Context
    ): DataNetwork = getDataNetwork(ctx.getString(R.string.data_repository_base_url))
}
