import java.io.*;
import java.net.*;
import javax.swing.Timer;
import java.awt.event.*;
import java.util.Random;

public class GameServer{
    
    private ServerSocket ss;
    private int numPlayers;
    private int maxPlayers;

    private Socket p1Socket;
    private Socket p2Socket;

    private ReadFromClient p1readRunnable;
    private ReadFromClient p2readRunnable;
    
    private WriteToClient p1writeRunnable;
    private WriteToClient p2writeRunnable;

    // player coordinates, angle, and needle coordinates
    private double p1x, p1y, p2x, p2y, p1a, p2a, p1nx, p1ny, p2nx, p2ny;
    private boolean isDashing;
    private int dashTimer;
    private int p1s, p2s;
    private int hx, hy;
    int p1Hitsp2, p2Hitsp1;
    int p1GetsH, p2GetsH;

    public GameServer(){
        setInitialGameState();
        numPlayers = 0;
        maxPlayers = 2;
        System.out.println("server is running");
        try{
            System.out.println("socket not yet created");
            ss = new ServerSocket(24396);
            System.out.println("socket created");
        }
        catch (IOException ex){
             System.out.println("IOException from GameServer Constructor");
        }
    }

    public void setInitialGameState(){
        p1x = 150;
        p1y = p2y = 200;
        p2x = 450;
        p1a = p2a = -1.570796327;
        isDashing = false;
        dashTimer = 0;
        hx = hy = -1;
        
    }

    public void acceptConnections(){
        try{
            System.out.println("waiting");
            while (numPlayers < maxPlayers){
                Socket s = ss.accept();
                DataInputStream in  = new DataInputStream(s.getInputStream());
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                
                numPlayers++;
                out.writeInt(numPlayers);
                System.out.println("Player #" + numPlayers + " has entered");
                
                ReadFromClient rfc = new ReadFromClient(numPlayers, in);
                WriteToClient wtc = new WriteToClient(numPlayers, out);

                if (numPlayers == 1){
                    p1Socket = s; 
                    p1readRunnable = rfc; 
                    p1writeRunnable = wtc;
                } else {
                    p2Socket = s;
                    p2readRunnable = rfc;
                    p2writeRunnable = wtc;

                    p1writeRunnable.sendStartMsg();
                    p2writeRunnable.sendStartMsg();

                    Thread readThread1 = new Thread(p1readRunnable);
                    Thread readThread2 = new Thread(p2readRunnable);
                    readThread1.start();
                    readThread2.start();
                    
                    Thread writeThread1 = new Thread(p1writeRunnable);
                    Thread writeThread2 = new Thread(p2writeRunnable);
                    writeThread1.start();
                    writeThread2.start();
                }
            }
            System.out.println("Lobby is full");
            TimerIncrement ti = new TimerIncrement();
            Thread timerThread = new Thread(ti);
            timerThread.start();
            
        }catch (IOException ex){
            System.out.println("IOException from acceptConnections");
        }
    }

    private class TimerIncrement implements Runnable{
        Random rand = new Random();
        public void run(){
            while (true){
                dashTimer++;
                if (dashTimer == Constants.DASHLIMIT+1){
                    dashTimer = 1;
                    hx = hy = -1;
                }
                else if (dashTimer == Constants.DASHLIMIT/2){
                    hx = rand.nextInt(100, Constants.FRAMEWIDTH-100);
                    hy = rand.nextInt(100, Constants.FRAMEHEIGHT-100);
                }
                try{
                    Thread.sleep(10);
                }catch (InterruptedException ex){
                    System.out.println("timerincrement is not running");
                }
            }
        }
    }

    private class ReadFromClient implements Runnable{

        private int playerID;
        private DataInputStream dataIn;

        public ReadFromClient(int pid, DataInputStream in){
            playerID = pid;
            dataIn = in;
            System.out.println("RFC" + playerID + " Runnable created");
        }

