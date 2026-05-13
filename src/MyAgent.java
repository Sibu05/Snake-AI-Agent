import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {
    private int width;
    private int height;
    private int nSnakes;
    private int gameStep = 0;
    private int lastAppleX = -1, lastAppleY = -1;
    private int appleAge = 0;

    private Map<Integer, List<int[]>> opponentHistory = new HashMap<>();
    private Map<Integer, int[]> lastOpponentPositions = new HashMap<>();
    private Map<Integer, Integer> opponentLengths = new HashMap<>();

    private Queue<int[]> plannedPath = new LinkedList<>();

    public static void main(String[] args) {
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            String[] initParams = initString.split(" ");
            nSnakes = Integer.parseInt(initParams[0]);
            width = Integer.parseInt(initParams[1]);
            height = Integer.parseInt(initParams[2]);

            while (true) {
                String line = br.readLine();
                if (line == null || line.contains("Game Over")) {
                    break;
                }

                gameStep++;

                String[] appleCoords = line.split(" ");
                int appleX = Integer.parseInt(appleCoords[0]);
                int appleY = Integer.parseInt(appleCoords[1]);

                if (lastAppleX != appleX || lastAppleY != appleY) {
                    appleAge = 0;
                    lastAppleX = appleX;
                    lastAppleY = appleY;
                    plannedPath.clear();
                } else {
                    appleAge++;
                }

                int appleValue = calculateAppleValue(appleAge);
                int mySnakeNum = Integer.parseInt(br.readLine());

                Set<String> occupied = new HashSet<>();
                Set<String> nextTurnOccupied = new HashSet<>();
                List<int[]> mySnake = new ArrayList<>();
                List<List<int[]>> allSnakes = new ArrayList<>();
                Map<Integer, SnakeInfo> snakeInfos = new HashMap<>();

                for (int i = 0; i < nSnakes; i++) {
                    String snakeLine = br.readLine();
                    String[] parts = snakeLine.split(" ");

                    if (parts[0].equals("dead")) {
                        allSnakes.add(new ArrayList<>());
                        continue;
                    }

                    int length = Integer.parseInt(parts[1]);
                    int kills = Integer.parseInt(parts[2]);
                    List<int[]> snake = new ArrayList<>();

                    for (int j = 3; j < parts.length; j++) {
                        String[] xy = parts[j].split(",");
                        int x = Integer.parseInt(xy[0]);
                        int y = Integer.parseInt(xy[1]);
                        snake.add(new int[] { x, y });
                    }
                    List<int[]> fullSnakeBody = expandSnakeBody(snake, length);
                    allSnakes.add(fullSnakeBody);

                    snakeInfos.put(i, new SnakeInfo(fullSnakeBody, length, kills));

                    if (i == mySnakeNum) {
                        mySnake = fullSnakeBody;
                    } else if (!fullSnakeBody.isEmpty()) {
                        updateOpponentHistory(i, fullSnakeBody.get(0), length);
                    }
                }

                if (mySnake.isEmpty()) {
                    System.out.println(0);
                    continue;
                }

                int[] head = mySnake.get(0);

                for (List<int[]> snake : allSnakes) {
                    for (int[] segment : snake) {
                        occupied.add(segment[0] + "," + segment[1]);
                    }
                }

                nextTurnOccupied = predictOpponentMoves(allSnakes, mySnakeNum, appleX, appleY);

                boolean willEatApple = false;
                Map<Integer, int[]> testMoves = new HashMap<>();
                testMoves.put(0, new int[] { head[0], head[1] - 1 });
                testMoves.put(1, new int[] { head[0], head[1] + 1 });
                testMoves.put(2, new int[] { head[0] - 1, head[1] });
                testMoves.put(3, new int[] { head[0] + 1, head[1] });

                for (int[] newPos : testMoves.values()) {
                    if (newPos[0] == appleX && newPos[1] == appleY) {
                        willEatApple = true;
                        break;
                    }
                }

                if (!willEatApple && mySnake.size() > 1) {
                    int[] tail = mySnake.get(mySnake.size() - 1);
                    occupied.remove(tail[0] + "," + tail[1]);
                }

                List<Integer> safeMoves = getSafeMoves(head, occupied);
                List<Integer> preferredMoves = getPreferredMoves(safeMoves, head, nextTurnOccupied);

                int move = chooseBestMoveEnhanced(preferredMoves, safeMoves, head, appleX, appleY,
                        occupied, nextTurnOccupied, appleValue, mySnake.size(), gameStep, allSnakes, mySnakeNum);

                System.out.println(move);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateOpponentHistory(int snakeId, int[] headPos, int length) {
        if (!opponentHistory.containsKey(snakeId)) {
            opponentHistory.put(snakeId, new ArrayList<>());
        }

        opponentHistory.get(snakeId).add(new int[] { headPos[0], headPos[1] });

        if (opponentHistory.get(snakeId).size() > 10) {
            opponentHistory.get(snakeId).remove(0);
        }

        lastOpponentPositions.put(snakeId, headPos);
        opponentLengths.put(snakeId, length);
    }

    private Set<String> predictOpponentMoves(List<List<int[]>> allSnakes, int mySnakeNum, int appleX, int appleY) {
        Set<String> predicted = new HashSet<>();

        for (int i = 0; i < allSnakes.size(); i++) {
            if (i == mySnakeNum || allSnakes.get(i).isEmpty())
                continue;

            int[] opponentHead = allSnakes.get(i).get(0);
            Set<int[]> possibleMoves = predictOpponentNextMoves(i, opponentHead, appleX, appleY);

            for (int[] move : possibleMoves) {
                if (move[0] >= 0 && move[0] < width && move[1] >= 0 && move[1] < height) {
                    predicted.add(move[0] + "," + move[1]);
                }
            }
        }

        return predicted;
    }

    private Set<int[]> predictOpponentNextMoves(int snakeId, int[] head, int appleX, int appleY) {
        Set<int[]> moves = new HashSet<>();
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        List<int[]> history = opponentHistory.get(snakeId);

        for (int[] dir : directions) {
            moves.add(new int[] { head[0] + dir[0], head[1] + dir[1] });
        }

        if (history != null && history.size() >= 3) {
            int appleDistance = Math.abs(head[0] - appleX) + Math.abs(head[1] - appleY);
            if (appleDistance <= 3) {
                int[] appleDirection = getDirectionToTarget(head, appleX, appleY);
                if (appleDirection != null) {
                    for (int i = 0; i < 3; i++) {
                        moves.add(new int[] { head[0] + appleDirection[0], head[1] + appleDirection[1] });
                    }
                }
            }

            int[] recentDirection = getRecentMovementDirection(history);
            if (recentDirection != null) {
                moves.add(new int[] { head[0] + recentDirection[0], head[1] + recentDirection[1] });
            }
        }

        return moves;
    }

    private int[] getDirectionToTarget(int[] from, int targetX, int targetY) {
        int dx = targetX - from[0];
        int dy = targetY - from[1];

        if (Math.abs(dx) > Math.abs(dy)) {
            return new int[] { dx > 0 ? 1 : -1, 0 };
        } else if (Math.abs(dy) > 0) {
            return new int[] { 0, dy > 0 ? 1 : -1 };
        }

        return null;
    }

    private int[] getRecentMovementDirection(List<int[]> history) {
        if (history.size() < 2)
            return null;

        int[] last = history.get(history.size() - 1);
        int[] prev = history.get(history.size() - 2);

        return new int[] { last[0] - prev[0], last[1] - prev[1] };
    }

    private List<Integer> getSafeMoves(int[] head, Set<String> occupied) {
        List<Integer> safeMoves = new ArrayList<>();
        Map<Integer, int[]> moves = new HashMap<>();
        moves.put(0, new int[] { head[0], head[1] - 1 });
        moves.put(1, new int[] { head[0], head[1] + 1 });
        moves.put(2, new int[] { head[0] - 1, head[1] });
        moves.put(3, new int[] { head[0] + 1, head[1] });

        for (Map.Entry<Integer, int[]> entry : moves.entrySet()) {
            int[] pos = entry.getValue();
            if (isSafeMove(pos[0], pos[1], occupied)) {
                safeMoves.add(entry.getKey());
            }
        }

        return safeMoves;
    }

    private List<Integer> getPreferredMoves(List<Integer> safeMoves, int[] head, Set<String> nextTurnOccupied) {
        List<Integer> preferred = new ArrayList<>();

        for (int move : safeMoves) {
            int[] newPos = getNewPosition(head, move);
            if (!nextTurnOccupied.contains(newPos[0] + "," + newPos[1])) {
                preferred.add(move);
            }
        }

        return preferred;
    }

    private int chooseBestMoveEnhanced(List<Integer> preferredMoves, List<Integer> safeMoves,
            int[] head, int appleX, int appleY, Set<String> occupied, Set<String> nextTurnOccupied,
            int appleValue, int snakeLength, int gameStep, List<List<int[]>> allSnakes, int mySnakeNum) {

        List<Integer> movesToConsider = !preferredMoves.isEmpty() ? preferredMoves : safeMoves;
        if (movesToConsider.isEmpty()) {
            return new Random().nextInt(4);
        }

        boolean appleIsSafe = isAppleSafeToEat(appleValue, snakeLength);

        EnhancedTrapAnalysis trapStatus = analyzeTrapSituationBFS(head, occupied, appleX, appleY);

        if (trapStatus.isTrapped || trapStatus.dangerLevel >= 4) {
            return chooseEscapeMoveBFS(movesToConsider, head, occupied, trapStatus);
        }

        if (appleIsSafe && appleValue >= 1 && trapStatus.dangerLevel <= 2) {
            int appleMoveChoice = executeMultiTurnAppleStrategy(movesToConsider, head, appleX, appleY,
                    occupied, nextTurnOccupied, trapStatus);
            if (appleMoveChoice != -1) {
                return appleMoveChoice;
            }
        }

        int aggressiveMove = evaluateAggressiveOpportunities(movesToConsider, head, allSnakes,
                mySnakeNum, snakeLength, occupied, nextTurnOccupied);
        if (aggressiveMove != -1) {
            return aggressiveMove;
        }

        return chooseSurvivalMoveEnhanced(movesToConsider, head, appleX, appleY, occupied,
                nextTurnOccupied, gameStep, snakeLength, trapStatus);
    }

    private EnhancedTrapAnalysis analyzeTrapSituationBFS(int[] pos, Set<String> occupied, int appleX, int appleY) {
        EnhancedTrapAnalysis analysis = new EnhancedTrapAnalysis();

        Set<String> reachable = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();
        queue.add(pos);
        reachable.add(pos[0] + "," + pos[1]);

        int maxDepth = 8;
        int currentDepth = 0;

        while (!queue.isEmpty() && currentDepth < maxDepth) {
            int size = queue.size();
            currentDepth++;

            for (int i = 0; i < size; i++) {
                int[] current = queue.poll();
                int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

                for (int[] dir : directions) {
                    int nx = current[0] + dir[0];
                    int ny = current[1] + dir[1];
                    String key = nx + "," + ny;

                    if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                            !occupied.contains(key) && !reachable.contains(key)) {
                        reachable.add(key);
                        queue.add(new int[] { nx, ny });
                    }
                }
            }
        }

        analysis.reachableArea = reachable.size();
        analysis.isTrapped = reachable.size() < 5;

        if (reachable.size() < 3) {
            analysis.dangerLevel = 5;
        } else if (reachable.size() < 8) {
            analysis.dangerLevel = 4;
        } else if (reachable.size() < 15) {
            analysis.dangerLevel = 3;
        } else if (reachable.size() < 25) {
            analysis.dangerLevel = 2;
        } else {
            analysis.dangerLevel = 1;
        }

        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
        for (int[] dir : directions) {
            int nx = pos[0] + dir[0];
            int ny = pos[1] + dir[1];
            if (isSafeMove(nx, ny, occupied)) {
                analysis.safeExits++;
            }
        }

        if (analysis.safeExits == 0) {
            analysis.isTrapped = true;
            analysis.dangerLevel = 5;
        }

        return analysis;
    }

    private int chooseEscapeMoveBFS(List<Integer> moves, int[] head, Set<String> occupied,
            EnhancedTrapAnalysis trapStatus) {
        int bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int move : moves) {
            int[] newPos = getNewPosition(head, move);

            EnhancedTrapAnalysis futureTraps = analyzeTrapSituationBFS(newPos, occupied, -1, -1);
            int score = futureTraps.reachableArea * 10;

            int centerX = width / 2;
            int centerY = height / 2;
            int distanceFromCenter = Math.abs(newPos[0] - centerX) + Math.abs(newPos[1] - centerY);
            score -= distanceFromCenter * 2;

            int edgeDistance = Math.min(Math.min(newPos[0], width - 1 - newPos[0]),
                    Math.min(newPos[1], height - 1 - newPos[1]));
            score += edgeDistance * 5;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int executeMultiTurnAppleStrategy(List<Integer> moves, int[] head, int appleX, int appleY,
            Set<String> occupied, Set<String> nextTurnOccupied, EnhancedTrapAnalysis trapStatus) {

        int[] applePath = findSafePathToAppleBFS(head, appleX, appleY, occupied);

        if (applePath != null) {
            int firstMove = getDirectMoveToPosition(head, applePath[0], applePath[1]);
            if (firstMove != -1 && moves.contains(firstMove)) {

                int[] newPos = getNewPosition(head, firstMove);
                EnhancedTrapAnalysis futureTraps = analyzeTrapSituationBFS(newPos, occupied, appleX, appleY);

                if (futureTraps.dangerLevel <= 3) {
                    return firstMove;
                }
            }
        }

        int directMove = getDirectMoveToApple(head, appleX, appleY);
        if (directMove != -1 && moves.contains(directMove)) {
            int[] newPos = getNewPosition(head, directMove);
            if (!nextTurnOccupied.contains(newPos[0] + "," + newPos[1])) {
                return directMove;
            }
        }

        return -1;
    }

    private int[] findSafePathToAppleBFS(int[] start, int appleX, int appleY, Set<String> occupied) {
        if (start[0] == appleX && start[1] == appleY) {
            return null;
        }

        Queue<PathNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(new PathNode(start[0], start[1], null));
        visited.add(start[0] + "," + start[1]);

        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();

            if (current.x == appleX && current.y == appleY) {
                PathNode step = current;
                while (step.parent != null && step.parent.parent != null) {
                    step = step.parent;
                }
                return new int[] { step.x, step.y };
            }

            for (int[] dir : directions) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                String key = nx + "," + ny;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                        !occupied.contains(key) && !visited.contains(key)) {

                    visited.add(key);
                    queue.add(new PathNode(nx, ny, current));
                }
            }
        }

        return null;
    }

    private int evaluateAggressiveOpportunities(List<Integer> moves, int[] head,
            List<List<int[]>> allSnakes, int mySnakeNum, int myLength,
            Set<String> occupied, Set<String> nextTurnOccupied) {

        for (int move : moves) {
            int[] newPos = getNewPosition(head, move);

            for (int i = 0; i < allSnakes.size(); i++) {
                if (i == mySnakeNum || allSnakes.get(i).isEmpty())
                    continue;

                List<int[]> opponent = allSnakes.get(i);
                int[] opponentHead = opponent.get(0);

                int distance = Math.abs(newPos[0] - opponentHead[0]) + Math.abs(newPos[1] - opponentHead[1]);

                if (distance <= 2 && myLength > opponent.size() + 2) {

                    int opponentEscapeRoutes = countEscapeRoutes(opponentHead, occupied);

                    if (opponentEscapeRoutes <= 2) {
                        EnhancedTrapAnalysis futureTraps = analyzeTrapSituationBFS(newPos, occupied, -1, -1);

                        if (futureTraps.dangerLevel <= 2) {
                            return move;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private int countEscapeRoutes(int[] pos, Set<String> occupied) {
        int routes = 0;
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        for (int[] dir : directions) {
            int nx = pos[0] + dir[0];
            int ny = pos[1] + dir[1];

            if (isSafeMove(nx, ny, occupied)) {
                routes++;
            }
        }

        return routes;
    }

    private int chooseSurvivalMoveEnhanced(List<Integer> moves, int[] head, int appleX, int appleY,
            Set<String> occupied, Set<String> nextTurnOccupied, int gameStep, int snakeLength,
            EnhancedTrapAnalysis trapStatus) {

        int bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int move : moves) {
            int[] newPos = getNewPosition(head, move);
            int score = 0;

            EnhancedTrapAnalysis futureTraps = analyzeTrapSituationBFS(newPos, occupied, appleX, appleY);
            score += futureTraps.reachableArea * 8;

            if (nextTurnOccupied.contains(newPos[0] + "," + newPos[1])) {
                score -= 50;
            }

            int centerX = width / 2;
            int centerY = height / 2;
            int distanceFromCenter = Math.abs(newPos[0] - centerX) + Math.abs(newPos[1] - centerY);
            score -= distanceFromCenter * 3;

            int distanceFromApple = Math.abs(newPos[0] - appleX) + Math.abs(newPos[1] - appleY);
            if (!isAppleSafeToEat(calculateAppleValue(appleAge), snakeLength)) {
                score += distanceFromApple * 5;
            }

            int edgeDistance = Math.min(Math.min(newPos[0], width - 1 - newPos[0]),
                    Math.min(newPos[1], height - 1 - newPos[1]));
            score += edgeDistance * 4;

            if (gameStep % 30 < 15) {
                if (move == 2 || move == 3)
                    score += 3;
            } else {
                if (move == 0 || move == 1)
                    score += 3;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int getDirectMoveToPosition(int[] from, int targetX, int targetY) {
        int dx = targetX - from[0];
        int dy = targetY - from[1];

        if (Math.abs(dx) + Math.abs(dy) == 1) {
            if (dx == 0 && dy == -1)
                return 0;
            if (dx == 0 && dy == 1)
                return 1;
            if (dx == -1 && dy == 0)
                return 2;
            if (dx == 1 && dy == 0)
                return 3;
        }

        return -1;
    }

    private static class EnhancedTrapAnalysis {
        boolean isTrapped = false;
        int dangerLevel = 0;
        int safeExits = 0;
        int reachableArea = 0;
    }

    private static class SnakeInfo {
        List<int[]> body;
        int length;
        int kills;

        SnakeInfo(List<int[]> body, int length, int kills) {
            this.body = body;
            this.length = length;
            this.kills = kills;
        }
    }

    private static class PathNode {
        int x, y;
        PathNode parent;

        PathNode(int x, int y, PathNode parent) {
            this.x = x;
            this.y = y;
            this.parent = parent;
        }
    }

    private int calculateAppleValue(int age) {
        double rawValue = 5.0 - (age * 0.1);
        return (int) Math.ceil(rawValue);
    }

    private boolean isAppleSafeToEat(int appleValue, int snakeLength) {
        if (appleValue <= -4)
            return false;
        if (snakeLength <= 3 && appleValue < 0)
            return false;
        if (snakeLength <= 5 && appleValue <= -2)
            return false;
        return true;
    }

    private List<int[]> expandSnakeBody(List<int[]> coordinateChain, int length) {
        if (coordinateChain.isEmpty())
            return new ArrayList<>();

        List<int[]> fullBody = new ArrayList<>();

        if (coordinateChain.size() == 1) {
            fullBody.add(new int[] { coordinateChain.get(0)[0], coordinateChain.get(0)[1] });
            return fullBody;
        }

        for (int i = 0; i < coordinateChain.size() - 1; i++) {
            int[] start = coordinateChain.get(i);
            int[] end = coordinateChain.get(i + 1);

            if (fullBody.isEmpty() || !positionExists(fullBody, start)) {
                fullBody.add(new int[] { start[0], start[1] });
            }

            if (start[0] == end[0]) {
                int step = start[1] < end[1] ? 1 : -1;
                for (int y = start[1] + step; y != end[1]; y += step) {
                    if (!positionExists(fullBody, new int[] { start[0], y })) {
                        fullBody.add(new int[] { start[0], y });
                    }
                }
            } else if (start[1] == end[1]) {
                int step = start[0] < end[0] ? 1 : -1;
                for (int x = start[0] + step; x != end[0]; x += step) {
                    if (!positionExists(fullBody, new int[] { x, start[1] })) {
                        fullBody.add(new int[] { x, start[1] });
                    }
                }
            }
        }

        int[] lastCoord = coordinateChain.get(coordinateChain.size() - 1);
        if (!positionExists(fullBody, lastCoord)) {
            fullBody.add(new int[] { lastCoord[0], lastCoord[1] });
        }

        while (fullBody.size() > length) {
            fullBody.remove(fullBody.size() - 1);
        }

        return fullBody;
    }

    private boolean positionExists(List<int[]> body, int[] pos) {
        for (int[] segment : body) {
            if (segment[0] == pos[0] && segment[1] == pos[1]) {
                return true;
            }
        }
        return false;
    }

    private boolean isSafeMove(int x, int y, Set<String> occupied) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        return !occupied.contains(x + "," + y);
    }

    private int[] getNewPosition(int[] head, int moveDir) {
        switch (moveDir) {
            case 0:
                return new int[] { head[0], head[1] - 1 };
            case 1:
                return new int[] { head[0], head[1] + 1 };
            case 2:
                return new int[] { head[0] - 1, head[1] };
            case 3:
                return new int[] { head[0] + 1, head[1] };
            default:
                return head;
        }
    }

    private int getDirectMoveToApple(int[] head, int appleX, int appleY) {
        int dx = appleX - head[0];
        int dy = appleY - head[1];

        if (Math.abs(dx) + Math.abs(dy) == 1) {
            if (dx == 0 && dy == -1)
                return 0;
            if (dx == 0 && dy == 1)
                return 1;
            if (dx == -1 && dy == 0)
                return 2;
            if (dx == 1 && dy == 0)
                return 3;
        }

        return -1;
    }
}