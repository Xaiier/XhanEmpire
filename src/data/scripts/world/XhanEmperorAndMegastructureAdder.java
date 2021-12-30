package data.scripts.world;

import com.fs.starfarer.api.EveryFrameScript;
import static com.fs.starfarer.api.Global.getLogger;
import static com.fs.starfarer.api.Global.getSector;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import static com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel.FULL;
import com.fs.starfarer.api.characters.PersonAPI;
import static com.fs.starfarer.api.impl.campaign.ids.Ranks.POST_FACTION_LEADER;
import static com.fs.starfarer.api.impl.campaign.ids.Skills.INDUSTRIAL_PLANNING;
import static com.fs.starfarer.api.impl.campaign.ids.Skills.PLANETARY_OPERATIONS;
import static com.fs.starfarer.api.util.Misc.getFactionMarkets;
import static data.scripts.XhanEmpireModPlugin.XHAN_FACTION_ID;
import static data.scripts.XhanEmpireModPlugin.PAMED_FACTION_ID;
import static data.scripts.XhanEmpireModPlugin.createAdmin;
import java.util.List;
import org.apache.log4j.Logger;

public class XhanEmperorAndMegastructureAdder implements EveryFrameScript {

    public static final String EMPEROR_PORTRAIT = "graphics/portraits/DivineEmperor.png";
    public static final String GENERALISSIMO_PORTRAIT = "graphics/portraits/PamedGeneralissimo.png";
    public static final String MEGASTRUCTURE = "xhan_planetary_megastructure";
    
    public static Logger log = getLogger(XhanEmperorAndMegastructureAdder.class);
    private boolean done = false;

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (!done) {
            doThing();
            doThingPamed();
            log.info("doing the thing");
            done = true;
        }
    }

    private void doThing() {
        List<MarketAPI> markets = getFactionMarkets(getSector().getFaction(XHAN_FACTION_ID));
        MarketAPI picked = null;
        int size = 0;
        for (MarketAPI market : markets) {
            int thisSize = market.getSize();
            if (thisSize > size) {
                size = thisSize;
                picked = market;
            }
        }
        
        if (picked != null) {
            PersonAPI divine = createAdmin(picked);
            divine.getStats().setSkillLevel(INDUSTRIAL_PLANNING, 3);
            divine.getStats().setSkillLevel(PLANETARY_OPERATIONS, 3);
            divine.setPortraitSprite(EMPEROR_PORTRAIT);
            divine.setPostId(POST_FACTION_LEADER);
            divine.setRankId(POST_FACTION_LEADER);
            divine.getName().setFirst("Okkatrid");
            divine.getName().setLast("Zetnon");
            
            picked.addCondition(MEGASTRUCTURE);
            picked.getFirstCondition(MEGASTRUCTURE).setSurveyed(true);
            picked.setSurveyLevel(FULL);
        }
    }

    private void doThingPamed() {
        List<MarketAPI> markets = getFactionMarkets(getSector().getFaction(PAMED_FACTION_ID));
        MarketAPI picked = null;
        int size = 0;
        for (MarketAPI market : markets) {
            int thisSize = market.getSize();
            if (thisSize > size) {
                size = thisSize;
                picked = market;
            }
        }

        if (picked != null) {
            PersonAPI pamedg = createAdmin(picked);
            pamedg.getStats().setSkillLevel(INDUSTRIAL_PLANNING, 3);
            pamedg.getStats().setSkillLevel(PLANETARY_OPERATIONS, 2);
            pamedg.setPortraitSprite(GENERALISSIMO_PORTRAIT);
            pamedg.setPostId(POST_FACTION_LEADER);
            pamedg.setRankId(POST_FACTION_LEADER);
            pamedg.getName().setFirst("Sisi");
            pamedg.getName().setLast("De Luca");

            picked.setSurveyLevel(FULL);
        }
    }
}
