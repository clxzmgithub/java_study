package org.example;

/**
 * Java 面向对象三大特性演示：封装、继承、多态
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("========== Java 面向对象三大特性演示 ==========\n");

        // 1. 封装演示
        System.out.println("【1. 封装（Encapsulation）演示】");
        demoEncapsulation();

        System.out.println("\n【2. 继承（Inheritance）演示】");
        demoInheritance();

        System.out.println("\n【3. 多态（Polymorphism）演示】");
        demoPolymorphism();
    }

    // ============ 1. 封装演示 ============
    private static void demoEncapsulation() {
        // 创建一个学生对象
        Student student = new Student("张三", 20);

        // 通过 getter 方法访问私有属性
        System.out.println("学生姓名: " + student.getName());
        System.out.println("学生年龄: " + student.getAge());

        // 通过 setter 方法修改私有属性（带验证）
        student.setAge(21);
        System.out.println("修改后年龄: " + student.getAge());

        // 尝试设置不合理的年龄
        student.setAge(-5);  // 会被拒绝
        System.out.println("设置无效年龄后: " + student.getAge());

        // 调用公开的方法
        student.study();
    }

    // ============ 2. 继承演示 ============
    private static void demoInheritance() {
        // 创建一个普通员工
        Employee employee = new Employee("李四", 5000);
        System.out.println("员工: " + employee.getName() + ", 工资: " + employee.getSalary());
        employee.work();

        // 创建一个经理（继承员工）
        Manager manager = new Manager("王五", 10000, "技术部");
        System.out.println("\n经理: " + manager.getName() + ", 工资: " + manager.getSalary() + ", 部门: " + manager.getDepartment());
        manager.work();
        manager.managePeople();  // 经理特有的方法
    }

    // ============ 3. 多态演示 ============
    private static void demoPolymorphism() {
        // 使用父类引用指向子类对象（多态的关键）
        Animal dog = new Dog("旺财");
        Animal cat = new Cat("咪咪");
        Animal bird = new Bird("鸟儿");

        // 调用同一个方法，不同对象表现不同的行为
        System.out.println("--- 动物发出声音 ---");
        dog.makeSound();
        cat.makeSound();
        bird.makeSound();

        System.out.println("\n--- 动物移动 ---");
        dog.move();
        cat.move();
        bird.move();

        // 通过多态实现通用的处理逻辑
        System.out.println("\n--- 使用数组处理多种动物 ---");
        Animal[] animals = {dog, cat, bird};
        for (Animal animal : animals) {
            System.out.print(animal.getName() + " - ");
            animal.makeSound();
        }
    }
}

// ============ 1. 封装示例：学生类 ============
class Student {
    // 私有属性（封装）
    private String name;
    private int age;

    // 构造方法
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 公开的 getter 方法
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    // 公开的 setter 方法（带数据验证）
    public void setAge(int age) {
        if (age > 0 && age < 150) {
            this.age = age;
            System.out.println("年龄设置成功: " + age);
        } else {
            System.out.println("年龄不合理，设置失败");
        }
    }

    // 公开的业务方法
    public void study() {
        System.out.println(name + " 正在学习...");
    }
}

// ============ 2. 继承示例 ============
// 父类：员工
class Employee {
    private String name;
    private double salary;

    public Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public double getSalary() {
        return salary;
    }

    public void work() {
        System.out.println(name + " 正在工作...");
    }
}

// 子类：经理（继承员工类）
class Manager extends Employee {
    private String department;

    public Manager(String name, double salary, String department) {
        super(name, salary);  // 调用父类构造方法
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    // 重写父类方法
    @Override
    public void work() {
        System.out.println(getName() + " 作为经理在 " + department + " 部门工作...");
    }

    // 子类特有的方法
    public void managePeople() {
        System.out.println(getName() + " 正在管理团队...");
    }
}

// ============ 3. 多态示例 ============
// 父类：动物
abstract class Animal {
    protected String name;

    public Animal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // 抽象方法（子类必须实现）
    abstract public void makeSound();

    // 普通方法（子类可以重写）
    public void move() {
        System.out.println(name + " 在移动...");
    }
}

// 子类：狗
class Dog extends Animal {
    public Dog(String name) {
        super(name);
    }

    @Override
    public void makeSound() {
        System.out.println(name + " 汪汪汪...");
    }

    @Override
    public void move() {
        System.out.println(name + " 用四条腿奔跑...");
    }
}

// 子类：猫
class Cat extends Animal {
    public Cat(String name) {
        super(name);
    }

    @Override
    public void makeSound() {
        System.out.println(name + " 喵喵喵...");
    }

    @Override
    public void move() {
        System.out.println(name + " 轻轻地走动...");
    }
}

// 子类：鸟
class Bird extends Animal {
    public Bird(String name) {
        super(name);
    }

    @Override
    public void makeSound() {
        System.out.println(name + " 叽叽喳喳...");
    }

    @Override
    public void move() {
        System.out.println(name + " 飞行中...");
    }
}
