/*
* Project 4.1.5: Escape Room Revisited
* * V1.0
* Copyright(c) 2024 PLTW to present. All rights reserved
*/
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game where a player maneuvers around a gameboard to answer
 * riddles or questions, collecing prizes with correct answers.
 */
public class GameGUI extends JComponent implements KeyListener
{
  static final long serialVersionUID = 415L;

  // constants for gameboard confg
  private static final int WIDTH = 510;
  private static final int HEIGHT = 360;
  private static final int SPACE_SIZE = 60;
  private static final int GRID_W = 8;
  private static final int GRID_H = 5;
  private static final int MOVE = 60;

  // frame and images for gameboard
  private JFrame frame;
  private Image bgImage;
  private Image prizeImage;
  private Image player;
  private Image playerQ;

  // player config
  private int currX = 15; 
  private int currY = 15;
  private boolean atPrize;
  private Point playerLoc;
  private int playerSteps;

  // walls, player level, and prizes
  private int numWalls = 8;
  private int playerLevel = 1;
  private Rectangle[] walls; 
  private Rectangle[] prizes;

  // scores, sometimes awarded as (negative) penalties
  private int goodMove = 1;
  private int offGridVal = 5; // penalty 
  private int hitWallVal = 5;  // penalty 
  private int wrongAns = 7; // penalty 
  private int score = 0; 

  // --- NEW VARIABLES FOR PROJECT 4.1.5 ---
  private static final int MAX_LEVEL = 5;
  private String[][] quizData;
  private int currentQuestionIndex = 0;

  /**
   * Constructor for the GameGUI class.
   */
  public GameGUI() throws IOException
  {
    newPlayerLevel();
    createQuiz();
    createBoard();
  }

