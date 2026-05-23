package org.example.java_base_test;

/**
 * MySQL NULL 值问题分析
 *
 * 说明：这个程序通过代码演示和文字说明来解释为什么不建议使用 NULL 作为列默认值
 */
public class DatabaseNullAnalysis {
    public static void main(String[] args) {
        System.out.println("========== MySQL NULL 值问题深度分析 ==========\n");

        // 1. NULL 的三值逻辑问题
        System.out.println("【1. NULL 的三值逻辑问题】");
        demoThreeValuedLogic();

        // 2. NULL 的存储和性能影响
        System.out.println("\n【2. NULL 对性能的影响】");
        demoPerformanceIssues();

        // 3. NULL 导致的数据一致性问题
        System.out.println("\n【3. 数据一致性问题】");
        demoDataConsistency();

        // 4. NULL 与索引的关系
        System.out.println("\n【4. NULL 与索引的问题】");
        demoIndexIssues();

        // 5. NULL 的应用场景
        System.out.println("\n【5. NULL 的正确使用场景】");
        demoCorrectNullUsage();

        // 6. 最佳实践
        System.out.println("\n【6. 最佳实践建议】");
        demoBestPractices();
    }

    // ============ 1. 三值逻辑问题 ============
    private static void demoThreeValuedLogic() {
        System.out.println("SQL 中的 NULL 使用三值逻辑（TRUE、FALSE、UNKNOWN）\n");

        System.out.println("--- 问题示例 1：WHERE 条件判断 ---");
        System.out.println("假设表 users 中有一条记录:");
        System.out.println("  id=1, name='张三', age=NULL\n");

        System.out.println("Java 代码中的比较:");
        Integer age = null;
        System.out.println("age = " + age);
        System.out.println("age == null: " + (age == null) + " // true");
        System.out.println("age != 18: " + (age != null ? (age != 18) : "null") + " // null");
        System.out.println("age > 18: " + (age != null && age > 18) + " // false");

        System.out.println("\nSQL 中的对应情况:");
        System.out.println("SELECT * FROM users WHERE age = 18;    // ❌ 不会返回 age=NULL 的行");
        System.out.println("SELECT * FROM users WHERE age != 18;   // ❌ 不会返回 age=NULL 的行");
        System.out.println("SELECT * FROM users WHERE age IS NULL; // ✅ 这样才能查到");

        System.out.println("\n--- 问题示例 2：聚合函数 ---");
        System.out.println("SELECT COUNT(*) FROM users;  // 所有行数（包括 age=NULL）");
        System.out.println("SELECT COUNT(age) FROM users; // 非 NULL 的行数（不包括 age=NULL）");
        System.out.println("SELECT AVG(age) FROM users;   // 平均值（自动忽略 NULL）");
        System.out.println("SELECT SUM(age) FROM users;   // 总和（自动忽略 NULL）");

        System.out.println("\n--- 问题示例 3：UNION 操作 ---");
        System.out.println("SELECT id FROM table1 WHERE status = NULL");
        System.out.println("UNION");
        System.out.println("SELECT id FROM table2 WHERE status = NULL;");
        System.out.println("// 结果：空集合！因为 NULL = NULL 的结果是 UNKNOWN");
    }

    // ============ 2. 性能影响 ============
    private static void demoPerformanceIssues() {
        System.out.println("--- 存储开销 ---");
        System.out.println("即使列允许 NULL，每一行都需要额外的空间来标记该列是否为 NULL");
        System.out.println("例如：");
        System.out.println("  非 NULL 列: VARCHAR(50) → 最多 50 字节");
        System.out.println("  允许 NULL 列: VARCHAR(50) → 50 字节 + 1 字节标记 = 51 字节");
        System.out.println("  对于百万级别的数据，这会增加显著的存储成本\n");

        System.out.println("--- 索引效率降低 ---");
        System.out.println("包含 NULL 的列建立的索引效率较低:");
        System.out.println("  1. 大多数数据库不会对 NULL 值建立索引");
        System.out.println("  2. 索引大小减小，但查询效率仍然不理想");
        System.out.println("  3. 需要全表扫描来查找 NULL 值\n");

        System.out.println("例子：");
        System.out.println("CREATE INDEX idx_email ON users(email);");
        System.out.println("SELECT * FROM users WHERE email IS NULL;");
        System.out.println("// ❌ 不会使用索引，进行全表扫描\n");

        System.out.println("--- CPU 处理成本 ---");
        System.out.println("数据库引擎在处理 NULL 值时需要额外的 CPU 指令");
        System.out.println("  1. 每次比较都需要检查是否为 NULL");
        System.out.println("  2. 每次聚合都需要特殊处理 NULL");
        System.out.println("  3. 大数据量查询时，成本呈指数级增长");
    }

