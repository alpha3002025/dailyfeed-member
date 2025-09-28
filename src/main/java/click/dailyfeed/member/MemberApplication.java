package click.dailyfeed.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@ComponentScan(basePackages = {
		"click.dailyfeed.feign",
        "click.dailyfeed.member",
        "click.dailyfeed.pagination",
        "click.dailyfeed.redis",
}, excludeFilters = {
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = click.dailyfeed.feign.config.web.FeignWebConfig.class)
})
public class MemberApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemberApplication.class, args);
	}

}
