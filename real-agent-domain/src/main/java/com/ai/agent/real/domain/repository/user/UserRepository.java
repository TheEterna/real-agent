package com.ai.agent.real.domain.repository.user;

import com.ai.agent.real.common.entity.user.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 用户数据访问层 (R2DBC)
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

	/**
	 * 根据外部ID查找用户
	 */
	Mono<User> findByExternalId(String externalId);

	/**
	 * 检查外部ID是否存在
	 */
	@Query("SELECT COUNT(*) > 0 FROM app_user.users WHERE external_id = :externalId")
	Mono<Boolean> existsByExternalId(String externalId);

}
