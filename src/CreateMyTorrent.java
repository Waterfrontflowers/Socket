import org.apache.commons.io.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
/**
 * @author Ginger
 * @date 2021/12/12
 */
public class CreateMyTorrent {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入服务器数量：");
        String str  = scanner.nextLine();
        int num = 0;
        while(true) {
            try {
                num = Integer.parseInt(str);
                if(num>0) {
                    break;
                }
                str = scanner.nextLine();
            } catch (Exception e) {
                System.out.println(e);
                str = scanner.nextLine();
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hostNumber",num);
        //final String torrentUrl;
        JSONArray jsonArray = new JSONArray();
        JSONObject jsHost = new JSONObject();
        System.out.print("请输入socket套接字：");
        String host = scanner.nextLine();
        String rule = ":";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(host);
        if(matcher.find()) {
            int port = Integer.parseInt(host.substring(matcher.end()));
            host = host.substring(0,matcher.start());
            jsHost.put("host",host);
            jsHost.put("port",port);
        }
        System.out.print("请输入路径：");
        String url = scanner.nextLine();
        jsHost.put("url",url);
        jsonArray.put(jsHost);
        url = url.replace("\\", "\\\\");
        //System.out.println(jsonArray);

        File resource = new File(url);
        jsonObject.put("size",FileUtils.sizeOf(resource));
        jsonObject.put("SHA512",Hash(FileUtils.readFileToString(resource,StandardCharsets.ISO_8859_1)));

        rule = "\\\\\\\\";
        pattern = Pattern.compile(rule);
        matcher = pattern.matcher(url);
        int end = 0;
        while (matcher.find()) {
            end = matcher.end();
        }
        String title = url.substring(end);
        rule = "\\.";
        pattern = Pattern.compile(rule);
        matcher = pattern.matcher(title);
        int dotEnd = title.length();
        while (matcher.find()) {
            dotEnd = matcher.start();
        }
        title = title.substring(0, dotEnd);

        final File file = new File(url.substring(0, end) + title + ".myTorrent");
        FileUtils.touch(file);

        for(int i = 1 ;i < num ;i ++){
            System.out.print("请输入socket套接字：");
            host = scanner.nextLine();
            rule = ":";
            pattern = Pattern.compile(rule);
            matcher = pattern.matcher(host);
            if(matcher.find()) {
                int port = Integer.parseInt(host.substring(matcher.end()));
                host = host.substring(0,matcher.start());
                jsHost = new JSONObject();
                jsHost.put("host",host);
                jsHost.put("port",port);
            }
            System.out.print("请输入路径：");
            url = scanner.nextLine();
            jsHost.put("url",url);
            jsonArray.put(jsHost);
        }

        jsonObject.put("hosts",jsonArray);
        System.out.println(jsonObject);

        //System.out.println(url.substring(0,end) + title + ".myTorrent");
        FileUtils.writeStringToFile(file,jsonObject.toString(4), StandardCharsets.US_ASCII);

    }

    public static String Hash(String plainText) throws NoSuchAlgorithmException {

        // 获取指定摘要算法的messageDigest对象
        MessageDigest messageDigest = MessageDigest.getInstance("SHA512"); // 此处的sha代表sha1

        // 调用digest方法，进行加密操作
        byte[] cipherBytes = messageDigest.digest(plainText.getBytes());

        String cipherStr = Hex.encodeHexString(cipherBytes);

        return cipherStr;
    }
}
