package com.sakury.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    /**
     * 将源列表中的对象复制到目标类型的新列表中
     * 通过反射创建目标类型的实例，并使用BeanUtils复制属性
     *
     * @param sList  源列表，包含需要复制的对象
     * @param classz 目标类型的Class对象，用于创建新实例
     * @param <T>    目标类型泛型
     * @param <S>    源类型泛型
     * @return 包含复制后对象的新列表，类型为T
     */
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            // 通过反射创建目标类型的实例
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 复制源对象的属性到目标对象
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    /**
     * 将源对象的属性复制到目标类型的实例中
     *
     * @param <T>    目标对象类型
     * @param <S>    源对象类型
     * @param s      源对象，从中复制属性值
     * @param classz 目标对象的Class类型，用于创建目标实例
     * @return 返回复制了源对象属性的目标类型实例，如果创建实例失败则返回null
     */
    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        // 创建目标类型的实例
        try {
            t = classz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 复制源对象到目标对象的属性
        BeanUtils.copyProperties(s, t);
        return t;
    }

    /**
     * 复制对象属性
     * 将源对象的属性值复制到目标对象中
     *
     * @param s   源对象，类型为S
     * @param t   目标对象，类型为T
     * @param <T> 目标对象的泛型类型
     * @param <S> 源对象的泛型类型
     */
    public static <T, S> void copyProperties(S s, T t) {
        BeanUtils.copyProperties(s, t);
    }
}
