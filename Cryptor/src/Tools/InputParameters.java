package Tools;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class InputParameters{
    public static final byte n=4;
    public static final byte m=3;
    public static final byte _8=8;
    public static final byte _16=16;
    public static final short _256=256;
    public static final short MaxLength=1024;
    public static final byte[][] Matrix={{0,1,2,3},
                                {4,5,6,7},
                                {8,9,10,11},
                                {12,13,14,15}};
    public static boolean InputParameterFileNotFound=false;
    public static short EndFileNameCharacter=(short)'\n';
    public static boolean[][] BinaryCoding=new boolean[InputParameters._256][InputParameters._8];
    public static short[] Power_2=new short[InputParameters._8];
    public static StringNumber[][] Scales;
    public static StringNumber[] MaxScales=new StringNumber[2];
    public static Point[] Points;
    public static StringNumber DefaultCode;
    public static Order DefaultOrder;
    public InputParameters() throws InterruptedException{
        InputParameters.Points=new Point[16];
        for(byte i=0;i<InputParameters.Points.length;i++)
            InputParameters.Points[i]=new Point(i,InputParameters.n);
        for(byte i=0;i<InputParameters._8;i++)
            InputParameters.Power_2[i]=(short)Math.pow(2,InputParameters._8-1-i);
        InputParameters.Scales=new StringNumber[2][];
        InputParameters.Scales[0]=new StringNumber[11];
        InputParameters.Scales[1]=new StringNumber[255];
        InputParameters.Scales[0][0]=InputParameters.Scales[1][0]=new StringNumber(1);
        Scanner scanner=null;
        try{
            scanner=new Scanner(new File("InputParameters"));
        }        
        catch(FileNotFoundException e){
            InputParameters.InputParameterFileNotFound=true;
            return;
        }
        InputParameters.EndFileNameCharacter=Short.valueOf(scanner.nextLine());
        for(short i=1;i<InputParameters.Scales[0].length;i++)
            InputParameters.Scales[0][i]=new StringNumber(scanner.nextLine());
        InputParameters.MaxScales[0]=new StringNumber(scanner.nextLine());
        for(short i=1;i<InputParameters.Scales[1].length;i++)
            InputParameters.Scales[1][i]=new StringNumber(scanner.nextLine());
        InputParameters.DefaultCode=new StringNumber(scanner.nextLine());
        Thread t=new Thread(()->InputParameters.DefaultOrder=new Order());
        t.start();
        for(short i=0;i<InputParameters._256;i++){
            String s=Integer.toBinaryString(i);
            int k=s.length()-1;
            for(byte j=InputParameters._8-1;;j--){
                InputParameters.BinaryCoding[i][j]=s.charAt(k)=='1';
                k--;
                if(k==-1)
                    break;
            }
        }
        InputParameters.MaxScales[1]=new StringNumber(scanner.nextLine());
        scanner.close();
        t.join();
    }
}