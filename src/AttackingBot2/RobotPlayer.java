package AttackingBot2;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    // Variables
    static int noTeams = 6;
    static int turnCount = 0;
    static int exploreMap = -1;
    static int retreatLength = 20;
    static int shouldMove;
    static int botNo;

    // Generate the random seed
    static Random rng;

    //Location it should defend
    static MapLocation toDefend = null;
    static MapLocation target = null;

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
        if (RobotPlayer.rng == null) RobotPlayer.rng = new Random(rc.getID());

        // This is how long you want to explore the map
        // NOTE: NOT USED YET
        if (exploreMap == -1) exploreMap = rc.getMapHeight() + rc.getMapWidth();
        MapLocation goal = null;
        while (true) {

            if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
                rc.buyGlobal(GlobalUpgrade.ACTION);
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
            }

            turnCount += 1;
            //Try/Catch blocks stop random stuff happening, idk what it does but "it just works"
            try {
                int roundNum = rc.getRoundNum();
                if(roundNum == 1){
                    botNo = utils.getBotNum(rc);
                    Clock.yield();
                }
                if(botNo >= noTeams && botNo<=noTeams+2){
                    utils.defendSpawn(rc);
                    continue;
                }
                // Try spawn in the bot
                if (!rc.isSpawned()){
                    if(target==null) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        // Pick a random spawn location to attempt spawning in
                        // Make the robot retreat to the location it spawned in
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                            toDefend = randomLoc;
                        }
                    }else{
                        utils.spawnClosest(rc,target);
                    }
                }

                // Actual code
                if (rc.isSpawned()) {
                    // Relevant robots to interact with
                    if(rc.getRoundNum()>200) {
                        utils.attackAndHeal(rc,botNo);
                    }
                    MapLocation[] people = new MapLocation[50];
                    for(int i = 0; i<50; i++) {
                        people[i] = new MapLocation((rc.readSharedArray(i) % 10000) / 100, rc.readSharedArray(i) % 100);
                    }
                    // Explore and get crumbs time
                    if (turnCount < 200)
                        Pathfind.explore(rc,botNo);
                        // Retreat to designated location
                    else{
                        MapLocation[] targets = rc.senseBroadcastFlagLocations();
                        FlagInfo[] flags = rc.senseNearbyFlags(-1);
                        if(botNo>=noTeams) {
                            for (FlagInfo flag : flags) {
                                if (flag.getTeam() != rc.getTeam() && !flag.isPickedUp()) {
                                    if (rc.canPickupFlag(flag.getLocation())) {
                                        rc.pickupFlag(flag.getLocation());
                                        goal = utils.ClosestSpawn(rc);
                                    } else {
                                        goal = flag.getLocation();
                                    }
                                    break;
                                }
                            }
                        }
                        if (goal != null && (rc.getLocation() == goal || rc.getLocation().isAdjacentTo(goal))){
                            if(rc.hasFlag()){
                                if(rc.canDropFlag(goal)){
                                    rc.dropFlag(goal);
                                    goal = null;
                                }
                            }else{
                                goal = null;
                            }
                        }
                        if (goal != null) {
                            Pathfind.moveTowards(rc, goal, botNo);
                        } else if(botNo < noTeams) {
                            if (targets.length > 0) {
                                goal = targets[botNo%targets.length];
                                target = targets[botNo%targets.length];
                                Pathfind.moveTowards(rc, targets[0], botNo);
                            } else {
                                Pathfind.explore(rc,botNo);
                            }
                            rc.setIndicatorDot(target,1,1,1);
                        } else{
                            MapLocation temp = new MapLocation((rc.readSharedArray(botNo%noTeams)%10000)/100,rc.readSharedArray(0)%100);
                            target = temp;
                            if (!rc.getLocation().isAdjacentTo(temp)) {
                                Pathfind.moveTowards(rc, temp, botNo);
                            }
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