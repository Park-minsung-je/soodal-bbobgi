package com.soodalbbobgi.app.core.di

import com.soodalbbobgi.app.data.repository.GachaRepositoryImpl
import com.soodalbbobgi.app.data.repository.InventoryRepositoryImpl
import com.soodalbbobgi.app.data.repository.ProfileCardRepositoryImpl
import com.soodalbbobgi.app.data.repository.SwimLogRepositoryImpl
import com.soodalbbobgi.app.data.repository.UserRepositoryImpl
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.repository.ProfileCardRepository
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindUserRepo(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindSwimLogRepo(impl: SwimLogRepositoryImpl): SwimLogRepository
    @Binds @Singleton abstract fun bindGachaRepo(impl: GachaRepositoryImpl): GachaRepository
    @Binds @Singleton abstract fun bindInventoryRepo(impl: InventoryRepositoryImpl): InventoryRepository
    @Binds @Singleton abstract fun bindProfileCardRepo(impl: ProfileCardRepositoryImpl): ProfileCardRepository
}
