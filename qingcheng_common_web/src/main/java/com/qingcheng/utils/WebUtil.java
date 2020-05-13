package com.qingcheng.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WebUtil {


    //GET字符集设置处理
    public static Map<String, String> convertCharsetToUTF8(Map<String, String> searchMap) throws Exception {
        Iterator<Map.Entry<String, String>> entries = searchMap.entrySet().iterator();
        Map map = new HashMap();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            map.put(new String(entry.getKey().getBytes("ISO8859-1"), "UTF-8"), new String(entry.getValue().getBytes("ISO8859-1"), "UTF-8"));
        }
        return map;
    }

    public static String getCityByIP(String ip) throws JSONException {
        if(ip.equals("0:0:0:0:0:0:0:1")){
            return "本地";
        }
        try {
            URL url = new URL("http://opendata.baidu.com/api.php?query=" + ip + "&co=&resource_id=6006&t=1433920989928&ie=utf8&oe=utf-8&format=json");
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            String line = null;
            StringBuffer result = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            JSONObject jsStr = JSONObject.parseObject(result.toString());
            JSONArray jsData = (JSONArray) jsStr.get("data");
            JSONObject data = (JSONObject) jsData.get(0);//位置
            return (String) data.get("location");
        } catch (IOException e) {
            return "读取失败";
        }
    }

}
