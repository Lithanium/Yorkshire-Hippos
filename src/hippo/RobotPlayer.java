package hippo;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public strictfp class RobotPlayer {
    // Variables
    static int turnCount = 0;
    static int exploreMap = -1;
    static int botNo = -1;

    // Generate the random seed
    static Random rng;

    //Location it should defend
    static MapLocation toDefend = null;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static void run(RobotController rc) throws GameActionException {
        // Make the rng seed
        if (rng == null) rng = new Random(rc.getID());

        // This is how long you want to explore the map
        // NOTE: NOT USED YET
        if (exploreMap == -1) exploreMap = rc.getMapHeight() + rc.getMapWidth();

        while (true) {
            // Check for global upgrades
            // First get ACTION and then HEALING
            if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) rc.buyGlobal(GlobalUpgrade.ACTION);
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) rc.buyGlobal(GlobalUpgrade.HEALING);

            turnCount += 1;
            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                // Assign a botNo
                if (botNo == -1) {
                    botNo = rc.readSharedArray(0) + 1;
                    rc.writeSharedArray(0, botNo);
                }

                // Try spawn in the bot
                if (!rc.isSpawned()){
                    MapLocation[] spawnLoc = rc.getAllySpawnLocations();
                    if (botNo <= 17) {
                        // Spawner 1 (0 -> 8, 4 is centre)
                        for (int i = 0; i <= 8; i ++) {
                            if (rc.canSpawn(spawnLoc[i])) {
                                rc.spawn(spawnLoc[i]);
                                break;
                            }
                        }
                        toDefend = spawnLoc[4];
                    } else if (botNo <= 34) {
                        // Spawner 2 (9 -> 17, 13 is centre)
                        for (int i = 9; i <= 17; i ++) {
                            if (rc.canSpawn(spawnLoc[i])) {
                                rc.spawn(spawnLoc[i]);
                                break;
                            }
                        }
                        toDefend = spawnLoc[13];
                    } else {
                        // Spawner 3 (18 -> 26, 22 is centre)
                        for (int i = 18; i <= 26; i ++) {
                            if (rc.canSpawn(spawnLoc[i])) {
                                rc.spawn(spawnLoc[i]);
                                break;
                            }
                        }
                        toDefend = spawnLoc[22];
                    }
                }

                // Actual code
                if (rc.isSpawned()) {
                    // Relevant robots to interact with
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
                    // Choose the best robots to attack
                    MapLocation toHeal = null;
                    int lowestHeal = 99999;
                    MapLocation toAttack = null;
                    int lowestAttack = 99999;
                    for (RobotInfo currRobot : nearbyRobots) {
                        // Checks for which team it's on
                        if (currRobot.team == rc.getTeam()) {
                            // Friendly team, try to heal it
                            if (currRobot.health < lowestHeal && rc.canHeal(currRobot.location)) {
                                lowestHeal = currRobot.health;
                                toHeal = currRobot.location;
                            }
                        } else {
                            // Enemy team, try attack it
                            if (currRobot.hasFlag && rc.canAttack(currRobot.location)) {
                                toAttack = currRobot.location;
                                break;
                            }
                            if (currRobot.health < lowestAttack && rc.canAttack(currRobot.location)) {
                                lowestAttack = currRobot.health;
                                toAttack = currRobot.location;
                            }
                        }
                    }
                    if (toAttack != null && rc.canAttack(toAttack)) rc.attack(toAttack);
                    if (toHeal != null && rc.canHeal(toHeal)) rc.heal(toHeal);

                    // Build explosive traps on spawn point
                    for (Direction dir : directions) {
                        if (!rc.canSenseLocation(rc.getLocation().add(dir))) continue;
                        MapInfo thisSquare = rc.senseMapInfo(rc.getLocation().add(dir));
                        if (thisSquare.isSpawnZone() && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(dir))) {
                            rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(dir));
                        }
                    }

                    // Explore and get crumbs time
                    if (turnCount <= exploreMap) {
                        Pathfind.explore(rc);
                    } else if (turnCount <= 170) {
                        if (rc.getLocation().distanceSquaredTo(toDefend) > 2) {
                            Pathfind.moveTowardsV1(rc, toDefend);
                        }
                    } else {
                        for (Direction checkValid : directions) {
                            // Dig where possible
                            MapLocation currLoc = rc.getLocation().add(checkValid);
                            if (rc.canDig(currLoc) && rc.getCrumbs() > 350)
                                rc.dig(currLoc);
                        }
                    }
                } else {
                    Pathfind.resetVar();
                }
            } catch (GameActionException e) {
                // Bad things happen
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Bad things happen
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // End the turn
                Clock.yield();
            }
        }
    }
}