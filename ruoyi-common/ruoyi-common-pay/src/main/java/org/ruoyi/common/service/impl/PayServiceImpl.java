package org.ruoyi.common.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.config.PayConfig;
import org.ruoyi.common.service.PayService;
import org.ruoyi.common.utils.MD5Util;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务
 * @author Admin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl implements PayService {

    private final PayConfig payConfig;

    @Override
    public String getPayUrl(String orderNo, String name, double money, String clientIp) {
        String out_trade_no = orderNo, sign = "";
        //封装请求参数
        String mdString = "clientip=" + clientIp + "&device=" + payConfig.getDevice() + "&money=" + money + "&name=" + name + "&" +
            "notify_url=" + payConfig.getNotify_url() + "&out_trade_no=" + out_trade_no + "&pid=" + payConfig.getPid() + "&return_url=" + payConfig.getReturn_url() +
            "&type=" + payConfig.getType() + payConfig.getKey();
        sign = MD5Util.GetMD5Code(mdString);
        Map<String, Object> map = new HashMap<>(10);
        map.put("clientip", clientIp);
        map.put("device", payConfig.getDevice());
        map.put("money", money);
        map.put("name", name);
        map.put("notify_url", payConfig.getNotify_url());
        map.put("out_trade_no", out_trade_no);
        map.put("pid", payConfig.getPid());
        map.put("return_url", payConfig.getReturn_url());
        map.put("sign_type", payConfig.getSign_type());
        map.put("type", payConfig.getType());
        map.put("sign", sign);
        String body = HttpUtil.post(payConfig.getPayUrl(), map);
        log.info("支付返回信息：{},配置信息: {}",body,payConfig);
        JSONObject jsonObject = new JSONObject(body);
        return (String) jsonObject.get("qrcode");
    }

}
