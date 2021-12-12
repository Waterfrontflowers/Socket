import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import org.apache.commons.io.*;
/**
 * @author Ginger
 * @date 2021/12/11
 */
public class Server {
    public static final int WRITE_READ_UTF_MAX_LENGTH = 10000;
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入开放端口：");
        int port = scanner.nextInt();
        ServerSocket serverSocket = new ServerSocket(port);
        while(true) {
            try {
                //创建一个ServerSocket,负责服务端这边监听对应端口号

                //接收客户端的请求，在没接收到请求会一直处于监听的状态
                Socket socket = serverSocket.accept();

                //处理客户端的请求,通过socket创建io输入流
                InputStream inputStream = socket.getInputStream();

                //通过io输入流创建数据传输流
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                //获取请求数据
                String url = dataInputStream.readUTF();
                System.out.println("服务端接收到的数据：[ " + url + " ]");

                String rule = " /\\d+";
                Pattern pattern = Pattern.compile(rule);
                Matcher matcher = pattern.matcher(url);
                int start = 0,end = 0;
                int matcherStart = 0;
                String startEnd="";
                if(matcher.find()){
                    matcherStart = matcher.start();
                    start = Integer.parseInt(matcher.group().substring(2));
                }
                if(matcher.find()){
                    end = Integer.parseInt(matcher.group().substring(2));
                }
                url = url.substring(0,matcherStart);

                //给客户端回写数据
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                String jpg = fileRead(url).substring(start,end);
                //给客户端响应数据
                //dataOutputStream.writeUTF(fileRead(s));

                //如果超过限定长度，将进行截取多次写出
                if (jpg.length() > WRITE_READ_UTF_MAX_LENGTH) {
                    for (int i = 1; i < jpg.length() / WRITE_READ_UTF_MAX_LENGTH + 2; i++) {
                        dataOutputStream.writeUTF(jpg.substring(WRITE_READ_UTF_MAX_LENGTH * (i - 1), WRITE_READ_UTF_MAX_LENGTH * i < jpg.length() ? WRITE_READ_UTF_MAX_LENGTH * i : jpg.length()));
                    }
                } else {
                    //长度在0-65535默认写出
                    dataOutputStream.writeUTF(jpg);
                }
                dataOutputStream.writeUTF("theJpgIsNowFinish");
                //关闭资源
                System.out.println("发送已结束！");
                dataInputStream.close();
                dataOutputStream.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String fileRead(String url) throws IOException {
        String str = FileUtils.readFileToString(new File(url), StandardCharsets.ISO_8859_1);
        return str;
    }

}
