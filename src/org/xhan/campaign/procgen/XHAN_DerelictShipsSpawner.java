package org.xhan.campaign.procgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * Script to generate a whole bunch of teaser ships in random orbits throughout the sector
 */
public class XHAN_DerelictShipsSpawner {
    public static final Logger LOGGER = Global.getLogger(XHAN_DerelictShipsSpawner.class);
    // --- Settings --- //

    //List of teaser ships to spawn and their count
    private static final List<Pair<String, Integer>> SHIP_SPAWNS = new ArrayList<>();

    static {
        SHIP_SPAWNS.add(new Pair<>("XHAN_Uvello_Matrix", 2));
        SHIP_SPAWNS.add(new Pair<>("XHAN_Velloun_Alvus", 2));
        SHIP_SPAWNS.add(new Pair<>("XHAN_Brakad_S_Hero", 1));
        SHIP_SPAWNS.add(new Pair<>("XHAN_Brakad_stalwart", 3));
        SHIP_SPAWNS.add(new Pair<>("XHAN_Cronosos_Grigora", 2));
    }

    //Systems that can never get a teaser ship spawned in them
    public static final List<String> BLACKLISTED_SYSTEMS = new ArrayList<>();

    static {
        BLACKLISTED_SYSTEMS.add("spookysecretsystem_omega");
    }

    //Systems with any of these tags can never get a teaser ship spawned in them
    public static final List<String> BLACKLISTED_SYSTEM_TAGS = new ArrayList<>();

    static {
        BLACKLISTED_SYSTEM_TAGS.add(Tags.THEME_HIDDEN);
        BLACKLISTED_SYSTEM_TAGS.add(Tags.THEME_SPECIAL);

        //Seeker
        BLACKLISTED_SYSTEM_TAGS.add("theme_plaguebearers");
        //DME
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_main");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_secondary");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_no_fleets");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_destroyed");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_suppressed");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_resurgent");
        BLACKLISTED_SYSTEM_TAGS.add("theme_breakers_homeworld");

        //Magellan
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_leveller");
        BLACKLISTED_SYSTEM_TAGS.add("theme_magellan_leveller_home_nebula");

        BLACKLISTED_SYSTEM_TAGS.add("theme_oci");
        BLACKLISTED_SYSTEM_TAGS.add("theme_oci_small");
        BLACKLISTED_SYSTEM_TAGS.add("theme_oci_large");
        BLACKLISTED_SYSTEM_TAGS.add("theme_oci_core");

        //HMI
        BLACKLISTED_SYSTEM_TAGS.add("theme_domres");
        BLACKLISTED_SYSTEM_TAGS.add("theme_hmi_nightmare");
        BLACKLISTED_SYSTEM_TAGS.add("theme_hmi_mess_remnant");
        BLACKLISTED_SYSTEM_TAGS.add("theme_messrem");
        BLACKLISTED_SYSTEM_TAGS.add("theme_domresboss");
    }

    //Weights for the different types of locations our teasers can spawn in
    private static final LinkedHashMap<LocationType, Float> WEIGHTS = new LinkedHashMap<>();

    static {
        WEIGHTS.put(LocationType.GAS_GIANT_ORBIT, 4f);
        WEIGHTS.put(LocationType.IN_ASTEROID_BELT, 1f);
        WEIGHTS.put(LocationType.IN_ASTEROID_FIELD, 2f);
        WEIGHTS.put(LocationType.STAR_ORBIT, 4f);
        WEIGHTS.put(LocationType.IN_SMALL_NEBULA, 1f);
        WEIGHTS.put(LocationType.JUMP_ORBIT, 1f);
    }

    // Functions

    /**
     * Spawns all the teaser ships into the sector: should be run once on sector generation
     *
     * @param sector the sector to spawn the ships in
     */
    public static void spawnXhanDerelicts(SectorAPI sector) {
        for (Pair<String, Integer> spawnData : SHIP_SPAWNS) {
            int numberOfSpawns = 0;
            while (numberOfSpawns < spawnData.two) {
                //Continue until we've found a place to spawn
                BaseThemeGenerator.EntityLocation placeToSpawn = null;
                StarSystemAPI system = null;
                while (placeToSpawn == null) {
                    system = getRandomSystemWithBlacklist(BLACKLISTED_SYSTEMS, BLACKLISTED_SYSTEM_TAGS, sector);
                    if (system == null) {
                        //We've somehow blacklisted every system in the sector: just don't spawn anything
                        return;
                    }

                    //Gets a list of random locations in the system, and picks one
                    WeightedRandomPicker<BaseThemeGenerator.EntityLocation> validPoints = BaseThemeGenerator.getLocations(new Random(), system, 50f, WEIGHTS);
                    placeToSpawn = validPoints.pick();
                }

                //Now, simply spawn the ship in the spawn location
                addDerelict(system, spawnData.one, placeToSpawn.orbit, ShipRecoverySpecial.ShipCondition.WRECKED, true, null);
                numberOfSpawns++;
            }
        }
    }


    /**
     * Utility function for getting a random system, with blacklist functionality in case some systems really shouldn't
     * be included.
     *
     * @param blacklist    A list of all the systems we are forbidden from picking
     * @param tagBlacklist A list of all the system tags that prevent a system from being picked
     * @param sector       The SectorAPI to check for systems in
     **/
    private static StarSystemAPI getRandomSystemWithBlacklist(List<String> blacklist, List<String> tagBlacklist, SectorAPI sector) {
        //First, get all the valid systems and put them in a separate list
        List<StarSystemAPI> validSystems = new ArrayList<>();
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (blacklist.contains(system.getId())) {
                continue;
            }
            boolean isValid = true;
            for (String bannedTag : tagBlacklist) {
                if (system.hasTag(bannedTag)) {
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                validSystems.add(system);
            }
        }

        //If that list is empty, return null
        if (validSystems.isEmpty()) {
            return null;
        }

        //Otherwise, get a random element in it and return that
        else {
            int rand = MathUtils.getRandomNumberInRange(0, validSystems.size() - 1);
            return validSystems.get(rand);
        }
    }


    //Mini-function for generating derelicts
    private static SectorEntityToken addDerelict(StarSystemAPI system, String variantId, OrbitAPI orbit,
                                                 ShipRecoverySpecial.ShipCondition condition, boolean recoverable,
                                                 @Nullable DefenderDataOverride defenders) {

        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        ship.setOrbit(orbit);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
        if (defenders != null) {
            Misc.setDefenderOverride(ship, defenders);
        }
        return ship;
    }
}
