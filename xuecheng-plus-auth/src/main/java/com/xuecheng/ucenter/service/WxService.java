package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;

public interface WxService {
    public XcUser wxauth(String code);
}
