package com.xuecheng;

import com.alibaba.fastjson.JSONObject;
import com.xuecheng.content.model.vo.CourseCategoryTreeVO;
import com.xuecheng.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ContentCategoryServiceTest {
    @Autowired
    private CourseCategoryService courseCategoryService;

    @Test
    void testQueryTreeNodes(){
        List<CourseCategoryTreeVO> courseCategoryTreeVOS = courseCategoryService.queryTreeNodes("1");
        // 打印结果
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("courseCategoryTreeVOS", courseCategoryTreeVOS);
        System.out.println(jsonObject.toJSONString());
    }
}
