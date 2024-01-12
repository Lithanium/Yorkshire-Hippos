package hippoV1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public strictfp class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    // Variables
    static int turnCount = 0;
    static int exploreMap = -1;
    static int retreatLength = 20;

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
            turnCount += 1;
            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                // Try spawn in the bot
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in
                    // Make the robot retreat to the location it spawned in
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) {
                        rc.spawn(randomLoc);
                        toDefend = randomLoc;
                    }
                }

                // Actual code
                if (rc.isSpawned()) {
                    // First heal units when possible

                    // Next attack units if seen

                    // Explore and get crumbs time
                    if (turnCount < 170)
                        Pathfind.explore(rc);
                    // Retreat to designated location
                    else if (turnCount < 200)
                        Pathfind.moveTowards(rc, toDefend);
                    else {
                        // Sense if there is a flag nearby
                        FlagInfo[] closeFlags = rc.senseNearbyFlags(-1, rc.getTeam());

                        // This is misleading, this is just
                        for (FlagInfo currFlag : closeFlags) {
                            // Choose and random direction to place a flag
                            Direction randomDirection = Direction.allDirections()[rng.nextInt(8)];

                            // If it still has plenty of crumbs
                            if (rc.getCrumbs() > 500)
                                // Build a trap if it can
                                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(randomDirection)))
                                    rc.build(TrapType.STUN, rc.getLocation().add(randomDirection));
                            break;
                        }

                        // Dig water for levels
                        for (Direction checkValid : directions) {
                            // Dig where possible
                            MapLocation currLoc = rc.getLocation().add(checkValid);
                            if (rc.canDig(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
                                rc.dig(currLoc);
                        }

                        // Fill water for levels
                        for (Direction checkValid: directions) {
                            // Fill where possible
                            MapLocation currLoc = rc.getLocation().add(checkValid);
                            if (rc.canFill(currLoc) && rc.getLevel(SkillType.BUILD) < 4)
                                rc.fill(currLoc);
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
}
