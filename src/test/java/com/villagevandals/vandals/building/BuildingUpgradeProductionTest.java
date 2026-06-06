package com.villagevandals.vandals.building;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_BASE_PRODUCTION_RATE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.building.dto.UpgradeRequestDTO;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.resource.Resource;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BuildingUpgradeProductionTest {

    private static final long VILLAGE_ID = 1L;
    private static final long SITE_ID = 1L;
    private static final String USERNAME = "testUser";

    @Mock VillageRepository villageRepository;
    @Mock ConstructionSiteRepository constructionSiteRepository;
    @Mock BuildingRepository buildingRepository;

    BuildingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ResourcesService resourcesService = new ResourcesService(villageRepository);
        service = new BuildingService(resourcesService, villageRepository, constructionSiteRepository, buildingRepository);
    }

    @Test
    void upgradeBuilding_farm_increasesFoodPerHour() {
        Farm farm = new Farm();

        User owner = mock(User.class);
        when(owner.getUsername()).thenReturn(USERNAME);
        Village village = villageWithResources(owner);

        ConstructionSite site = siteWith(farm, village);
        when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
            .thenReturn(Optional.of(site));
        when(villageRepository.findById(VILLAGE_ID)).thenReturn(Optional.of(village));
        when(buildingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int foodBefore = village.getProduction().getFoodPerHour();

        service.upgradeBuilding(new UpgradeRequestDTO(VILLAGE_ID, SITE_ID), USERNAME);

        assertThat(village.getProduction().getFoodPerHour())
            .as("foodPerHour should increase by DEFAULT_ECONOMICAL_PRODUCTION_RATE after upgrade")
            .isEqualTo(foodBefore + DEFAULT_ECONOMICAL_PRODUCTION_RATE);
    }

    @Test
    void upgradeBuilding_farm_doesNotChangeOtherResources() {
        Farm farm = new Farm();
        User owner = mock(User.class);
        when(owner.getUsername()).thenReturn(USERNAME);
        Village village = villageWithResources(owner);

        ConstructionSite site = siteWith(farm, village);
        when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
            .thenReturn(Optional.of(site));
        when(villageRepository.findById(VILLAGE_ID)).thenReturn(Optional.of(village));
        when(buildingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int woodBefore = village.getProduction().getWoodPerHour();
        int bricksBefore = village.getProduction().getBricksPerHour();
        int ironBefore = village.getProduction().getIronPerHour();

        service.upgradeBuilding(new UpgradeRequestDTO(VILLAGE_ID, SITE_ID), USERNAME);

        assertThat(village.getProduction().getWoodPerHour()).isEqualTo(woodBefore);
        assertThat(village.getProduction().getBricksPerHour()).isEqualTo(bricksBefore);
        assertThat(village.getProduction().getIronPerHour()).isEqualTo(ironBefore);
    }

    @Test
    void upgradeBuilding_farm_stacksAcrossMultipleUpgrades() {
        Farm farm = new Farm();
        User owner = mock(User.class);
        when(owner.getUsername()).thenReturn(USERNAME);
        Village village = villageWithResources(owner);

        ConstructionSite site = siteWith(farm, village);
        when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
            .thenReturn(Optional.of(site));
        when(villageRepository.findById(VILLAGE_ID)).thenReturn(Optional.of(village));
        when(buildingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int foodAfterConstruction = village.getProduction().getFoodPerHour();

        service.upgradeBuilding(new UpgradeRequestDTO(VILLAGE_ID, SITE_ID), USERNAME);
        service.upgradeBuilding(new UpgradeRequestDTO(VILLAGE_ID, SITE_ID), USERNAME);

        assertThat(village.getProduction().getFoodPerHour())
            .isEqualTo(foodAfterConstruction + DEFAULT_ECONOMICAL_PRODUCTION_RATE * 2);
    }

    @Test
    void upgradeBuilding_barrack_doesNotChangeAnyProductionRate() {
        Barrack barrack = new Barrack();
        User owner = mock(User.class);
        when(owner.getUsername()).thenReturn(USERNAME);
        Village village = villageWithResources(owner);

        ConstructionSite site = siteWith(barrack, village);
        when(constructionSiteRepository.findByIdAndVillageId(SITE_ID, VILLAGE_ID))
            .thenReturn(Optional.of(site));
        when(villageRepository.findById(VILLAGE_ID)).thenReturn(Optional.of(village));
        when(buildingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int foodBefore = village.getProduction().getFoodPerHour();
        int woodBefore = village.getProduction().getWoodPerHour();
        int bricksBefore = village.getProduction().getBricksPerHour();
        int ironBefore = village.getProduction().getIronPerHour();

        service.upgradeBuilding(new UpgradeRequestDTO(VILLAGE_ID, SITE_ID), USERNAME);

        assertThat(village.getProduction().getFoodPerHour()).isEqualTo(foodBefore);
        assertThat(village.getProduction().getWoodPerHour()).isEqualTo(woodBefore);
        assertThat(village.getProduction().getBricksPerHour()).isEqualTo(bricksBefore);
        assertThat(village.getProduction().getIronPerHour()).isEqualTo(ironBefore);
    }

    private Village villageWithResources(User owner) {
        Village village = new Village(0, 0, owner);
        village.getStorage().set(Resource.WOOD, 5000);
        village.getStorage().set(Resource.BRICKS, 5000);
        village.getStorage().set(Resource.FOOD, 5000);
        village.getStorage().set(Resource.IRON, 5000);
        return village;
    }

    private ConstructionSite siteWith(com.villagevandals.vandals.building.buildings.Building building, Village village) {
        ConstructionSite site = mock(ConstructionSite.class);
        when(site.getBuilding()).thenReturn(building);
        when(site.getVillage()).thenReturn(village);
        return site;
    }
}
