package AttackingBot2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfind {

    static int turnDir = RobotPlayer.rng.nextInt(2);

    static Direction exploreDirection = Direction.NORTH;

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
    static MapLocation lastLocation = null;
    static int tried_turns = 0;
    static Direction dir;
    public static void moveTowards(RobotController rc, MapLocation loc, int botNo) throws GameActionException{
        // Find the best direction to go to target
        if(lastLocation == null || lastLocation != loc){
            tried_turns = 0;
            lastLocation = loc;
        }
        dir = rc.getLocation().directionTo(loc);
        tried_turns ++;
        if(tried_turns%30 == 0){
            turnDir += 1;
            turnDir%=2;
        }

        // Keep trying to move towards the target unless not possible
        // In that case turn 45 degrees in a random direction
        // To avoid left -> right -> left etc cycles, make it turn 1 direction the entire time
        if (turnDir == 1) {
            // Turn Left
            int attempts = 0;
            while (attempts<8) {
                attempts++;
                // If there is water in the way, fill it
                if (rc.canFill(rc.getLocation().add(dir))) {
                    rc.fill(rc.getLocation().add(dir));
                }

                // Try move in that direction
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    if(rc.getRoundNum()!=1) {
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                    break;
                }

                // Rotate 45 degrees left and try again
                dir = dir.rotateLeft();
            }
        } else {
            // Turn Right
            int attempts = 0;
            while (attempts<8) {
                attempts++;
                // If there is water in the way, fill it
                if (rc.canFill(rc.getLocation().add(dir))) {
                    rc.fill(rc.getLocation().add(dir));
                }

                //Try move in that direction
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    if(rc.getRoundNum()!=1) {
                        rc.writeSharedArray(botNo, (rc.readSharedArray(botNo) / 10000) * 10000 + rc.getLocation().x * 100 + rc.getLocation().y);
                    }
                    break;
                }

                // Rotate 45 degrees right and try again
                dir = dir.rotateRight();
            }
        }
    }

    public static void explore(RobotController rc, int botNo) throws GameActionException{
        // If water can be filled, fill it
        for (Direction currDirection : directions) {
            if (rc.canFill(rc.getLocation().add(currDirection))) {
                rc.fill(rc.getLocation().add(currDirection));
            }
        }

        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
        MapLocation target = null;

        // Iterate through the crumbs and choose the first one to get
        for (MapLocation crumb : crumbs) {
            target = crumb;
            break;
        }

        if (target != null) {
            // If there are crumbs to get, get them
            moveTowards(rc, target,botNo);
        } else {
            // Do the normal exploration
            if (rc.isMovementReady()) {
                // If there is not a set direction, choose a random direction

                // If you can keep moving in that direction, do it unless there is a random change of direction
                if (rc.canMove(exploreDirection) && RobotPlayer.rng.nextInt(50) != 0) rc.move(exploreDirection);

                    // If it has reached the end, pick a random number of turns in a random direction
                else {
                    // Random number of turns that are needed to do
                    // Guarantees that it doesn't to an 180 degree turn
                    /*int todo = RobotPlayer.rng.nextInt(4);

                    for (int i = 0; i <= todo; i ++) {
                        // Keep the random direction different from pathfind to make a better rng distribution
                        if (turnDir == 1) exploreDirection = exploreDirection.rotateRight();
                        else exploreDirection = exploreDirection.rotateLeft();
                    }*/
                    exploreDirection = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
                }
            }
        }
    }
}