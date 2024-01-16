package reechee;

import battlecode.common.*;

import java.awt.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.abs;

public strictfp class RobotPlayer {
    // SHARED ARRAY INFO
    // 0 - 2: Information about team flags - contains x, y, number of booms placed

    // Settings
    static int numDefence = 3;
    static int flagMove1 = 3;
    static int flagMove2 = 5;
    static TrapType defensiveTrapType = TrapType.EXPLOSIVE;

    // Game Variables
    static int roundNum = 0;
    static int id = -1;

    // Code Variables
    static Random rng;
    static int teamFlagInfo = -1;
    static MapLocation teamFlagLocation = null;
    static int role = -1;
    // 0: Defender, 1: Attacker
    static MapLocation flagTargetLocation = null;
    static Direction[] oscillationDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };
    static int currentOscillationDirection = 0;

    public static void run(RobotController rc) throws GameActionException {
        // Make the rng seed
        if (rng == null) rng = new Random(rc.getID());

        while (true) {
            try {
                rc.setIndicatorString(String.valueOf(id));

                //Get global upgrades if available
                getGlobalUpgrades(rc);

                // Get info about bot
                roundNum = rc.getRoundNum();

                // Get Bot ID on round one (0 indexed)
                if (roundNum == 1) {
                    id = rc.readSharedArray(0);
                    rc.writeSharedArray(0, id + 1);
                    if (id < numDefence) role = 0;
                    else role = 1;
                    Clock.yield();
                }
                // Reset shared array after id's are set
                if (roundNum == 2) {
                    rc.writeSharedArray(0, 0);
                }

                // If the bot isn't spawned
                if (!rc.isSpawned()){
                    if (id > numDefence) {
                        // Attempt to spawn the bot (arbitrarily)

                        //REMOVED FOR TESTING
                        //spawnBot(rc);
                    } else {
                        // Spawn the bot in a location with a flag without a defender
                        // Identify possible spawn locations
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        MapLocation spawnLoc = null;
                        // Choose an available location to spawn
                        for (MapLocation i : spawnLocs) {
                            boolean isTaken = false;
                            for (int j = 0; j < numDefence; j++) {
                                if (j == id) {
                                    continue;
                                }
                                if (abs(i.x - rc.readSharedArray(j) / 60) < 4 && abs(i.y - rc.readSharedArray(j) % 60) < 4) {
                                    isTaken = true;
                                }
                            }
                            if (rc.canSpawn(i) && !isTaken) {
                                spawnLoc = i;
                            }
                        }
                        // Spawn in that location
                        if (spawnLoc != null) {
                            rc.spawn(spawnLoc);
                        }
                    }
                }

                // If the robot has been spawned
                if (rc.isSpawned()) {
                    // If the bot is a defender
                    if (role == 0) {
                        // Identify nearby flag/s
                        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
                        // Double check there is a flag to avoid errors
                        if (nearbyFlags.length > 0) {
                            // Broadcast flag's x and y
                            if (teamFlagInfo == -1) {
                                teamFlagInfo = nearbyFlags[0].getLocation().x * 60 + nearbyFlags[0].getLocation().y;
                                rc.writeSharedArray(id, teamFlagInfo);
                                teamFlagLocation = nearbyFlags[0].getLocation();
                            }

                            // In the setup phase, move the flag
                            if (roundNum < 150) {
                                // Find target location (2, 1, 0 tiles corner-ward depending on how far to the side it is)
                                int x = 0, y = 0, sx = 0, sy = 0;
                                MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                                for (MapLocation i : spawnLocs) {
                                    sx += i.x;
                                    sy += i.y;
                                }
                                sx = sx / 27;
                                sy = sy / 27;
                                if (sx < rc.getMapWidth() / flagMove1) x = -1;
                                if (sx < rc.getMapWidth() / flagMove2) x = -2;
                                if (sx > rc.getMapWidth() - (rc.getMapWidth() / flagMove1)) x = 1;
                                if (sx > rc.getMapWidth() - (rc.getMapWidth() / flagMove2)) x = 2;

                                if (sy < rc.getMapHeight() / flagMove1) y = -1;
                                if (sy < rc.getMapHeight() / flagMove2) y = -2;
                                if (sy > rc.getMapHeight() - (rc.getMapHeight() / flagMove1)) y = 1;
                                if (sy > rc.getMapHeight() - (rc.getMapHeight() / flagMove2)) y = 2;

                                flagTargetLocation = new MapLocation(teamFlagLocation.x + x, teamFlagLocation.y + y);

                                rc.setIndicatorString(id + ": " + sx + " " + sy);

                                // Move the flag if it is not in the target position
                                if (!nearbyFlags[0].getLocation().equals(flagTargetLocation)) {
                                    if (rc.canPickupFlag(nearbyFlags[0].getLocation())) {
                                        rc.pickupFlag(nearbyFlags[0].getLocation());
                                    }
                                    if (rc.hasFlag()) {
                                        if (rc.canDropFlag(flagTargetLocation)) {
                                            rc.dropFlag(flagTargetLocation);
                                        }
                                        Pathfind.moveTowards(rc, flagTargetLocation);
                                        if (rc.canDropFlag(flagTargetLocation)) {
                                            rc.dropFlag(flagTargetLocation);
                                        }
                                    }
                                }
                            }
                            // In the main phase, build traps around the spawn zone
                            else {
                                // Drop the flag if it's still holding the flag
                                if (rc.hasFlag()) {
                                    if (rc.canDropFlag(flagTargetLocation)) {
                                        rc.dropFlag(flagTargetLocation);
                                    }
                                }
                                // Oscillate around the spawn zone
                                if (rc.getLocation().equals(teamFlagLocation.add(oscillationDirections[currentOscillationDirection]))) {
                                    currentOscillationDirection += 1;
                                    if (currentOscillationDirection == 8) {
                                        currentOscillationDirection = 0;
                                    }
                                }
                                Pathfind.moveTowardsV1(rc, teamFlagLocation.add(oscillationDirections[currentOscillationDirection]));
                                // Place traps on rounds where _  for even traps
                                if (roundNum % numDefence == id) placeTrapsNearFlag(rc);
                            }
                        }
                    }

                    // If the bot is an attacker
                    if (role == 1) {
                        // In the setup phase, explore to gather crumbs
                        if (roundNum < 150) {
                            Pathfind.explore(rc);
                            Clock.yield();
                        }
                    }
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

    public static void getGlobalUpgrades(RobotController rc) throws  GameActionException {
        // Check for global upgrades
        if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
            rc.buyGlobal(GlobalUpgrade.HEALING);
        // First get HEALING and then action
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
            rc.buyGlobal(GlobalUpgrade.ACTION);
    }

    public static void spawnBot(RobotController rc) throws  GameActionException {
        // Identify possible spawn locations
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation spawnLoc = null;
        // Choose an available location to spawn
        for (MapLocation i : spawnLocs) {
            if (rc.canSpawn(i)) {
                spawnLoc = i;
            }
        }
        // Spawn in that location
        if (spawnLoc != null) {
            rc.spawn(spawnLoc);
        }
    }

    public static void xDefense(RobotController rc) throws  GameActionException {
        // Get all possible building locations
        MapInfo[] buildLocs = rc.senseNearbyMapInfos(2);
        MapLocation buildLoc = null;
        int distanceToFlag = 1000000;
        boolean isTrap = false; // if false, water
        // Find build location closest to flag
        for (MapInfo i : buildLocs) {
            if (abs(i.getMapLocation().x - flagTargetLocation.x) == abs(i.getMapLocation().y - flagTargetLocation.y)) {
                if (rc.canBuild(defensiveTrapType, i.getMapLocation())) {
                    if (flagTargetLocation.distanceSquaredTo(i.getMapLocation()) < distanceToFlag) {
                        distanceToFlag = flagTargetLocation.distanceSquaredTo(i.getMapLocation());
                        buildLoc = i.getMapLocation();
                        isTrap = true;
                    }
                }
            } else {
                if (rc.canDig(i.getMapLocation())) {
                    if (flagTargetLocation.distanceSquaredTo(i.getMapLocation()) < distanceToFlag) {
                        distanceToFlag = flagTargetLocation.distanceSquaredTo(i.getMapLocation());
                        buildLoc = i.getMapLocation();
                        isTrap = false;
                    }
                }
            }
        }
        // If a location is found, build there
        if (buildLoc != null) {
            if (isTrap) rc.build(defensiveTrapType, buildLoc);
            else rc.dig(buildLoc);
        }
    }

    public static void placeTrapsNearFlag(RobotController rc) throws  GameActionException {
        // Get all possible building locations
        MapInfo[] buildLocs = rc.senseNearbyMapInfos(2);
        MapLocation buildLoc = null;
        int distanceToFlag = 1000000;
        // Find build location closest to flag
        for (MapInfo i : buildLocs) {
            if (rc.canBuild(defensiveTrapType, i.getMapLocation())) {
                if (flagTargetLocation.distanceSquaredTo(i.getMapLocation()) < distanceToFlag) {
                    distanceToFlag = flagTargetLocation.distanceSquaredTo(i.getMapLocation());
                    buildLoc = i.getMapLocation();
                }
            }
        }
        // If a location is found, build there
        if (buildLoc != null) {
            rc.build(defensiveTrapType, buildLoc);
        }
    }

    public static void healNearby(RobotController rc) throws GameActionException {
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
        if (toHeal != null && rc.canHeal(toHeal)) rc.heal(toHeal);
        if (toAttack != null && rc.canHeal(toAttack)) rc.attack(toAttack);
    }

    public static void attackNearby(RobotController rc) throws GameActionException {
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
        if (toAttack != null && rc.canHeal(toAttack)) rc.attack(toAttack);
        if (toHeal != null && rc.canHeal(toHeal)) rc.heal(toHeal);
    }
}