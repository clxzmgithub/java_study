package org.example.java_base_test;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 浮点数精度丢失问题及解决方案演示
 */
public class FloatingPointPrecision {
    public static void main(String[] args) {
        System.out.println("========== 浮点数精度丢失问题 ==========\n");

        // 1. 浮点数精度丢失现象
        System.out.println("【1. 浮点数精度丢失现象】");
        demoPrecisionLoss();

        // 2. 二进制表示问题
        System.out.println("\n【2. 为什么会精度丢失（二进制表示原理）】");
        demoBinaryRepresentation();

        // 3. 浮点数比较问题
        System.out.println("\n【3. 浮点数比较的危险】");
        demoComparison();

        // 4. 使用 BigDecimal 解决问题
        System.out.println("\n【4. 使用 BigDecimal 解决精度问题】");
        demoBigDecimal();

        // 5. 实际应用场景
        System.out.println("\n【5. 实际应用场景（金融计算）】");
        demoFinancialCalculation();
    }

    // ============ 1. 精度丢失现象 ============
    private static void demoPrecisionLoss() {
        // 案例 1：简单加法
        double a = 0.1;
        double b = 0.2;
        double result = a + b;

        System.out.println("0.1 + 0.2 = " + result);
        System.out.println("预期值: 0.3");
        System.out.println("是否相等: " + (result == 0.3));
        System.out.println("差值: " + (result - 0.3));

        // 案例 2：乘法
        System.out.println("\n--- 乘法示例 ---");
        double c = 0.1 * 7;
        System.out.println("0.1 * 7 = " + c);
        System.out.println("预期值: 0.7");
        System.out.println("是否相等: " + (c == 0.7));
        System.out.println("差值: " + (c - 0.7));

        // 案例 3：多次运算
        System.out.println("\n--- 多次运算示例 ---");
        double sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += 0.1;
        }
        System.out.println("0.1 + 0.1 + ... + 0.1 (共10次) = " + sum);
        System.out.println("预期值: 1.0");
        System.out.println("是否相等: " + (sum == 1.0));
        System.out.println("差值: " + (sum - 1.0));
    }

    // ============ 2. 二进制表示原理 ============
    private static void demoBinaryRepresentation() {
        System.out.println("--- 为什么 0.1 + 0.2 ≠ 0.3 ---\n");

        // Java 浮点数使用 IEEE 754 标准
        // double 是 64 位浮点数：1 位符号 + 11 位指数 + 52 位尾数

        System.out.println("十进制 0.1 的二进制表示（无限循环）:");
        System.out.println("0.1₁₀ = 0.0001100110011001100...₂");
        System.out.println("        （0011 无限循环）");

        System.out.println("\n因为 52 位尾数的限制，无法精确表示");
        System.out.println("存储值 ≈ " + String.format("%.20f", 0.1));

        System.out.println("\n十进制 0.2 的二进制表示:");
        System.out.println("0.2₁₀ = 0.001100110011001100...₂");
        System.out.println("        （0011 无限循环）");
        System.out.println("存储值 ≈ " + String.format("%.20f", 0.2));

        System.out.println("\n两者相加:");
        System.out.println("存储的 0.1 + 存储的 0.2 ≈ " + String.format("%.20f", 0.1 + 0.2));
        System.out.println("理想的 0.3 应该是 ≈ " + String.format("%.20f", 0.3));

        System.out.println("\n结论：0.1 和 0.2 都无法用二进制精确表示");
        System.out.println("      在有限的存储空间内，必然会有精度丢失");
    }

    // ============ 3. 浮点数比较问题 ============
    private static void demoComparison() {
        System.out.println("--- 危险的直接比较 ---\n");

        double x = 0.1 + 0.2;
        double y = 0.3;

        System.out.println("x = 0.1 + 0.2 = " + x);
        System.out.println("y = 0.3 = " + y);
        System.out.println("x == y: " + (x == y) + " ❌ 错误！");

        // 正确的比较方法：使用误差范围
        System.out.println("\n--- 正确的比较方法 ---\n");

        double epsilon = 1e-9;  // 误差阈值
        boolean isEqual = Math.abs(x - y) < epsilon;
        System.out.println("误差阈值 (epsilon): " + epsilon);
        System.out.println("|x - y| = " + Math.abs(x - y));
        System.out.println("|x - y| < epsilon: " + isEqual + " ✅ 正确");

        // 更安全的比较
        System.out.println("\n--- 使用 compareTo 比较 ---");
        BigDecimal bd1 = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        BigDecimal bd2 = new BigDecimal("0.3");
        System.out.println("BigDecimal(0.1 + 0.2).compareTo(BigDecimal(0.3)) = "
                           + bd1.compareTo(bd2) + " (0 表示相等) ✅");
    }

    // ============ 4. BigDecimal 解决方案 ============
    private static void demoBigDecimal() {
        System.out.println("--- BigDecimal 的优势 ---\n");

        // 使用字符串构造（避免精度丢失）
        BigDecimal a = new BigDecimal("0.1");
        BigDecimal b = new BigDecimal("0.2");
        BigDecimal c = new BigDecimal("0.3");

        BigDecimal result = a.add(b);

        System.out.println("使用 BigDecimal:");
        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("a + b = " + result);
        System.out.println("c = " + c);
        System.out.println("(a + b).equals(c): " + result.equals(c) + " ✅");

        // 注意：不要用 double 构造 BigDecimal
        System.out.println("\n--- 错误用法（不要这样做）---");
        BigDecimal wrong = new BigDecimal(0.1);  // ❌ 错误
        System.out.println("BigDecimal(0.1) = " + wrong + " ❌");

        System.out.println("\n--- 正确用法（推荐）---");
        BigDecimal correct = new BigDecimal("0.1");  // ✅ 正确
        System.out.println("BigDecimal(\"0.1\") = " + correct + " ✅");

        // BigDecimal 的各种运算
        System.out.println("\n--- BigDecimal 的四则运算 ---");
        BigDecimal x = new BigDecimal("10.5");
        BigDecimal y = new BigDecimal("3");

        System.out.println("加法: " + x.add(y));
        System.out.println("减法: " + x.subtract(y));
        System.out.println("乘法: " + x.multiply(y));
        System.out.println("除法: " + x.divide(y, 2, RoundingMode.HALF_UP));  // 保留 2 位小数
    }

    // ============ 5. 实际应用（金融计算）============
    private static void demoFinancialCalculation() {
        System.out.println("--- 银行利息计算示例 ---\n");

        // 场景：计算客户总利息
        // 客户 A：存款 100.01 元
        // 客户 B：存款 100.02 元
        // 利率：0.05 (5%)

        // ❌ 错误做法：使用 double
        System.out.println("❌ 使用 double 的结果：");
        double depositA = 100.01;
        double depositB = 100.02;
        double rate = 0.05;

        double interestA = depositA * rate;
        double interestB = depositB * rate;
        double totalInterest = interestA + interestB;
        double totalDeposit = depositA + depositB;

        System.out.println("客户 A 利息: " + interestA);
        System.out.println("客户 B 利息: " + interestB);
        System.out.println("总利息: " + totalInterest);
        System.out.println("总存款: " + totalDeposit);
        System.out.println("验证: " + (totalDeposit * rate) + " (应该 = " + totalInterest + ")");
        System.out.println("误差: " + (totalDeposit * rate - totalInterest));

        // ✅ 正确做法：使用 BigDecimal
        System.out.println("\n✅ 使用 BigDecimal 的结果：");
        BigDecimal bdDepositA = new BigDecimal("100.01");
        BigDecimal bdDepositB = new BigDecimal("100.02");
        BigDecimal bdRate = new BigDecimal("0.05");

        BigDecimal bdInterestA = bdDepositA.multiply(bdRate);
        BigDecimal bdInterestB = bdDepositB.multiply(bdRate);
        BigDecimal bdTotalInterest = bdInterestA.add(bdInterestB);
        BigDecimal bdTotalDeposit = bdDepositA.add(bdDepositB);

        System.out.println("客户 A 利息: " + bdInterestA);
        System.out.println("客户 B 利息: " + bdInterestB);
        System.out.println("总利息: " + bdTotalInterest);
        System.out.println("总存款: " + bdTotalDeposit);
        System.out.println("验证: " + bdTotalDeposit.multiply(bdRate));
        System.out.println("误差: 0 (完全精确)");

        // 实际场景：超市收银
        System.out.println("\n--- 超市收银示例 ---\n");
        System.out.println("购物清单:");

        BigDecimal[] prices = {
            new BigDecimal("9.99"),
            new BigDecimal("14.99"),
            new BigDecimal("7.50"),
            new BigDecimal("3.01")
        };

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < prices.length; i++) {
            System.out.println("商品 " + (i + 1) + ": " + prices[i]);
            total = total.add(prices[i]);
        }

        System.out.println("\n总价: " + total);

        // 计算折扣（打 9 折）
        BigDecimal discount = new BigDecimal("0.9");
        BigDecimal finalPrice = total.multiply(discount);
        System.out.println("折后价（9 折）: " + finalPrice);
    }
}

