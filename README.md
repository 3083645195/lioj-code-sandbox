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

### 1. 占用时间资源，导致程序卡死，不释放资源

无限睡眠(阻塞程序执行)

```
long ONE HOUR = 60 * 60* 1000L;
Thread.sleep(ONE HOUR);
```

### 2. 占用内存

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
如图:<br>![img_1](https://zhaoli-image.oss-cn-beijing.aliyuncs.com/img/img_1.png)

### 3. 读文件，信息泄露

比如直接通过相对路径获取项目配置文件，获取到密码

```
String userDir=System.getProperty(user.dir");
String filePath = userDir + file.separator + "src/main/resources/application.yml";
List<string>allLines = Files.readAllLines(Paths.get(filePath));
System.out.println(String.join("\n",allLines));
```

### 4. 写文件，植入木马

可以直接向服务器上写入文件，比如一个木马程序:```java -version 2>&1```(示例命令)

> 1.java -version 用于显示 Java 版本信息。这会将版本信息输出到标准错误流(stderr)而不是标准输出流(stdout)<br>
> 2.2>&1 将标准错误流重定向到标准输出流。这样，Java版本信息就会被发送到标准输出流。

```
String userDir = System.getProperty("user.dir");
String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
String errorProgram = "java - version 2 > & 1 ";
Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
```

### 5. 运行其他程序

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

### 6. 执行高危操作

甚至都不用写木马文件，直接执行系统自带的危险命令!

- 比如删除服务器的所有文件
- 比如执行 dir(windows)、ls(linux)获取你系统上的所有文件信息

## Java 程序安全控制

### 1. 超时控制<br>

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

### 2. 限制资源分配

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
> Java 进程放置在特定的cgroup 中，你可以实现限制其使用的内存和 CPU 数。

### 3. 限制代码-黑白名单

实现:先定义一个黑白名单，比如哪些操作是禁止的，可以就是一个列表

   ```
   /**
    * 用户代码执行黑名单
    */
   private static final List<String> BLACK_LIST = Arrays.asList("Files","exec");
   ```

还可以使用字典树代替列表存储单词，用 **更少的空间** 存储更多的敏感词汇，并且实现 **更高效** 的敏感词查找<br>
字典树的原理:<br>
![img_2](https://zhaoli-image.oss-cn-beijing.aliyuncs.com/img/img_2.png)
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

### 4. 限制权限-Java 安全管理器

目标:限制用户对文件、内存、CPU、网络等资源的操作和访问。<br>
**Java 安全管理器使用**<br>
Java 安全管理器(Security Manager)是Java 提供的保护JM、Java 安全的机制，可以实现更严格的资源和操作限制。<br>

编写安全管理器，只需要继承 Security Manager

    - 所有权限放开
    - 所有权限拒绝
    - 限制读权限
    - 限制写文件权限
    - 限制执行文件权限
    - 限制网络连接权限

**结合项目运用**
实际情况下，不应该在主类(开发者自己写的程序)
中做限制，只需要限制子程序的权限即可。启动子进程执行命令时，设置安全管理器，而不是在外层设置(
会限制住测试用例的读写和子命令的执行)<br>
具体操作如下:

1. 根据需要开发自定义的安全管理器(比如 MySecurityManager)
2. 复制 MySecurityManager类到 resources/security 目录下，移除类的包名
3. 手动输入命令编译 MySecurityManager 类，得到 class 文件
4. 在运行java 程序时，指定安全管理器 class 文件的路径、安全管理器的名称。

> 注意，windows 下要用分号间隔多个类路径!

```
 /**
  * security 所在的路径
  */
 private static final String SECURITY_MANAGER_PATH = "D:\\java\\项目\\OJ判题系统\\lioj-code-sandbox\\src\\main\\resources\\security";
 /**
  * SecurityManager.class 的名称
  */
 private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

 String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
```

依次执行之前的所有测试用例，发现资源成功被限制。
![img_3](https://zhaoli-image.oss-cn-beijing.aliyuncs.com/img/img_3.png)

**安全管理器优点**

- 权限控制很灵活
- 实现简单

**安全管理器缺点**

- 如果要做比较严格的权限限制，需要自己去判断哪些文件、包名需要允许读写。粒度太细了，难以精细化控制。
- 安全管理器本身也是Java 代码，也有可能存在漏洞。本质上还是程序层面的限制，没深入系统的层面。

### 5. 运行环境隔离

原理:操作系统层面上，把用户程序封装到沙箱里，和宿主机(我们的电脑/服务器)隔离开，使得用户的程序无法影响宿主机。<br>
实现方式:Docker容器技术(底层是用 cgroup、namespace 等方式实现的)，也可以直接使用cgroup 实现。

## 代码沙箱 Docker 实现(非本人实现)

### Docker 容器技术

为什么要用 Docker 容器技术?<br>
为了进一步提升系统的安全性，把不同的程序和宿主机进行隔离，使得某个程序应用)的执行不会影响到系统本身。<br>
Docker 技术可以实现程序和宿主机的隔离

### Docker 实现核心

Docker 能实现哪些资源的隔离?<br>
看图理解:

1. Docker运行在 Linux 内核上
2. CGroups:实现了容器的资源隔离，底层是 Linux Cgroup 命令，能够控制进程使用的资源
3. Network 网络:实现容器的网络隔离，docker 容器内部的网络互不影响
4. Namespaces 命名空间:可以把进程隔离在不同的命名空间下，每个容器他都可以有自己的命名空间，不同的命名空间下的进程互不影响。
5. Storage 存储空间:容器内的文件是相互隔离的，也可以去使用宿主机的文件
   ![img_4](https://zhaoli-image.oss-cn-beijing.aliyuncs.com/img/img_4.png)

### Java 操作 Docker

**前置准备**<br>
使用 Docker-Java:hhttps://github.com/docker-java/docker-java
官方入门: https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md

- DockerClientConfig:用于定义初始化 DockerClient 的配置(类比 MySQL 的连接、线程数配置)
- DockerHttpClient:用于向 Docker 守护进程(操作 Docker 的接口)发送请求的客户端，低层封装(不推荐使用)，你要自己构建请求参数(
  简单地理解成JDBC)
- DockerClient(推荐):才是真正和 Docker 守护进程交互的、最方便的 SDK，高层封装，对DockerHttpClient 再进行了一层封装(理解成
  MyBatis)，提供了现成的增删改查

### Docker 实现代码沙箱

实现思路:docker负责运行java 程序，并且得到结果<br>
流程几乎和 Java 原生实现流程相同:

1. 把用户的代码保存为文件
2. 编译代码，得到 class 文件
3. 把编译好的文件上传到容器环境内
4. 在容器中执行代码，得到输出结果
5. 收集整理输出结果
6. 文件清理，释放空间
7. 错误处理，提升程序健壮性

### Docker 容器安全性

**超时控制**<br>
执行容器时，可以增加超时参数控制值<br>
但是，这种方式无论超时与否，都会往下执行，无法判断是否超时<br>
解决方案:可以定义一个标志，如果程序执行完成，把超时标志设置为 false。<br>
**内存资源**<br>
通过 HostConfig 的 withMemory等方法，设置容器的最大内存和资源限制<br>
**网络资源**<br>
创建容器时，设置网络配置为关闭<br>
**权限管理**<br>
Docker 容器已经做了系统层面的隔离，比较安全，但不能保证绝对安全

- 结合 Java 安全管理器和其他策略去使用
- 限制用户不能向 root 根目录写文件
- Linux 自带的一些安全管理措施，比如 seccomp(Secure Computing Mode)是一个用于 Linux
  内核的安全功能，它允许你限制进程可以执行的系统调用，从而减少潜在的攻击面和提高容器的安全性。通过配置seccomp，你可以控制容器内进程可以使用的系统调用类型和参数。
- 在 hostConfig 中开启安全机制

## 模板方法优化代码沙箱

1. 模板方法:定义一套通用的执行流程，让子类负责每个执行步骤的具体实现模板方法的
2. 适用场景:适用于有规范的流程，且执行流程可以复用
3. 作用:大幅节省重复代码量，便于项目扩展、更好维护 <br>

**1、抽象出具体的流程**<br>
定义一个模板方法抽象类。<br>
先复制具体的实现类，把代码从完整的方法抽离成一个一个子写法<br>
**2、定义子类的具体实现**<br>
Java 原生代码沙箱实现，直接复用模板方法定义好的方法实现<br>
Docker 代码沙箱实现，需要自行重写 RunFile<br>

## 给代码沙箱提供开放 API

直接在 controller 暴露 CodeSandbox 定义的接口

### 调用安全性

如果将服务不做任何的权限校验，直接发到公网，是不安全的。

1. 调用方与服务提供方之间约定一个字符串(**最好加密**)

- 优点:实现最简单，比较适合内部系统之间相互调用(相对可信的环境内部调用)
- 缺点:不够灵活，如果 key 泄露或变更，需要重启代码

2. API签名认证<br>
给允许调用的人员分配 accessKey、secretKey，然后校验这两组 key 是否匹配。