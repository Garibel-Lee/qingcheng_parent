package com.qingcheng.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.utils.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@Controller
public class SearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    public String search(Model model, @RequestParam Map<String, String> searchMap) throws Exception {
        //字符集处理
        searchMap = WebUtil.convertCharsetToUTF8(searchMap);

        //页码容错处理
        if(searchMap.get("pageNo")==null){
            searchMap.put("pageNo","1");//设置页码默认值
        }
        //排序参数容错处理
        if(searchMap.get("sortRule")==null){
            searchMap.put("sortRule","desc");
        }
        if(searchMap.get("sortField")==null){
            searchMap.put("sortField","");
        }

        //远程调用接口
        Map result = skuSearchService.search(searchMap);
        model.addAttribute("result", result);
        //返回查询对象
        model.addAttribute("searchMap", searchMap);

        //返回的url字符串
        StringBuffer url = new StringBuffer("/search.do?");
        for (String key : searchMap.keySet()) {
            url.append("&" + key + "=" + searchMap.get(key));
        }
        model.addAttribute("url",url.toString());

        //当前页码
        int pageNo =Integer.parseInt(searchMap.get("pageNo"))  ;//当前页
        model.addAttribute("pageNo",pageNo);

        //页码显示优化
        int totalPages= ((Long) result.get("totalPages")).intValue();//得到总页数
        int startPage=1;//开始页码
        int endPage=totalPages;//截至页码
        if(totalPages>7){
            //得到当前页
            startPage=pageNo-2;
            //负数处理（如果开始页码小于1）
            if(startPage<1){
                startPage=1;
            }
            endPage=startPage+4;
        }
        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endPage);


        return "search";
    }


}
