package com.example.resilient_api;

import com.example.resilient_api.domain.spi.UserPersistencePort;
import com.example.resilient_api.domain.usecase.UserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = ResilientApiApplication.class)
class ResilientApiApplicationTests {

	@MockBean
	private UserPersistencePort userPersistencePort;

	@Autowired
	private UserUseCase userUseCase;

	@Test
	void contextLoads() {
	}

}
