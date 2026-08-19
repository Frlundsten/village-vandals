package com.villagevandals.vandals.building;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BuildingDeleteTest {

  private static final long VILLAGE_ID = 1L;
  private static final long SITE_ID = 1L;
  private static final String OWNER = "owner";
  private static final String INTRUDER = "intruder";

  @Mock VillageRepository villageRepository;
  @Mock ConstructionSiteRepository constructionSiteRepository;
  @Mock BuildingRepository buildingRepository;
  @Mock ResourcesService resourcesService;

  BuildingService service;

  /**
   * Returns a mocked building service with given resources and construction sites
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        new BuildingService(
            resourcesService, villageRepository, constructionSiteRepository, buildingRepository);
  }

  /**
   * Deletes a building from the farm at villageOwnedBy and frees its orphaned site.
   */
  @Test
  void deleteBuilding_economicBuilding_snapshotsThenReversesProductionAndFreesSite() {
    Farm farm = new Farm();
    Village village = villageOwnedBy(OWNER);
    ConstructionSite site = new ConstructionSite(village, farm, 1);
    when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
        .thenReturn(Optional.of(site));

    int expectedProduction = farm.productionPerHour();

    service.deleteBuilding(VILLAGE_ID, SITE_ID, OWNER);

    // snapshot must be committed BEFORE the production rate drops
    InOrder inOrder = inOrder(resourcesService);
    inOrder.verify(resourcesService).snapshotCurrentResources(VILLAGE_ID);
    inOrder.verify(resourcesService).updateProductionDelta(farm, VILLAGE_ID, -expectedProduction);

    // site freed and orphaned building removed
    assertThat(site.getBuilding()).isNull();
    verify(constructionSiteRepository).save(site);
    verify(buildingRepository).delete(farm);
  }

  /**
   * Test method to ensure building deletion does not affect production but frees the site
   */
  @Test
  void deleteBuilding_nonEconomicBuilding_doesNotTouchProductionButFreesSite() {
    Barrack barrack = new Barrack();
    Village village = villageOwnedBy(OWNER);
    ConstructionSite site = new ConstructionSite(village, barrack, 1);
    when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
        .thenReturn(Optional.of(site));

    service.deleteBuilding(VILLAGE_ID, SITE_ID, OWNER);

    verify(resourcesService, never())
        .updateProductionDelta(any(), anyLong(), anyInt());
    assertThat(site.getBuilding()).isNull();
    verify(constructionSiteRepository).save(site);
    verify(buildingRepository).delete(barrack);
  }

  /**
   * Tests to verify that building deletion does not throw an exception when the site is owned by a different person.
   */
  @Test
  void deleteBuilding_nonOwner_throwsAndChangesNothing() {
    Farm farm = new Farm();
    Village village = villageOwnedBy(OWNER);
    ConstructionSite site = new ConstructionSite(village, farm, 1);
    when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
        .thenReturn(Optional.of(site));

    assertThatThrownBy(() -> service.deleteBuilding(VILLAGE_ID, SITE_ID, INTRUDER))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(site.getBuilding()).isEqualTo(farm);
    verifyNoInteractions(resourcesService);
    verify(buildingRepository, never()).delete(any());
    verify(constructionSiteRepository, never()).save(any());
  }

  /**
   * Throws a `NoSuchResource` exception if the specified resource cannot be found or does not exist.
   */
  @Test
  void deleteBuilding_emptySite_throwsAndChangesNothing() {
    Village village = villageOwnedBy(OWNER);
    ConstructionSite site = new ConstructionSite(village, null, 1);
    when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
        .thenReturn(Optional.of(site));

    assertThatThrownBy(() -> service.deleteBuilding(VILLAGE_ID, SITE_ID, OWNER))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(resourcesService);
    verify(buildingRepository, never()).delete(any());
  }

  /**
   * Tests to verify the deleteBuilding method throws an IllegalArgumentException when trying to delete a building from unknown site.
   */
  @Test
  void deleteBuilding_unknownSite_throws() {
    when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteBuilding(VILLAGE_ID, SITE_ID, OWNER))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(resourcesService);
    verify(buildingRepository, never()).delete(any());
  }

  /**
   * Returns a new Village object with details based on the given username.
   *
   * @param username The name of the parameter for user.
   * @return
   */
  private Village villageOwnedBy(String username) {
    User owner = mock(User.class);
    when(owner.getUsername()).thenReturn(username);
    return new Village(0, 0, owner);
  }
}
