package com.easylive.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
//忽略未知属性
@JsonIgnoreProperties(ignoreUnknown = true)
//实现序列化
public class TokenUserInfoDto implements Serializable {

    private static final long serialVersionUID = 9170480547933408839L;
    private String userId;//用户id
    private String nickName;//昵称
    private String avatar;//头像
    private Long expireAt;//过期时间
    private String token;

    private Integer fansCount;
    private Integer currentCoinCount;
    private Integer focusCount;
}
