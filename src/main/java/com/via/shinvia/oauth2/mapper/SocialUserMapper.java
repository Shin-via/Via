package com.via.shinvia.oauth2.mapper;

import com.via.shinvia.oauth2.domain.SocialProvider;
import com.via.shinvia.oauth2.domain.SocialUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SocialUserMapper {
    SocialUser findByProviderAndProviderUserId(@Param("provider") SocialProvider provider,
                                               @Param("providerUserId") String providerUserId);
    int insertSocialUser(SocialUser socialUser);
}
