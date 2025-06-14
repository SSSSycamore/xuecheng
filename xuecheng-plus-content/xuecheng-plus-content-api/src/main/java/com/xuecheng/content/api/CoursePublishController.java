package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * @description 课程预览，发布
 * @author Mr.M
 * @date 2022/9/16 14:48
 * @version 1.0
 */
 @Controller
public class CoursePublishController {


 @GetMapping("/coursepreview/{courseId}")
 public ModelAndView preview(@PathVariable("courseId") Long courseId){

      ModelAndView modelAndView = new ModelAndView();
      modelAndView.addObject("model",null);
      modelAndView.setViewName("course_template");
   return modelAndView;
  }

}