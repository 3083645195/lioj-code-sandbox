import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 运行其他程序（比如危险木马）
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String userDir = System.getProperty("user.dir");
//        String userDir="D:\\java\\鱼皮\\项目\\OJ判题系统\\lioj-code-sandbox";
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
    }
}