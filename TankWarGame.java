import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 游戏状态枚举
enum GameState {
    MENU, PLAYING, PAUSED, GAME_OVER
}

// 难度枚举
enum Difficulty {
    EASY(1500, 80, 2), MEDIUM(1000, 50, 3), HARD(700, 30, 5);

    private final int spawnRate;  // 敌人生成间隔(毫秒)
    private final int fireRate;   // 敌人开火频率(越低越快)
    private final int maxBullets; // 最大子弹数

    Difficulty(int spawnRate, int fireRate, int maxBullets) {
        this.spawnRate = spawnRate;
        this.fireRate = fireRate;
        this.maxBullets = maxBullets;
    }

    public int getSpawnRate() {
        return spawnRate;
    }

    public int getFireRate() {
        return fireRate;
    }

    public int getMaxBullets() {
        return maxBullets;
    }
}

// 方向枚举 - 8个方向
enum Direction {
    UP(0, -1), UP_RIGHT(1, -1), RIGHT(1, 0), DOWN_RIGHT(1, 1),
    DOWN(0, 1), DOWN_LEFT(-1, 1), LEFT(-1, 0), UP_LEFT(-1, -1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    // 获取角度对应的方向
    public static Direction fromAngle(double angle) {
        // 将角度标准化到0-360度
        angle = angle % 360;
        if (angle < 0) angle += 360;

        // 划分8个方向区间
        if (angle >= 337.5 || angle < 22.5) return UP;
        if (angle >= 22.5 && angle < 67.5) return UP_RIGHT;
        if (angle >= 67.5 && angle < 112.5) return RIGHT;
        if (angle >= 112.5 && angle < 157.5) return DOWN_RIGHT;
        if (angle >= 157.5 && angle < 202.5) return DOWN;
        if (angle >= 202.5 && angle < 247.5) return DOWN_LEFT;
        if (angle >= 247.5 && angle < 292.5) return LEFT;
        if (angle >= 292.5 && angle < 337.5) return UP_LEFT;

        return UP; // 默认返回上
    }
}

// 主游戏类
public class TankWarGame extends JFrame {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    public TankWarGame() {
        initUI();
    }

    private void initUI() {
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);

        setTitle("坦克大战");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TankWarGame game = new TankWarGame();
            game.setVisible(true);
        });
    }
}

// 游戏面板类
class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {
    private static final int DELAY = 10;

    private PlayerTank player;
    private List<EnemyTank> enemies;
    private List<Bullet> bullets;
    private List<Explosion> explosions;

    private Timer timer;
    private Random random;
    private int score;
    private int lives;
    private GameState gameState;
    private Difficulty difficulty = Difficulty.MEDIUM;

    private long lastEnemySpawnTime;
    private int maxEnemies = 5;

    public GamePanel() {
        initGame();
    }