        public void run(){
            try{
                while (true){
                    if (playerID == 1) {
                        p1x = dataIn.readDouble();
                        p1y = dataIn.readDouble();
                        p1a = dataIn.readDouble();
                        p1nx = p1x - Math.round(Math.cos(p1a)*Constants.NEEDLEDIST * 100) / 100;
                        p1ny = p1y - Math.round(Math.sin(p1a)*Constants.NEEDLEDIST * 100) / 100;
                        p1s = dataIn.readInt();
                    }
                    else if (playerID == 2) {
                        p2x = dataIn.readDouble();
                        p2y = dataIn.readDouble();
                        p2a = dataIn.readDouble();
                        p2nx = p2x - Math.round(Math.cos(p2a)*Constants.NEEDLEDIST * 100) / 100;
                        p2ny = p2y - Math.round(Math.sin(p2a)*Constants.NEEDLEDIST * 100) / 100;
                        p2s = dataIn.readInt();
                    } 

                }
            }catch (IOException ex){
                System.out.println("IOException from RFC run()");
            }
        }
    }

    private class WriteToClient implements Runnable{

        private int playerID;
        private DataOutputStream dataOut;

        public WriteToClient(int pid, DataOutputStream out){
            playerID = pid;
            dataOut = out;
            System.out.println("WTC" + playerID + " Runnable created");
        }
        
        public double getDistance(double x1, double y1, double x2, double y2){
            return Math.round(( Math.sqrt( Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2) ) ) * 100) / 100;
        }

        public void run(){
            try{
                while(true){
                    p1Hitsp2 = p2Hitsp1 = p1GetsH = p2GetsH = 0;                 

                    // Check bee collision
                    if (getDistance(p1x, p1y, p2nx, p2ny) <= Constants.BODYRADIUS){
                        p2Hitsp1 = 1;
                    }
                    if (getDistance(p2x, p2y, p1nx, p1ny) <= Constants.BODYRADIUS){
                        p1Hitsp2 = 1;
                    }

                    // Check honey collision
                    double p1toH = getDistance(p1x, p1y, (double)hx, (double)hy);
                    double p2toH = getDistance(p2x, p2y, (double)hx, (double)hy);
                    if (Math.min(p1toH, p2toH) <= Constants.HONEYRAD){
                        if (p1toH <= p2toH)
                            p1GetsH = 1;
                        else
                            p2GetsH = 1;
                    }

                    if (playerID == 1){
                        dataOut.writeDouble(p2x);
                        dataOut.writeDouble(p2y);
                        dataOut.writeDouble(p2a);
                        // if i got punctured
                        dataOut.writeInt(p2Hitsp1);
                        // if i punctured the enemy
                        dataOut.writeInt(p1Hitsp2);
                        // if i got honey
                        dataOut.writeInt(p1GetsH);
                        // if enemy got honey
                        dataOut.writeInt(p2GetsH);
                    }
                    else if (playerID == 2){
                        dataOut.writeDouble(p1x);
                        dataOut.writeDouble(p1y);
                        dataOut.writeDouble(p1a);
                        // if i got punctured
                        dataOut.writeInt(p1Hitsp2);
                        // if i punctured the enemy
                        dataOut.writeInt(p2Hitsp1);
                        // if i got honey
                        dataOut.writeInt(p2GetsH);
                        // if enemy got honey
                        dataOut.writeInt(p1GetsH);

                    }
                    dataOut.writeInt(dashTimer);
                    dataOut.writeInt(hx);
                    dataOut.writeInt(hy);
                    dataOut.flush();
                    // System.out.println(dashTimer);
                    try{
                        Thread.sleep(10);
                    }catch (InterruptedException ex){
                        System.out.println("InterruptedException from wtc run");
                    }
                }
            }catch (IOException ex){
                System.out.println("IOException from wtc run");
            }
        }

        public void sendStartMsg(){
            try{
                dataOut.writeUTF("We now have two players");
            }catch (IOException ex){
                System.out.println("IOException from sendmessage");
            }
        }
    }

    public static void main (String[] args){
        GameServer gs = new GameServer();
        gs.acceptConnections();
    }
}