  /**
   * Create array of questions, answers, and points from the quiz.txt file.
   * Randomizes the questions so they don't repeat in the same order.
   */
  private void createQuiz() 
  {
    List<String[]> allQuestions = new ArrayList<>();
    try {
      Scanner scanner = new Scanner(new File("quiz.txt"));
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] parts = line.split(":"); // Splits at colon: Question:Answer:Points
        if (parts.length >= 3) {
          allQuestions.add(parts);
        }
      }
      scanner.close();
    } catch (IOException e) {
      System.err.println("Error reading quiz.txt");
    }

    // Randomize the questions
    Collections.shuffle(allQuestions);

    // Initialize array based on MAX_LEVEL (1 question per level max used)
    // Dimension 3 holds Question, Answer, and Points
    quizData = new String[MAX_LEVEL][3];
    
    for (int i = 0; i < MAX_LEVEL && i < allQuestions.size(); i++) {
        quizData[i][0] = allQuestions.get(i)[0]; // Question
        quizData[i][1] = allQuestions.get(i)[1]; // Answer
        quizData[i][2] = allQuestions.get(i)[2]; // Points
    }
  }

  /**
   * Update the instance variables playerLevel and numWalls
   * based on user level stored in the level.txt file.
   */
  private void newPlayerLevel() 
  {
    try {
      Scanner scanner = new Scanner(new File("level.txt"));
      if (scanner.hasNextInt()) {
        playerLevel = scanner.nextInt();
      }
      scanner.close();
      
      // Enforce the maximum level limit
      if (playerLevel > MAX_LEVEL) {
        playerLevel = MAX_LEVEL;
      }
      
      // Increase walls based on the current level
      numWalls = 8 + (playerLevel * 3); 
      
    } catch (IOException e) {
      System.err.println("Could not read level.txt. Defaulting to Level 1.");
      playerLevel = 1;
      numWalls = 8;
    }
  }

  /**
   * Manage the input from the keybard: arrow keys, wasd keys, p, q, and h keys.
   */
  @Override
  public void keyPressed(KeyEvent e)
  {
    // P Key: Pick up prize
    if (e.getKeyCode() == KeyEvent.VK_P )
    {
      if (atPrize) {
          if (currentQuestionIndex < MAX_LEVEL) {
              String question = quizData[currentQuestionIndex][0];
              String correctAnswer = quizData[currentQuestionIndex][1];
              int questionPoints = Integer.parseInt(quizData[currentQuestionIndex][2].trim()); // Dynamic points
              
              String answer = askQuestion(question + "\n\n(Worth " + questionPoints + " points)");
              
              // Check if user clicked OK and answer matches (ignoring case/spaces)
              if (answer != null && answer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                  pickupPrize();
                  score += questionPoints; // Add dynamic points
                  currentQuestionIndex++; // Move to next question
                  
                  // Check if that was the last coin
                  boolean allCollected = true;
                  for (Rectangle p : prizes) {
                      if (p.getWidth() > 0) {
                          allCollected = false;
                          break;
                      }
                  }
                  
                  if (allCollected) {
                      showMessage("Correct! You earned " + questionPoints + " points.\n\nYou picked up the final coin! Press 'Q' to escape the room and see your final score.");
                  } else {
                      showMessage("Correct! You earned " + questionPoints + " points.\nCurrent Score: " + score);
                  }
                  
              } else if (answer != null) {
                  score -= wrongAns;
                  showMessage("Incorrect! The answer was: " + correctAnswer + "\nTry again on another coin.\nCurrent Score: " + score);
              }
          }
      } else {
          showMessage("There's no coin here to pick up!");
      }
    }

    // Q key: quit game
    if (e.getKeyCode() == KeyEvent.VK_Q)
    {
      boolean allCollected = true;
      for (Rectangle p : prizes) {
          if (p.getWidth() > 0) {
              allCollected = false;
              break;
          }
      }
      
      if (!allCollected) {
          showMessage("You cannot escape yet! You must collect all the coins first.");
      } else {
          int finalScore = score - playerSteps;
          showMessage("You escaped!\nBase Score: " + score + "\nSteps Taken: " + playerSteps + "\nFinal Score: " + finalScore);
          
          if (finalScore > 0) {
              if (playerLevel < MAX_LEVEL) {
                  playerLevel++;
                  showMessage("Congratulations! You leveled up to Level " + playerLevel + "!");
              } else {
                  showMessage("Incredible! You have beaten the maximum level!");
              }
          } else {
              showMessage("Your score was too low to level up. Better luck next time!");
          }
          endGame();
      }
    }

    // H key: help
    if (e.getKeyCode() == KeyEvent.VK_H)
    {
      String msg = "Move player: arrows or WASD keys\n" + 
      "Jump (2 spaces): Hold SHIFT + arrows\n" +
      "Pickup prize: p\n" +
      "Quit: q\n" +
      "Help: h\n";
      showMessage(msg);
    }
    
    // Determine distance: Regular move = MOVE, Jump = MOVE * 2
    int distance = e.isShiftDown() ? MOVE * 2 : MOVE;

    // Arrow and WASD keys: moved down, up, left or right
    if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S )
    {
      score += movePlayer(0, distance);
    }
    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W)
    {
      score += movePlayer(0, -distance);
    }
    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A)
    {
      score += movePlayer(-distance, 0);
    }
    if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D)
    {
      score += movePlayer(distance, 0);
    }
  } 

  @Override
  public void keyReleased(KeyEvent e) 
  { 
    checkForPrize();
  }

  @Override
  public void keyTyped(KeyEvent e) { }

  private void createBoard() throws IOException
  {    
    prizes = new Rectangle[playerLevel];
    createPrizes();

    walls = new Rectangle[numWalls];
    createWalls();

    bgImage = ImageIO.read(new File("grid.png"));
    prizeImage = ImageIO.read(new File("coin.png"));
    player = ImageIO.read(new File("player.png")); 
    playerQ = ImageIO.read(new File("playerQ.png")); 
    
    playerLoc = new Point(currX, currY);

    frame = new JFrame();
    frame.setTitle("EscapeRoom");
    frame.setSize(WIDTH, HEIGHT);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(this);
    frame.setVisible(true);
    frame.setResizable(false); 
    frame.addKeyListener(this);

    checkForPrize();

    showMessage("Welcome to the Escape Room. Press h to learn how to play.");
  }

  private int movePlayer(int incrx, int incry)
  {
      int newX = currX + incrx;
      int newY = currY + incry;

      // check if off grid horizontally and vertically
      if ( (newX < 0 || newX > WIDTH-SPACE_SIZE) || (newY < 0 || newY > HEIGHT-SPACE_SIZE) )
      {
        showMessage("You have tried to go off the grid!");
        return -offGridVal;
      }

      // determine if a wall is in the way
      for (Rectangle r: walls)
      {
        int startX =  (int)r.getX();
        int endX  =  (int)r.getX() + (int)r.getWidth();
        int startY =  (int)r.getY();
        int endY = (int) r.getY() + (int)r.getHeight();

        // The path-checking logic natively blocks jumping *through* walls
        if ((incrx > 0) && (currX <= startX) && (startX <= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        else if ((incrx < 0) && (currX >= startX) && (startX >= newX) && (currY >= startY) && (currY <= endY))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        else if ((incry > 0) && (currY <= startY && startY <= newY && currX >= startX && currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }
        else if ((incry < 0) && (currY >= startY) && (startY >= newY) && (currX >= startX) && (currX <= endX))
        {
          showMessage("A wall is in the way.");
          return -hitWallVal;
        }    
      }

      // all is well, move player
      playerSteps++;
      currX += incrx;
      currY += incry;
      repaint();   
      return goodMove;
  }

  private void showMessage(String str)
  {
    JOptionPane.showMessageDialog(frame,str );
  }

  private String askQuestion(String q)
  {
    return JOptionPane.showInputDialog(q.replace("\\n","\n") , JOptionPane.OK_OPTION);  
  }

  private void checkForPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle r: prizes)
    {
      if (r.contains(px, py))
      {
        atPrize = true;
        repaint();
        return;
      }
    }
    atPrize = false;
  }

  private void  pickupPrize()
  {
    double px = playerLoc.getX();
    double py = playerLoc.getY();

    for (Rectangle p: prizes)
    {
      if (p.getWidth() > 0 && p.contains(px, py))
      {
        p.setSize(0,0);
        atPrize = false;
        repaint();
      }
    }
  }

  private void endGame() 
  {
    try {
      FileWriter fw = new FileWriter("level.txt"); 
      String s = playerLevel + "\n";
      fw.write(s);
      fw.close();
    } catch (IOException e)  { System.err.println("Could not level up."); }
  
    setVisible(false);
    frame.dispose();
  }

  private void createPrizes()
  {
    int s = SPACE_SIZE; 
    Random rand = new Random();
    for (int numPrizes = 0; numPrizes < playerLevel; numPrizes++)
    {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);
      Rectangle r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);

       for (Rectangle p : prizes) {
        while (p != null && p.equals(r)) {
          h = rand.nextInt(GRID_H);
          w = rand.nextInt(GRID_W);
          r = new Rectangle((w*s + 15),(h*s + 15), 15, 15);
        }
      }
      prizes[numPrizes] = r;
    }
  }

  private void createWalls()
  {
     int s = SPACE_SIZE; 
     Random rand = new Random();

     for (int n = 0; n < numWalls; n++)
     {
      int h = rand.nextInt(GRID_H);
      int w = rand.nextInt(GRID_W);

      Rectangle r;
      if (rand.nextInt(2) == 0) 
      {
        // vertical
        r = new Rectangle((w*s + s - 5),h*s, 8,s);
      }
      else
      {
        /// horizontal
        r = new Rectangle(w*s,(h*s + s - 5), s, 8);
      }

      walls[n] = r;
    }
  }

  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;

    g.drawImage(bgImage, 0, 0, null);

    for (Rectangle p : prizes)
    {
      if (p.getWidth() > 0) 
      {
      int px = (int)p.getX();
      int py = (int)p.getY();
      g.drawImage(prizeImage, px, py, null);
      }
    }

    for (Rectangle r : walls) 
    {
      g2.setPaint(Color.BLACK);
      g2.fill(r);
    }
   
    if(atPrize)
    {
      g.drawImage(playerQ, currX, currY, 40,40, null);
    }
    else
    {
      g.drawImage(player, currX, currY, 40,40, null);
    }
    playerLoc.setLocation(currX, currY);
  }

}