    private void initGame() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(TankWarGame.WIDTH, TankWarGame.HEIGHT));
        setFocusable(true);

        addKeyListener(this);
        addMouseListener(this);

        gameState = GameState.MENU;

        random = new Random();
        score = 0;
        lives = 3;

        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void startGame() {
        player = new PlayerTank(375, 500);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        explosions = new ArrayList<>();

        lastEnemySpawnTime = System.currentTimeMillis();
        score = 0;
        lives = 3;
        gameState = GameState.PLAYING;
    }

    private void spawnEnemy() {
        if (enemies.size() < maxEnemies &&
                System.currentTimeMillis() - lastEnemySpawnTime > difficulty.getSpawnRate()) {
            int x = random.nextInt(TankWarGame.WIDTH - 40);
            int y = random.nextInt(TankWarGame.HEIGHT / 3); // 在上部1/3区域生成
            enemies.add(new EnemyTank(x, y, player));
            lastEnemySpawnTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        switch (gameState) {
            case MENU:
                drawMenu(g);
                break;
            case PLAYING:
            case PAUSED:
                drawGame(g);
                if (gameState == GameState.PAUSED) {
                    drawPauseScreen(g);
                }
                break;
            case GAME_OVER:
                drawGame(g);
                drawGameOver(g);
                break;
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawMenu(Graphics g) {
        // 绘制背景
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, TankWarGame.WIDTH, TankWarGame.HEIGHT);

        // 绘制标题
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("坦克大战", 280, 120);

        // 绘制菜单选项
        g.setFont(new Font("Arial", Font.BOLD, 32));

        // 开始游戏
        g.setColor(Color.WHITE);
        g.drawString("开始游戏", 330, 220);

        // 难度选择
        g.setColor(Color.WHITE);
        g.drawString("难度选择", 330, 270);

        // 显示当前难度
        g.setColor(Color.CYAN);
        String difficultyText = "当前难度: " + difficulty.name();
        g.drawString(difficultyText, 330, 320);

        // 退出游戏
        g.setColor(Color.WHITE);
        g.drawString("退出游戏", 330, 370);

        // 绘制操作说明
        g.setColor(Color.GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("方向键移动, 空格键发射", 280, 420);

        // 绘制坦克示例
        drawClassicTank(g, 250, 480, true);  // 玩家坦克
        drawClassicTank(g, 500, 480, false); // 敌人坦克
    }

    private void drawClassicTank(Graphics g, int x, int y, boolean isPlayer) {
        Graphics2D g2d = (Graphics2D) g;

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 坦克颜色
        Color mainColor = isPlayer ? Color.GREEN : Color.RED;
        Color darkColor = isPlayer ? new Color(0, 100, 0) : new Color(150, 0, 0);

        // 坦克底座
        g2d.setColor(mainColor);
        g2d.fillRect(x, y, 40, 40);

        // 坦克履带
        g2d.setColor(darkColor);
        g2d.fillRect(x - 5, y, 5, 40);
        g2d.fillRect(x + 40, y, 5, 40);

        // 坦克炮塔
        g2d.setColor(darkColor);
        g2d.fillOval(x + 5, y + 5, 30, 30);

        // 坦克炮管(朝上)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x + 18, y - 10, 4, 25);

        // 坦克观察窗
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x + 15, y + 15, 10, 10);

        // 坦克顶部装饰
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 18, y + 18, 4, 4);

        // 坦克履带纹路
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(x - 5, y + i * 10, 5, 2);
            g2d.fillRect(x + 40, y + i * 10, 5, 2);
        }
    }

    private void drawGame(Graphics g) {
        if (player != null) {
            player.draw(g);
        }

        if (enemies != null) {
            for (EnemyTank enemy : enemies) {
                enemy.draw(g);
            }
        }

        if (bullets != null) {
            for (Bullet bullet : bullets) {
                bullet.draw(g);
            }
        }

        if (explosions != null) {
            for (Explosion explosion : explosions) {
                explosion.draw(g);
            }
        }

        drawHUD(g);
    }

    private void drawHUD(Graphics g) {
        // 绘制分数
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("分数: " + score, 20, 30);

        // 绘制生命
        g.drawString("生命: " + lives, 20, 60);

        // 绘制难度
        g.drawString("难度: " + difficulty.name(), 20, 90);

        // 绘制暂停提示
        g.drawString("按 P 暂停", 700, 30);
    }

    private void drawPauseScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, TankWarGame.WIDTH, TankWarGame.HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("游戏暂停", 300, 250);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("按 P 继续", 330, 320);
        g.drawString("按 ESC 返回菜单", 300, 370);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, TankWarGame.WIDTH, TankWarGame.HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("游戏结束", 300, 250);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("最终分数: " + score, 320, 320);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("按任意键返回菜单", 300, 370);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            update();
            checkCollisions();
            spawnEnemy();
        }
        repaint();
    }

    private void update() {
        player.update();

        // 更新敌人坦克
        List<EnemyTank> enemiesToRemove = new ArrayList<>();
        for (EnemyTank enemy : enemies) {
            enemy.update();

            // 敌人发射子弹（限制子弹数量）
            int enemyBulletCount = getEnemyBulletCount();
            if (enemyBulletCount < difficulty.getMaxBullets() &&
                    random.nextInt(100) < difficulty.getFireRate() / 10) {
                bullets.add(enemy.fire());
            }

            // 检查敌人是否离开屏幕
            if (enemy.getY() > TankWarGame.HEIGHT) {
                enemiesToRemove.add(enemy);
                lives--;
                if (lives <= 0) {
                    gameState = GameState.GAME_OVER;
                }
            }
        }
        enemies.removeAll(enemiesToRemove);

        // 更新子弹
        List<Bullet> bulletsToRemove = new ArrayList<>();
        for (Bullet bullet : bullets) {
            bullet.update();
            if (bullet.isOutOfBounds()) {
                bulletsToRemove.add(bullet);
            }
        }
        bullets.removeAll(bulletsToRemove);

        // 更新爆炸效果
        List<Explosion> explosionsToRemove = new ArrayList<>();
        for (Explosion explosion : explosions) {
            explosion.update();
            if (explosion.isFinished()) {
                explosionsToRemove.add(explosion);
            }
        }
        explosions.removeAll(explosionsToRemove);
    }

    private int getEnemyBulletCount() {
        int count = 0;
        for (Bullet bullet : bullets) {
            if (!bullet.isPlayerBullet()) {
                count++;
            }
        }
        return count;
    }

    private void checkCollisions() {
        // 检查子弹与坦克的碰撞
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<EnemyTank> enemiesToRemove = new ArrayList<>();

        for (Bullet bullet : bullets) {
            // 只处理玩家发射的子弹对敌人的碰撞
            if (bullet.isPlayerBullet()) {
                for (EnemyTank enemy : enemies) {
                    if (bullet.getBounds().intersects(enemy.getBounds())) {
                        bulletsToRemove.add(bullet);
                        enemiesToRemove.add(enemy);
                        explosions.add(new Explosion(enemy.getX(), enemy.getY()));
                        score += 10;
                        break;
                    }
                }
            }

            // 检查子弹是否击中玩家坦克
            if (!bulletsToRemove.contains(bullet) &&
                    bullet.getBounds().intersects(player.getBounds()) &&
                    !bullet.isPlayerBullet()) {
                bulletsToRemove.add(bullet);
                explosions.add(new Explosion(player.getX(), player.getY()));
                lives--;
                player.reset();
                if (lives <= 0) {
                    gameState = GameState.GAME_OVER;
                }
                break;
            }
        }

        bullets.removeAll(bulletsToRemove);
        enemies.removeAll(enemiesToRemove);

        // 检查玩家坦克与敌人坦克的碰撞
        for (EnemyTank enemy : enemies) {
            if (player.getBounds().intersects(enemy.getBounds())) {
                player.undoMove();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == GameState.PLAYING) {
            player.keyPressed(e);

            // 暂停游戏
            if (key == KeyEvent.VK_P) {
                gameState = GameState.PAUSED;
            }
        } else if (gameState == GameState.PAUSED) {
            // 继续游戏
            if (key == KeyEvent.VK_P) {
                gameState = GameState.PLAYING;
            }
            // 返回菜单
            if (key == KeyEvent.VK_ESCAPE) {
                gameState = GameState.MENU;
            }
        } else if (gameState == GameState.GAME_OVER) {
            // 返回菜单
            gameState = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.PLAYING) {
            player.keyReleased(e);

            // 空格键发射子弹
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                bullets.add(player.fire());
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // 不需要实现
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (gameState == GameState.MENU) {
            int x = e.getX();
            int y = e.getY();

            // 开始游戏
            if (x >= 330 && x <= 470 && y >= 200 && y <= 230) {
                startGame();
            }
            // 难度选择
            else if (x >= 330 && x <= 470 && y >= 250 && y <= 280) {
                cycleDifficulty();
            }
            // 退出游戏
            else if (x >= 330 && x <= 470 && y >= 350 && y <= 380) {
                System.exit(0);
            }
        }
    }

    private void cycleDifficulty() {
        switch (difficulty) {
            case EASY:
                difficulty = Difficulty.MEDIUM;
                break;
            case MEDIUM:
                difficulty = Difficulty.HARD;
                break;
            case HARD:
                difficulty = Difficulty.EASY;
                break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // 不需要实现
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // 不需要实现
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // 不需要实现
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // 不需要实现
    }
}

// 坦克基类
abstract class Tank {
    protected int x, y;
    protected int dx, dy;
    protected int width = 40;
    protected int height = 40;
    protected int speed = 3;
    protected Direction direction = Direction.UP;

    public Tank(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void draw(Graphics g);

    public void update() {
        int prevX = x;
        int prevY = y;

        x += dx;
        y += dy;

        // 边界检查
        if (x < 0) {
            x = 0;
        } else if (x > TankWarGame.WIDTH - width) {
            x = TankWarGame.WIDTH - width;
        }

        if (y < 0) {
            y = 0;
        } else if (y > TankWarGame.HEIGHT - height) {
            y = TankWarGame.HEIGHT - height;
        }
    }

    public void undoMove() {
        x -= dx;
        y -= dy;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Bullet fire() {
        int bulletX = x + width / 2 - 3;
        int bulletY = y + height / 2 - 3;

        return new Bullet(bulletX, bulletY, direction, isPlayerBullet());
    }

    public abstract boolean isPlayerBullet();
}

// 玩家坦克类
class PlayerTank extends Tank {
    private boolean[] keys = new boolean[4]; // 上,右,下,左

    public PlayerTank(int x, int y) {
        super(x, y);
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 坦克颜色
        Color mainColor = Color.GREEN;
        Color darkColor = new Color(0, 100, 0);

        // 坦克底座
        g2d.setColor(mainColor);
        g2d.fillRect(x, y, width, height);

        // 坦克履带
        g2d.setColor(darkColor);
        g2d.fillRect(x - 5, y, 5, 40);
        g2d.fillRect(x + 40, y, 5, 40);

        // 坦克炮塔
        g2d.setColor(darkColor);
        g2d.fillOval(x + 5, y + 5, 30, 30);

        // 坦克炮管（根据方向绘制）
        g2d.setColor(Color.BLACK);
        drawCannon(g2d);

        // 坦克观察窗
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x + 15, y + 15, 10, 10);

        // 坦克顶部装饰
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 18, y + 18, 4, 4);

        // 坦克履带纹路
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(x - 5, y + i * 10, 5, 2);
            g2d.fillRect(x + 40, y + i * 10, 5, 2);
        }
    }

    private void drawCannon(Graphics2D g2d) {
        switch (direction) {
            case UP:
                g2d.fillRect(x + 18, y - 10, 4, 25);
                break;
            case UP_RIGHT:
                g2d.fillPolygon(new int[]{x + 20, x + 30, x + 25},
                        new int[]{y, y + 10, y + 15}, 3);
                break;
            case RIGHT:
                g2d.fillRect(x + 25, y + 18, 25, 4);
                break;
            case DOWN_RIGHT:
                g2d.fillPolygon(new int[]{x + 20, x + 30, x + 25},
                        new int[]{y + 40, y + 30, y + 25}, 3);
                break;
            case DOWN:
                g2d.fillRect(x + 18, y + 25, 4, 25);
                break;
            case DOWN_LEFT:
                g2d.fillPolygon(new int[]{x + 20, x + 10, x + 15},
                        new int[]{y + 40, y + 30, y + 25}, 3);
                break;
            case LEFT:
                g2d.fillRect(x - 10, y + 18, 25, 4);
                break;
            case UP_LEFT:
                g2d.fillPolygon(new int[]{x + 20, x + 10, x + 15},
                        new int[]{y, y + 10, y + 15}, 3);
                break;
        }
    }

    @Override
    public void update() {
        dx = 0;
        dy = 0;

        // 8方向移动逻辑
        if (keys[0] && !keys[2]) { // 上
            dy = -speed;
            if (keys[1] && !keys[3]) {
                dx = speed;
                direction = Direction.UP_RIGHT;
            } else if (!keys[1] && keys[3]) {
                dx = -speed;
                direction = Direction.UP_LEFT;
            } else {
                direction = Direction.UP;
            }
        } else if (!keys[0] && keys[2]) { // 下
            dy = speed;
            if (keys[1] && !keys[3]) {
                dx = speed;
                direction = Direction.DOWN_RIGHT;
            } else if (!keys[1] && keys[3]) {
                dx = -speed;
                direction = Direction.DOWN_LEFT;
            } else {
                direction = Direction.DOWN;
            }
        } else if (keys[1] && !keys[3]) { // 右
            dx = speed;
            direction = Direction.RIGHT;
        } else if (!keys[1] && keys[3]) { // 左
            dx = -speed;
            direction = Direction.LEFT;
        }

        super.update();
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_UP) {
            keys[0] = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
            keys[1] = true;
        }
        if (key == KeyEvent.VK_DOWN) {
            keys[2] = true;
        }
        if (key == KeyEvent.VK_LEFT) {
            keys[3] = true;
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_UP) {
            keys[0] = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            keys[1] = false;
        }
        if (key == KeyEvent.VK_DOWN) {
            keys[2] = false;
        }
        if (key == KeyEvent.VK_LEFT) {
            keys[3] = false;
        }
    }

    public void reset() {
        x = 375;
        y = 500;
    }

    @Override
    public boolean isPlayerBullet() {
        return true;
    }
}

