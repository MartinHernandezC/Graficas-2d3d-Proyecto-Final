import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.Math;

public class ProyectoFinal extends JFrame implements KeyListener, Runnable{
    private BufferedImage pixel;
    private JPanel panel;
    private JTextArea textArea;

    private int cantPuntos = 32;
    private int cantCurvas = 16;
    private int timer = 6; //(milisegundos)

    private float color = 0.5F;
    private int recorrer = 0;

    private BufferedImage buffer;
    private Image fondo;
    private double forma = 10.4;
    private int grosor = 1;

    int figura[][][] = new int [cantCurvas][cantPuntos][4];
    int curva[][] = new int[cantPuntos][4];

    double[] escalar = {0.2,0.2,0.2};
    double[] rotar = {0,0,0};
    int[] trasladar = {500, 350, 0};

    int[] planoProyeccion = {0,0,100};

    private Thread hilo;

    private int rango = 0;

    public ProyectoFinal(){
        panel = new JPanel();
        textArea = new JTextArea();
        textArea.addKeyListener(this);
        getContentPane().add(textArea, BorderLayout.CENTER);
        getContentPane().add(panel, BorderLayout.CENTER);

        setTitle("Proyecto final");
        setResizable(true);
		setSize(1280,720);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fondo = panel.createImage(panel.getWidth(), panel.getHeight());
        fondo.getGraphics().setColor(Color.BLACK);
        fondo.getGraphics().fillRect(0,0, panel.getWidth(), panel.getHeight());
        pixel = new BufferedImage(grosor, grosor, BufferedImage.TYPE_INT_RGB);

        dibujar();

        hilo = new Thread(this);
        hilo.start();
    }

    public void dibujarPixel(int x, int y, Color c){
        pixel.setRGB(0, 0, c.getRGB());
        buffer.getGraphics().drawImage(pixel, x, y, this);
    }

    public void dibujarLinea(int x0, int y0, int x1, int y1, Color c){
        int dx = x1 - x0;
        int dy = y1 - y0;

        int A = 2 * dy;
        int B = 2 * dy - 2 * dx;
        int p = 2 * dy - dx;

        int steps = Math.abs(dx) > Math.abs(dy) ? Math.abs(dx) : Math.abs(dy);
        float xinc = (float) dx / steps;
        float yinc = (float) dy / steps;

        float x = x0;
        float y = y0;

        for(int k = 1; k <= steps; ++k){
            if(p < 0){
                dibujarPixel(Math.round(x) + 1, Math.round(y), c);
                p = p + A;
            } else {
                dibujarPixel(Math.round(x) + 1, Math.round(y) + 1, c);
                p = p + B;
            }
            x = x + xinc;
            y = y + yinc;
        }
    }

    public void calcularCurva(int curva[][]){
        double tempX, tempY, tempZ;

        double maxX = 0, minX = 0;
        double maxY = 0, minY = 0;
        double maxZ = 0, minZ = 0;
        
        double t = 0;
        double incr =  2 * Math.PI / cantPuntos;
        
        for (int i = 0; i < cantPuntos; i++) {
            t = incr * i;

            //ECUACIONES PARAMETRICAS
            tempX = (10 * Math.cos(t/2)) * 100;
            tempY = -(10 * Math.cos(t) * 100);
            
            curva[i][0] = (int)tempX;
            curva[i][1] = (int)tempY;
            curva[i][2] = 0;
            curva[i][3] = 1;

            if(i == 0){
                maxY = tempY;
                minY = tempY;
            }

            if (tempX < minX){
                minX = tempX;
            }
            if (tempX > maxX){
                maxX = tempX;
            }

            if (tempY < minY){
                minY = tempY;
            }
            if (tempY > maxY){
                maxY = tempY;
            }
        }

        double rangoX = maxX-minX;
        double rangoY = maxY-minY;
        double rangoZ = maxZ-minZ;

        if((rangoX) > (rangoY)){
            if((rangoX) > (rangoZ)){
                rango = (int)Math.round(rangoX);
            }
            else{
                rango = (int)Math.round(rangoZ);
            }
        }
        else if((rangoY) > (rangoZ)){
            rango = (int)Math.round(rangoY);
        }
        else{
            rango = (int)Math.round(rangoZ);
        }

        planoProyeccion[2] = rango;

        int temp[][] = {{0,0,0,1}};
        for (int i = 0; i < cantCurvas; i++) {
            for (int j = 0; j < cantPuntos; j++){
                temp[0][0] = curva[j][0];
                temp[0][1] = curva[j][1];
                temp[0][2] = curva[j][2];
                temp[0][3] = curva[j][3];

                rotar(temp, 0, forma * Math.PI/cantCurvas * i, 0);

                figura[i][j][0] = temp[0][0];
                figura[i][j][1] = temp[0][1];
                figura[i][j][2] = temp[0][2];
                figura[i][j][3] = temp[0][3];
            }
        }
    }

