package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import java.awt.Color;

import data.scripts.campaign.XHAN_DerelictShipsSpawner;
import data.scripts.campaign.XHAN_DroneshipEliteProductionListener;
import data.scripts.campaign.XHAN_DroneshipProductionListener;
import data.scripts.world.XhanProcGen;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import data.scripts.weapons.ai.Xhan_Mega_Buster_Bomb_AI;
import data.scripts.weapons.ai.Xhan_Psy_Buster_Bomb_AI;
import data.scripts.world.XhanEmperorAndMegastructureAdder;
import static data.scripts.world.XhanEmperorAndMegastructureAdder.EMPEROR_PORTRAIT;
import static data.scripts.world.XhanEmperorAndMegastructureAdder.GENERALISSIMO_PORTRAIT;
import data.scripts.world.XhanEmpireGen;
import exerelin.campaign.SectorManager;
import java.io.IOException;
import static java.lang.Math.random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

public class XhanEmpireModPlugin extends BaseModPlugin
{
    // Missiles that use custom AI
    public static final String MEGA_BUSTER_BOMB_ID = "Xhan_Pharrek_pulse";
    public static final String PSY_BUSTER_BOMB_ID = "Xhan_Psy_pulse";

    public static Logger log = Global.getLogger(XhanEmpireModPlugin.class);

    public static final String XHAN_FACTION_ID = "xhanempire";
    public static final String PAMED_FACTION_ID = "unitedpamed";
    public static final String Myrianous_System_ID = "XhanMyrianousSystem";

    public static boolean EXERELIN_LOADED;
    public static Set<String> EXERELIN_ACTIVE = new HashSet<>();

    @Override
    public void onApplicationLoad() throws Exception
    {
        try
        {
            Global.getSettings().getScriptClassLoader().loadClass("org.lazywizard.lazylib.ModUtils");
        }
        catch (ClassNotFoundException lazy)
        {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "LazyLib is required to run Xhan Empire."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download LazyLib at http://fractalsoftworks.com/forum/index.php?topic=5444"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        try
        {
            Global.getSettings().getScriptClassLoader().loadClass("org.magiclib.util.MagicTargeting");
        }
        catch (ClassNotFoundException magic)
        {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "MagicLib is required to run Xhan Empire."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download MagicLib at http://fractalsoftworks.com/forum/index.php?topic=13718.0"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        //Check ShaderLib for lights
        try
        {
            Global.getSettings().getScriptClassLoader().loadClass("org.dark.shaders.util.ShaderLib");
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/xhan_light_data.csv");
            TextureData.readTextureDataCSV("data/lights/xhan_texture_data.csv");
        }
        catch (ClassNotFoundException ex)
        {
        }

        EXERELIN_LOADED = Global.getSettings().getModManager().isModEnabled("nexerelin");
    }


    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new XhanEmpireGen().generate(Global.getSector());
        }
        new XhanProcGen().generate(Global.getSector());
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip)
    {
        switch (missile.getProjectileSpecId())
        {
            case PSY_BUSTER_BOMB_ID:
                return new PluginPick<MissileAIPlugin>(new Xhan_Psy_Buster_Bomb_AI(missile), CampaignPlugin.PickPriority.MOD_SET);
            case MEGA_BUSTER_BOMB_ID:
                return new PluginPick<MissileAIPlugin>(new Xhan_Mega_Buster_Bomb_AI(missile), CampaignPlugin.PickPriority.MOD_SET);
            default:
                return null;
        }
    }

    @Override
    public void onNewGameAfterTimePass()
    {

        log.info("new game started, adding scripts");
        Global.getSector().addScript(new XhanEmperorAndMegastructureAdder());
        XHAN_DerelictShipsSpawner.spawnXhanDerelicts(Global.getSector());

    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        {
            XHAN_DroneshipProductionListener listener = new XHAN_DroneshipProductionListener();
            Global.getSector().addTransientListener(listener);
            Global.getSector().getListenerManager().addListener(listener, true);

            Global.getLogger(XhanEmpireModPlugin.class).info("Added listener");
        }

        {
            XHAN_DroneshipEliteProductionListener listener = new XHAN_DroneshipEliteProductionListener();
            Global.getSector().addTransientListener(listener);
            Global.getSector().getListenerManager().addListener(listener, true);

            Global.getLogger(XhanEmpireModPlugin.class).info("Added listener");
        }

        try
        {
            Global.getSettings().loadTexture(EMPEROR_PORTRAIT);
            Global.getSettings().loadTexture(GENERALISSIMO_PORTRAIT);
        }
        catch (IOException ex)
        {
            log.error("couldn't load divine emperor portrait :(");
        }
    }

    private static void genSystem()
    {
        new XhanEmpireGen().generate(Global.getSector());
    }

