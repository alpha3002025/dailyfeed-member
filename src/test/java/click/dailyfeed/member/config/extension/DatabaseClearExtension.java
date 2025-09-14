package click.dailyfeed.member.config.extension;

import click.dailyfeed.member.config.TruncateAllProcessor;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Profile({"mysql-test"})
public class DatabaseClearExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // 1) TruncateAllProcessor Bean 조회
        TruncateAllProcessor processorBean = findTruncateAllProcessorBean(context);
        // 2) processorBean 의 removeAll 을 호출
        removeAll(processorBean);
    }

    public TruncateAllProcessor findTruncateAllProcessorBean(ExtensionContext context){
        return SpringExtension
                .getApplicationContext(context)
                .getBean(TruncateAllProcessor.class);
    }

    public void removeAll(TruncateAllProcessor processorBean){
        processorBean.removeAll();
    }

}
