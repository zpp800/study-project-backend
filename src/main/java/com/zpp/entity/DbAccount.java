package com.zpp.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * (DbAccount)表实体类
 *
 * @author makejava
 * @since 2023-07-10 14:34:40
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class DbAccount  {
    private Integer id;

    private String email;
    
    private String username;
    
    private String password;



}
