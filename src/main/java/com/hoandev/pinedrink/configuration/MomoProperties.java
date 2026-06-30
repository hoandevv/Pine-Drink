package com.hoandev.pinedrink.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.momo")
@Getter
@Setter
public class MomoProperties {

    private String partnerCode = "MOMO";
    private String accessKey = "";
    private String secretKey = "";
    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String redirectUrl = "http://localhost:4200/payment/momo-return";
    private String ipnUrl = "";
    private String requestType = "payWithMethod";
    private String lang = "vi";
    private Boolean autoCapture = true;
    private String partnerName = "Pine Drink";
    private String storeId = "PineDrinkStore";
    private String orderGroupId = "";
}