// 敌人坦克类
class EnemyTank extends Tank {
    private PlayerTank player;
    private int moveTime = 0;
    private int moveInterval = 100;
    private Random random;
    private int followTime = 0;
    private int followInterval = 300;

    public EnemyTank(int x, int y, PlayerTank player) {
        super(x, y);
        speed = 1;
        this.player = player;
        random = new Random();
        direction = Direction.DOWN;
        setDirection(direction);
    }

    private void setDirection(Direction dir) {
        direction = dir;
        dx = direction.getDx() * speed;
        dy = direction.getDy() * speed;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 坦克颜色
        Color mainColor = Color.RED;
        Color darkColor = new Color(150, 0, 0);

        // 坦克底座
        g2d.setColor(mainColor);
        g2d.fillRect(x, y, width, height);

        // 坦克履带
        g2d.setColor(darkColor);
        g2d.fillRect(x - 5, y, 5, 40);
        g2d.fillRect(x + 40, y, 5, 40);

        // 坦克炮塔
        g2d.setColor(darkColor);
        g2d.fillOval(x + 5, y + 5, 30, 30);

        // 坦克炮管（根据方向绘制）
        g2d.setColor(Color.BLACK);
        drawCannon(g2d);

        // 坦克观察窗
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(x + 15, y + 15, 10, 10);

        // 坦克顶部装饰
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 18, y + 18, 4, 4);

