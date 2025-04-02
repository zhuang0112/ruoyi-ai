package org.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.ComponentScan;

/**
 * 启动程序
 *
 * @author Lion Li
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.zhuang.*", "org.ruoyi.*"})
public class RuoYiAIApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiAIApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  RuoYiAI启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
