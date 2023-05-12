import java.util.*;
import java.io.*;
import java.net.*;

public class GameClient{
    private GameCanvas canvas;

    private int playerID;
    private Socket socket;
    private ReadFromServer rfsRunnable;
    private WriteToServer wtsRunnable;

    public void setGameCanvas(GameCanvas gc){
        canvas = gc;
    }

    public int getPlayerID(){
        return playerID;
    }
    
    public void connectToServer(){
        System.out.println("Client");
        try{
            Scanner scan = new Scanner(System.in);
            System.out.print("Insert IP Address: ");
            String ipAddress = scan.nextLine();
            System.out.print("Port: ");
            int portNumber = Integer.parseInt(scan.nextLine());
            // String ipAddress = "localhost";
            // int portNumber = 24396;
            socket = new Socket(ipAddress, portNumber);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            playerID = in.readInt();
            System.out.println("Connected as p#" + playerID);
            rfsRunnable = new ReadFromServer(in);
            wtsRunnable = new WriteToServer(out);
            rfsRunnable.waitForStartMsg();
        }catch (IOException ex){
            System.out.println("Server not found");
        }
    }

    private class ReadFromServer implements Runnable{

        private DataInputStream dataIn;

        public ReadFromServer(DataInputStream in){
            dataIn = in;
            System.out.println("RFS Runnable created");
        }

        public void run(){
            try{
                while (true){
                    int serverGameState = dataIn.readInt();
                    double ex = dataIn.readDouble();
                    double ey = dataIn.readDouble();
                    double eA = dataIn.readDouble();
                    int gotPunctured = dataIn.readInt();
                    int enemyPunctured = dataIn.readInt();
                    int gotHoney = dataIn.readInt();
                    int enemyHoney = dataIn.readInt();
                    int dashTimer = dataIn.readInt();
                    int hx = dataIn.readInt();
                    int hy = dataIn.readInt();
                    

                    if (!canvas.doesEnemyExists()) continue;

                    if (canvas.getGameState() == 0 && serverGameState == 1){
                        canvas.setGameState(1);
                    }
                    else if (canvas.getGameState() == 1 && serverGameState == 2){
                        canvas.setGameState(2);
                    }
                    else if (canvas.getGameState() == 2 && serverGameState == 0){
                        canvas.pressRestart();
                    }
                    canvas.setDashTimer(dashTimer);
                    canvas.getEnemy().setX(ex);
                    canvas.getEnemy().setY(ey);
                    canvas.getEnemy().setAngle(eA);
                    canvas.getEnemy().setNeedlePoint();
                    canvas.getHoney().setX(hx);
                    canvas.getHoney().setY(hy);
                    
                    if (canvas.getGameState() == 1){
                        if (gotPunctured == 1 && !canvas.getYou().isInvincible()){
                            canvas.getYou().bodyPunctured();
                            canvas.getEnemy().addScore(1);
                            canvas.getYou().addScore(-1);
                            // System.out.println("gotpuncutred");
                        }
                        if (enemyPunctured == 1 && !canvas.getEnemy().isInvincible()){
                            canvas.getEnemy().bodyPunctured();
                            canvas.getYou().addScore(1);
                            canvas.getEnemy().addScore(-1);
                            // System.out.println("enemyPunctured");
                        }
    
                        if (gotHoney == 1 && !canvas.getYou().justGotHoney()){
                            canvas.getYou().addScore(1);
                            canvas.getYou().gotHoney();
                        }
                        if (enemyHoney == 1 && !canvas.getEnemy().justGotHoney()){
                            canvas.getEnemy().addScore(1);
                            canvas.getEnemy().gotHoney();
                        }

                        if (Math.abs(dashTimer - Constants.DASHTRIGGER) < 7){
                            canvas.getYou().toggleDash();
                            canvas.getEnemy().toggleDash();
                        }
                    }
                    
                    
        
                }
            }catch (IOException ex){
                System.out.println("IOException from RFS run");
            }
        }

        public void waitForStartMsg(){
            try{
                String startMsg = dataIn.readUTF();
                System.out.println("Message from server: " + startMsg);
                
                Thread readThread = new Thread(rfsRunnable);
                Thread writeThread = new Thread(wtsRunnable);
                readThread.start();
                writeThread.start();
                
            }catch (IOException ex){
                System.out.println("IOException for wait for start");
            }
        }
    }

    private class WriteToServer implements Runnable{

        private DataOutputStream dataOut;

        public WriteToServer(DataOutputStream out){
            dataOut = out;
            System.out.println("WTS Runnable created");
        }

        public void run(){
            try{
                while (true){

                    if (canvas.doesEnemyExists()){
                        dataOut.writeInt(canvas.getCanvasState());
                        dataOut.writeDouble(canvas.getYou().getX());
                        dataOut.writeDouble(canvas.getYou().getY());
                        dataOut.writeDouble(canvas.getYou().getAngle());
                        dataOut.writeInt(canvas.getYou().getScore());
                        dataOut.flush();
                    }
                    try{
                        Thread.sleep(10);
                    }catch (InterruptedException ex){
                        System.out.println("InterruptedException fr wts run");
                    }
                }
            } catch (IOException ex){
                System.out.println("IOException from wts run");
            }
        }
    }

}