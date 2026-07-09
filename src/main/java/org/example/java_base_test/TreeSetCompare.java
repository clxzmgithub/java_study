package org.example.java_base_test;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * @Description TreeSetCompare - TreeSet排序原理详解
 * @Author 98468
 * @Date 2026-07-09
 * @Remark 通过代码示例讲解TreeSet的Comparator工作原理
 * @Copyright Copyright © XX公司 All Rights Reserved
 */
public class TreeSetCompare {

    public static void main(String[] args) {
        System.out.println("========== TreeSet 排序原理详解 ==========");
        System.out.println();

        // 1. 核心法则演示
        demoCoreRule();

        // 2. 升序实现
        demoAscendingOrder();

        // 3. 降序实现
        demoDescendingOrder();

        // 4. 多级排序
        demoMultiLevelSort();

        // 5. 不同类型比较
        demoDifferentTypes();

        // 6. 去重机制
        demoDeduplication();

        // 7. 逆序排序与红黑树遍历详解
        demoReverseOrderWithTreeTraversal();
    }

    /**
     * 1. 核心法则：返回值决定顺序（铁律）
     * 
     * TreeSet（红黑树）只认compare返回值的符号，不认逻辑本身：
     * - 负数 (-1): o1 小于 o2 → o1 排在 o2 前面（左子树）
     * - 零 (0):    o1 等于 o2 → 去重（后添加的忽略）
     * - 正数 (1):  o1 大于 o2 → o1 排在 o2 后面（右子树）
     */
    private static void demoCoreRule() {
        System.out.println("【1】核心法则：返回值决定顺序");
        System.out.println("-----------------------------------");
        System.out.println("TreeSet只认compare返回值的符号：");
        System.out.println("  负数(-1) → o1 < o2 → o1排前面");
        System.out.println("  零(0)    → o1 = o2 → 去重");
        System.out.println("  正数(1)  → o1 > o2 → o1排后面");
        System.out.println();

        // 创建一个简单的比较器，手动控制返回值
        Comparator<Integer> manualComparator = (o1, o2) -> {
            if (o1 < o2) return -1;  // 负数：o1排前面
            if (o1.equals(o2)) return 0;   // 零：去重
            return 1;               // 正数：o1排后面
        };

        TreeSet<Integer> treeSet = new TreeSet<>(manualComparator);
        treeSet.add(30);
        treeSet.add(10);
        treeSet.add(20);
        treeSet.add(10);  // 重复，会被去重

        System.out.println("添加顺序: 30, 10, 20, 10(重复)");
        System.out.println("TreeSet结果: " + treeSet);
        System.out.println("说明: 自动按升序排列，且去重");
        System.out.println();
    }

    /**
     * 2. 升序实现原理（标准写法）
     * 
     * Integer.compare(o1.getAge(), o2.getAge())
     * 底层逻辑：compare(x, y) 返回 x - y 的符号
     * 参数顺序：前者减后者（o1 - o2）
     * 结果：年龄小的o1返回负数 → 被树判定为"小于" → 自然排在前面 → 年龄升序
     */
    private static void demoAscendingOrder() {
        System.out.println("【2】升序实现（标准写法）");
        System.out.println("-----------------------------------");

        // 使用Integer.compare实现升序
        Comparator<Person> ageAscComparator = (o1, o2) -> 
            Integer.compare(o1.getAge(), o2.getAge());

        TreeSet<Person> ascSet = new TreeSet<>(ageAscComparator);
        ascSet.add(new Person("张三", 28));
        ascSet.add(new Person("李四", 18));
        ascSet.add(new Person("王五", 25));

        System.out.println("比较器: Integer.compare(o1.getAge(), o2.getAge())");
        System.out.println("逻辑: o1.age - o2.age（前者减后者）");
        System.out.println("添加顺序: 张三(28), 李四(18), 王五(25)");
        System.out.println("TreeSet结果（按年龄升序）:");
        for (Person p : ascSet) {
            System.out.println("  " + p);
        }
        System.out.println();
    }

    /**
     * 3. 降序实现原理（调换参数）
     * 
     * Integer.compare(o2.getAge(), o1.getAge())
     * 底层逻辑：变成 o2 - o1
     * 结果：当o1=18，o2=28时，实际比较28-18得正数 → 树将o1判定为"大于"o2 → 18被排到28后面 → 年龄降序
     */
    private static void demoDescendingOrder() {
        System.out.println("【3】降序实现（调换参数）");
        System.out.println("-----------------------------------");

        // 使用Integer.compare实现降序（参数反写）
        Comparator<Person> ageDescComparator = (o1, o2) -> 
            Integer.compare(o2.getAge(), o1.getAge());

        TreeSet<Person> descSet = new TreeSet<>(ageDescComparator);
        descSet.add(new Person("张三", 28));
        descSet.add(new Person("李四", 18));
        descSet.add(new Person("王五", 25));

        System.out.println("比较器: Integer.compare(o2.getAge(), o1.getAge())");
        System.out.println("逻辑: o2.age - o1.age（参数反写）");
        System.out.println("添加顺序: 张三(28), 李四(18), 王五(25)");
        System.out.println("TreeSet结果（按年龄降序）:");
        for (Person p : descSet) {
            System.out.println("  " + p);
        }
        System.out.println();
    }

