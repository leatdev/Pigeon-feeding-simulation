// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.sound.sampled.*;
import java.io.*;
public class Main extends JPanel {
    // ... other code ...
    private double scareProbability = 0.001;
    private boolean isScared = false;
    private final CopyOnWriteArrayList<Pigeon> pigeons = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Food> foods = new CopyOnWriteArrayList<>();
    private final ReentrantLock foodLock = new ReentrantLock();

    private final int updateInterval = 10;

    public Main() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                foods.forEach(Food::makeStale);  // Make all existing food stale
                foods.add(new Food(e.getPoint()));  // Add fresh food
            }
        });


        Timer repaintTimer = new Timer(16, e -> repaint());  // 60 fps
        repaintTimer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        foodLock.lock();
        try {
            for (Food food : foods) {
                food.draw(g);
            }
        } finally {
            foodLock.unlock();
        }
        for (Pigeon pigeon : pigeons) {
            pigeon.draw(g);
        }
    }

    public void initializePigeons() {
        for (int i = 0; i < 5; i++) {  // Create 5 pigeons for example
            Pigeon pigeon = new Pigeon((int) (Math.random() * getWidth()), (int) (Math.random() * getHeight()));
            pigeons.add(pigeon);
            new Thread(pigeon).start();
        }
    }

    private void playScareSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("scream.wav").getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
            TimeUnit.SECONDS.sleep(1);
            clip.stop();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    class Pigeon implements Runnable {
        //private Point position = new Point((int) (Math.random() * getWidth()), (int) (Math.random() * getHeight()));

        private Point position;
        private final int radius = 10;
        private Point scareTarget;

        private final int stepSize = 1;

        public Pigeon(int x, int y) {
            this.position = new Point(x, y);
        }

        public Point getPosition() {
            return position;
        }

        public int getRadius() {
            return radius;
        }

        public void setPosition(Point newPosition) {
            this.position = newPosition;
        }
        private boolean isColliding(Point newPosition, Pigeon otherPigeon) {
            double distance = newPosition.distance(otherPigeon.getPosition());
            return distance < (radius + otherPigeon.getRadius());
        }
        private boolean isCloseEnoughToEat(Food food) {
            double distance = position.distance(food.getPosition());
            return distance < 10;  // Assume pigeon can eat food within 10 pixels distance
        }

        private void scarePigeons() {
            playScareSound();
            int step = 30;
            int dx;
            int dy;

                dx = (int) ((Math.random() * getWidth()) - position.x);
                dy = (int) ((Math.random() * getHeight())-  position.y);
                Point newPosition = new Point(position.x + step * Integer.signum(dx), position.y + step * Integer.signum(dy));


            for (Pigeon otherPigeon : pigeons) {
                if (otherPigeon != this && isColliding(newPosition, otherPigeon)) {
                    newPosition = new Point(
                            position.x - step * Integer.signum(dx),
                            position.y - step * Integer.signum(dy)
                    );
                    break;
                }
            }

            position = newPosition;  // Update position



        }

        public void setScareTarget(Point target) {
            this.scareTarget = target;
        }



        public void run() {
            while (true) {

                    Food closestFood = findClosestFood();
                    if (closestFood != null) {
                        moveTowards(closestFood);
                        if (isCloseEnoughToEat(closestFood)) {
                            synchronized (foods) {
                                if (foods.contains(closestFood)) {
                                    foods.remove(closestFood);
                                }
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(updateInterval);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }


                // Scare logic

                    if (foods.isEmpty() && Math.random() < scareProbability) {
                        scarePigeons();
                    }


                try {
                    Thread.sleep(100);  // Sleep for a while to simulate real time movement
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }



        private Food findClosestFood() {
            Food closestFood = null;
            double closestDistance = Double.MAX_VALUE;
            foodLock.lock();
            try {
                for (Food food : foods) {
                    if (!food.isFresh()) continue;  // Ignore stale food
                    double distance = position.distance(food.getPosition());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestFood = food;
                    }
                }
            } finally {
                foodLock.unlock();
            }
            return closestFood;
        }

        private void moveTowards(Food food) {
            int dx = food.getPosition().x - position.x;
            int dy = food.getPosition().y - position.y;
            int step = 10;  // Adjust step size to control speed

            Point newPosition = new Point(
                    position.x + step * Integer.signum(dx),
                    position.y + step * Integer.signum(dy)
            );

            // Adjust direction if a collision with other pigeons would occur
            for (Pigeon otherPigeon : pigeons) {
                if (otherPigeon != this && isColliding(newPosition, otherPigeon)) {
                    newPosition = new Point(
                            position.x - step * Integer.signum(dx),
                            position.y - step * Integer.signum(dy)
                    );
                    break;
                }
            }

            position = newPosition;  // Update position

        }

        public void draw(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillOval(position.x - 10, position.y - 10, 20, 20);
        }
    }
    }

    class Food {
        private final Point position;
        private boolean isFresh;

        public Food(Point position) {
            this.position = position;
            this.isFresh = true;  // Food is fresh when created
        }

        public Point getPosition() {
            return position;
        }

        public boolean isFresh() {
            return isFresh;
        }

        public void makeStale() {
            isFresh = false;
        }

        public void draw(Graphics g) {
            g.setColor(isFresh ? Color.GREEN : Color.RED);
            g.fillRect(position.x - 5, position.y - 5, 10, 10);
        }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pigeon Feeding Simulation");
            Main simulation = new Main();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(simulation);
            frame.setVisible(true);
            simulation.initializePigeons();
        });
    }
}
