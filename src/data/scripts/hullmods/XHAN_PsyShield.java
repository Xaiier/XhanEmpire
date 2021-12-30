package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class XHAN_PsyShield extends BaseHullMod {
    
    private final String INNERLARGE = "";
    private final String OUTERLARGE = "";
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, OUTERLARGE);
        ship.getShield().setInnerRotationRate(0.05f);
    }
}
