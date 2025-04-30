package org.ruoyi.domin;

import lombok.Data;

/**
 * @author https://www.wdbyte.com
 */
@Data
public class WeixinQrCode {

    private String ticket;
    private Long expireSeconds;
    private String url;
    private String qrCodeUrl;
}
