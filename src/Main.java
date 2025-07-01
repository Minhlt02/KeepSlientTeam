import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.socket.data.emit_data.PlayerUseItemAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "132497";
    private static final String PLAYER_NAME = "KeepSilentTeam";
    private static final String SECRET_KEY = "sk-_1irmgqtSXaXZgo_RzZX7g:Golt35Z3_QRxULejbH8i-QuX5PPd7phYr93oiySC4U5ZYSSdXhYmF_4epe36F6y5rc8KEOuFiEQzv-ftDvleKg";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }

            List<Node> nodesToAvoid = getNodesToAvoid(gameMap);
            Node safePlayer = new Node(gameMap.getSafeZone(), gameMap.getSafeZone());
            boolean checkSafeZone = PathUtils.checkInsideSafeArea(gameMap.getCurrentPlayer(), gameMap.getSafeZone(), gameMap.getMapSize());
            String getShortestSafeZone = PathUtils.getShortestPath(gameMap, nodesToAvoid, player.getPosition(), safePlayer, false);
            if (checkSafeZone) {
                if (hero.getInventory().getGun() == null) {
                    handleSearchForGun(gameMap, player, nodesToAvoid);
                } else {
                    handleShootEnemy(gameMap, player, nodesToAvoid);
                }
            } else {
                hero.move(getShortestSafeZone);
            }



        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSearchForGun(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No gun found. Searching for a gun.");
        String pathToGun = findPathToGun(gameMap, nodesToAvoid, player);

        Node safePlayer = new Node(gameMap.getSafeZone(), gameMap.getSafeZone());
        boolean checkSafeZone = PathUtils.checkInsideSafeArea(gameMap.getCurrentPlayer(), gameMap.getSafeZone(), gameMap.getMapSize());
        String getShortestSafeZone = PathUtils.getShortestPath(gameMap, nodesToAvoid, player.getPosition(), safePlayer, false);

        if (pathToGun != null) {
            if (!checkSafeZone) {
                hero.move(getShortestSafeZone);
            } else {
                if (pathToGun.isEmpty()) {
                    hero.pickupItem();
                } else {
                    hero.move(pathToGun);
                }
            }
        }
    }

    private void handleShootEnemy(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        String pathToOtherPlayer = findPathToOtherPlayer(gameMap, nodesToAvoid, player);
        System.out.println("Found enemy at: " + pathToOtherPlayer);
        Player player1 = getNearestPlayer(gameMap,player);

        int oldPos = player1.getY();


        if (pathToOtherPlayer != null) {
            if (player1.getY() != oldPos) {
                if (player.getX() == player1.getX() && player.getY() - player1.getY() < 3 || pathToOtherPlayer.length() < 3) {
                    if (pathToOtherPlayer.length() > 1) {
                        hero.shoot(String.valueOf(pathToOtherPlayer.charAt(1)));
                    } else if (pathToOtherPlayer.length() == 1) {
                        hero.shoot(pathToOtherPlayer);
                    }
                    System.out.println("P Pos: " + player.getPosition());
                    System.out.println("E Pos: " + player1.getPosition());
                    System.out.println("Shoot: " + pathToOtherPlayer);
                } else {
                    hero.move(pathToOtherPlayer);
                    System.out.println("P Pos: " + player.getPosition());
                    System.out.println("E Pos: " + player1.getPosition());
                    System.out.println("Move: " + pathToOtherPlayer);
                }
            } else {
                if (player.getY() == player1.getY() && player.getX() - player1.getX() < 3 || pathToOtherPlayer.length() < 3) {
                    if (pathToOtherPlayer.length() > 1) {
                        hero.shoot(String.valueOf(pathToOtherPlayer.charAt(1)));
                    } else if (pathToOtherPlayer.length() == 1) {
                        hero.shoot(pathToOtherPlayer);
                    }
                    System.out.println("P Pos: " + player.getPosition());
                    System.out.println("E Pos: " + player1.getPosition());
                    System.out.println("Shoot: " + pathToOtherPlayer);
                } else {
                    hero.move(pathToOtherPlayer);
                    System.out.println("P Pos: " + player.getPosition());
                    System.out.println("E Pos: " + player1.getPosition());
                    System.out.println("Move: " + pathToOtherPlayer);
                }
            }

        }
    }

    private List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());

        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        return nodes;
    }

    private String findPathToGun(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Weapon nearestGun = getNearestGun(gameMap, player);
        if (nearestGun == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);
    }

    private String findPathToEnemy(GameMap gameMap, List<Node> nodesToAvoid, Player player) throws IOException {
        Enemy enemy = getNearestEnemy(gameMap, player);

        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, enemy, false);
    }

    private String findPathToOtherPlayer(GameMap gameMap, List<Node> nodesToAvoid, Player player) throws IOException {
        Player otherPlayer = getNearestPlayer(gameMap, player);

        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, otherPlayer, false);
    }

    private Weapon getNearestGun(GameMap gameMap, Player player) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            double distance = PathUtils.distance(player, gun);
            if (distance < minDistance) {
                minDistance = distance;
                nearestGun = gun;
            }
        }
        return nearestGun;
    }

    private Enemy getNearestEnemy(GameMap gameMap, Player player) {
        List<Enemy> enemies = gameMap.getListEnemies();
        Enemy nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            double distance = PathUtils.distance(player, enemy);
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = enemy;
            }
        }
        return nearestEnemy;
    }

    private Player getNearestPlayer(GameMap gameMap, Player player) {
        List<Player> playerList = gameMap.getOtherPlayerInfo();
        Player nearestPlayer = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player1 : playerList) {
            double distance = PathUtils.distance(player, player1);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPlayer= player1;
            }
        }
        return nearestPlayer;
    }
}