    /**
     * 4. 多级排序（级联比较）标准模板
     * 
     * 当第一条件相等时，必须逐级向后传递，只有当前一级res==0时才比较下一级：
     * int res = 第一条件;
     * if (res == 0) res = 第二条件;
     * if (res == 0) res = 第三条件;
     * return res;
     * 
     * 作用：确保排序唯一性，且若所有指定字段相等则返回0，自动去重
     */
    private static void demoMultiLevelSort() {
        System.out.println("【4】多级排序（级联比较）");
        System.out.println("-----------------------------------");

        // 多级排序：先按年龄升序，年龄相同再按姓名字典序升序
        Comparator<Person> multiLevelComparator = (o1, o2) -> {
            // 第一级：按年龄升序
            int res = Integer.compare(o1.getAge(), o2.getAge());
            
            // 如果年龄相同，进入第二级：按姓名升序
            if (res == 0) {
                res = o1.getName().compareTo(o2.getName());
            }
            
            return res;
        };

        TreeSet<Person> multiSet = new TreeSet<>(multiLevelComparator);
        multiSet.add(new Person("张三", 25));
        multiSet.add(new Person("李四", 18));
        multiSet.add(new Person("王五", 25));  // 年龄与张三相同
        multiSet.add(new Person("赵六", 18));  // 年龄与李四相同
        multiSet.add(new Person("张三", 25));  // 完全重复，会被去重

        System.out.println("比较器: 先按年龄升序，年龄相同按姓名升序");
        System.out.println("添加顺序: 张三(25), 李四(18), 王五(25), 赵六(18), 张三(25)(重复)");
        System.out.println("TreeSet结果（多级排序）:");
        for (Person p : multiSet) {
            System.out.println("  " + p);
        }
        System.out.println("说明: 18岁的赵六和李四按姓名排序，25岁的张三和王五按姓名排序");
        System.out.println();
    }

    /**
     * 5. 各类型比较的正确写法
     * 
     * 避免精度损失或空指针，请使用包装类提供的静态方法：
     * - 整数（int）：Integer.compare(o1.getAge(), o2.getAge())
     * - 浮点数（double）：Double.compare(o1.getHeight(), o2.getHeight())
     * - 字符串（String）：o1.getName().compareTo(o2.getName())（已按字典序）
     */
    private static void demoDifferentTypes() {
        System.out.println("【5】不同类型比较的正确写法");
        System.out.println("-----------------------------------");

        // 创建带身高信息的扩展Person类
        class PersonWithHeight extends Person {
            private double height;

            public PersonWithHeight(String name, int age, double height) {
                super(name, age);
                this.height = height;
            }

            public double getHeight() {
                return height;
            }

            @Override
            public String toString() {
                return "Person{" +
                        "name='" + getName() + '\'' +
                        ", age=" + getAge() +
                        ", height=" + height +
                        '}';
            }
        }

        // 多类型比较器：年龄(int) + 身高(double) + 姓名(String)
        Comparator<PersonWithHeight> multiTypeComparator = (o1, o2) -> {
            // 第一级：按年龄升序（使用Integer.compare）
            int res = Integer.compare(o1.getAge(), o2.getAge());
            
            // 第二级：按身高升序（使用Double.compare处理精度）
            if (res == 0) {
                res = Double.compare(o1.getHeight(), o2.getHeight());
            }
            
            // 第三级：按姓名字典序（使用String.compareTo）
            if (res == 0) {
                res = o1.getName().compareTo(o2.getName());
            }
            
            return res;
        };

        TreeSet<PersonWithHeight> typeSet = new TreeSet<>(multiTypeComparator);
        typeSet.add(new PersonWithHeight("张三", 25, 1.75));
        typeSet.add(new PersonWithHeight("李四", 18, 1.80));
        typeSet.add(new PersonWithHeight("王五", 25, 1.70));
        typeSet.add(new PersonWithHeight("赵六", 25, 1.75));  // 年龄身高与张三相同

        System.out.println("比较器: 年龄(Integer.compare) → 身高(Double.compare) → 姓名(String.compareTo)");
        System.out.println("添加顺序:");
        System.out.println("  张三(25岁, 1.75m), 李四(18岁, 1.80m)");
        System.out.println("  王五(25岁, 1.70m), 赵六(25岁, 1.75m)");
        System.out.println("TreeSet结果（多类型排序）:");
        for (PersonWithHeight p : typeSet) {
            System.out.println("  " + p);
        }
        System.out.println("说明: ");
        System.out.println("  - 18岁李四排最前");
        System.out.println("  - 25岁三人中，王五(1.70m) < 张三/赵六(1.75m)");
        System.out.println("  - 张三和赵六身高相同，按姓名字典序：张三 < 赵六");
        System.out.println();
    }

