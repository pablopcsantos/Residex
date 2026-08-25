package com.pablopcsantos.residex.di

import com.pablopcsantos.residex.residency.data.SelectionRepositoryImpl
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    
    
    @Provides
    @Singleton
    fun provideSelectionRepository(
        repository: SelectionRepositoryImpl
    ): SelectionRepository = repository
}
