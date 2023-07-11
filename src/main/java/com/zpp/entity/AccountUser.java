package com.zpp.entity;

import lombok.Data;

/**
 * 用户基本信息
 */
@Data
public class AccountUser {
    int id;
    String username;
    String email;
}