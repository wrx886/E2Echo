package com.github.wrx886.e2echo.client.srv.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.common.model.entity.Alias;
import com.github.wrx886.e2echo.client.srv.mapper.AliasMapper;
import com.github.wrx886.e2echo.client.srv.service.AliasService;

@Service
public class AliasServiceImpl extends ServiceImpl<AliasMapper, Alias> implements AliasService {

}
