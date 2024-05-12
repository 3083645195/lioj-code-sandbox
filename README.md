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
   此处我们把用户输入代码的类名限制为 com.zhaoli.liojcodesandbox.testCode.simpleComputeArgs.Main(参考 Poj)
   ，可以减少编译时类名不一致的风险;而且不用从用户代码中提取类名，更方便。<br>
   文件名 **com.zhaoli.liojcodesandbox.testCode.simpleComputeArgs.Main.java**<br>
   实际执行命令时，可以统一使用 com.zhaoli.liojcodesandbox.testCode.simpleComputeArgs.Main 类名:
    ```shell
    javac -encoding utf-8 .com.zhaoli.liojcodesandbox.testCode.simpleComputeArgs.Main.java
    java-cp.Main12
    ```

### 核心流程实现

核心实现思路:用程序代替人工，用程序来操作命令行，去编译执行代码核心依赖:Java 进程类 Process

1. 把用户的代码保存为文件

- 引入 Hutool 工具类，提高操作文件效率
- 新建目录，将每个用户的代码都存放在独立目录下，通过 UUID 随机生成目录名，便于隔离和维护

2. 编译代码，得到 class 文件

- 使用 Process 类在终端执行命令
- 执行 process.waitFor等待程序执行完成，并通过返回的 exitValue 判断程序是否正常返回，然后从 Process 的输入流 inputStream
  和错误流 errorStream 获取控制台输出。
- 可以把上述实现的代码提取为工具类 ProcessUtils，执行进程并获取输出，并且使用 StringBuilder 拼接控制台输出信息

3. 执行代码，得到输出结果

- 同样是使用 Process 类运行 java 命令， 命令中增加```-Dfile.encoding=UTF-8``` 参数，解决中文乱码
- 上述命令适用于执行从输入参数(args)中获取值的代码
- 很多 OJ都是 ACM 模式，需要和用户交互，让用户不断输入内容并获取输出
    - 对于此类程序，我们需要使用 OutputStream 向程序终端发送参数，并及时获取结果，注意最后要关闭流释放资源。

4. 收集整理输出结果
    1. 通过 for 循环遍历执行结果，从中获取输出列表
    2. 获取程序执行时间

    - 可以使用 Spring 的 StopWatch 获取一段程序的执行时间
        ```
        StopWatch stopWatch=new stopwatch();
        stopWatch.start();
        //程序执行...
        stopWatch.stop();
        stopWatch.getLastTaskTimeMillis();//获取时间间
        ```
    - 此处我们使用最大值来统计时间，便于后续判题服务计算程序是否超时

    3. 获取内存信息

    - 实现比较复杂，因为无法从 Process 对象中获取到子进程号，也不推荐在 Java 原生实现代码沙箱的过程中获取
5. 文件清理，释放空间

- 防止服务器空间不足，删除代码目录

6. 错误处理，提升程序健壮性

- 封装一个错误处理方法，当程序抛出异常时，直接返回错误响应

## Java 程序异常情况

> 到目前为止，核心流程已经实现，但是想要上线的话，安全么?<br>
> 用户提交恶意代码，怎么办?

1. 占用时间资源，导致程序卡死，不释放资源<br>
   无限睡眠(阻塞程序执行)

```
long ONE HOUR = 60 * 60* 1000L;
Thread.sleep(ONE HOUR);
```

2. 占用内存<br>
   占用内存资源，导致空间浪费

```
List<byte[]> bytes = new ArrayList<>();
while(true){
    bytes .add(new byte[10000]);
}
```

实际运行上述程序时，我们会发现，内存占用到达一定空间后，程序就自动报错:```.java.lang.0utOfMemoryError:Java heap space```
，而不是无限增加内存占用，直到系统死机。<br>
**这是 JVM 的一个保护机制**。
可以使用 IisualVM 或JConsole 工具，连接到 IM 虚拟机上来可视化查看运行状态<br>
如图:<br>
![img_1.png](doc%2Fimg_1.png)

3. 读文件，信息泄露<br>
   比如直接通过相对路径获取项目配置文件，获取到密码

```
String userDir=System.getProperty(user.dir");
String filePath = userDir + file.separator + "src/main/resources/application.yml";
List<string>allLines = Files.readAllLines(Paths.get(filePath));
System.out.println(String.join("\n",allLines));
```