        // 坦克履带纹路
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < 4; i++) {
            g2d.fillRect(x - 5, y + i * 10, 5, 2);
            g2d.fillRect(x + 40, y + i * 10, 5, 2);
        }
    }

    private void drawCannon(Graphics2D g2d) {
        switch (direction) {
            case UP:
                g2d.fillRect(x + 18, y - 10, 4, 25);
                break;
            case UP_RIGHT:
                g2d.fillPolygon(new int[]{x + 20, x + 30, x + 25},
                        new int[]{y, y + 10, y + 15}, 3);
                break;
            case RIGHT:
                g2d.fillRect(x + 25, y + 18, 25, 4);
                break;
            case DOWN_RIGHT:
                g2d.fillPolygon(new int[]{x + 20, x + 30, x + 25},
                        new int[]{y + 40, y + 30, y + 25}, 3);
                break;
            case DOWN:
                g2d.fillRect(x + 18, y + 25, 4, 25);
                break;
            case DOWN_LEFT:
                g2d.fillPolygon(new int[]{x + 20, x + 10, x + 15},
                        new int[]{y + 40, y + 30, y + 25}, 3);
                break;
            case LEFT:
                g2d.fillRect(x - 10, y + 18, 25, 4);
                break;
            case UP_LEFT:
                g2d.fillPolygon(new int[]{x + 20, x + 10, x + 15},
                        new int[]{y, y + 10, y + 15}, 3);
                break;
        }
    }

    @Override
    public void update() {
        moveTime++;
        followTime++;

        // 每3秒尝试追踪玩家
        if (followTime >= followInterval) {
            followPlayer();
            followTime = 0;
        }

        // 每1秒随机改变方向
        if (moveTime >= moveInterval) {
            // 有20%概率随机改变方向
            if (random.nextInt(100) < 20) {
                changeDirection();
            }
            moveTime = 0;
        }

        super.update();
    }

    private void followPlayer() {
        if (player == null) return;

        int playerX = player.getX() + player.width / 2;
        int playerY = player.getY() + player.height / 2;
        int tankX = x + width / 2;
        int tankY = y + height / 2;

        // 计算方向角度
        double angle = Math.toDegrees(Math.atan2(playerY - tankY, playerX - tankX));

        // 获取最接近的8方向
        Direction newDirection = Direction.fromAngle(angle);
        setDirection(newDirection);
    }

    public void changeDirection() {
        Direction[] directions = Direction.values();
        Direction newDirection = directions[random.nextInt(directions.length)];
        setDirection(newDirection);
    }

    @Override
    public boolean isPlayerBullet() {
        return false;
    }
}

