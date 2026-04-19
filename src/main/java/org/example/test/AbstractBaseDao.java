package org.example.test;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础 DAO 类 - 泛型类
 * 提供通用的 CRUD 操作实现
 */
public abstract class AbstractBaseDao<T> implements BaseDao<T> {

    @Override
    public void insert(T entity) {
        System.out.println("插入数据: " + entity);
    }

    @Override
    public T selectById(Long id) {
        System.out.println("查询 ID: " + id);
        return null;
    }

    @Override
    public List<T> selectAll() {
        System.out.println("查询所有数据");
        return new ArrayList<>();
    }

    @Override
    public void update(T entity) {
        System.out.println("更新数据: " + entity);
    }

    @Override
    public void delete(Long id) {
        System.out.println("删除 ID: " + id);
    }
}

