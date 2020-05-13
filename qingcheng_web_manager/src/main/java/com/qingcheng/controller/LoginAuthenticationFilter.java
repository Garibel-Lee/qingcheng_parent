package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.utils.WebUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class LoginAuthenticationFilter implements AuthenticationSuccessHandler {

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        System.out.println("经过过滤器");
        String username = authentication.getName();  //  request.getParameter("username");
        //浏览器代理信息
        String agent =request.getHeader("user-agent").toLowerCase();
       //获取浏览器名称
        String browserName=getBrowserName(agent);
        //获取访问ip地址
        String ip = request.getRemoteAddr();

        LoginLog loginLog = new LoginLog();
        loginLog.setIp(ip);
        loginLog.setLoginName(username);
        loginLog.setBrowserName(browserName);
        loginLog.setLoginTime(new Date());
        loginLog.setLocation(WebUtil.getCityByIP(ip));//保存城市信息
        loginLogService.add(loginLog);
        request.getRequestDispatcher("/index.html").forward(request, response);
    }


    //浏览器类型
    private String getBrowserName(String agent) {
        if (agent.indexOf("msie 7") > 0) {
            return "ie7";
        } else if (agent.indexOf("msie 8") > 0) {
            return "ie8";
        } else if (agent.indexOf("msie 9") > 0) {
            return "ie9";
        } else if (agent.indexOf("msie 10") > 0) {
            return "ie10";
        } else if (agent.indexOf("msie") > 0) {
            return "ie";
        } else if (agent.indexOf("opera") > 0) {
            return "opera";
        } else if (agent.indexOf("opera") > 0) {
            return "opera";
        } else if (agent.indexOf("firefox") > 0) {
            return "firefox";
        } else if (agent.indexOf("webkit") > 0) {
            return "webkit";
        } else if (agent.indexOf("gecko") > 0 && agent.indexOf("rv:11") > 0) {
            return "ie11";
        } else {
            return "Others";
        }
    }

}


/**
 *  System.out.println("客户端系统名称："+System.getProperty("os.name"));
 System.out.println("客户端系统版本："+System.getProperty("os.version"));
 System.out.println("客户端操作系统位数："+System.getProperty("os.arch"));
 System.out.println("HTTP协议版本："+request.getProtocol());
 System.out.println("请求编码格式："+request.getCharacterEncoding());
 System.out.println("Accept："+request.getHeader("Accept"));
 System.out.println("Accept-语言："+request.getHeader("Accept-Language"));
 System.out.println("Accept-编码："+request.getHeader("Accept-Encoding"));
 System.out.println("Connection："+request.getHeader("Connection"));
 System.out.println("Cookie："+request.getHeader("Cookie"));
 System.out.println("客户端发出请求时的完整URL"+request.getRequestURL());
 System.out.println("请求行中的资源名部分"+request.getRequestURI());
 System.out.println("请求行中的参数部分"+request.getRemoteAddr());
 System.out.println("客户机所使用的网络端口号"+request.getRemotePort());
 System.out.println("WEB服务器的IP地址"+request.getLocalAddr());
 System.out.println("WEB服务器的主机名"+request.getLocalName());
 System.out.println("客户机请求方式"+request.getMethod());
 System.out.println("请求的文件的路径"+request.getServerName());
 System.out.println("请求体的数据流"+request.getReader());
 BufferedReader br=request.getReader();
 String res = "";
 while ((res = br.readLine()) != null) {
 System.out.println("request body:" + res);
 }
 System.out.println("请求所使用的协议名称"+request.getProtocol());
 System.out.println("请求中所有参数的名字"+request.getParameterNames());
 Enumeration enumNames= request.getParameterNames();
 while (enumNames.hasMoreElements()) {
 String key = (String) enumNames.nextElement();
 System.out.println("参数名称："+key);
 }

 System.out.println("---------------------------------------------------------------------------------------------");
 System.out.println("---------------------------------------------------------------------------------------------");
 System.out.println("---------------------------------------------------------------------------------------------");
 System.out.println("---------------------------------------------------------------------------------------------");
 System.out.println("---------------------------------------------------------------------------------------------");
 */

