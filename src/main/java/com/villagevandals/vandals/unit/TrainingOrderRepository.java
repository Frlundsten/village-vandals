package com.villagevandals.vandals.unit;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingOrderRepository extends JpaRepository<TrainingOrder, Long> {

  List<TrainingOrder> findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(long villageId);

  List<TrainingOrder> findByVillage_IdAndCompletedFalseAndFinishesAtBefore(
      long villageId, Instant cutoff);
}
