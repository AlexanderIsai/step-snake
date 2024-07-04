package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.Dice;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.services.RandomDice;

import java.util.*;

public class YourSolver3 implements Solver<Board> {

    private Dice dice;
    private Board board;
    private static final int LENGTH_THRESHOLD = 40;

    public YourSolver3(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        this.board = board;
        return doSolve(board).toString();
    }

    Direction doSolve(Board board) {
        Point head = board.getHead();
        if (head == null) return Direction.UP;
        Point apple = board.getApples().get(0);
        Point stone = board.getStones().get(0);

        List<Point> barriers = new ArrayList<>(board.getBarriers());
        barriers.addAll(board.getSnake());

        if (board.getSnake().size() >= LENGTH_THRESHOLD) {
            barriers.remove(stone);
            List<Point> pathToStone = findPath(head, stone, barriers);
            if (!pathToStone.isEmpty() && pathToStone.size() > 1) {
                Point nextStep = pathToStone.get(1);
                return getDirection(head, nextStep);
            }
        }

        List<Point> pathToApple = findPath(head, apple, barriers);

        if (!pathToApple.isEmpty() && pathToApple.size() > 1) {
            Point nextStep = pathToApple.get(1);
            if (canUnwindAfterEating(nextStep, apple, barriers)) {
                return getDirection(head, nextStep);
            }
        }

        barriers.remove(stone);
        List<Point> pathToStone = findPath(head, stone, barriers);
        if (!pathToStone.isEmpty() && pathToStone.size() > 1) {
            Point nextStep = pathToStone.get(1);
            return getDirection(head, nextStep);
        }

        List<Point> snake = board.getSnake();
        if (snake.size() > 1) {
            Point secondSegment = snake.get(1);
            return getDirection(head, secondSegment);
        }

        return getSafeDirection(head, barriers);
    }

    private boolean canUnwindAfterEating(Point nextStep, Point apple, List<Point> barriers) {
        List<Point> futureSnake = new ArrayList<>(board.getSnake());
        futureSnake.add(0, nextStep);
        futureSnake.add(0, apple);

        futureSnake.remove(futureSnake.size() - 1);

        barriers.addAll(futureSnake);
        for (Direction direction : Direction.values()) {
            Point futureHead = direction.change(apple);
            if (!barriers.contains(futureHead) && isWithinBounds(futureHead, board.size())) {
                return true;
            }
        }
        return false;
    }

    private List<Point> findPath(Point start, Point end, List<Point> barriers) {
        Set<Point> visited = new HashSet<>();
        PriorityQueue<Node> noVisited = new PriorityQueue<>(Comparator.comparingInt(node -> node.f));
        Map<Point, Point> prePoint = new HashMap<>();

        Node startNode = new Node(start, 0, getHeuristicCost(start, end));
        noVisited.add(startNode);

        while (!noVisited.isEmpty()) {
            Node current = noVisited.poll();

            if (current.point.equals(end)) {
                return createPath(prePoint, current.point);
            }

            visited.add(current.point);

            for (Point neighbor : getNeighbors(current.point)) {
                if (visited.contains(neighbor) || barriers.contains(neighbor)) {
                    continue;
                }

                int tentativeG = current.g + 1;

                Node neighborNode = new Node(neighbor, tentativeG, getHeuristicCost(neighbor, end));

                if (!noVisited.contains(neighborNode) || tentativeG < neighborNode.g) {
                    prePoint.put(neighbor, current.point);
                    neighborNode.g = tentativeG;
                    neighborNode.f = neighborNode.g + neighborNode.h;

                    if (!noVisited.contains(neighborNode)) {
                        noVisited.add(neighborNode);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Point> createPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private List<Point> getNeighbors(Point point) {
        List<Point> neighbors = new ArrayList<>();
        neighbors.add(new PointImpl(point.getX() + 1, point.getY()));
        neighbors.add(new PointImpl(point.getX() - 1, point.getY()));
        neighbors.add(new PointImpl(point.getX(), point.getY() + 1));
        neighbors.add(new PointImpl(point.getX(), point.getY() - 1));
        return neighbors;
    }

    private int getHeuristicCost(Point a, Point b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private Direction getSafeDirection(Point head, List<Point> barriers) {
        List<Direction> directions = Arrays.asList(Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN);
        for (Direction direction : directions) {
            Point next = direction.change(head);
            if (!barriers.contains(next)) {
                return direction;
            }
        }
        return Direction.UP;
    }

    private boolean isWithinBounds(Point point, int boardSize) {
        return point.getX() >= 0 && point.getX() < boardSize && point.getY() >= 0 && point.getY() < boardSize;
    }

    private Direction getDirection(Point from, Point to) {
        if (to.getX() < from.getX()) return Direction.LEFT;
        if (to.getX() > from.getX()) return Direction.RIGHT;
        if (to.getY() < from.getY()) return Direction.DOWN;
        if (to.getY() > from.getY()) return Direction.UP;
        return null;
    }

    static class Node {
        Point point;
        int g;
        int h;
        int f;

        Node(Point point, int g, int h) {
            this.point = point;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }

    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // вставьте сюда URL страницы с доской из браузера после регистрации
                "http://138.197.189.109/codenjoy-contest/board/player/5k1qubs8qwzzbhfvsi1c?code=4542803429519937099",
                new YourSolver3(new RandomDice()),
                new Board()
        );
    }
}