    public void dibujarFigura(int figura[][][], int cam[], Color c){
        int superficie2D[][][] = new int [cantCurvas][cantPuntos][3];

        if (color > 0.98){
            recorrer = 1;
        }
        else if(color < 0.51){
            recorrer = 0;
        }
        
        if (recorrer == 0){
            color = color + 0.01F;
        }
        else{
            color = color - 0.01F;
        }
        
        for (int i = 0; i < cantCurvas; i++) {
            for (int j = 0; j < cantPuntos; j++){
                superficie2D[i][j] = proyectarParalela(figura[i][j][0], figura[i][j][1], figura[i][j][2], cam[0], cam[1], cam[2]);
            }
        }

        for (int i = 1; i < cantCurvas; i++) {
            for (int j = 1; j < cantPuntos; j++){
                dibujarLinea(superficie2D[i-1][j-1][0], superficie2D[i-1][j-1][1], superficie2D[i-1][j][0], superficie2D[i-1][j][1], Color.getHSBColor(color, 1F, 1F));
                dibujarLinea(superficie2D[i-1][j-1][0], superficie2D[i-1][j-1][1], superficie2D[i][j-1][0], superficie2D[i][j-1][1], Color.getHSBColor(color, 1F, 1F));
                dibujarLinea(superficie2D[i-1][j-1][0], superficie2D[i-1][j-1][1], superficie2D[i][j][0], superficie2D[i][j][1], Color.getHSBColor(color, 1F, 1F));
                if(j == cantPuntos - 1) {
                    dibujarLinea(superficie2D[i-1][j][0], superficie2D[i-1][j][1], superficie2D[i][j][0], superficie2D[i][j][1], Color.getHSBColor(color, 1F, 1F));
                }
                
                if(i == cantCurvas - 1){
                    dibujarLinea(superficie2D[i][j-1][0], superficie2D[i][j-1][1], superficie2D[i][j][0], superficie2D[i][j][1], Color.getHSBColor(color, 1F, 1F));
                    dibujarLinea(superficie2D[0][j-1][0], superficie2D[0][j-1][1], superficie2D[i][j-1][0], superficie2D[i][j-1][1], Color.getHSBColor(color, 1F, 1F));
                    dibujarLinea(superficie2D[0][j][0], superficie2D[0][j][1], superficie2D[i][j-1][0], superficie2D[i][j-1][1], Color.getHSBColor(color, 1F, 1F));
                    if(j == cantPuntos - 1) {
                        dibujarLinea(superficie2D[0][j][0], superficie2D[0][j][1], superficie2D[i][j][0], superficie2D[i][j][1], Color.getHSBColor(color, 1F, 1F));
                    }
                }
            }
        }
    }

    public void dibujar(){
        buffer = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        buffer.getGraphics().drawImage(fondo, 0, 0, this);

        calcularCurva(curva);
        
        for (int i=0; i<cantCurvas; i++){
            escalar(figura[i], escalar[0], escalar[1], escalar[2]);
            rotar(figura[i], rotar[0], rotar[1], rotar[2]);
            trasladar(figura[i], trasladar[0], trasladar[1], trasladar[2]);
        }

        dibujarFigura(figura, planoProyeccion, Color.BLUE);

        panel.getGraphics().drawImage(buffer, 0, 0, this);
    }

    public int[] proyectarParalela(int X, int Y, int Z, int Xc, int Yc, int Zc){      
        int x2, y2;

        x2 = X + ((Xc * Z)/Zc);
        y2 = Y + ((Yc * Z)/Zc);

        int punto[] = {(int)x2, (int)y2};
        return punto;
    }

    public void escalar(int [][] figura, double Sx, double Sy, double Sz){
        for(int x1=0; x1<=figura.length - 1; x1++){
            double r[]={0,0,0,0};
            double [] P = {figura[x1][0], figura[x1][1],figura[x1][2], figura[x1][3]};
            double [][] T = {
                {Sx,0,0,0},
                {0,Sy,0,0},
                {0,0,Sz,0},
                {0,0,0,1}

            };
            int i,j;
            for(i=0;i<4;i++){
                for(j=0;j<4;j++){
                    r[i] += P[j]*T[i][j];
                }
            }
            figura[x1][0]=(int)r[0];
            figura[x1][1]=(int)r[1];
            figura[x1][2]=(int)r[2];
            figura[x1][3]=(int)r[3];
        }
    }

