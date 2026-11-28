package com.pacificaqua.repo;

import com.pacificaqua.domain.PlantTrip;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantTripRepository extends JpaRepository<PlantTrip, Long> {

  @Query("""
      select t from PlantTrip t
      where (:from is null or t.startedAt >= :from)
        and (:to is null or t.startedAt < :to)
      order by t.startedAt desc
      """)
  List<PlantTrip> search(@Param("from") Instant from, @Param("to") Instant to);
}
