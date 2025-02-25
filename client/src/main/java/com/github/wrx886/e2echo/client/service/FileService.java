package com.github.wrx886.e2echo.client.service;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.mapper.FileMapper;
import com.github.wrx886.e2echo.client.model.entity.File;

@Service
public class FileService extends ServiceImpl<FileMapper, File> {

}
