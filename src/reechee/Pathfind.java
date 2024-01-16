package reechee;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

    // Variables for path find v2
    private static int bugState = 0;
    private static MapLocation closestObstacle = null;
    private static int closestObstacleDist = 99999;
    private static Direction pathDir = null;

    // Variables for path find v3
    private static MapLocation prevDest = null;
    private static HashSet<MapLocation> destLine = null;
    private static int obstacleStartDist = 0;

    public static void resetVar() {
        bugState = 0;
        closestObstacle = null;
        closestObstacleDist = 99999;
        pathDir = null;
    }

    private static HashSet<MapLocation> createLine(MapLocation a, MapLocation b) {
        HashSet<MapLocation> locs = new HashSet<>();
        int x = a.x, y = a.y;
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int sx = (int) Math.signum(dx);
        int sy = (int) Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        int d = Math.max(dx, dy);
        int r = d/2;
        if (dx > dy) {
            for (int i = 0; i < d; i ++) {
                locs.add(new MapLocation(x, y));
                x += sx;
                r += dy;
                if (r >= dx) {
                    locs.add(new MapLocation(x, y));
                    y += sy;
                    r -= dx;
                }
            }
        } else {
            for (int i = 0; i < d; i ++) {
                locs.add(new MapLocation(x, y));
                y += sy;
                r += dx;
                if (r >= dy) {
                    locs.add(new MapLocation(x, y));
                    x += sx;
                    r -= dy;
                }
            }
        }
        locs.add(new MapLocation(x, y));
        return locs;
    }

    public static void moveTowardsV1(RobotController rc, MapLocation loc) throws GameActionException{
        // Find the best direction to go to target
        Direction dir = rc.getLocation().directionTo(loc);

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
                    break;
                }

                // Rotate 45 degrees right and try again
                dir = dir.rotateRight();
            }
        }
    }

    public static void moveTowardsV2(RobotController rc, MapLocation loc) throws GameActionException {
        // bugState: 0 for head towards obstacle, 1 for circle obstacle
        if (bugState == 0) {
            // If you can move, move. Otherwise, entire obstacle circle mode
            pathDir = rc.getLocation().directionTo(loc);
            if (rc.canFill(rc.getLocation().add(pathDir))) {
                rc.fill(rc.getLocation().add(pathDir));
            }
            if (rc.canMove(pathDir)) {
                rc.move(pathDir);
            } else {
                bugState = 1;
                closestObstacle = null;
                closestObstacleDist = 99999;
            }
        } else {
            if (rc.getLocation().equals(closestObstacle)) {
                bugState = 0;
            }
            if (rc.getLocation().distanceSquaredTo(loc) < closestObstacleDist) {
                closestObstacleDist = rc.getLocation().distanceSquaredTo(loc);
                closestObstacle = rc.getLocation();
            }
            for (int i = 0; i < 8; i ++) {
                if (rc.canFill(rc.getLocation().add(pathDir))) {
                    rc.fill(rc.getLocation().add(pathDir));
                }
                if (rc.canMove(pathDir)) {
                    rc.move(pathDir);
                    pathDir = pathDir.rotateLeft();
                    pathDir = pathDir.rotateLeft();
                    break;
                } else {
                    pathDir = pathDir.rotateRight();
                }
            }
        }
    }

    public static void moveTowards(RobotController rc, MapLocation loc) throws GameActionException {
        // If needs a new destination
        if (!loc.equals(prevDest)) {
            prevDest = loc;
            destLine = createLine(rc.getLocation(), loc);
        }

        for (MapLocation point : destLine) {
            rc.setIndicatorDot(point, 255, 0, 0);
        }

        // bugState: 0 for head towards obstacle, 1 for circle obstacle
        if (bugState == 0) {
            // If you can move, move. Otherwise, entire obstacle circle mode
            pathDir = rc.getLocation().directionTo(loc);
            MapInfo toMove = rc.senseMapInfo(rc.getLocation().add(pathDir));
            if (rc.canFill(rc.getLocation().add(pathDir))) {
                rc.fill(rc.getLocation().add(pathDir));
            }
            if (rc.canMove(pathDir)) {
                rc.move(pathDir);
            } else if (!toMove.isWater()) {
                bugState = 1;
                obstacleStartDist = rc.getLocation().distanceSquaredTo(loc);
            }
        } else {
            // Checks if it sees the line again
            if (destLine.contains(rc.getLocation()) && rc.getLocation().distanceSquaredTo(loc) < obstacleStartDist) {
                bugState = 0;
                return;
            }

            // Follows around the obstacle
            for (int i = 0; i < 8; i ++) {
                if (rc.canFill(rc.getLocation().add(pathDir))) {
                    rc.fill(rc.getLocation().add(pathDir));
                }
                if (rc.canMove(pathDir)) {
                    rc.move(pathDir);
                    pathDir = pathDir.rotateLeft();
                    break;
                } else {
                    pathDir = pathDir.rotateRight();
                }
            }
        }
    }

    public static void explore(RobotController rc) throws GameActionException{
        /*
        // If water can be filled, fill it
        for (Direction currDirection : directions) {
            if (rc.canFill(rc.getLocation().add(currDirection))) {
                rc.fill(rc.getLocation().add(currDirection));
            }
        }
        */
        MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
        MapLocation target = null;
        int bestDist = 99999;

        // Iterate through the crumbs and choose the first one to get
        for (MapLocation crumb : crumbs) {
            if (rc.getLocation().distanceSquaredTo(crumb) < bestDist) {
                target = crumb;
                bestDist = rc.getLocation().distanceSquaredTo(crumb);
            }
        }

        if (target != null) {
            // If there are crumbs to get, get them
            moveTowards(rc, target);
        } else {
            // Do the normal exploration
            if (rc.isMovementReady()) {
                // If there is not a set direction, choose a random direction

                // If you can keep moving in that direction, do it unless there is a random change of direction
                if (rc.canFill(rc.getLocation().add(exploreDirection))) {
                    rc.fill(rc.getLocation().add(exploreDirection));
                }
                if (rc.canMove(exploreDirection) && RobotPlayer.rng.nextInt(50) != 0) rc.move(exploreDirection);

                    // If it has reached the end, pick a random number of turns in a random direction
                else {
                    // Random number of turns that are needed to do
                    // Guarantees that it doesn't to an 180 degree turn
                    exploreDirection = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
                }
            }
        }
    }
}