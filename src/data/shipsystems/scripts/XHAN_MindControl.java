/*
code by Xaiier
*/

package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicAnim;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class XHAN_MindControl extends BaseShipSystemScript {

    private final boolean DEBUG = true;

    public static final Color JITTER_COLOR = new Color(255, 155, 255, 75);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 155, 255, 155);
    public static Color TEXT_COLOR = new Color(255, 155, 255, 255);
    private static final Color CHARGEUP_GLOW_COLOUR = new Color(213, 28, 255, 255);

    protected static float RANGE = 1500f;

    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
    }

    public static void changeSides(ShipAPI ship) {
        if (ship.getOwner() == 100) return; //dead ships

        final int newOwner = (ship.getOwner() == 0 ? 1 : 0);

        if (newOwner == 0) {
            ship.setAlly(true);
        } else {
            ship.setAlly(false);
        }

        // Switch to the opposite side
        ship.setOwner(newOwner);
        //ship.setOriginalOwner(newOwner); //I think this affects post-battle recovery

        // Force AI to re-evaluate surroundings
        if (ship.getShipAI() != null) {
            ship.getShipAI().forceCircumstanceEvaluation();
        }

        // Also switch sides of any drones (doesn't affect any new ones)
        if (ship.getDeployedDrones() != null) {
            for (ShipAPI drone : ship.getDeployedDrones()) {
                drone.setOwner(newOwner);
                drone.getShipAI().forceCircumstanceEvaluation();
            }
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        //make sure we only apply the effect once - this method will be called multiple times during IN state
        if (state == State.IN && ship.getCustomData().get("XHAN_MindControl_applied") == null) {
            ship.setCustomData("XHAN_MindControl_applied", ""); //set applied flag

            final ShipAPI target = findTarget(ship);
            if (target != null) { //possibility findTarget no longer has a valid target when this is called

                MindControlData mindControlData = (MindControlData) target.getCustomData().get("XHAN_MindControl");
                if (mindControlData == null) {
                    mindControlData = new MindControlData(ship, target);
                    target.setCustomData("XHAN_MindControl", mindControlData);
                } else {
                    mindControlData = (MindControlData) target.getCustomData().get("XHAN_MindControl");
                }

                //draw floating text
                if (target.getFluxTracker().showFloaty() || ship == Global.getCombatEngine().getPlayerShip() || target == Global.getCombatEngine().getPlayerShip()) {
                    target.getFluxTracker().showOverloadFloatyIfNeeded("Mind Controlled!", TEXT_COLOR, 4f, true);
                }

                final MindControlData mindControlDataFinal = mindControlData; //for use within inline BaseEveryFrameCombatPlugin
                if (mindControlDataFinal.targetEffectPlugin == null) {
                    mindControlDataFinal.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {

                        @Override
                        public void init(CombatEngineAPI engine) {
                            changeSides(mindControlDataFinal.target);
                            for (FighterWingAPI wing : mindControlDataFinal.target.getAllWings()) {
                                wing.setWingOwner(mindControlDataFinal.target.getOwner());
                            }
                        }

                        @Override
                        public void advance(float amount, List<InputEventAPI> events) {
                            if (Global.getCombatEngine().isPaused()) return;

                            mindControlDataFinal.elaspedAfterInState += amount;

                            if (DEBUG) Global.getCombatEngine().addFloatingText(mindControlDataFinal.target.getLocation(), Math.floor((mindControlDataFinal.durationEnd - mindControlDataFinal.elaspedAfterInState)) + "s", 50f, Color.white, mindControlDataFinal.target, 0f, 2f);

                            Vector2f delta = new Vector2f();
                            Vector2f.sub(mindControlDataFinal.target.getLocation(), mindControlDataFinal.ship.getLocation(), delta);
                            if (MathUtils.getRandomNumberInRange(0, 10) == 0) Global.getCombatEngine().addSmoothParticle(MathUtils.getRandomPointInCircle(mindControlDataFinal.ship.getShieldCenterEvenIfNoShield(), mindControlDataFinal.target.getShieldRadiusEvenIfNoShield() / 2f),
                                                                                                                         delta,
                                                                                                                         10f,
                                                                                                                         0.5f,
                                                                                                                         0.5f,
                                                                                                                         1f,
                                                                                                                         CHARGEUP_GLOW_COLOUR);


                            float jitterLevel = 1.0f - (mindControlDataFinal.elaspedAfterInState / mindControlDataFinal.durationEnd);
                            jitterLevel = MagicAnim.offsetToRange(jitterLevel, 0.4f, 1.0f);

                            float maxRangeBonus = 25f;
                            float jitterRangeBonus = jitterLevel * maxRangeBonus;

                            mindControlDataFinal.target.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
                            mindControlDataFinal.target.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

                            if ((mindControlDataFinal.elaspedAfterInState > mindControlDataFinal.durationEnd || !mindControlDataFinal.ship.isAlive()) && mindControlDataFinal.target.isAlive()) { //duration over or controlling ship dies, but target is still alive
                                mindControlDataFinal.target.removeCustomData("XHAN_MindControl");
                                changeSides(mindControlDataFinal.target);
                                for (FighterWingAPI wing : mindControlDataFinal.target.getAllWings()) {
                                    wing.setWingOwner(mindControlDataFinal.target.getOwner());
                                }
                                Global.getCombatEngine().removePlugin(mindControlDataFinal.targetEffectPlugin);
                            } else if (!mindControlDataFinal.target.isAlive()) { //target is dead
                                Global.getCombatEngine().removePlugin(mindControlDataFinal.targetEffectPlugin);
                            }
                        }
                    };
                    Global.getCombatEngine().addPlugin(mindControlDataFinal.targetEffectPlugin);
                } else {
                    mindControlDataFinal.ApplyDuration(); //another charge being applied to a ship that is already under mind control
                }
            }
        }

        float jitterLevel = effectLevel;
        if (state == State.OUT) {
            jitterLevel *= jitterLevel;
            ship.removeCustomData("XHAN_MindControl_applied");
        }
        float maxRangeBonus = 25f;
        float jitterRangeBonus = jitterLevel * maxRangeBonus;

        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
        ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

        Vector2f target = ship.getMouseTarget();
        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target);
            float max = getMaxRange(ship) + ship.getCollisionRadius();
            if (dist > max) {
                return "OUT OF RANGE";
            } else {
                return "READY";
            }
        }
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }

    protected ShipAPI findTarget(ShipAPI ship) {
        float range = getMaxRange(ship);
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();
        ShipAPI controlledTarget = null;
        if (target == null) {
            //no target
        }
        else if (target.getHullSize() == ShipAPI.HullSize.FIGHTER) {
            //target is a fighter, invalid
            target = null;
        }
        else if (ship.getOwner() == target.getOwner() && target.getCustomData().get("XHAN_MindControl") != null) {
            //target is an "ally" that is already under mind control
            //save this for later to in case there's no other valid targets in range
            controlledTarget = ship.getShipTarget();
        }
        else if (ship.getOwner() == target.getOwner()) {
            //target is a regular ally, invalid
            target = null;
        }
        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
        } else {
            if (player) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, range, true);
            } else {
                Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                if (test instanceof ShipAPI) {
                    target = (ShipAPI) test;
                    float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                    float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                    if (dist > range + radSum) target = null;
                }
            }
            if (target == null) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FRIGATE, range, true);
            }
        }

        if (target == null) { //target to apply stacks if there's nothing else in range
            target = controlledTarget;
        }

        return target;
    }

    public static class MindControlData {
        public ShipAPI ship;
        public ShipAPI target;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float elaspedAfterInState = 0f;
        public float durationEnd = 0f;

        public MindControlData(ShipAPI ship, ShipAPI target) {
            this.ship = ship;
            this.target = target;
            ApplyDuration();
        }

        public void ApplyDuration() {
            this.durationEnd += Math.pow(100f - this.target.getHullSpec().getSuppliesPerMonth(), 1.4f) / 14f;
        }
    }
}
