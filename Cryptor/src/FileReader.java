
import Tools.InputParameters;
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
public class FileReader extends SwingWorker{
    private short Index=0;
    private boolean Cancel=false;
    private boolean StopReadingFile=false;
    private boolean PauseReading=false;
    private final Vector<byte[]> InputData;
    private final RandomAccessFile InputRAF;
    
    public FileReader(RandomAccessFile RAF) throws IOException, InterruptedException{
        this.InputData=new Vector<>();
        this.InputRAF=RAF;
    }
    
    short ReadUnsignedByte() throws IOException, InterruptedException{
        short Byte=this.InputData.firstElement()[this.Index];
        if(Byte<0)
            Byte+=InputParameters._256;
        this.Index++;
        if(this.Index==this.InputData.firstElement().length){
            this.InputData.removeElementAt(0);
            this.Index=0;
        }
        return Byte;
    }
    
    @Override
    protected Void doInBackground() throws IOException, InterruptedException{
        while(!this.Cancel && this.InputRAF.length()>this.InputRAF.getFilePointer()){
            synchronized(this){
                while(this.PauseReading && !this.Cancel)
                    this.wait();
                byte[] tab;
                if(InputParameters.MaxLength<=this.InputRAF.length()-this.InputRAF.getFilePointer())
                    tab=new byte[InputParameters.MaxLength];
                else
                    tab=new byte[(int)(this.InputRAF.length()-this.InputRAF.getFilePointer())];
                this.InputRAF.read(tab);
                this.InputData.addElement(tab);
                this.notifyAll();
    //            System.out.println(Arrays.toString(this.InputData.lastElement()));
                if(this.InputData.size()>=10)
                    this.PauseReading();
            }
        }
        this.InputRAF.close();
        this.StopReadingFile=true;
        synchronized(this){
            this.PauseReading=false;
            this.notify();
        }
//        System.out.println("EndReading");
        return null;
    }
    
    void Cancel(){
        this.Cancel=true;
    }
    
    void PauseReading(){
        this.PauseReading=true;
    }
    
    void ResumeReading(){
        synchronized(this){
            this.PauseReading=false;
            this.notifyAll();
        }
    }
    
    boolean HasMoreData(){
        return !this.StopReadingFile || !this.InputData.isEmpty();
    }
    
    void Pause() throws InterruptedException{
        synchronized(this){
            while(!this.StopReadingFile && this.InputData.isEmpty())
                this.wait();
        }
    }
    
    void CloseFile() throws IOException{
        this.InputRAF.close();
    }
    
    long getFileSize() throws IOException {
        return this.InputRAF.length();
    }
}