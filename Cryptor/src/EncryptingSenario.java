
import Tools.Order;
import Tools.InputParameters;
import Tools.StringNumber;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
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
public class EncryptingSenario extends SwingWorker implements PropertyChangeListener,Runnable{
    public boolean Cancel=false;
    private byte ZeroCounter=0;
    private byte Previous=InputParameters.n;
    private byte Index=0;
    private short OutputByte=0;
    private final short[] FirstStep;
    private final short[] SecondStep;
    private final File InputFile;
    private FileWriter FW=null;
    
    public EncryptingSenario(File file,char[] password){
        this.InputFile=file;
        StringNumber PW=StringNumber.getPassword(password);
        this.FirstStep=new Order(InputParameters._16,PW).getOrder();
        this.SecondStep=new Order(InputParameters._256,PW).getOrder();
    }
    
    @Override
    protected Void doInBackground() throws FileNotFoundException,IOException,InterruptedException{
//        long begining=System.currentTimeMillis();
        String InputFileName=this.InputFile.getName();
        File OutputFile=new File(this.InputFile.getParent()+"\\"+this.getEncryptingFileName(InputFileName)+"cr");
        OutputFile.delete();
        this.FW=new FileWriter(this.InputFile,OutputFile,false);
        if(this.NoEnoughFreeSpace()){
            this.setProgress(100);
            return null;
        }
        this.FW.addPropertyChangeListener(this);
        this.FW.execute();
        this.toCryptedFile((short)InputFileName.length());
        IntStream.range(0,InputFileName.length())
                .map(InputFileName::charAt)
                .forEach(c->{
                    try {
                        this.toCryptedFile((short)((short)c%InputParameters._256));
                        this.toCryptedFile((short)((short)c/InputParameters._256));
                    } catch (IOException ex) {
                        Logger.getLogger(EncryptingSenario.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(EncryptingSenario.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        this.toCryptedFile(InputParameters.EndFileNameCharacter);
        while(this.FW.HasMoreData()){
            if(this.Cancel){
                this.FW.Cancel();
                break;
            }
            this.FW.Pause();
            this.toCryptedFile(this.FW.ReadUnsignedByte());
        }
        if(this.Cancel){
            Thread.sleep(10);
            OutputFile.delete();
            this.setProgress(100);
            return null;
        }
        if(this.Previous<InputParameters.m){
            for(byte j=0;j<this.Previous;j++)
                this.Index++;
            this.OutputByte+=InputParameters.Power_2[this.Index];
        }
        if(this.Index>0)
            this.FW.WriteByte((byte)this.SecondStep[this.OutputByte]);
        this.FW.EndWriting();
//        System.out.println("Encrypting time = "+(System.currentTimeMillis()-begining)+" ms");
        return null;
    }
    
    private void toCryptedFile(short UnsignedByte) throws IOException,InterruptedException{
        for(byte i=0;i<InputParameters._8;i++){
            if(InputParameters.BinaryCoding[UnsignedByte][i]){
                if(this.Previous<InputParameters.n){
                    this.toCryptedFile(InputParameters.Matrix[this.Previous][this.ZeroCounter]);
                    this.Previous=InputParameters.n;
                }
                else
                    this.Previous=this.ZeroCounter;
                this.ZeroCounter=0;
            }
            else{
                this.ZeroCounter++;
                if(this.ZeroCounter==InputParameters.m){
                    if(this.Previous<InputParameters.n){
                        this.toCryptedFile(InputParameters.Matrix[this.Previous][this.ZeroCounter]);
                        this.Previous=InputParameters.n;
                    }
                    else
                        this.Previous=this.ZeroCounter;
                    this.ZeroCounter=0;
                }
            }
        }
    }
    
    private void toCryptedFile(byte Byte) throws IOException,InterruptedException{
        byte a=InputParameters.Points[this.FirstStep[Byte]].X;
        byte b=InputParameters.Points[this.FirstStep[Byte]].Y;
        for(byte j=0;j<a;j++){
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.SecondStep[this.OutputByte]);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        if(a<InputParameters.m){
            this.OutputByte+=InputParameters.Power_2[this.Index];
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.SecondStep[this.OutputByte]);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        for(byte j=0;j<b;j++){
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.SecondStep[this.OutputByte]);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        if(b<InputParameters.m){
            this.OutputByte+=InputParameters.Power_2[this.Index];
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.SecondStep[this.OutputByte]);
                this.Index=0;
                this.OutputByte=0;
            }
        }
    }
    
    void Cancel(){
        this.Cancel=true;
    }
    
    boolean isCanceled(){
        return this.Cancel;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent e){
        if("progress".matches(e.getPropertyName()))
            this.setProgress((int)e.getNewValue());
    }    
    
    boolean NoEnoughFreeSpace(){
        return this.FW.NoEnoughFreeSpace();
    }
    
    private String getEncryptingFileName(String InputFileName){
        StringTokenizer st=new StringTokenizer(InputFileName,".");
        if(st.countTokens()==1)
            return st.nextToken()+".";
        String name="",extension="",NToken;
        while(st.hasMoreTokens())
            extension=st.nextToken();
        st=new StringTokenizer(InputFileName,".");
        while(st.hasMoreTokens()){
            NToken=st.nextToken();
            if(extension.equals(NToken))
                break;
            name+=NToken+".";
        }
        return name;
    }
}