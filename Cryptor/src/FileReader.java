
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
    public short ReadUnsignedByte() throws IOException, InterruptedException{
        short n=this.InputData.firstElement()[this.Index];
        if(n<0)
            n+=InputParameters._256;
        this.Index++;
        if(this.Index==this.InputData.firstElement().length){
            this.InputData.removeElementAt(0);
            this.Index=0;
        }
        return n;
    }
    @Override
    protected Void doInBackground() throws IOException, InterruptedException{
        while(!this.Cancel && this.InputRAF.length()>this.InputRAF.getFilePointer()){
            while(this.PauseReading){
                if(this.Cancel)
                    break;
                Thread.sleep(0,1);
            }
            byte[] tab;
            if(InputParameters.MaxLength<=this.InputRAF.length()-this.InputRAF.getFilePointer())
                tab=new byte[InputParameters.MaxLength];
            else
                tab=new byte[(int)(this.InputRAF.length()-this.InputRAF.getFilePointer())];
            this.InputRAF.read(tab);
            this.InputData.addElement(tab);
//            System.out.println(Arrays.toString(this.InputData.lastElement()));
            if(this.InputData.size()>=10)
                this.PauseReading=true;
        }
        this.InputRAF.close();
        this.StopReadingFile=true;
        this.PauseReading=false;
//        System.out.println("EndReading");
        return null;
    }
    public void Cancel(){
        this.Cancel=true;
    }
    public void PauseReading(){
        this.PauseReading=true;
    }
    public void PlayReading(){
        if(!this.Cancel)
            this.PauseReading=false;
    }
    public boolean HasMoreData(){
        return !this.StopReadingFile || !this.InputData.isEmpty();
    }
    public void Pause() throws InterruptedException{
        while(!this.StopReadingFile && this.InputData.isEmpty())
            Thread.sleep(0,1);
    }
    public void CloseFile() throws IOException{
        this.InputRAF.close();
    }
    public long getFileSize() throws IOException {
        return this.InputRAF.length();
    }
}