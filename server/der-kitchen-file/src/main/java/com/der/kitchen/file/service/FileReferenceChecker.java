package com.der.kitchen.file.service;

/**
 * 文件引用检查扩展点。上层业务模块实现此接口，文件模块无需反向依赖业务代码。
 */
public interface FileReferenceChecker {

    boolean isReferenced(Long fileId);

    String referenceName();
}