    /**
     * 6. 去重机制演示
     * 
     * 去重依据：只有最终res==0才算重复
     * 若仅年龄相等但姓名不同，不会去重
     */
    private static void demoDeduplication() {
        System.out.println("【6】去重机制");
        System.out.println("-----------------------------------");

        // 只按年龄比较的比较器
        Comparator<Person> ageOnlyComparator = (o1, o2) -> 
            Integer.compare(o1.getAge(), o2.getAge());

        TreeSet<Person> dedupSet = new TreeSet<>(ageOnlyComparator);
        dedupSet.add(new Person("张三", 25));
        dedupSet.add(new Person("李四", 25));  // 年龄相同，姓名不同
        dedupSet.add(new Person("王五", 25));  // 年龄相同，姓名不同
        dedupSet.add(new Person("张三", 25));  // 完全相同的年龄

        System.out.println("比较器: 只按年龄比较");
        System.out.println("添加顺序: 张三(25), 李四(25), 王五(25), 张三(25)");
        System.out.println("TreeSet结果:");
        for (Person p : dedupSet) {
            System.out.println("  " + p);
        }
        System.out.println("说明: ");
        System.out.println("  - 因为比较器只比较年龄，所以年龄相同的都被视为'相等'");
        System.out.println("  - 只会保留第一个添加的张三(25)，后面的都被去重了");
        System.out.println();

        // 对比：使用多级排序的比较器
        Comparator<Person> multiComparator = (o1, o2) -> {
            int res = Integer.compare(o1.getAge(), o2.getAge());
            if (res == 0) {
                res = o1.getName().compareTo(o2.getName());
            }
            return res;
        };

        TreeSet<Person> multiDedupSet = new TreeSet<>(multiComparator);
        multiDedupSet.add(new Person("张三", 25));
        multiDedupSet.add(new Person("李四", 25));  // 年龄相同，姓名不同
        multiDedupSet.add(new Person("王五", 25));  // 年龄相同，姓名不同
        multiDedupSet.add(new Person("张三", 25));  // 完全重复

        System.out.println("比较器: 先按年龄，再按姓名");
        System.out.println("添加顺序: 张三(25), 李四(25), 王五(25), 张三(25)");
        System.out.println("TreeSet结果:");
        for (Person p : multiDedupSet) {
            System.out.println("  " + p);
        }
        System.out.println("说明: ");
        System.out.println("  - 年龄相同但姓名不同，不会被去重");
        System.out.println("  - 只有完全相同的张三(25)才会被去重");
        System.out.println();
    }