    // ============ 3. 数据一致性问题 ============
    private static void demoDataConsistency() {
        System.out.println("--- 问题 1：无法区分\"没有值\"和\"不适用\" ---");
        System.out.println("表 users:");
        System.out.println("┌────┬──────┬──────────┬────────┐");
        System.out.println("│ id │ name │ phone    │ remark │");
        System.out.println("├────┼──────┼──────────┼────────┤");
        System.out.println("│ 1  │ 张三 │ NULL     │ ?      │");
        System.out.println("│ 2  │ 李四 │ 13800000 │ ?      │");
        System.out.println("└────┴──────┴──────────┴────────┘\n");

        System.out.println("张三的 phone 为 NULL，代表什么？");
        System.out.println("  ❓ 他没有手机？");
        System.out.println("  ❓ 他有手机但不提供号码？");
        System.out.println("  ❓ 系统录入失败？");
        System.out.println("  ❓ 该字段尚未填写？");
        System.out.println("无法确定！\n");

        System.out.println("--- 问题 2：应用层处理复杂化 ---");
        System.out.println("Java 代码示例:");
        System.out.println("```java");
        System.out.println("User user = getUserFromDB(1);");
        System.out.println("if (user.getPhone() == null) {");
        System.out.println("    // 这里需要处理 NULL 情况");
        System.out.println("    // 但无法确定具体含义");
        System.out.println("}");
        System.out.println("```\n");

        System.out.println("--- 问题 3：JOIN 查询的歧义 ---");
        System.out.println("表 users 和 orders:");
        System.out.println("users:  id=1, name='张三', status=NULL");
        System.out.println("orders: user_id=1, amount=100\n");

        System.out.println("SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id");
        System.out.println("WHERE u.status = '活跃';");
        System.out.println("// ❌ status=NULL 的用户无法通过 WHERE 过滤查到\n");
    }

    // ============ 4. NULL 与索引 ============
    private static void demoIndexIssues() {
        System.out.println("--- 问题 1：NULL 不被索引 ---");
        System.out.println("表定义:");
        System.out.println("CREATE TABLE products (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100),");
        System.out.println("  category VARCHAR(50),");
        System.out.println("  INDEX idx_category(category)");
        System.out.println(");\n");

        System.out.println("数据:");
        System.out.println("┌────┬────────┬──────────┐");
        System.out.println("│ id │ name   │ category │");
        System.out.println("├────┼────────┼──────────┤");
        System.out.println("│ 1  │ 产品A  │ 电子     │");
        System.out.println("│ 2  │ 产品B  │ NULL     │");
        System.out.println("│ 3  │ 产品C  │ 书籍     │");
        System.out.println("└────┴────────┴──────────┘\n");

        System.out.println("查询:");
        System.out.println("SELECT * FROM products WHERE category IS NULL;");
        System.out.println("// ❌ 无法使用索引 idx_category");
        System.out.println("// ❌ 必须进行全表扫描\n");

        System.out.println("--- 问题 2：复合索引的问题 ---");
        System.out.println("CREATE INDEX idx_user ON orders(user_id, status, created_at);\n");

        System.out.println("如果 status 允许 NULL：");
        System.out.println("SELECT * FROM orders WHERE user_id = 1 AND status = 'pending';");
        System.out.println("// ✅ 可以使用索引\n");

        System.out.println("SELECT * FROM orders WHERE user_id = 1 AND status IS NULL;");
        System.out.println("// ❌ 无法完全使用索引，效率下降\n");

        System.out.println("--- 问题 3：DISTINCT 和 GROUP BY ---");
        System.out.println("SELECT DISTINCT category FROM products;");
        System.out.println("// 多个 NULL 被视为一个不同的值");
        System.out.println("// 结果可能包含 (电子, NULL, 书籍)");
    }

