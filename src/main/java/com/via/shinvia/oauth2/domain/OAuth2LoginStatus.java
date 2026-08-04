package com.via.shinvia.oauth2.domain;

public enum OAuth2LoginStatus {
    EXISTING_USER, //social_user_id 있음
    LINK_REQUIRED,  //social_user_id는 없고 같은 이메일인 user_id 존재
    NEW_USER  //둘 다 없음
}
