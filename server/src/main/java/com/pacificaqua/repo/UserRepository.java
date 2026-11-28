package com.pacificaqua.repo;

import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByPhone(String phone);
  Optional<User> findByEmailIgnoreCase(String email);

  @Query("""
      select u from User u
      where (:q is null or :q = '' or lower(u.name) like lower(concat('%', :q, '%'))
         or u.phone like concat('%', :q, '%')
         or lower(coalesce(u.email, '')) like lower(concat('%', :q, '%')))
        and (:role is null or u.role = :role)
        and (:status is null or u.status = :status)
      order by u.name
      """)
  List<User> search(@Param("q") String q, @Param("role") UserRole role, @Param("status") UserStatus status);

  boolean existsByPhone(String phone);
  boolean existsByEmailIgnoreCase(String email);
}
