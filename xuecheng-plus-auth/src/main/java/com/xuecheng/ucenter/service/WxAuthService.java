package com.xuecheng.ucenter.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("wx_authservice")
public class WxAuthService implements AuthService, WxService{
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    RestTemplate restTemplate;
    @Value("${app.id}")
    String appId;
    @Value("${app.secret}")
    String appSecret;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String username = authParamsDto.getUsername();
        //根据用户名查询用户信息
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(xcUser == null){
            //用户不存在
            return null;
        }
        XcUserExt xcUserExt = new XcUserExt();
        //将用户信息拷贝到XcUserExt中
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxauth(String code) {
        //获取令牌
        Map<String,String> access_token_obj = getAccessToken(code);
        String access_token = access_token_obj.get("access_token");
        String openid = access_token_obj.get("openid");
        //根据授权码获取用户信息
        Map<String,String> userInfo = getUserInfo(access_token, openid);
        //用户信息写入数据库
        XcUser xcUser = storeUserInfo(userInfo);
        return xcUser;
    }

    /**
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     *
     * 根据code获取access_token和openid
     * @param code 微信授权码
     * @return 包含access_token和openid的map
     */
    private Map<String,String> getAccessToken(String code) {
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String format =String.format(url_template,appId, appSecret, code);

        log.info("调用微信接口申请access_token, url:{}", format);
        //调用微信接口获取access_token和openid
        ResponseEntity<String> exchange = restTemplate.exchange(format, HttpMethod.POST, null, String.class);
        String body = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", body);
        if (body == null || body.isEmpty()) {
            throw new RuntimeException("获取微信access_token失败");
        }
        Map<String,String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    /**
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     *
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *
     * }
     * 根据access_token和openid获取用户信息
     */
    private Map<String,String> getUserInfo(String access_token, String openId){
        String url_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String format = String.format(url_template, access_token, openId);
        log.info("调用微信接口申请用户信息, url:{}", format);
        ResponseEntity<String> exchange = restTemplate.exchange(format, HttpMethod.GET, null, String.class);
        String body = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("调用微信接口申请用户信息: 返回值:{}", body);
        Map<String,String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    private XcUser storeUserInfo(Map<String, String> userInfo) {
        String unionid = userInfo.get("unionid").toString();
        //根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(xcUser!=null){
            return xcUser;
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo.get("nickname").toString());
        xcUser.setUserpic(userInfo.get("headimgurl").toString());
        xcUser.setName(userInfo.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
