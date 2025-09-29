package click.dailyfeed.member;

import click.dailyfeed.member.domain.follow.repository.mongo.FollowingMongoRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableMongoAuditing
@EnableJpaRepositories(
		basePackages = "click.dailyfeed.member.domain.**.repository.jpa",
		entityManagerFactoryRef = "entityManagerFactory",
		transactionManagerRef = "transactionManager",
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = FollowingMongoRepository.class
		)
)
@EnableMongoRepositories(
		basePackages = "click.dailyfeed.member.domain.**.repository.mongo",
		mongoTemplateRef = "mongoTemplate"
)
@EnableTransactionManagement
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
