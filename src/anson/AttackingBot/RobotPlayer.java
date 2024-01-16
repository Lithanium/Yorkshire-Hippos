package AttackingBot;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    // Variables
    static int noTeams = 9;
    static int turnCount = 0;
    static int exploreMap = -1;
    static int retreatLength = 20;
    static int shouldMove;
    static int botNo;

    // Generate the random seed
    static Random rng;

    //Location it should defend
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
                if(botNo >= noTeams && botNo<=noTeams+2) {
                    utils.defendSpawn(rc);
                    continue;
                }
                // Try spawn in the bot
                if (!rc.isSpawned()){
                    MapLocation target = null;
                    int maxno = 0;
                    for(int i = noTeams; i<noTeams+3; i++){
                        if(rc.readSharedArray(i)>maxno || target == null){
                            target = rc.getAllySpawnLocations()[i%3*9];
                            maxno = rc.readSharedArray(i);
                        }
                    }
                    if(!utils.Valid(rc,target)){
                        MapLocation []nearflags = rc.senseBroadcastFlagLocations();
                        if(nearflags.length>0) {
                            target = nearflags[botNo % nearflags.length];
                        }
                    }
                    rc.writeSharedArray(botNo,target.x*100+target.y);
                    if(!utils.Valid(rc,target) || turnCount<200) {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        // Pick a random spawn location to attempt spawning in
                        // Make the robot retreat to the location it spawned in
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                        }
                    }else{
                        utils.spawnClosest(rc,target);
                    }
                }

                // Actual code
                if (rc.isSpawned()) {
                    if (turnCount < 160)
                        Pathfind.explore(rc);
                    else{
                        if(rc.hasFlag()){
                            utils.whenHasFlag(rc);
                        }
                        FlagInfo[] flags = rc.senseNearbyFlags(-1);
                        for(FlagInfo flag : flags){
                            utils.whenSensedFlag(rc,flag);
                        }
                        utils.attackAndHeal(rc,botNo);
                        if(target == rc.getLocation()){
                            Pathfind.explore(rc);
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