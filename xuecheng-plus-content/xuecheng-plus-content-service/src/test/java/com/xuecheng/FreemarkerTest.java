package com.xuecheng;

import com.alibaba.fastjson.JSONObject;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.vo.CourseCategoryTreeVO;
import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class FreemarkerTest {
    @Autowired
    CoursePublishService coursePublishService;

    @Test
    public void testFreeMarker() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());
        String classpath = this.getClass().getResource("/").getPath();
        configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates"));
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate("course_template.ftl");
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(121L);

        Map<String, Object> map = new HashMap<>();
        map.put("model",coursePreviewInfo);

        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template,map);

        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        FileOutputStream fileOutputStream = new FileOutputStream(new File("D:\\121.html"));
        IOUtils.copy(inputStream,fileOutputStream);

    }
}
