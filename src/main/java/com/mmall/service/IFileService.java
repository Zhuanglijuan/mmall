package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Zhuang on 2018/5/16.
 */
public interface IFileService {
    public String upload(MultipartFile file, String path);
}
