import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author Ginger
 * @date 2021/12/11
 */
public class Client {
    final static int BLOCK_SIZE =  262144;
    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {

        System.out.print("请输入myTorrent的文件路径：");
        Scanner scanner = new Scanner(System.in);
        String torrent = scanner.nextLine();
        torrent.replace("\\","\\\\");
        JSONObject jsonObject = new JSONObject(FileUtils.readFileToString(new File(torrent),StandardCharsets.UTF_8));
        System.out.println(jsonObject);
        HashMap<String,String> hashMap = new HashMap<>(3);
        hashMap.put("hostNumber",jsonObject.get("hostNumber").toString());
        hashMap.put("size",jsonObject.get("size").toString());
        hashMap.put("SHA512",jsonObject.get("SHA512").toString());
        JSONArray hosts = jsonObject.getJSONArray("hosts");

        String rule = "\\\\";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(((JSONObject)hosts.get(0)).get("url").toString());
        int fileNameStart = 0;
        while (matcher.find()){
            fileNameStart = matcher.end();
        }
        String fileName = ((JSONObject)hosts.get(0)).get("url").toString().substring(fileNameStart);

        final int BLOCK_MAX = (Long.parseLong(hashMap.get("size")) % BLOCK_SIZE)!=0 ? (int) (Long.parseLong(hashMap.get("size")) / BLOCK_SIZE) + 1 : (int) (Long.parseLong(hashMap.get("size")) / BLOCK_SIZE);
        System.out.println("准备创建" + BLOCK_MAX +"块");
        StringBuffer[] fileString = new StringBuffer[BLOCK_MAX];

        DownloadThread[] downloadThreads = new DownloadThread[Integer.parseInt(hashMap.get("hostNumber"))];
        JSONObject[] hostUrl = new JSONObject[Integer.parseInt(hashMap.get("hostNumber"))];
        for(int i = 0 ;  i < Integer.parseInt(hashMap.get("hostNumber")); i++){
            hostUrl[i] = (JSONObject) hosts.get(i);
        }
        CountDownLatch latch = new CountDownLatch(BLOCK_MAX);
        ArrayList<DownloadThread> threads = new ArrayList<>(BLOCK_MAX);
        int i = 0;
        while(i < BLOCK_MAX){
            for(int j = 0 ;  j < Integer.parseInt(hashMap.get("hostNumber")) && i < BLOCK_MAX; j++) {
                if (downloadThreads[j] == null || !downloadThreads[j].isAlive()){
                    downloadThreads[j] = new DownloadThread("host"+ j ,hostUrl[j] ,i,fileString,Integer.parseInt(hashMap.get("size")),latch,threads);
                    threads.add(downloadThreads[j]);
                    downloadThreads[j].start();
                    i++;
                }
            }
        }


        latch.await();

        StringBuilder receive = new StringBuilder();
        for (StringBuffer s : fileString) {
            receive.append(s.toString());
        }
        System.out.println("正在进行SHA512校验");
        if(Hash(receive.toString()).equals(hashMap.get("SHA512"))) {
            File file = new File(fileName);
            FileUtils.touch(file);
            FileUtils.writeStringToFile(file, receive.toString(), StandardCharsets.ISO_8859_1, false);
            System.out.println("校验通过，文件已存储。");
        }
        else {
            System.out.println("校验失败！SHA512:" + Hash(receive.toString()));
        }

    }

    public static String Hash(String plainText) throws NoSuchAlgorithmException {

        // 获取指定摘要算法的messageDigest对象
        MessageDigest messageDigest = MessageDigest.getInstance("SHA512"); // 此处的sha代表sha1

        // 调用digest方法，进行加密操作
        byte[] cipherBytes = messageDigest.digest(plainText.getBytes());

        return Hex.encodeHexString(cipherBytes);
    }
}

class DownloadThread extends Thread{
    private Thread thread;
    private final String threadName;
    private JSONObject host;
    private final int block;
    private final StringBuffer[] fileString;
    final static int BLOCK_SIZE = 262144;
    private final int fileSize;
    private final CountDownLatch latch;
    private final ArrayList<DownloadThread> threads;
    private boolean hostError;


    public DownloadThread(String threadName,JSONObject host,int block,StringBuffer[] fileString,int fileSize,CountDownLatch latch ,ArrayList<DownloadThread> threads){
        this.threadName = threadName;
        this.host = host;
        this.block = block;
        this.fileString = fileString;
        this.fileSize = fileSize;
        this.latch = latch;
        this.threads = threads;
    }

    @Override
    public void start(){
        if(this.thread == null){
            this.thread = new Thread(this,this.threadName);
            thread.start();
        }
    }

    @Override
    public void run(){
        while(true) {
            System.out.println("正在尝试向 " + this.host.get("host") + " 获取第 " + this.block + "块文件");
            Socket socket;
            try {
                //主机ip
                String host = this.host.get("host").toString();

                //主机端口号
                int port = Integer.parseInt(this.host.get("port").toString());

                //创建套接字,套接字是传输层Tcp像应用层Http开的一个编程接口，开发人员主要是通过套接字对tcp进行编程
                socket = new Socket(host, port);

                //向服务端发起一个请求，通过socket创建io输出流
                OutputStream outputStream = socket.getOutputStream();

                //通过io输出流创建数据输出流
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);


                int start = BLOCK_SIZE * this.block;
                int end = Math.min(BLOCK_SIZE * (this.block + 1), fileSize);
                String url = this.host.get("url").toString();
                String blockUrl = url + " /" + start + " /" + end;

                //发起请求，这里直接传了一个hello过去
                dataOutputStream.writeUTF(blockUrl);

                //通过socket创建io输入流
                InputStream inputStream = socket.getInputStream();

                //通过io输入流创建数据输入流
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                //接收服务端的响应
                StringBuilder receive = new StringBuilder();
                String s;
                while (!"theJpgIsNowFinish".equals(s = dataInputStream.readUTF())) {
                    receive.append(s);
                }
                this.fileString[this.block] = new StringBuffer();
                this.fileString[this.block].append(receive);

                System.out.println("第 " + this.block + " 块文件获取成功，来自 " + this.host.get("host"));
                //关闭数据传输流
                dataOutputStream.close();
                dataInputStream.close();
                latch.countDown();
                break;

            } catch (IOException e) {
                System.out.println(this.host.get("host") + " 服务器连接异常，正在将请求交付其他服务器");
                hostError = true;
                Random random = new Random();
                int index = random.nextInt(threads.size());
                boolean unchanged = true;
                do {

                    if(!this.host.toString().equals(threads.get(index).getHost().toString())) {
                        this.host = threads.get(index).getHost();
                        unchanged = false;
                    }
                    index = random.nextInt(threads.size());
                }while (!threads.get(index).isHostError() || unchanged);

                System.out.println("第 " + this.block + " 块任务尝试交给 " + this.host.get("host"));
            }
        }
    }

    public JSONObject getHost() {
        return host;
    }

    public boolean isHostError() {
        return hostError;
    }
}