    /**
     * 7. 逆序排序与红黑树遍历详解
     * 
     * 这个例子会详细展示：
     * 1. 逆序比较器的实现原理
     * 2. 元素逐个添加到红黑树的过程
     * 3. 中序遍历如何得到逆序结果
     */
    private static void demoReverseOrderWithTreeTraversal() {
        System.out.println("【7】逆序排序与红黑树遍历详解");
        System.out.println("=========================================");
        System.out.println();

        System.out.println("📌 核心概念回顾：");
        System.out.println("  • TreeSet底层是红黑树（自平衡二叉搜索树）");
        System.out.println("  • 左子树 < 根节点 < 右子树");
        System.out.println("  • 中序遍历（左→根→右）得到有序序列");
        System.out.println("  • 逆序 = 改变比较规则，让大的数排在左边");
        System.out.println();

        // 创建逆序比较器
        Comparator<Integer> reverseComparator = (o1, o2) -> {
            // 关键：参数反写，o2 - o1
            // 当o1=30, o2=10时，返回 10-30 = -20（负数）
            // 树认为 o1 < o2，所以30会被放到10的左边
            return Integer.compare(o2, o1);
        };

        TreeSet<Integer> reverseSet = new TreeSet<>(reverseComparator);

        System.out.println("🔍 逐步添加元素到红黑树的过程：");
        System.out.println("比较器逻辑: Integer.compare(o2, o1) → 大的数放左边");
        System.out.println();

        // 第1步：添加 30
        System.out.println("【步骤1】添加 30");
        reverseSet.add(30);
        System.out.println("  树结构:");
        System.out.println("      30 (根节点)");
        System.out.println("     /  \\");
        System.out.println("   null  null");
        System.out.println("  当前集合: [30]");
        System.out.println();

        // 第2步：添加 10
        System.out.println("【步骤2】添加 10");
        System.out.println("  比较: compare(10, 30) = Integer.compare(30, 10) = 正数");
        System.out.println("  判定: 10 > 30，所以10放在30的右边");
        reverseSet.add(10);
        System.out.println("  树结构:");
        System.out.println("      30");
        System.out.println("     /  \\");
        System.out.println("   null  10");
        System.out.println("  当前集合: [30, 10]");
        System.out.println();

        // 第3步：添加 20
        System.out.println("【步骤3】添加 20");
        System.out.println("  第1次比较: compare(20, 30) = Integer.compare(30, 20) = 正数");
        System.out.println("           → 20 > 30，往右走");
        System.out.println("  第2次比较: compare(20, 10) = Integer.compare(10, 20) = 负数");
        System.out.println("           → 20 < 10，往左走");
        reverseSet.add(20);
        System.out.println("  树结构:");
        System.out.println("      30");
        System.out.println("     /  \\");
        System.out.println("   null  10");
        System.out.println("        /");
        System.out.println("      20");
        System.out.println("  当前集合: [30, 20, 10] ← 注意！这是中序遍历的结果");
        System.out.println();

        // 第4步：添加 40
        System.out.println("【步骤4】添加 40");
        System.out.println("  第1次比较: compare(40, 30) = Integer.compare(30, 40) = 负数");
        System.out.println("           → 40 < 30，往左走");
        reverseSet.add(40);
        System.out.println("  树结构:");
        System.out.println("      30");
        System.out.println("     /  \\");
        System.out.println("   40    10");
        System.out.println("        /");
        System.out.println("      20");
        System.out.println("  当前集合: [40, 30, 20, 10]");
        System.out.println();

        // 第5步：添加 25
        System.out.println("【步骤5】添加 25");
        System.out.println("  第1次比较: compare(25, 30) = Integer.compare(30, 25) = 正数");
        System.out.println("           → 25 > 30，往右走");
        System.out.println("  第2次比较: compare(25, 10) = Integer.compare(10, 25) = 负数");
        System.out.println("           → 25 < 10，往左走");
        System.out.println("  第3次比较: compare(25, 20) = Integer.compare(20, 25) = 负数");
        System.out.println("           → 25 < 20，往左走");
        reverseSet.add(25);
        System.out.println("  树结构:");
        System.out.println("        30");
        System.out.println("       /  \\");
        System.out.println("     40    10");
        System.out.println("         /");
        System.out.println("       20");
        System.out.println("      /");
        System.out.println("    25");
        System.out.println();

        // 输出最终结果
        System.out.println("✅ 最终红黑树结构:");
        System.out.println("        30");
        System.out.println("       /  \\");
        System.out.println("     40    10");
        System.out.println("         /");
        System.out.println("       20");
        System.out.println("      /");
        System.out.println("    25");
        System.out.println();

        System.out.println("🔄 中序遍历过程（左→根→右）:");
        System.out.println("  1. 从根节点30开始");
        System.out.println("  2. 遍历左子树40:");
        System.out.println("     - 40的左子树为空");
        System.out.println("     - 访问40 → 输出 [40]");
        System.out.println("     - 40的右子树为空");
        System.out.println("  3. 访问根节点30 → 输出 [40, 30]");
        System.out.println("  4. 遍历右子树10:");
        System.out.println("     - 先遍历10的左子树20:");
        System.out.println("       · 先遍历20的左子树25:");
        System.out.println("         » 25的左子树为空");
        System.out.println("         » 访问25 → 输出 [40, 30, 25]");
        System.out.println("         » 25的右子树为空");
        System.out.println("       · 访问20 → 输出 [40, 30, 25, 20]");
        System.out.println("       · 20的右子树为空");
        System.out.println("     - 访问10 → 输出 [40, 30, 25, 20, 10]");
        System.out.println("     - 10的右子树为空");
        System.out.println();

        System.out.println("📊 TreeSet实际输出结果:");
        System.out.println("  添加顺序: 30, 10, 20, 40, 25");
        System.out.println("  遍历结果: " + reverseSet);
        System.out.println();

        System.out.println("💡 关键理解:");
        System.out.println("  • 逆序的本质：通过反转比较规则，改变了树的构建方式");
        System.out.println("  • 大的数被放到左边，小的数被放到右边");
        System.out.println("  • 中序遍历仍然是'左→根→右'，但因为左边是大数，所以得到降序");
        System.out.println("  • 这就是为什么 Integer.compare(o2, o1) 能实现逆序的原因");
        System.out.println();

        System.out.println("🎯 对比升序和逆序:");
        System.out.println("  升序: Integer.compare(o1, o2) → 小数在左 → 中序遍历得升序");
        System.out.println("  逆序: Integer.compare(o2, o1) → 大数在左 → 中序遍历得降序");
        System.out.println();
    }
}