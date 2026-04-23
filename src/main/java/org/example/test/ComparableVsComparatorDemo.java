package org.example.test;

import java.util.*;

/**
 * Comparable vs Comparator 完整对比
 *
 * 核心区别一句话：
 *   Comparable  → 对象自己知道怎么比较自己（"我天生就能排序"）
 *   Comparator  → 外部裁判决定怎么比较两个对象（"我来制定排序规则"）
 *
 * 目录：
 *   第一部分：接口定义 & 核心区别
 *   第二部分：Comparable 实战（员工按工号排序）
 *   第三部分：Comparator 实战（同一个员工类，多种排序维度）
 *   第四部分：Comparable vs Comparator 选择时机
 *   第五部分：真实项目场景（电商商品多维度排序）
 */
public class ComparableVsComparatorDemo {

    public static void main(String[] args) {

        sep("第一部分：接口定义 & 核心区别");
        InterfaceDefinitionSection.run();

        sep("第二部分：Comparable —— 对象内置排序规则");
        ComparableSection.run();

        sep("第三部分：Comparator —— 外部灵活排序规则");
        ComparatorSection.run();

        sep("第四部分：如果第三方类没实现 Comparable，Comparator 是唯一选择");
        ThirdPartySection.run();

        sep("第五部分：真实项目 —— 电商商品多维度排序");
        EcommerceSection.run();
    }

    static void sep(String title) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" " + title);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}

// ====================================================================
// 第一部分：接口定义 & 核心区别
// ====================================================================
class InterfaceDefinitionSection {
    static void run() {
        System.out.println("【Comparable 接口定义】");
        System.out.println("  public interface Comparable<T> {");
        System.out.println("      int compareTo(T o);  // 和另一个对象比，在哪里？我定义");
        System.out.println("  }");
        System.out.println("  特点：");
        System.out.println("    • 由类自身实现（implements Comparable<自己>）");
        System.out.println("    • compareTo 写在类内部，是对象的'天然排序'");
        System.out.println("    • 一个类只能有一种 compareTo（只有一个天然排序）");
        System.out.println("    • Collections.sort(list) / Arrays.sort(arr) 依赖它");

        System.out.println("\n【Comparator 接口定义】");
        System.out.println("  public interface Comparator<T> {");
        System.out.println("      int compare(T o1, T o2); // 比较两个对象，规则由我说了算");
        System.out.println("  }");
        System.out.println("  特点：");
        System.out.println("    • 独立的比较器类（不需要修改被比较的类）");
        System.out.println("    • 可以有任意多个 Comparator，随时切换排序规则");
        System.out.println("    • Collections.sort(list, comparator) 传入比较器");
        System.out.println("    • Java 8 后支持 Lambda 和链式写法");

        System.out.println("\n【compareTo / compare 返回值规则（两者一样）】");
        System.out.println("  返回负数 → 左边 < 右边（左边排前面）");
        System.out.println("  返回  0  → 左边 == 右边（相等）");
        System.out.println("  返回正数 → 左边 > 右边（右边排前面）");

        System.out.println("\n【生活类比】");
        System.out.println("  Comparable → 身份证上的出生日期（你天生就带着排序规则，按年龄排）");
        System.out.println("  Comparator → 比赛裁判（同一批运动员，跳高比赛按高度排，100米按速度排）");
        System.out.println("               → 裁判规则可以随时换，运动员本身不需要改变");
    }
}

// ====================================================================
// 第二部分：Comparable —— 员工类内置按工号排序
// ====================================================================
class ComparableSection {

    // 员工类：实现 Comparable，按工号从小到大排序（天然排序）
    static class Employee implements Comparable<Employee> {
        private final int    empId;    // 工号
        private final String name;    // 姓名
        private final double salary;  // 薪资
        private final int    age;     // 年龄

        Employee(int empId, String name, double salary, int age) {
            this.empId  = empId;
            this.name   = name;
            this.salary = salary;
            this.age    = age;
        }

        // ★ Comparable 的核心：定义天然排序规则（按工号升序）
        @Override
        public int compareTo(Employee other) {
            // 写法 1：直接相减（适用于整数，不会溢出的情况）
            return this.empId - other.empId;

            // 写法 2：用 Integer.compare（推荐，更安全）
            // return Integer.compare(this.empId, other.empId);
        }

        @Override
        public String toString() {
            return String.format("Employee{id=%d, name='%s', salary=%.0f, age=%d}",
                    empId, name, salary, age);
        }

        int    getEmpId()  { return empId; }
        String getName()   { return name; }
        double getSalary() { return salary; }
        int    getAge()    { return age; }
    }

