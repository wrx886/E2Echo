package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.entity.SysParam;
import com.github.wrx886.e2echo.client.enums.SysParamEnum;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.SysParamRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

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
     * 系统参数数据缓存。
     */
    private final ConcurrentHashMap<String, Optional<String>> sysParamMap = new ConcurrentHashMap<>();

    /**
     * 读取当前登入用户的参数值。
     *
     * @param key 参数名
     * @return 参数值，未设置时为空
     * @throws E2EchoException 尚未登入
     */
    public String find(String key) {
        // 缓存
        sysParamMap.computeIfAbsent(key, k -> sysParamRepository
                .findByOwnerAndKey(currentOwner(), key)
                .map(SysParam::getValue));
        // 获取，这里极端情况下，前面写入就被其他线程删除了，所以要额外再判断一次
        Optional<String> val = sysParamMap.get(key);
        return val != null ? val.orElse(null) : null;
    }

    /**
     * 读取当前登入用户的参数值（按枚举取参数名）。
     *
     * @param sysParamEnum 参数名枚举
     * @return 参数值，未设置时为 {@code null}
     */
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
        sysParamMap.remove(key);
    }

    /**
     * 按枚举写入参数，等价于用枚举项名称作为参数名。
     *
     * @param sysParamEnum 参数名枚举
     * @param value        参数值
     */
    @Transactional
    public void put(SysParamEnum sysParamEnum, String value) {
        put(sysParamEnum.name(), value);
    }


    /**
     * 新增参数，不判重：参数名已存在时会因唯一约束失败，存在则更新请用
     * {@link #put(String, String)}。
     *
     * @param key   参数名
     * @param value 参数值
     */
    @Transactional
    public void putIfAbsent(String key, String value) {
        SysParam created = new SysParam();
        created.setKey(key);
        created.setValue(value);
        sysParamRepository.save(created);
        sysParamMap.remove(key);
    }

    /**
     * 按枚举新增参数，等价于用枚举项名称作为参数名。
     *
     * @param sysParamEnum 参数名枚举
     * @param value        参数值
     */
    @Transactional
    public void putIfAbsent(SysParamEnum sysParamEnum, String value) {
        putIfAbsent(sysParamEnum.name(), value);
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

    /**
     * 按枚举删除参数。
     *
     * @param sysParamEnum 参数名枚举
     * @return 删除成功返回 {@code true}，参数不存在返回 {@code false}
     */
    @Transactional
    public boolean remove(SysParamEnum sysParamEnum) {
        return remove(sysParamEnum.name());
    }

}
