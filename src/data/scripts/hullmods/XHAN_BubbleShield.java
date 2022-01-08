/*
code by Xaiier
*/

package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class XHAN_BubbleShield extends BaseHullMod {

    private static final Color EFFECT_COLOUR = new Color(213, 130, 255, 164);

    ShieldState shieldState = null;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }

        shieldState = (ShieldState) ship.getCustomData().get("bubbleshield");

        ShieldAPI shield = ship.getShield();

        if (shield != null) {
            if (shield.isOn()) {
                if (shieldState == null) {
                    shieldState = new ShieldState(1f, ship.getHullSpec().getShieldSpec().getRadius());
                    shield.setActiveArc(360f);

                    //TODO: draw differently
                    /*
                    //draw expanding circle
                    for (int i = 0; i < 360; i++) {
                        Vector2f loc = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getHullSpec().getShieldSpec().getRadius(), i);
                        Vector2f vel = Vector2f.sub(loc, ship.getLocation(), new Vector2f());
                        vel = Vector2f.sub(vel, ship.getVelocity(), vel);
                        //vel.normalise();
                        vel.scale(-1f);

                        Global.getCombatEngine().addSmoothParticle(ship.getLocation(), vel, 20f, 1f, 1.3f, EFFECT_COLOUR);
                    }
                    */
                }
                shieldState.advance(amount);

                float radius = shieldState.onlineTimer * shieldState.shieldRadius;
                shield.setRadius(radius);
                ship.setCustomData("bubbleshield", shieldState);
            }
            else if (shield.isOff()) {
                ship.removeCustomData("bubbleshield");
            }
        }
    }

    private static final class ShieldState {

        float onlineTimer;
        float multTime;
        float shieldRadius;

        private ShieldState(float multTime, float shieldRadius) {
            this.onlineTimer = 0f;
            this.multTime = multTime;
            this.shieldRadius = shieldRadius;
        }

        public void advance(float amount) {
            onlineTimer += amount * multTime;
            if (onlineTimer > 1f) {
                onlineTimer = 1f;
            }
        }

    }
}
