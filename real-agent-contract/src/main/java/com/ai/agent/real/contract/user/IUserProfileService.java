package com.ai.agent.real.contract.user;

import com.ai.agent.real.domain.vo.user.UserProfileVO;

/**
 * @author han
 * @time 2025/11/11 22:58
 */
public interface IUserProfileService {

	UserProfileVO getUserProfileById(String userId);

}
