package data.scripts.world;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import data.scripts.world.systems.XhanMyrianousSystem;


@SuppressWarnings("unchecked")
public class XhanProcGen implements SectorGeneratorPlugin {
    @Override
    public void generate(SectorAPI sector) {
        new XhanMyrianousSystem().generate(sector);
    }
}