    public void rotar(int [][] figura, double Ax, double Ay, double Az){
        for(int x1=0; x1<=figura.length - 1; x1++){
            double r[]={0,0,0,0};
            double [] P = {figura[x1][0], figura[x1][1],figura[x1][2], figura[x1][3]};
            double [][] T = {
                {Math.cos(Ax),-Math.sin(Ax),0,0},
                {Math.sin(Ax),Math.cos(Ax),0,0},
                {0,0,1,0},
                {0,0,0,1}
            };
            int i,j;
            for(i=0;i<4;i++){
                for(j=0;j<4;j++){
                    r[i] += P[j]*T[i][j];
                }
            }
            figura[x1][0]=(int)r[0];
            figura[x1][1]=(int)r[1];
            figura[x1][2]=(int)r[2];
            figura[x1][3]=(int)r[3];
        }
        for(int x1=0; x1<=figura.length - 1; x1++){
            double r[]={0,0,0,0};
            double [] P = {figura[x1][0], figura[x1][1],figura[x1][2], figura[x1][3]};
            double [][] T = {
                {Math.cos(Ay), 0, Math.sin(Ay), 0},
                {0, 1, 0, 0},
                {-Math.sin(Ay), 0, Math.cos(Ay), 0},
                {0, 0, 0, 1}
            };
            int i,j;
            for(i=0;i<4;i++){
                for(j=0;j<4;j++){
                    r[i] += P[j]*T[i][j];
                }
            }
            figura[x1][0]=(int)r[0];
            figura[x1][1]=(int)r[1];
            figura[x1][2]=(int)r[2];
            figura[x1][3]=(int)r[3];
        }

        for(int x1=0; x1<=figura.length - 1; x1++){
            double r[]={0,0,0,0};
            double [] P = {figura[x1][0], figura[x1][1],figura[x1][2], figura[x1][3]};
            double [][] T = {
                {1,0,0,0},
                {0,Math.cos(Az),-Math.sin(Az),0},
                {0,Math.sin(Az),Math.cos(Az),0},
                {0,0,0,1}
            };
            int i,j;
            for(i=0;i<4;i++){
                for(j=0;j<4;j++){
                    r[i] += P[j]*T[i][j];
                }
            }
            figura[x1][0]=(int)r[0];
            figura[x1][1]=(int)r[1];
            figura[x1][2]=(int)r[2];
            figura[x1][3]=(int)r[3];
        }
    }

    public void trasladar(int [][] figura, int dx,int dy, int dz){
        for(int x1=0; x1<=figura.length - 1; x1++){
            int r[]={0,0,0,0};
            int [] P = {figura[x1][0], figura[x1][1],figura[x1][2], figura[x1][3]};
            int [][] T = {{1,0,0,dx},
                          {0,1,0,dy},
                          {0,0,1,dz},
                          {0,0,0,1}};
            int i,j;
            for(i=0;i<4;i++){
                for(j=0;j<4;j++){
                    r[i] += P[j]*T[i][j];
                }
            }
            figura[x1][0]=r[0];
            figura[x1][1]=r[1];
            figura[x1][2]=r[2];
            figura[x1][3]=r[3];
        }
    }

    public void keyPressed(KeyEvent e) {
        
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            trasladar[0] = trasladar[0] - 50;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            trasladar[0] = trasladar[0] + 50;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            trasladar[1] = trasladar[1] - 50;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            trasladar[1] = trasladar[1] + 50;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_A) {
            rotar[0] = rotar[0] - Math.PI/16;
        }
        if (e.getKeyCode() == KeyEvent.VK_D) {
            rotar[0] = rotar[0] + Math.PI/16;
        }
        if (e.getKeyCode() == KeyEvent.VK_W) {
            rotar[1] = rotar[1] + Math.PI/16;
        }
        if (e.getKeyCode() == KeyEvent.VK_S) {
            rotar[1] = rotar[1] - Math.PI/16;
        }
        if (e.getKeyCode() == KeyEvent.VK_Q) {
            rotar[2] = rotar[2] - Math.PI/16;
        }
        if (e.getKeyCode() == KeyEvent.VK_E) {
            rotar[2] = rotar[2] + Math.PI/16;
        }

        if (e.getKeyCode() == KeyEvent.VK_PERIOD) {
            escalar[0] = escalar[0] + 0.05;
            escalar[1] = escalar[1] + 0.05;
            escalar[2] = escalar[2] + 0.05;
        }
        if (e.getKeyCode() == KeyEvent.VK_COMMA) {
            escalar[0] = escalar[0] - 0.05;
            escalar[1] = escalar[1] - 0.05;
            escalar[2] = escalar[2] - 0.05;
        }
    }

    public void keyReleased(KeyEvent e) {

    } 

    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void run() {
        while (true) {
            forma = forma + 0.1;
            dibujar();

            try {
                Thread.sleep(timer);
            } catch (InterruptedException e) {

            }
        }
    }

    public static void main(String[] args) {
        ProyectoFinal ventana = new ProyectoFinal();
    }
}
