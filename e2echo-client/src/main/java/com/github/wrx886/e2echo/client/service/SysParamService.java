package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.entity.SysParam;
import com.github.wrx886.e2echo.client.enums.SysParamEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.SysParamRepository;
import com.github.wrx886.e2echo.ecc.Ecc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统参数业务逻辑层。
 *
 * <p>参数是键值形式、按登入用户隔离的本机配置，读写都只针对当前用户：读取时按
 * {@code owner + 参数名} 查询，写入时新增或更新同一用户下的同名参数。</p>
 *
 * <p>注意这边存的是“每个用户一份”的参数。全局唯一的东西（例如给消息序号
 * {@code Message.seq} 分配序号的计数器）不适合放在这里，否则每个用户各有一个计数器、分配出的
 * 序号会互相重复。</p>
 */
@Service
@RequiredArgsConstructor
public class SysParamService {

    /**
     * 系统参数数据访问对象。
     */
    private final SysParamRepository sysParamRepository;

    /**
     * 读取当前登入用户的参数值。
     *
     * @param key 参数名
     * @return 参数值，未设置时为空
     * @throws E2EchoException 尚未登入
     */
    public String find(String key) {
        return sysParamRepository
                .findByOwnerAndKey(currentOwner(), key)
                .map(SysParam::getValue)
                .orElse(null);
    }

    public String find(SysParamEnum sysParamEnum) {
        return find(sysParamEnum.name());
    }

    /**
     * 写入参数：当前用户没有这个参数就新增，已经有就更新。
     *
     * @param key   参数名
     * @param value 参数值
     * @throws E2EchoException 尚未登入
     */
    @Transactional
    public void put(String key, String value) {
        SysParam param = sysParamRepository.findByOwnerAndKey(currentOwner(), key).orElseGet(() -> {
            SysParam created = new SysParam();
            created.setKey(key);
            return created;
        });
        param.setValue(value);
        sysParamRepository.save(param);
    }

    @Transactional
    public void put(SysParamEnum sysParamEnum, String value) {
        put(sysParamEnum.name(), value);
    }

    /**
     * 删除参数。
     *
     * @param key 参数名
     * @return 删除成功返回 {@code true}，参数不存在返回 {@code false}
     * @throws E2EchoException 尚未登入
     */
    @Transactional
    public boolean remove(String key) {
        return sysParamRepository.deleteByOwnerAndKey(currentOwner(), key) > 0;
    }

    @Transactional
    public boolean remove(SysParamEnum sysParamEnum) {
        return remove(sysParamEnum.name());
    }

    /**
     * 获取当前登入用户的公钥。
     *
     * @return 当前登入用户的公钥
     * @throws E2EchoException 尚未登入（未保存密钥对）
     */
    private String currentOwner() {

        String owner = Ecc.getPublicKey();
        if (owner == null) {
            throw new E2EchoException("尚未登入！");
        }
        return owner;
    }

}
