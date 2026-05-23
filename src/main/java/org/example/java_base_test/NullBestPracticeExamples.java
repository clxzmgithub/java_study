package org.example.java_base_test;

/**
 * NOT NULL + 默认值 vs NULL 的详细对比
 * 通过具体的业务场景来理解最佳实践
 */
public class NullBestPracticeExamples {
    public static void main(String[] args) {
        System.out.println("========== NOT NULL + 默认值 vs NULL 详解 ==========\n");

        // 场景 1：用户状态
        System.out.println("【场景 1：用户账号状态】");
        demoUserStatus();

        // 场景 2：订单金额
        System.out.println("\n【场景 2：订单金额】");
        demoOrderAmount();

        // 场景 3：员工部门
        System.out.println("\n【场景 3：员工部门】");
        demoDepartment();

        // 场景 4：评分
        System.out.println("\n【场景 4：产品评分】");
        demoRating();

        // 场景 5：真正可选字段
        System.out.println("\n【场景 5：真正可选字段（可以用 NULL）】");
        demoOptionalFields();
    }

    // ============ 场景 1：用户账号状态 ============
    private static void demoUserStatus() {
        System.out.println("需求：记录用户账户的状态\n");

        System.out.println("❌ 不推荐方案：允许 NULL");
        System.out.println("CREATE TABLE users (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  username VARCHAR(100) NOT NULL,");
        System.out.println("  status VARCHAR(20)  -- 允许 NULL");
        System.out.println(");\n");

        System.out.println("问题：");
        System.out.println("  status = NULL 是什么意思？");
        System.out.println("    → 激活状态？");
        System.out.println("    → 未激活？");
        System.out.println("    → 系统错误？");
        System.out.println("    → 无法理解！\n");

        System.out.println("✅ 推荐方案：NOT NULL + 默认值");
        System.out.println("CREATE TABLE users (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  username VARCHAR(100) NOT NULL,");
        System.out.println("  status VARCHAR(20) NOT NULL DEFAULT 'inactive'");
        System.out.println(");\n");

        System.out.println("优势：");
        System.out.println("  ✅ status 永远不会为 NULL");
        System.out.println("  ✅ 新用户默认状态是 'inactive'，语义明确");
        System.out.println("  ✅ 查询逻辑简单清晰");
        System.out.println("  ✅ 代码易于维护和理解\n");
    }

    // ============ 场景 2：订单金额 ============
    private static void demoOrderAmount() {
        System.out.println("需求：记录订单的总金额\n");

        System.out.println("❌ 不推荐方案：允许 NULL");
        System.out.println("CREATE TABLE orders (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  total_amount DECIMAL(10, 2)  -- 允许 NULL");
        System.out.println(");\n");

        System.out.println("问题：");
        System.out.println("  total_amount = NULL 代表什么？");
        System.out.println("    → 金额为 0？");
        System.out.println("    → 金额未计算？");
        System.out.println("    → 订单无效？\n");

        System.out.println("✅ 推荐方案：NOT NULL + 默认值");
        System.out.println("CREATE TABLE orders (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0");
        System.out.println(");\n");

        System.out.println("优势：");
        System.out.println("  ✅ 金额永远不会为 NULL");
        System.out.println("  ✅ 新订单金额默认为 0，语义明确");
        System.out.println("  ✅ 数据计算准确（SUM、AVG 等）");
        System.out.println("  ✅ 无需特殊处理 NULL 情况\n");
    }

    // ============ 场景 3：员工部门 ============
    private static void demoDepartment() {
        System.out.println("需求：记录员工所属的部门\n");

        System.out.println("❌ 不推荐方案：允许 NULL");
        System.out.println("CREATE TABLE employees (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,");
        System.out.println("  department VARCHAR(50)  -- 允许 NULL");
        System.out.println(");\n");

        System.out.println("问题：");
        System.out.println("  department = NULL 代表什么？");
        System.out.println("    → 员工未分配部门？");
        System.out.println("    → 员工是自由职业者？");
        System.out.println("    → 数据还未填写？\n");

        System.out.println("✅ 推荐方案：NOT NULL + 默认值");
        System.out.println("CREATE TABLE employees (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,");
        System.out.println("  department VARCHAR(50) NOT NULL DEFAULT 'unassigned'");
        System.out.println(");\n");

        System.out.println("优势：");
        System.out.println("  ✅ department 永远有值");
        System.out.println("  ✅ 'unassigned' 明确表示未分配");
        System.out.println("  ✅ 可以按部门统计，不会漏掉未分配的");
        System.out.println("  ✅ 代码更直观\n");
    }

    // ============ 场景 4：评分 ============
    private static void demoRating() {
        System.out.println("需求：记录产品的平均评分\n");

        System.out.println("❌ 不推荐方案：允许 NULL");
        System.out.println("CREATE TABLE products (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,");
        System.out.println("  avg_rating DECIMAL(3, 2)  -- 允许 NULL");
        System.out.println(");\n");

        System.out.println("问题：");
        System.out.println("  avg_rating = NULL 代表什么？");
        System.out.println("    → 没有评分？");
        System.out.println("    → 评分还未计算？\n");

        System.out.println("✅ 推荐方案：NOT NULL + 默认值");
        System.out.println("CREATE TABLE products (");
        System.out.println("  id INT PRIMARY KEY,");
        System.out.println("  name VARCHAR(100) NOT NULL,");
        System.out.println("  avg_rating DECIMAL(3, 2) NOT NULL DEFAULT 0,");
        System.out.println("  rating_count INT NOT NULL DEFAULT 0");
        System.out.println(");\n");

        System.out.println("优势：");
        System.out.println("  ✅ avg_rating = 0 明确表示没有评分");
        System.out.println("  ✅ rating_count = 0 表示未被评分过");
        System.out.println("  ✅ 可区分：新产品 vs 差产品");
        System.out.println("  ✅ 逻辑清晰\n");
    }

    // ============ 场景 5：真正可选字段 ============
    private static void demoOptionalFields() {
        System.out.println("什么是真正可选的字段（可以用 NULL）？\n");

        System.out.println("1. 用户头像 URL");
        System.out.println("   理由：用户可能没有上传头像");
        System.out.println("   无法用默认值表示\n");

        System.out.println("2. 用户签名");
        System.out.println("   理由：用户可能不填写个人签名");
        System.out.println("   NULL 明确表示无签名\n");

        System.out.println("3. 订单的备注");
        System.out.println("   理由：有些订单没有备注");
        System.out.println("   NULL 表示无备注\n");

        System.out.println("4. 订单的发货时间");
        System.out.println("   理由：订单还未发货时应该是 NULL");
        System.out.println("   NULL 表示事件尚未发生\n");

        System.out.println("总结：这些字段可以允许 NULL，因为：");
        System.out.println("  • 无法用默认值表示其含义");
        System.out.println("  • NULL 就是最恰当的表示方式");
        System.out.println("  • 业务上确实可能不存在这个值\n");
    }
}

