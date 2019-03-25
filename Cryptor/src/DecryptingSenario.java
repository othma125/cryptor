
import Tools.InputParameters;
import Tools.Order;
import Tools.StringNumber;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
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
public class DecryptingSenario extends SwingWorker implements PropertyChangeListener{
    public boolean Cancel=false;
    public boolean OpenDecryptedFile;
    private byte ZeroCounter=0;
    private byte Previous=InputParameters.n;
    private byte Index=0;
    private short OutputByte=0;
    private final short[] FirstStep;
    private final short[] SecondStep;
    private final File InputFile;
    private FileWriter FW=null;
    public DecryptingSenario(File file,char[] password,boolean OpenFile){
        this.InputFile=file;
        this.OpenDecryptedFile=OpenFile;
        StringNumber PW=StringNumber.getPassword(password);
        this.FirstStep=Order.Inverse(new Order(InputParameters._256,PW).getOrder());
        this.SecondStep=Order.Inverse(new Order(InputParameters._16,PW).getOrder());
    }
    @Override
    protected Void doInBackground() throws FileNotFoundException,IOException,InterruptedException{
        long begining=System.currentTimeMillis();
        RandomAccessFile InputRAF=new RandomAccessFile(this.InputFile,"r");
        File OutputFile=this.getOutputFile(InputRAF,this.InputFile.getParentFile().getAbsolutePath());
        if(OutputFile==null){
            this.setProgress(100);
            return null;
        }
        long pointer=InputRAF.getFilePointer();
        InputRAF.close();
        this.FW=new FileWriter(this.InputFile,OutputFile,true);
        if(this.NoEnoughFreeSpace()){
            this.setProgress(100);
            return null;
        }
        this.FW.addPropertyChangeListener(this);
        this.FW.execute();
        this.FW.Pause();
        while(pointer>0){
            pointer--;
            this.FW.ReadUnsignedByte();
        }
        if(this.Index==InputParameters._8){
            this.FW.WriteByte((byte)this.OutputByte);
            this.Index=0;
            this.OutputByte=0;
        }
        while(this.FW.HasMoreData()){
            if(this.Cancel){
                this.FW.Cancel();
                break;
            }
            this.FW.Pause();
            this.toDecryptedFile(this.FirstStep[this.FW.ReadUnsignedByte()]);
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
            this.FW.WriteByte((byte)this.OutputByte);
        this.OpenDecryptedFile();
        this.FW.EndWriting();
//        System.out.println("Decrypting time = "+(System.currentTimeMillis()-begining)+" ms");
        return null;
    }
    private File getOutputFile(RandomAccessFile InputRAF,String OutputPath) throws InterruptedException,IOException{
        byte a,b;
        boolean c=true;
        short OutputFileNameLength=Short.MAX_VALUE,x;
        Vector<Short> v=new Vector<>();
        String OutputFileName;
        File OutputFile=null;
        while(OutputFile==null && InputRAF.getFilePointer()<InputRAF.length()){
            x=this.FirstStep[InputRAF.readUnsignedByte()];
            for(byte y=0;y<InputParameters._8;y++){
                if(InputParameters.BinaryCoding[x][y]){
                    if(this.Previous<InputParameters.n){
                        a=InputParameters.Points[this.SecondStep[InputParameters.Matrix[this.Previous][this.ZeroCounter]]].X;
                        b=InputParameters.Points[this.SecondStep[InputParameters.Matrix[this.Previous][this.ZeroCounter]]].Y;
                        for(byte j=0;j<a;j++){
                            this.Index++;
                            if(this.Index==InputParameters._8){
                                if(c){
                                    OutputFileNameLength=this.OutputByte;
                                    c=false;
                                }
                                else if(OutputFile==null){
                                    if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                        if(v.size()==2*OutputFileNameLength){
                                            OutputFileName=this.getOutputFileName(v);
                                            try{
                                                OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                            }catch(InvalidPathException e){
                                                return null;
                                            }
                                            OutputFile.delete();
                                            v.removeAllElements();
                                            this.Index=0;
                                            this.OutputByte=0;
                                        }
                                        else
                                            v.addElement(this.OutputByte);
                                    }
                                    else{
                                        v.addElement(this.OutputByte);
                                        if(v.size()>2*OutputFileNameLength)
                                            return null;
                                    }
                                }
                                if(OutputFile==null){
                                    this.Index=0;
                                    this.OutputByte=0;
                                }
                            }
                        }
                        if(a<InputParameters.m){
                            this.OutputByte+=InputParameters.Power_2[this.Index];
                            this.Index++;
                            if(this.Index==InputParameters._8){
                                if(c){
                                    OutputFileNameLength=this.OutputByte;
                                    c=false;
                                }
                                else if(OutputFile==null){
                                    if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                        if(v.size()==2*OutputFileNameLength){
                                            OutputFileName=this.getOutputFileName(v);
                                            try{
                                                OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                            }catch(InvalidPathException e){
                                                return null;
                                            }
                                            OutputFile.delete();
                                            v.removeAllElements();
                                            this.Index=0;
                                            this.OutputByte=0;
                                        }
                                        else
                                            v.addElement(this.OutputByte);
                                    }
                                    else{
                                        v.addElement(this.OutputByte);
                                        if(v.size()>2*OutputFileNameLength)
                                            return null;
                                    }
                                }
                                if(OutputFile==null){
                                    this.Index=0;
                                    this.OutputByte=0;
                                }
                            }
                        }
                        for(byte j=0;j<b;j++){
                            this.Index++;
                            if(this.Index==InputParameters._8){
                                if(c){
                                    OutputFileNameLength=this.OutputByte;
                                    c=false;
                                }
                                else if(OutputFile==null){
                                    if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                        if(v.size()==2*OutputFileNameLength){
                                            OutputFileName=this.getOutputFileName(v);
                                            try{
                                                OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                            }catch(InvalidPathException e){
                                                return null;
                                            }
                                            OutputFile.delete();
                                            v.removeAllElements();
                                            this.Index=0;
                                            this.OutputByte=0;
                                        }
                                        else
                                            v.addElement(this.OutputByte);
                                    }
                                    else{
                                        v.addElement(this.OutputByte);
                                        if(v.size()>2*OutputFileNameLength)
                                            return null;
                                    }
                                }
                                if(OutputFile==null){
                                    this.Index=0;
                                    this.OutputByte=0;
                                }
                            }
                        }
                        if(b<InputParameters.m){
                            this.OutputByte+=InputParameters.Power_2[this.Index];
                            this.Index++;
                            if(this.Index==InputParameters._8){
                                if(c){
                                    OutputFileNameLength=this.OutputByte;
                                    c=false;
                                }
                                else if(OutputFile==null){
                                    if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                        if(v.size()==2*OutputFileNameLength){
                                            OutputFileName=this.getOutputFileName(v);
                                            try{
                                                OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                            }catch(InvalidPathException e){
                                                return null;
                                            }
                                            OutputFile.delete();
                                            v.removeAllElements();
                                            this.Index=0;
                                            this.OutputByte=0;
                                        }
                                        else
                                            v.addElement(this.OutputByte);
                                    }
                                    else{
                                        v.addElement(this.OutputByte);
                                        if(v.size()>2*OutputFileNameLength)
                                            return null;
                                    }
                                }
                                if(OutputFile==null){
                                    this.Index=0;
                                    this.OutputByte=0;
                                }
                            }
                        }
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
                            a=InputParameters.Points[this.SecondStep[InputParameters.Matrix[this.Previous][this.ZeroCounter]]].X;
                            b=InputParameters.Points[this.SecondStep[InputParameters.Matrix[this.Previous][this.ZeroCounter]]].Y;
                            for(byte j=0;j<a;j++){
                                this.Index++;
                                if(this.Index==InputParameters._8){
                                    if(c){
                                        OutputFileNameLength=this.OutputByte;
                                        c=false;
                                    }
                                    else if(OutputFile==null){
                                        if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                            if(v.size()==2*OutputFileNameLength){
                                                OutputFileName=this.getOutputFileName(v);
                                                try{
                                                    OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                                }catch(InvalidPathException e){
                                                    return null;
                                                }
                                                OutputFile.delete();
                                                v.removeAllElements();
                                                this.Index=0;
                                                this.OutputByte=0;
                                            }
                                            else
                                                v.addElement(this.OutputByte);
                                        }
                                        else{
                                            v.addElement(this.OutputByte);
                                            if(v.size()>2*OutputFileNameLength)
                                                return null;
                                        }
                                    }
                                    if(OutputFile==null){
                                        this.Index=0;
                                        this.OutputByte=0;
                                    }
                                }
                            }
                            if(a<InputParameters.m){
                                this.OutputByte+=InputParameters.Power_2[this.Index];
                                this.Index++;
                                if(this.Index==InputParameters._8){
                                    if(c){
                                        OutputFileNameLength=this.OutputByte;
                                        c=false;
                                    }
                                    else if(OutputFile==null){
                                        if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                            if(v.size()==2*OutputFileNameLength){
                                                OutputFileName=this.getOutputFileName(v);
                                                try{
                                                    OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                                }catch(InvalidPathException e){
                                                    return null;
                                                }
                                                OutputFile.delete();
                                                v.removeAllElements();
                                                this.Index=0;
                                                this.OutputByte=0;
                                            }
                                            else
                                                v.addElement(this.OutputByte);
                                        }
                                        else{
                                            v.addElement(this.OutputByte);
                                            if(v.size()>2*OutputFileNameLength)
                                                return null;
                                        }
                                    }
                                    if(OutputFile==null){
                                        this.Index=0;
                                        this.OutputByte=0;
                                    }
                                }
                            }
                            for(byte j=0;j<b;j++){
                                this.Index++;
                                if(this.Index==InputParameters._8){
                                    if(c){
                                        OutputFileNameLength=this.OutputByte;
                                        c=false;
                                    }
                                    else if(OutputFile==null){
                                        if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                            if(v.size()==2*OutputFileNameLength){
                                                OutputFileName=this.getOutputFileName(v);
                                                try{
                                                    OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                                }catch(InvalidPathException e){
                                                    return null;
                                                }
                                                OutputFile.delete();
                                                v.removeAllElements();
                                                this.Index=0;
                                                this.OutputByte=0;
                                            }
                                            else
                                                v.addElement(this.OutputByte);
                                        }
                                        else{
                                            v.addElement(this.OutputByte);
                                            if(v.size()>2*OutputFileNameLength)
                                                return null;
                                        }
                                    }
                                    if(OutputFile==null){
                                        this.Index=0;
                                        this.OutputByte=0;
                                    }
                                }
                            }
                            if(b<InputParameters.m){
                                this.OutputByte+=InputParameters.Power_2[this.Index];
                                this.Index++;
                                if(this.Index==InputParameters._8){
                                    if(c){
                                        OutputFileNameLength=this.OutputByte;
                                        c=false;
                                    }
                                    else if(OutputFile==null){
                                        if(this.OutputByte==InputParameters.EndFileNameCharacter){
                                            if(v.size()==2*OutputFileNameLength){
                                                OutputFileName=this.getOutputFileName(v);
                                                try{
                                                    OutputFile=Paths.get(OutputPath+"\\"+OutputFileName).toFile();
                                                }catch(InvalidPathException e){
                                                    return null;
                                                }
                                                OutputFile.delete();
                                                v.removeAllElements();
                                                this.Index=0;
                                                this.OutputByte=0;
                                            }
                                            else
                                                v.addElement(this.OutputByte);
                                        }
                                        else{
                                            v.addElement(this.OutputByte);
                                            if(v.size()>2*OutputFileNameLength)
                                                return null;
                                        }
                                    }
                                    if(OutputFile==null){
                                        this.Index=0;
                                        this.OutputByte=0;
                                    }
                                }
                            }
                            this.Previous=InputParameters.n;
                        }
                        else
                            this.Previous=this.ZeroCounter;
                        this.ZeroCounter=0;
                    }
                }
            }
        }
        return OutputFile;
    }
    private void toDecryptedFile(short k) throws IOException,InterruptedException{
        for(byte i=0;i<InputParameters._8;i++){
            if(InputParameters.BinaryCoding[k][i]){
                if(this.Previous<InputParameters.n){
                    this.toDecryptedFile(InputParameters.Matrix[this.Previous][this.ZeroCounter]);
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
                        this.toDecryptedFile(InputParameters.Matrix[this.Previous][this.ZeroCounter]);
                        this.Previous=InputParameters.n;
                    }
                    else
                        this.Previous=this.ZeroCounter;
                    this.ZeroCounter=0;
                }
            }
        }
    }
    private void toDecryptedFile(byte k) throws IOException,InterruptedException{
        byte a=InputParameters.Points[this.SecondStep[k]].X;
        byte b=InputParameters.Points[this.SecondStep[k]].Y;
        for(byte j=0;j<a;j++){
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.OutputByte);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        if(a<InputParameters.m){
            this.OutputByte+=InputParameters.Power_2[this.Index];
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.OutputByte);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        for(byte j=0;j<b;j++){
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.OutputByte);
                this.Index=0;
                this.OutputByte=0;
            }
        }
        if(b<InputParameters.m){
            this.OutputByte+=InputParameters.Power_2[this.Index];
            this.Index++;
            if(this.Index==InputParameters._8){
                this.FW.WriteByte((byte)this.OutputByte);
                this.Index=0;
                this.OutputByte=0;
            }
        }
    }
    public boolean WrongPassword(){
        return this.FW==null;
    }
    public void Cancel(){
        this.Cancel=true;
    }
    public void OpenDecryptedFile(){
        if(this.OpenDecryptedFile)
            this.FW.OpenOutputFile();
    }
    public boolean isCanceled(){
        return this.Cancel;
    }
    public boolean NoEnoughFreeSpace(){
        return this.FW.NoEnoughFreeSpace();
    }
    @Override
    public void propertyChange(PropertyChangeEvent e){
        if("progress".matches(e.getPropertyName()))
            this.setProgress((int)e.getNewValue());
    }    
    private String getOutputFileName(Vector<Short> v){
        String name="";
        short character=0;
        for(int i=0;i<v.size();i++){
            if(i%2==0)
                character=v.elementAt(i);
            else{
                character+=InputParameters._256*v.elementAt(i);
                name+=(char)character;
            }
        }
//        System.out.println(name);
        return name;
    }
}