    public static float randomRange(float min, float max)
    {
        return (float) (random() * (max - min) + min);
    }

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
            int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, boolean WithJunkAndChatter, boolean PirateMode, boolean freePort)
    {

        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String entityId = primaryEntity.getId();
        String marketId = entityId + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketId, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", newMarket.getFaction().getTariffFraction());

        if (submarkets != null)
        {
            for (String market : submarkets)
            {
                newMarket.addSubmarket(market);
            }
        }

        newMarket.addCondition("population_" + size);
        if (marketConditions != null)
        {
            for (String condition : marketConditions)
            {
                try
                {
                    newMarket.addCondition(condition);
                }
                catch (RuntimeException e)
                {
                    newMarket.addIndustry(condition);
                }
            }
        }

        if (connectedEntities != null)
        {
            for (SectorEntityToken entity : connectedEntities)
            {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, WithJunkAndChatter);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        createAdmin(newMarket);

        if (connectedEntities != null)
        {
            for (SectorEntityToken entity : connectedEntities)
            {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        if (PirateMode)
        {
            newMarket.setEconGroup(newMarket.getId());
            newMarket.setHidden(true);
            primaryEntity.setSensorProfile(1f);
            primaryEntity.setDiscoverable(true);
            primaryEntity.getDetectedRangeMod().modifyFlat("gen", 5000f);
            newMarket.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);
        }

        newMarket.setFreePort(freePort);

        for (MarketConditionAPI mc : newMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        newMarket.setSurveyLevel(SurveyLevel.FULL);

        newMarket.reapplyIndustries();

        log.info("created " + factionID + " market " + name);

        return newMarket;
    }

    public static PersonAPI createAdmin(MarketAPI market)
    {
        FactionAPI faction = market.getFaction();
        PersonAPI admin = faction.createRandomPerson();
        int size = market.getSize();

        switch (size)
        {
            case 3:
            case 4:
                admin.setRankId(Ranks.GROUND_CAPTAIN);
                break;
            case 5:
                admin.setRankId(Ranks.GROUND_MAJOR);
                break;
            case 6:
                admin.setRankId(Ranks.GROUND_COLONEL);
                break;
            case 7:
            case 8:
            case 9:
            case 10:
                admin.setRankId(Ranks.GROUND_GENERAL);
                break;
            default:
                admin.setRankId(Ranks.GROUND_LIEUTENANT);
                break;
        }

        List<String> skills = Global.getSettings().getSortedSkillIds();

        int industries = 0;
        int defenses = 0;
        boolean military = market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY);

        for (Industry curr : market.getIndustries())
        {
            if (curr.isIndustry())
            {
                industries++;
            }
            if (curr.getSpec().hasTag(Industries.TAG_GROUNDDEFENSES))
            {
                defenses++;
            }
        }

        admin.getStats().setSkipRefresh(true);

        int num = 0;
        if (industries >= 2 || (industries == 1 && defenses == 1))
        {
            if (skills.contains(Skills.INDUSTRIAL_PLANNING))
            {
                admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
            }
            num++;
        }

        if (num == 0 || size >= 7)
        {
            if (military)
            {
                if (skills.contains(Skills.SPACE_OPERATIONS))
                {
                    admin.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, 3);
                }
            }
            else if (defenses > 0)
            {
                if (skills.contains(Skills.PLANETARY_OPERATIONS))
                {
                    admin.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 3);
                }
            }
            else
            {
                // nothing else suitable, so just make sure there's at least one skill, if this wasn't already set
                if (skills.contains(Skills.INDUSTRIAL_PLANNING))
                {
                    admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
                }
            }
        }

        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        admin.getStats().setSkipRefresh(false);
        admin.getStats().refreshCharacterStatsEffects();
        admin.setPostId(Ranks.POST_ADMINISTRATOR);
        market.addPerson(admin);
        market.setAdmin(admin);
        market.getCommDirectory().addPerson(admin);
        ip.addPerson(admin);
        ip.getData(admin).getLocation().setMarket(market);
        ip.checkOutPerson(admin, "permanent_staff");

        log.info(String.format("Applying admin %s %s to market %s", market.getFaction().getRank(admin.getRankId()), admin.getNameString(), market.getName()));

        return admin;
    }

    public static String[] JSONArrayToStringArray(JSONArray jsonArray)
    {
        try
        {
            String[] ret = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++)
            {
                ret[i] = jsonArray.getString(i);
            }
            return ret;
        }
        catch (JSONException e)
        {
            log.warn(e);
            return new String[]
            {
            };
        }
    }

    public static String aOrAn(String input)
    {

        ArrayList<String> vowels = new ArrayList<>(Arrays.asList(
                "a",
                "e",
                "i",
                "o",
                "u"));

        String firstLetter = input.substring(0, 1).toLowerCase();

        if (vowels.contains(firstLetter))
        {
            return "an";
        }
        else
        {
            return "a";
        }
    }
}
