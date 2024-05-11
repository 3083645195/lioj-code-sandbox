# lioj-code-sandbox 代码沙箱原生实现

## 代码沙箱项目初始化

- 代码沙箱的定位:只负责接受代码和输入，返回编译运行的结果，不负责判题(
  可以作为独立的项目/服务，提供给其他的需要执行代码的项目去使用)
- 以 Java 编程语言为主，带大家实现代码沙箱，重要的是 学思想、学关键流程
- 由于代码沙箱是能够通过 API调用的独立服务，所以新建一个Spring Boot Web 项目。最终这个项目要提供一个能够执行代码、操作代码沙箱的接口。
- 将 zhaoli-oj-backend 项目的 Judgelnfo 类移到 model 目录下，然后复制 model 包和 CodeSandbox 接囗到该沙箱项目，便于字段的统一。

## Java 原生实现代码沙箱

原生:尽可能不借助第三方库和依赖，用最干净、最原始的方式实现代码沙箱

### 通过命令行执行

1. **Java 程序执行流程**
   > 接收代码=>编译代码(javac)=>执行代码(java)

- 先编写示例代码，注意要去掉包名，放到resources 目录下
- **用javac 命令编译代码**<br>
    ```shell
    javac {Java代码路径)
    ```
- **用java 命令执行代码**<br>
    ```shell
    {编译后的class文件所在路径}SimpleCompute12java -cp
    ```

2. **程序中文乱码问题**<br>
   为什么编译后的 class 文件出现中文乱码呢?<br>
   原因:命令行终端的编码是 GBK，和java 代码文件本身的编码 UTF-8 不一致，导致乱码。<br>
   通过chcp 命令查看命令行编码，GBK是936，UTF-8是65001。<br>
   但是 **不建议** 大家改变终端编码来解决编译乱码，因为其他运行你代码的人也要改变环境，兼容性很差。<br>
   **推荐的 javac 编译命令**，用```-encoding utf-8``` 参数解决。
3. **统一类名**<br>
   实际的 OJ系统中，对用户输入的代码会有一定的要求。便于系统进行统一处理和判题。<br>
   此处我们把用户输入代码的类名限制为 Main(参考 Poj)
   ，可以减少编译时类名不一致的风险;而且不用从用户代码中提取类名，更方便。<br>
   文件名 **Main.java**<br>
   实际执行命令时，可以统一使用 Main 类名:
    ```shell
    javac -encoding utf-8 .Main.java
    java-cp.Main12
    ```

### 核心流程实现