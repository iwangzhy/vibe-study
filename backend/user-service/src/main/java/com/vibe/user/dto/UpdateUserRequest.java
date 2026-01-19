package com.vibe.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新用户信息请求DTO
 */
@Data
public class UpdateUserRequest {

    /**
     * 昵称
     */
    @Size(min = 1, max = 20, message = "昵称长度为1-20个字符")
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别（0-未知，1-男，2-女）
     */
    private Integer gender;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 个人简介
     */
    @Size(max = 200, message = "个人简介不能超过200个字符")
    private String bio;
}
