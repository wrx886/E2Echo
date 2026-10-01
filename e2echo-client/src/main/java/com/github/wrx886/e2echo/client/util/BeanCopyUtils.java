package com.github.wrx886.e2echo.client.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BeanCopyUtils {

    /**
     * 复制非 null 属性（null 视为不存在，不覆盖目标）
     */
    public static void copyNonNullProperties(Object source, Object target) {
        copyNonNullProperties(source, target, new String[0]);
    }

    /**
     * 复制非 null 属性，并额外忽略指定属性
     */
    public static void copyNonNullProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreProperties));
        ignoreSet.addAll(Arrays.asList(getNullPropertyNames(source)));
        BeanUtils.copyProperties(source, target, ignoreSet.toArray(new String[0]));
    }

    /**
     * 获取源对象中值为 null 的属性名
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> nullNames = new HashSet<>();

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();

            // 跳过 class 属性，以及没有 getter 的属性
            if ("class".equals(propertyName) || pd.getReadMethod() == null) {
                continue;
            }

            try {
                Object srcValue = src.getPropertyValue(propertyName);
                if (srcValue == null) {
                    nullNames.add(propertyName);
                }
            } catch (Exception e) {
                // 某些 getter 可能抛异常，忽略即可
            }
        }
        return nullNames.toArray(new String[0]);
    }
}