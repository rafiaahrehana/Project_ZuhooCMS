package com.businessos.shared.payment.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sslcommerz")
@Getter @Setter
public class SslCommerzProperties {

    private String storeId;
    private String storePassword;
    /** true = sandbox.sslcommerz.com, false = securepay.sslcommerz.com */
    private boolean sandbox = true;
    private String currency = "BDT";

    public String baseUrl() {
        return sandbox ? "https://sandbox.sslcommerz.com" : "https://securepay.sslcommerz.com";
    }

    public String initiateUrl()   { return baseUrl() + "/gwprocess/v4/api.php"; }
    public String validationUrl() { return baseUrl() + "/validator/api/validationserverAPI.php"; }
}
