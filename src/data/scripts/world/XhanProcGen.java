package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.world.systems.*;
import org.json.JSONException;
import org.json.JSONObject;
import data.scripts.world.systems.XhanMyrianousSystem;

import java.io.IOException;
import java.util.List;


@SuppressWarnings("unchecked")
    public class XhanProcGen implements SectorGeneratorPlugin {
    @Override
    public void generate(SectorAPI sector) {
        new XhanMyrianousSystem().generate(sector);
    }
}

