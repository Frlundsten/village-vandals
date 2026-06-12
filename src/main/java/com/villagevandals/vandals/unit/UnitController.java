package com.villagevandals.vandals.unit;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unit")
public class UnitController {

  private static final Logger LOG = LoggerFactory.getLogger(UnitController.class);

  private final UnitService unitService;

  public UnitController(UnitService unitService) {
    this.unitService = unitService;
  }

  @PostMapping("/train")
  public ResponseEntity<?> trainUnit(@RequestBody TrainRequestDTO dto) {
    LOG.debug(
        "Training {} unit(s) for village {} at building {}",
        dto.quantity(), dto.villageId(), dto.buildingId());
    try {
      List<TrainingOrderDTO> queue =
          unitService.trainVandal(dto.villageId(), dto.buildingId(), dto.quantity());
      return ResponseEntity.ok(queue);
    } catch (IllegalArgumentException e) {
      LOG.warn("Train unit failed: {}", e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/training")
  public ResponseEntity<List<TrainingOrderDTO>> getTrainingQueue(@RequestParam long villageId) {
    LOG.debug("Fetching training queue for village {}", villageId);
    return ResponseEntity.ok(unitService.getTrainingQueue(villageId));
  }

  @GetMapping
  public ResponseEntity<List<UnitRosterDTO>> getRoster(@RequestParam long villageId) {
    LOG.debug("Fetching unit roster for village {}", villageId);
    return ResponseEntity.ok(unitService.getRoster(villageId));
  }
}