// 子弹类
class Bullet {
    private int x, y;
    private int speed = 5;
    private Direction direction;
    private boolean playerBullet;
    private int width = 6;
    private int height = 6;

    public Bullet(int x, int y, Direction direction, boolean playerBullet) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.playerBullet = playerBullet;
    }

    public void update() {
        x += direction.getDx() * speed;
        y += direction.getDy() * speed;
    }

    public void draw(Graphics g) {
        g.setColor(playerBullet ? Color.CYAN : Color.YELLOW);
        g.fillOval(x, y, width, height);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public boolean isOutOfBounds() {
        return x < 0 || x > TankWarGame.WIDTH || y < 0 || y > TankWarGame.HEIGHT;
    }

    public boolean isPlayerBullet() {
        return playerBullet;
    }
}

// 爆炸效果类
class Explosion {
    private int x, y;
    private int radius = 5;
    private int maxRadius = 30;
    private int step = 2;
    private boolean finished = false;

    public Explosion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        radius += step;
        if (radius >= maxRadius) {
            finished = true;
        }
    }

    public void draw(Graphics g) {
        if (finished) return;

        // 绘制爆炸效果
        g.setColor(Color.ORANGE);
        g.fillOval(x + 20 - radius/2, y + 20 - radius/2, radius, radius);

        g.setColor(Color.YELLOW);
        g.fillOval(x + 20 - radius/4, y + 20 - radius/4, radius/2, radius/2);
    }

    public boolean isFinished() {
        return finished;
    }
}