4. 写文件，植入木马<br>
   可以直接向服务器上写入文件，比如一个木马程序:```java -version 2>&1```(示例命令)

> 1.java -version 用于显示 Java 版本信息。这会将版本信息输出到标准错误流(stderr)而不是标准输出流(stdout)<br>
> 2.2>&1 将标准错误流重定向到标准输出流。这样，Java版本信息就会被发送到标准输出流。

```
String userDir = System.getProperty("user.dir");
String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
String errorProgram = "java - version 2 > & 1 ";
Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
```

5. 运行其他程序<br>
   直接通过 Process 执行危险程序，或者电脑上的其他程序

```
String userDir = System.getProperty("user.dir");
String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
Process process = Runtime.getRuntime().exec(filePath);
process.waitFor();
//分批获取进程的正常输出
BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//逐行读取
String compile0utputLine;
while ((compile0utputLine = bufferedReader.readLine()) != null) {
    System.out.println(compile0utputLine);
    System.out.println("执行异常程序成功");
}
```

6. 执行高危操作<br>
   甚至都不用写木马文件，直接执行系统自带的危险命令!

- 比如删除服务器的所有文件(太残暴、不演示)
- 比如执行 dir(windows)、ls(linux)获取你系统上的所有文件信息

## Java 程序安全控制

1.超时控制<br>
通过创建一个守护线程，超时后自动中断 Process 实现

   ```
   //执行外部命令，并设置了一个超时时间，在超时之后如果进程仍未执行完毕，则销毁这个进程
    new Thread(()->{
        try {
            Thread.sleep(TIME_OUT);
        } catch (InterruptedException e) {
           throw new IllegalStateException();
        }
        if (runProcess.isAlive()) {
            System.out.println("超时了，中断！");
            runProcess.destroy();
        }
    }).start();
   ```

2. 限制资源分配<br>
   我们不能让每个 java 进程的执行占用的 JM 最大堆内存空间都和系统默认的一致(我的 JM 默认最大占用8G 内存)，实际上应该更小(
   执行用户的题目代码也不需要这么多)，比如说 512MB。<br>
   在启动 Java 程序时，可以指定JVM 的参数:-Xmx256m(最大堆空间大小)示例命令如下

   ```
   java -Xmx256m
   ```

   **注意!-Xmx 参数、JM 的堆内存限制，不等同于系统实际占用的最大资源，可能会超出**<br>
   如果需要更严格的内存限制，要在**系统层面**去限制，而不是 JVM 层面的限制。<br>
   如果是 Linux 系统，可以使用 cgroup 来实现对某个进程的 CPU、内存等资源的分配。

   > **cgroup** 是 Linux 内核提供的一种机制，可以用来限制进程组(包括子进程)的资源使用，例如内存、CPU、磁盘 I/0 等。通过将
   Java 进程放置在特定的cgroup 中，你可以实现限制其使用的内存和 CPU 数。
3. 限制代码-黑白名单<br>
   实现:先定义一个黑白名单，比如哪些操作是禁止的，可以就是一个列表
   ```
   /**
    * 用户代码执行黑名单
    */
   private static final List<String> BLACK_LIST = Arrays.asList("Files","exec");
   ```
   还可以使用字典树代替列表存储单词，用**更少的空间**存储更多的敏感词汇，并且实现**更高效**的敏感词查找<br>
   字典树的原理:<br>
   ![img_2.png](doc%2Fimg_2.png)
   此处使用 HuTool 工具库的字典树工具类:WordTree，不用自己写字典树!<br>

- 先初始化字典树，插入禁用词:

```
private static final WordTree WORD_TREE;

static {
  //初始化字典树
  WORD_TREE = new WordTree();
  WORD_TREE.addWords(BLACK_LIST);
}
```

- 校验用户代码是否包含禁用词:

```
//校验用户代码是否有黑名单中的操作
FoundWord foundWord = WORD_TREE.matchWord(code);
if(foundWord!=null){
   System.out.println("包含敏感词："+foundWord.getFoundWord());
   return null;
}
```

**本方案缺点**

- 你无法遍历所有的黑名单
- 不同的编程语言，你对应的领域、关键词都不一样，限制人工成本很大