    static void run() {
        List<Employee> employees = new ArrayList<>(Arrays.asList(
                new Employee(1003, "王五",  15000, 32),
                new Employee(1001, "张三",  20000, 28),
                new Employee(1005, "赵六",   8000, 24),
                new Employee(1002, "李四",  12000, 35),
                new Employee(1004, "钱七",  25000, 40)
        ));

        System.out.println("【排序前】");
        employees.forEach(e -> System.out.println("  " + e));

        // Collections.sort 内部调用每个对象的 compareTo 方法
        Collections.sort(employees);
        // 等价于：employees.sort(null);

        System.out.println("\n【按工号排序后（Comparable 天然排序）】");
        employees.forEach(e -> System.out.println("  " + e));

        System.out.println("\n【Comparable 的问题：只能定义一种排序规则】");
        System.out.println("  如果现在老板说'按薪资排'，怎么办？");
        System.out.println("  → 不能修改 compareTo（已经按工号排了）");
        System.out.println("  → 如果改成按薪资，原来按工号的排序逻辑就丢了");
        System.out.println("  → 这就是 Comparable 的局限性：一个类只能有一种天然排序");
        System.out.println("  → 解决方案：引入 Comparator（见第三部分）");
    }
}

// ====================================================================
// 第三部分：Comparator —— 同一个员工类，多种排序维度
// ====================================================================
class ComparatorSection {

    // 使用第二部分的 Employee 类（不修改它！）

    // ── 比较器 1：按薪资升序 ──
    static class SalaryAscComparator implements Comparator<ComparableSection.Employee> {
        @Override
        public int compare(ComparableSection.Employee e1, ComparableSection.Employee e2) {
            return Double.compare(e1.getSalary(), e2.getSalary());
        }
    }

    // ── 比较器 2：按薪资降序 ──
    static class SalaryDescComparator implements Comparator<ComparableSection.Employee> {
        @Override
        public int compare(ComparableSection.Employee e1, ComparableSection.Employee e2) {
            return Double.compare(e2.getSalary(), e1.getSalary()); // 注意 e2 在前
        }
    }

    // ── 比较器 3：按姓名字典序 ──
    static class NameComparator implements Comparator<ComparableSection.Employee> {
        @Override
        public int compare(ComparableSection.Employee e1, ComparableSection.Employee e2) {
            return e1.getName().compareTo(e2.getName()); // String 自身的 compareTo
        }
    }

    static void run() {
        List<ComparableSection.Employee> employees = new ArrayList<>(Arrays.asList(
                new ComparableSection.Employee(1003, "王五",  15000, 32),
                new ComparableSection.Employee(1001, "张三",  20000, 28),
                new ComparableSection.Employee(1005, "赵六",   8000, 24),
                new ComparableSection.Employee(1002, "李四",  12000, 35),
                new ComparableSection.Employee(1004, "钱七",  25000, 40)
        ));

        // ---- 方式 A：传统 Comparator 实现类 ----
        System.out.println("【方式 A：传统 Comparator 实现类】\n");

        List<ComparableSection.Employee> copy1 = new ArrayList<>(employees);
        copy1.sort(new SalaryAscComparator());
        System.out.println("按薪资升序：");
        copy1.forEach(e -> System.out.println("  " + e));

        // ---- 方式 B：匿名内部类 ----
        System.out.println("\n【方式 B：匿名内部类（Java 7 及之前常见写法）】\n");

        List<ComparableSection.Employee> copy2 = new ArrayList<>(employees);
        copy2.sort(new Comparator<ComparableSection.Employee>() {
            @Override
            public int compare(ComparableSection.Employee e1, ComparableSection.Employee e2) {
                return Integer.compare(e1.getAge(), e2.getAge());
            }
        });
        System.out.println("按年龄升序：");
        copy2.forEach(e -> System.out.println("  " + e));

        // ---- 方式 C：Lambda（Java 8+，最常用）----
        System.out.println("\n【方式 C：Lambda（Java 8+ 推荐写法）】\n");

        List<ComparableSection.Employee> copy3 = new ArrayList<>(employees);
        copy3.sort((e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary())); // 薪资降序
        System.out.println("按薪资降序（Lambda）：");
        copy3.forEach(e -> System.out.println("  " + e));

        // ---- 方式 D：Comparator 链式 API（Java 8+，最优雅）----
        System.out.println("\n【方式 D：Comparator 链式 API（Java 8+ 最优雅）】\n");

        List<ComparableSection.Employee> copy4 = new ArrayList<>(employees);

        // thenComparing：第一个条件相同时，再按第二个条件排
        copy4.sort(
            Comparator.comparingDouble(ComparableSection.Employee::getSalary)  // 先按薪资升序
                      .reversed()                                               // 改为降序
                      .thenComparingInt(ComparableSection.Employee::getAge)    // 薪资相同再按年龄
                      .thenComparing(ComparableSection.Employee::getName)       // 年龄也相同再按姓名
        );
        System.out.println("链式排序（薪资降序 → 年龄升序 → 姓名字典序）：");
        copy4.forEach(e -> System.out.println("  " + e));

        System.out.println("\n【Comparator.comparing 工厂方法说明】");
        System.out.println("  Comparator.comparing(Employee::getName)       按 String 字段排（字典序）");
        System.out.println("  Comparator.comparingInt(Employee::getAge)     按 int 字段排");
        System.out.println("  Comparator.comparingDouble(Employee::getSalary) 按 double 字段排");
        System.out.println("  .reversed()                                   反转当前排序方向");
        System.out.println("  .thenComparing(...)                           次级排序条件");
        System.out.println("  Comparator.naturalOrder()                     自然顺序（依赖 Comparable）");
        System.out.println("  Comparator.reverseOrder()                     自然顺序的逆序");
        System.out.println("  Comparator.nullsFirst(comparator)             null 排在最前");
        System.out.println("  Comparator.nullsLast(comparator)              null 排在最后");
    }
}

