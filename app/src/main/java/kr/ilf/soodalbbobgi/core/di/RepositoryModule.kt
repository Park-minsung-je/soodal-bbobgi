package kr.ilf.soodalbbobgi.core.di

import kr.ilf.soodalbbobgi.data.repository.SwimLogRepositoryImpl
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 바인딩. 서버 기반 아키텍처에서는 swim_logs 한 종류만 Room 영속화한다.
 * 나머지 상태는 [kr.ilf.soodalbbobgi.core.state.AppState]에서 메모리로 관리.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindSwimLogRepo(impl: SwimLogRepositoryImpl): SwimLogRepository
}
