package com.pacificaqua.config;

import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import com.pacificaqua.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public DataSeeder(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (users.findByPhone("9999999999").isEmpty()) {
      User owner = new User();
      owner.setName("Sameer");
      owner.setPhone("9999999999");
      owner.setEmail("sameer@pacificaqua.local");
      owner.setPasswordHash(encoder.encode("admin123"));
      owner.setRole(UserRole.OWNER);
      owner.setStatus(UserStatus.ACTIVE);
      users.save(owner);
      log.info("Seeded owner user phone=9999999999 password=admin123");
    }
    seedHelper("Tikesh", "9000000001");
    seedHelper("Himanshu", "9000000002");
    seedHelper("Deepak", "9000000003");
  }

  private void seedHelper(String name, String phone) {
    if (users.findByPhone(phone).isEmpty()) {
      User helper = new User();
      helper.setName(name);
      helper.setPhone(phone);
      helper.setPasswordHash(encoder.encode("helper123"));
      helper.setRole(UserRole.HELPER);
      helper.setStatus(UserStatus.ACTIVE);
      users.save(helper);
    }
  }
}