// ====================================================================
// 第四部分：第三方类没实现 Comparable，Comparator 是唯一出路
// ====================================================================
class ThirdPartySection {

    // 模拟第三方库的类（我们无法修改源码）
    static class ThirdPartyProduct {
        private final String productName;
        private final double price;
        private final int    stock;

        ThirdPartyProduct(String productName, double price, int stock) {
            this.productName = productName;
            this.price       = price;
            this.stock       = stock;
        }

        // 没有实现 Comparable！也无法修改这个类的源码
        String getProductName() { return productName; }
        double getPrice()       { return price; }
        int    getStock()       { return stock; }

        @Override
        public String toString() {
            return String.format("Product{name='%s', price=%.0f, stock=%d}",
                    productName, price, stock);
        }
    }

    static void run() {
        List<ThirdPartyProduct> products = new ArrayList<>(Arrays.asList(
                new ThirdPartyProduct("手机",  3999, 100),
                new ThirdPartyProduct("耳机",   299,  50),
                new ThirdPartyProduct("电脑",  8999,  20),
                new ThirdPartyProduct("平板",  2999,  30)
        ));

        System.out.println("【场景：第三方库的 ThirdPartyProduct 没有实现 Comparable】\n");

        // ❌ 无法直接 Collections.sort(products)，会编译报错：
        //    ThirdPartyProduct 没有实现 Comparable
        // Collections.sort(products); // 编译错误！

        System.out.println("直接 Collections.sort(products) → 编译报错：没实现 Comparable");
        System.out.println("解决方案：提供 Comparator（不修改第三方类，从外部制定规则）\n");

        // ✅ 用 Comparator 解决
        products.sort(Comparator.comparingDouble(ThirdPartyProduct::getPrice));
        System.out.println("按价格升序（Comparator 解决）：");
        products.forEach(p -> System.out.println("  " + p));

        products.sort(Comparator.comparingInt(ThirdPartyProduct::getStock).reversed());
        System.out.println("\n按库存降序：");
        products.forEach(p -> System.out.println("  " + p));

        System.out.println("\n结论：不能修改第三方类时，Comparator 是唯一选择");
    }
}

// ====================================================================
// 第五部分：真实项目 —— 电商商品多维度排序
// ====================================================================
class EcommerceSection {

    // 电商商品（实现 Comparable，天然排序=默认综合排序得分）
    static class GoodsItem implements Comparable<GoodsItem> {
        private final String  name;          // 商品名
        private final double  price;         // 价格
        private final double  salesVolume;   // 销量
        private final double  rating;        // 评分（0-5）
        private final int     deliveryDays;  // 发货天数（越小越好）
        private final double  score;         // 综合得分（平台算法算出来的）

        GoodsItem(String name, double price, double salesVolume,
                  double rating, int deliveryDays, double score) {
            this.name          = name;
            this.price         = price;
            this.salesVolume   = salesVolume;
            this.rating        = rating;
            this.deliveryDays  = deliveryDays;
            this.score         = score;
        }

        // 天然排序：按平台综合得分降序（默认的"综合排序"）
        @Override
        public int compareTo(GoodsItem other) {
            return Double.compare(other.score, this.score); // other 在前 = 降序
        }

