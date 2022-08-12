
import Tools.InputParameters;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import javax.swing.SwingWorker;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class FileWriter extends SwingWorker{
    private short Index=0;
    private boolean Cancel=false;
    private boolean EndWriting=false;
    private boolean OpenOutputFile=false;
    private double Constant=0d;
    private FileReader FR=null;
    private File OutputFile;
    private RandomAccessFile OutputRAF=null;
    private final Vector<byte[]> OutputData=new Vector<>();
    
    public FileWriter(File InputFile,File OF,boolean decrypt) throws IOException, InterruptedException{
        this.OutputFile=OF;
        this.FR=new FileReader(new RandomAccessFile(InputFile,"r"));
        long FileSize=this.FR.getFileSize();
        FileSize+=(decrypt)?-2*this.OutputFile.getName().length()-2:2*InputFile.getName().length()+2;
        if(FileSize<=InputFile.getFreeSpace()){
            this.FR.execute();
            this.OutputRAF=new RandomAccessFile(this.OutputFile,"rw");
            this.OutputRAF.setLength(FileSize);
            this.Constant=100d/FileSize;
            this.OutputData.addElement(new byte[InputParameters.MaxLength]);
            this.setProgress(0);
//            System.out.println("0%= "+this.OutputRAF.getFilePointer());
        }
        else
            this.FR.CloseFile();
    }
    
    void WriteByte(byte Byte) throws IOException, InterruptedException{
        this.OutputData.lastElement()[this.Index]=Byte;
        this.Index++;
        if(this.Index==this.OutputData.lastElement().length){
            synchronized(this){
                this.OutputData.addElement(new byte[InputParameters.MaxLength]);
                this.notify();
                this.Index=0;
            }
        }
    }
    @Override
    protected Void doInBackground() throws IOException, InterruptedException{
        int x;
        while(!this.Cancel){
            if(this.OutputRAF.length()-this.OutputRAF.getFilePointer()<=InputParameters.MaxLength)
                break;
            synchronized(this){
                while(!this.EndWriting  && !this.Cancel && this.OutputData.size()==1)
                    this.wait();
                if(Math.random()<0.01d){
                    x=(int)(this.OutputRAF.getFilePointer()*this.Constant);
                    if(x>this.getProgress()){
                        this.setProgress(x);
    //                    System.out.println(x+"%= "+this.OutputRAF.getFilePointer());
                    }
                }
                this.FR.PauseReading();
                this.OutputRAF.write(this.OutputData.firstElement());
                this.OutputData.removeElementAt(0);
                if(this.OutputData.size()<=2)
                    this.FR.ResumeReading();
            }
        }
        if(!this.Cancel){
            while(!this.EndWriting)
                Thread.sleep(0,10);
            int i=0;
            while(this.OutputRAF.length()>this.OutputRAF.getFilePointer()){
                this.OutputRAF.write(this.OutputData.firstElement()[i]);
                i++;
            }
        }
        this.OutputRAF.close();
//        System.out.println("End");
        this.OutputData.removeAllElements();
        if(!this.Cancel){
            this.setProgress(100);
            if(this.OpenOutputFile)
                Desktop.getDesktop().open(this.OutputFile);
        }
        return null;
    }
    
    void Cancel(){
        this.Cancel=true;
        this.FR.Cancel();
    }
    
    short ReadUnsignedByte() throws IOException, InterruptedException{
        return this.FR.ReadUnsignedByte();
    }
    
    boolean HasMoreData(){
        return this.FR.HasMoreData();
    }
//    public void WaitAWhile() throws InterruptedException{
//        synchronized(this.OutputData){
//            while(!this.OutputData.isEmpty())
//                Thread.sleep(0,1);
//        }
//    }
    
    void Pause() throws InterruptedException {
        this.FR.Pause();
    }
    
    void EndWriting(){
        this.EndWriting=true;
    }
    
    boolean NoEnoughFreeSpace(){
        return this.OutputRAF==null;
    }
    
    void OpenOutputFile(){
        this.OpenOutputFile=true;
    }
}