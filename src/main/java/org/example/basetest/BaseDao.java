package org.example.basetest;

import java.util.List;

/**
 * 基础 DAO 接口 - 泛型接口
 * 定义通用的 CRUD 操作
 */
public interface BaseDao<T> {
    /**
     * 插入数据
     */
    void insert(T entity);

    /**
     * 根据 ID 查询
     */
    T selectById(Long id);

    /**
     * 查询所有
     */
    List<T> selectAll();

    /**
     * 更新
     */
    void update(T entity);

    /**
     * 删除
     */
    void delete(Long id);
}