        @Override
        public String toString() {
            return String.format("%-12s 价格:%-6.0f 销量:%-6.0f 评分:%.1f 发货:%d天 得分:%.1f",
                    name, price, salesVolume, rating, deliveryDays, score);
        }

        double getPrice()       { return price; }
        double getSalesVolume() { return salesVolume; }
        double getRating()      { return rating; }
        int    getDeliveryDays(){ return deliveryDays; }
        double getScore()       { return score; }
        String getName()        { return name; }
    }

    // ── Comparator 1：价格升序（用户点击"价格从低到高"）──
    static final Comparator<GoodsItem> PRICE_ASC =
            Comparator.comparingDouble(GoodsItem::getPrice);

    // ── Comparator 2：价格降序（用户点击"价格从高到低"）──
    static final Comparator<GoodsItem> PRICE_DESC =
            Comparator.comparingDouble(GoodsItem::getPrice).reversed();

    // ── Comparator 3：销量降序（用户点击"销量优先"）──
    static final Comparator<GoodsItem> SALES_DESC =
            Comparator.comparingDouble(GoodsItem::getSalesVolume).reversed();

    // ── Comparator 4：评分降序（用户点击"好评优先"）──
    static final Comparator<GoodsItem> RATING_DESC =
            Comparator.comparingDouble(GoodsItem::getRating).reversed();

    // ── Comparator 5：复合排序（评分高 + 价格低 + 发货快）──
    // 真实项目中用户可能筛选"4分以上 + 价格200以内 + 发货最快"
    static final Comparator<GoodsItem> QUALITY_SORT =
            Comparator.comparingDouble(GoodsItem::getRating).reversed()   // 先按评分降序
                      .thenComparingDouble(GoodsItem::getPrice)           // 评分相同按价格升序
                      .thenComparingInt(GoodsItem::getDeliveryDays);      // 价格相同按发货天数升序

    // ── 工厂方法：根据用户选择返回对应 Comparator（前端传参 → 动态切换）──
    static Comparator<GoodsItem> getComparator(String sortType) {
        switch (sortType) {
            case "price_asc":   return PRICE_ASC;
            case "price_desc":  return PRICE_DESC;
            case "sales":       return SALES_DESC;
            case "rating":      return RATING_DESC;
            case "quality":     return QUALITY_SORT;
            default:            return Comparator.naturalOrder(); // 综合排序（依赖 Comparable）
        }
    }

    static void run() {
        List<GoodsItem> goods = new ArrayList<>(Arrays.asList(
                new GoodsItem("华为手机",   3999, 5000, 4.8, 1, 92.5),
                new GoodsItem("小米手机",   1999, 8000, 4.5, 2, 88.0),
                new GoodsItem("苹果手机",   6999, 3000, 4.9, 1, 95.0),
                new GoodsItem("OPPO手机",  2499, 6000, 4.6, 3, 85.0),
                new GoodsItem("vivo手机",  2199, 4000, 4.4, 2, 83.0),
                new GoodsItem("三星手机",   5499, 2000, 4.7, 1, 87.5)
        ));

        System.out.println("【场景：电商搜索结果页，用户可以按不同维度排序】\n");
        System.out.println("原始商品列表：");
        goods.forEach(g -> System.out.println("  " + g));

        // 模拟用户切换排序方式
        String[] sortChoices = {"default", "price_asc", "price_desc", "sales", "rating", "quality"};
        String[] sortLabels  = {"综合排序（Comparable天然排序）",
                                "价格从低到高", "价格从高到低",
                                "销量优先", "好评优先",
                                "品质排序（评分↓ + 价格↑ + 发货速度↑）"};

        for (int i = 0; i < sortChoices.length; i++) {
            List<GoodsItem> sorted = new ArrayList<>(goods);

            if ("default".equals(sortChoices[i])) {
                Collections.sort(sorted);           // 用 Comparable（天然排序）
            } else {
                sorted.sort(getComparator(sortChoices[i]));  // 用 Comparator（外部规则）
            }

            System.out.println("\n用户选择【" + sortLabels[i] + "】：");
            sorted.forEach(g -> System.out.println("  " + g));
        }

        System.out.println("\n【设计总结】");
        System.out.println("  Comparable  → 定义'综合得分排序'，作为商品的默认/天然排序规则");
        System.out.println("               → Collections.sort() 直接用，不需要额外参数");
        System.out.println("  Comparator  → 定义'价格/销量/评分'等多种排序，由前端参数动态选择");
        System.out.println("               → 加新的排序维度只需新增一个 Comparator，不改商品类");
        System.out.println("               → 符合开闭原则：对扩展开放，对修改关闭");
    }
}