    // ============ 5. NULL 的正确使用场景 ============
    private static void demoCorrectNullUsage() {
        System.out.println("--- 场景 1：真正可选的字段 ---");
        System.out.println("✅ 正确例子：用户个人中心");
        System.out.println("CREATE TABLE user_profile (");
        System.out.println("  user_id INT PRIMARY KEY,");
        System.out.println("  nickname VARCHAR(50) NOT NULL,");
        System.out.println("  avatar_url VARCHAR(200),        // ✅ 用户可能没有上传头像");
        System.out.println("  bio VARCHAR(500),               // ✅ 用户可能没有签名");
        System.out.println("  phone VARCHAR(20),              // ✅ 用户可能不提供手机");
        System.out.println("  company VARCHAR(100)            // ✅ 用户可能不填公司");
        System.out.println(");\n");

        System.out.println("--- 场景 2：外键关联 ---");
        System.out.println("✅ 正确例子：订单管理");
        System.out.println("CREATE TABLE orders (");
        System.out.println("  order_id INT PRIMARY KEY,");
        System.out.println("  user_id INT NOT NULL,           // ✅ 订单必须有用户");
        System.out.println("  parent_order_id INT,            // ✅ 子订单的父订单可能为空");
        System.out.println("  FOREIGN KEY (parent_order_id) REFERENCES orders(order_id)");
        System.out.println(");\n");

        System.out.println("--- 场景 3：时间戳字段 ---");
        System.out.println("✅ 正确例子：订单状态跟踪");
        System.out.println("CREATE TABLE order_tracking (");
        System.out.println("  order_id INT,");
        System.out.println("  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,");
        System.out.println("  shipped_at TIMESTAMP,           // ✅ 尚未发货的订单为 NULL");
        System.out.println("  received_at TIMESTAMP,          // ✅ 尚未收货的订单为 NULL");
        System.out.println("  cancelled_at TIMESTAMP          // ✅ 未取消的订单为 NULL");
        System.out.println(");\n");
    }

    // ============ 6. 最佳实践 ============
    private static void demoBestPractices() {
        System.out.println("--- 实践 1：使用 NOT NULL + 默认值 ---");
        System.out.println("❌ 反面例子：");
        System.out.println("CREATE TABLE users (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NULL DEFAULT NULL,    // 不推荐");
        System.out.println("  age INT NULL DEFAULT NULL,              // 不推荐");
        System.out.println("  status VARCHAR(20)                      // 没有默认值");
        System.out.println(");\n");

        System.out.println("✅ 正面例子：");
        System.out.println("CREATE TABLE users (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,             // 必填");
        System.out.println("  age INT NOT NULL DEFAULT 0,             // 有默认值");
        System.out.println("  status VARCHAR(20) NOT NULL DEFAULT 'inactive',  // 有默认值");
        System.out.println("  phone VARCHAR(20),                       // 允许 NULL（真正可选）");
        System.out.println("  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        System.out.println(");\n");

        System.out.println("--- 实践 2：清晰的业务语义 ---");
        System.out.println("❌ 模糊的设计：");
        System.out.println("CREATE TABLE products (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100),");
        System.out.println("  discount INT           // NULL 表示什么？无折扣？");
        System.out.println(");\n");

        System.out.println("✅ 清晰的设计：");
        System.out.println("CREATE TABLE products (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,");
        System.out.println("  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 0,  // 0 表示无折扣");
        System.out.println("  is_on_sale BOOLEAN NOT NULL DEFAULT FALSE        // 显式标记是否促销");
        System.out.println(");\n");

        System.out.println("--- 实践 3：创建表时的建议 ---");
        System.out.println("CREATE TABLE best_practice (");
        System.out.println("  -- 主键：始终 NOT NULL");
        System.out.println("  id INT PRIMARY KEY AUTO_INCREMENT,");
        System.out.println("");
        System.out.println("  -- 业务必填字段：NOT NULL");
        System.out.println("  user_name VARCHAR(100) NOT NULL,");
        System.out.println("  email VARCHAR(100) NOT NULL UNIQUE,");
        System.out.println("  status VARCHAR(20) NOT NULL DEFAULT 'pending',");
        System.out.println("");
        System.out.println("  -- 时间字段：NOT NULL，有默认值");
        System.out.println("  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,");
        System.out.println("  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        System.out.println("                                  ON UPDATE CURRENT_TIMESTAMP,");
        System.out.println("");
        System.out.println("  -- 真正可选字段：允许 NULL（但需要业务明确）");
        System.out.println("  phone VARCHAR(20),");
        System.out.println("  avatar_url VARCHAR(200),");
        System.out.println("  bio TEXT");
        System.out.println(");\n");

        System.out.println("--- 实践 4：查询时的最佳做法 ---");
        System.out.println("Java ORM 代码示例（使用 MyBatis 或 JPA）：\n");
        System.out.println("// ❌ 不推荐");
        System.out.println("SELECT * FROM users WHERE status = NULL;  // 永远查不到\n");

        System.out.println("// ✅ 推荐");
        System.out.println("SELECT * FROM users WHERE status IS NULL;  // 如果必须查询 NULL");
        System.out.println("SELECT * FROM users WHERE status = 'active';  // 通常只查特定值");
        System.out.println("SELECT * FROM users WHERE status IS NOT NULL;  // 排除 NULL\n");
    }
}

