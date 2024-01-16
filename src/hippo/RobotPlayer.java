//   0 1 2 3 4 5 6 7 8 9
// 0 F     H H H F H A A
// 1         A A A A A
// 2     A       A A
// 3 A
// 4 H A
// 5 H A A
// 6 F A A
// 7 H A
// 8 A A
// 9 A

package hippo;
import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
    // Settings
    static int numDefence = 30;
    static int numBuilder = 0;
    static int numAttack = 20;

    static final MapLocation[] flagLocationsLayout = {
            new MapLocation(0, 0), new MapLocation(0, 6), new MapLocation(6, 0)
    };
    static final MapLocation[] defenceLayout = {
            new MapLocation(0, 0), new MapLocation(2, 2), new MapLocation(0, 8), new MapLocation(8, 0), new MapLocation(1, 7), new MapLocation(7, 1), new MapLocation(1, 6), new MapLocation(6, 1),
            new MapLocation(0, 7), new MapLocation(7, 0), new MapLocation(0, 6), new MapLocation(6, 0), new MapLocation(0, 9), new MapLocation(9, 0), new MapLocation(1, 8), new MapLocation(8, 1),
            new MapLocation(2, 7), new MapLocation(7, 2), new MapLocation(2, 6), new MapLocation(6, 2), new MapLocation(0, 5), new MapLocation(5, 0), new MapLocation(1, 5), new MapLocation(5, 1),
            new MapLocation(0, 4), new MapLocation(4, 0), new MapLocation(1, 4), new MapLocation(4, 1), new MapLocation(0, 3), new MapLocation(3, 0)

    };
    static final int[] defenceLayoutRole = { // 0 = attack, 1 = heal prioritise attackers, 2 = heal prioritise healers     (Healers will attack when more than 4 ducks in the formation have been killed)
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 0, 0,
            1, 1, 0, 0, 2, 2
    };

    // Game Variables
    static int turnCount = 0;
    static int roundNum = 0;
    static int id = -1;

    // Code Variables
    static int role = -1; // 0 = defence, 1 = builder, 2 = attacker
    static boolean flagInPosition = false;

    // Generate the random seed
    static Random rng;

    //Array containing all the possible movement directions.
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
        System.out.println("123");

        while (true) {
            // Check for global upgrades
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
                rc.buyGlobal(GlobalUpgrade.HEALING);
            // First get HEALING and then action
            if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
                rc.buyGlobal(GlobalUpgrade.ACTION);

            turnCount += 1;

            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                // get round number
                roundNum = rc.getRoundNum();
                rc.setIndicatorString(String.valueOf(id));
                Pathfind.turnDir = rng.nextInt(2);

                // get indexed 0 bot id on round 1 and set role
                if (roundNum == 1) {
                    // get id
                    int i = rc.readSharedArray(0);
                    id = i;
                    rc.writeSharedArray(0, i + 1);
                    // set role of bot
                    if (role == -1 && id < numDefence) role = 0;
                    else if (role == -1 && id < numDefence + numBuilder) role = 1;
                    else if (role == -1) role = 2;
                    Clock.yield();
                }
                if (roundNum == 2) {
                    rc.writeSharedArray(0, 0);
                }

                // Try spawn in the bot
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in
                    // Make the robot retreat to the location it spawned in
                    MapLocation spawnLoc;
                    if (id < 3 && roundNum < 150) {
                        spawnLoc = spawnLocs[id * 9];
                    } else {
                        spawnLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    }
                    if (rc.canSpawn(spawnLoc)) {
                        rc.spawn(spawnLoc);
                    }
                }

                // Actual code
                if (rc.isSpawned()) {
                    // defence
                    if (role == 0) {
                        // bring flags into positions
                        if (roundNum < 200 && id < 3 && !flagInPosition) {
                            FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
                            for (FlagInfo i : nearbyFlags) {
                                if (rc.canPickupFlag(i.getLocation())) {
                                    rc.pickupFlag(i.getLocation());
                                }
                            }
                            if (rc.hasFlag()) {
                                Pathfind.moveTowards(rc, flagLocationsLayout[id]);

                                if (rc.canDropFlag(flagLocationsLayout[id])) {
                                    rc.dropFlag(flagLocationsLayout[id]);
                                    flagInPosition = true;
                                }
                            }
                            Clock.yield();
                        }
                        if (roundNum < 180) {
                            Pathfind.explore(rc);
                            Clock.yield();
                        }
                        // defensive layout
                        else {
                            for (int i = 0; i < defenceLayout.length; i++) {
                                MapLocation locToDefend = defenceLayout[i];
                                boolean occupied = false;
                                if (i < 15) {
                                    occupied = Bit.get(rc.readSharedArray(0), i);
                                } else {
                                    occupied = Bit.get(rc.readSharedArray(1), i % 15);
                                }
                                if (!occupied) { // loc to defend isn't occupied
                                    Pathfind.moveTowards(rc, locToDefend); // move towards loc
                                }
                                if (locToDefend.equals(rc.getLocation())) {
                                    if (!occupied) {
                                        if (i < 15) {
                                            rc.writeSharedArray(0, Bit.write((short) rc.readSharedArray(0), i, true));
                                        } else {
                                            rc.writeSharedArray(1, Bit.write((short) rc.readSharedArray(1), i % 15, true));
                                        }
                                    }
                                    if (defenceLayoutRole[i] == 0) { // attack
                                        attackNearby(rc);
                                        healNearby(rc);
                                    }
                                    if (defenceLayoutRole[i] == 1) { // heal
                                        healNearby(rc);
                                        attackNearby(rc);
                                    }
                                    if (defenceLayoutRole[i] == 2) { // heal healers
                                        healNearbyHealers(rc);
                                        healNearby(rc);
                                        attackNearby(rc);
                                    }
                                    break;
                                }
                                attackNearby(rc);
                                healNearby(rc);
                                if (!occupied) {
                                    break;
                                }
                            }
                        }
                    }

                    // attacker
                    if (role == 2) {
                        // Explore and get crumbs time
                        if (turnCount < 150) {
                            Pathfind.explore(rc);
                        } else {
                            Pathfind.moveTowards(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                        }
                    }




//                    // Retreat to designated location
//                    else if (turnCount < 200) {
//                        Pathfind.moveTowards(rc, new MapLocation(0, 0));
//                    } else {
//                        // Sense if there is a flag nearby
//                        FlagInfo[] closeFlags = rc.senseNearbyFlags(-1, rc.getTeam());
//
//                        // This is misleading, this is just
//                        for (FlagInfo currFlag : closeFlags) {
//                            // Choose and random direction to place a flag
//                            Direction randomDirection = Direction.allDirections()[rng.nextInt(8)];
//
//                            // If it still has plenty of crumbs
//                            if (rc.getCrumbs() > 500)
//                                // Build a trap if it can
//                                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(randomDirection)))
//                                    rc.build(TrapType.STUN, rc.getLocation().add(randomDirection));
//                            break;
//                        }
//
//                        // Dig water for levels
//                        for (Direction checkValid : directions) {
//                            // Dig where possible
//                            MapLocation currLoc = rc.getLocation().add(checkValid);
//                            if (rc.canDig(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
//                                rc.dig(currLoc);
//                        }
//
//                        // Fill water for levels
//                        for (Direction checkValid: directions) {
//                            // Fill where possible
//                            MapLocation currLoc = rc.getLocation().add(checkValid);
//                            if (rc.canFill(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
//                                rc.fill(currLoc);
//                        }
//                    }




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

    public static void healNearby(RobotController rc) throws GameActionException {
        // Relevant robots to interact with
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
        for (RobotInfo currRobot : nearbyRobots) {
            // Checks for which team it's on
            if (currRobot.team == rc.getTeam()) {
                // Friendly team, try to heal it
                if (rc.canHeal(currRobot.location))
                    rc.heal(currRobot.location);
            }
        }
    }

    public static void healNearbyHealers(RobotController rc) throws GameActionException {
        // Relevant robots to interact with
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
        for (RobotInfo currRobot : nearbyRobots) {
            // Checks for which team it's on
            if (currRobot.team == rc.getTeam()) {
                // Friendly team, try to heal it
                for (int i = 0; i < defenceLayout.length; i++) {
                    if (defenceLayout[i].equals(currRobot.location)) {
                        if (defenceLayoutRole[i] == 1 || defenceLayoutRole[i] == 2) {
                            if (rc.canHeal(currRobot.location))
                                rc.heal(currRobot.location);
                        }
                    }
                }
            }
        }
    }

    public static void attackNearby(RobotController rc) throws GameActionException {
        // Relevant robots to interact with
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
        for (RobotInfo currRobot : nearbyRobots) {
            // Checks for which team it's on
            if (currRobot.team != rc.getTeam()) {
                // Enemy team, try attack it
                if (rc.canAttack(currRobot.location))
                    rc.attack(currRobot.location);
            }
        }
    }
}