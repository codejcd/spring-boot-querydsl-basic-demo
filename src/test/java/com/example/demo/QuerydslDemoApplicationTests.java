package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.entity.Hello;
import com.example.demo.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;

@SpringBootTest
@Transactional
class QuerydslDemoApplicationTests {
    
    @Autowired
    EntityManager em;

	@Test
	void contextLoads() {
	    Hello hello = new Hello();
	    em.persist(hello);
	    
	    JPAQueryFactory query = new JPAQueryFactory(em);
	    QHello qHello = new QHello("h");
	    
	    Hello result = query.selectFrom(qHello)
	                        .fetchOne();
	    
	    assertThat(result).isEqualTo(hello);
	    assertThat(result.getId()).isEqualTo(hello.getId());
	    
	}

}
