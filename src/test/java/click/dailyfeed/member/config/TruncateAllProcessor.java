package click.dailyfeed.member.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Profile({"mysql-test"})
@Component
public class TruncateAllProcessor {
    private final List<String> tableNames = new ArrayList<>();
    private final EntityManager entityManager;

    public TruncateAllProcessor(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void findAllTableNames(){
        List<String> tables = entityManager
                .createNativeQuery("SHOW TABLES")
                .getResultList();

        tables.forEach(t -> {
            tableNames.add(t);
        });
    }

    // 의존성 주입 후 초기화 수행시 Table 을 조회합니다.
    private void truncateAllTables(){
//    entityManager
//        .createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS %d", 0)).executeUpdate();

//    tableNames.forEach(tableName -> {
//      entityManager
//          .createNativeQuery(String.format("TRUNCATE TABLE %s", tableName))
//          .executeUpdate();
//    });
        for(String table: tableNames){
            entityManager
                    .createNativeQuery(String.format("TRUNCATE TABLE %s", table))
                    .executeUpdate();
        }

//    entityManager
//        .createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS %d", 1)).executeUpdate();
    }

    @Transactional
    public void removeAll(){
        entityManager.clear();
        truncateAllTables();
    }

}
